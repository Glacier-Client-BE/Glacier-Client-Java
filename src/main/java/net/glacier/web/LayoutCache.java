package net.glacier.web;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Persistent memo for {@link Layout}: per-node box results for incremental relayout, plus intrinsic and
 * min-content widths and wrapped text lines. Keys are node identity (no Node fields are added).
 *
 * <p>Contract for an incremental pass ({@code Layout.layout(root, w, h, m, cache)}):
 * <ul>
 *   <li>every change to a node's computed style, children or text must be reported with
 *       {@link Node#markDirty(int)} using {@link Node#DIRTY_LAYOUT} (or {@code DIRTY_STYLE}, which implies a relayout
 *       of that node). The cascade does not set these flags by itself;</li>
 *   <li>the same cache must be used for every layout of that tree. A layout of any tree that does not go
 *       through this cache (or through a different cache) invalidates the cache, so mixing is safe but
 *       slower.</li>
 * </ul>
 * A cache is not thread-safe; layout runs on the render thread.
 */
public final class LayoutCache {
	/** Memo state for one node. Fields are owned by the layout files. */
	static final class Entry {
		/* box memo: valid only while the node and its subtree are clean */
		boolean valid;
		float aw, ah, fw, fh;      // inputs of the last box() call
		float dx, dy;              // translate applied at box exit (relative to the box origin)
		float w, h, contentH;      // results

		/* intrinsic / min-content memo */
		int intrPass = -1;
		boolean intrOk;
		float intr;
		int minPass = -1;
		boolean minOk;
		float minC;

		/* wrapped text memo: c.lines holds the result of this key while wrapOk */
		boolean wrapOk, wrapBold;
		String wrapSrc;
		float wrapW, wrapSize, wrapLineW, wrapLastW;
	}

	private static final int MIN_BITS = 6;

	private Node[] keys;
	private Entry[] vals;
	private int bits;
	private int count;
	private int compactAt = 1024;
	/** Layout epoch of the last pass that used this cache (see {@link Layout}). */
	long epoch = -1;
	/** Measurer the entries were computed with. */
	Layout.TextMeasurer owner;

	/** Creates an empty cache. */
	public LayoutCache() {
		alloc(MIN_BITS);
	}

	/** Drops every memo entry. */
	public void clear() {
		Arrays.fill(keys, null);
		Arrays.fill(vals, null);
		count = 0;
	}

	private void alloc(int b) {
		bits = b;
		keys = new Node[1 << b];
		vals = new Entry[1 << b];
		count = 0;
	}

	private static int slot(Node n, int bits) {
		return (System.identityHashCode(n) * 0x9E3779B1) >>> (32 - bits);
	}

	/** The entry of n, or null when there is none. */
	Entry get(Node n) {
		return find(keys, vals, bits, n);
	}

	private static Entry find(Node[] ks, Entry[] vs, int bits, Node n) {
		int mask = ks.length - 1;
		for (int i = slot(n, bits); ; i = (i + 1) & mask) {
			Node k = ks[i];
			if (k == n) return vs[i];
			if (k == null) return null;
		}
	}

	/** The entry of n, created when missing. */
	Entry entry(Node n) {
		int mask = keys.length - 1;
		for (int i = slot(n, bits); ; i = (i + 1) & mask) {
			Node k = keys[i];
			if (k == n) return vals[i];
			if (k == null) {
				if ((count + 1) * 2 > keys.length) {
					grow();
					return entry(n);
				}
				Entry e = new Entry();
				keys[i] = n;
				vals[i] = e;
				count++;
				return e;
			}
		}
	}

	private void grow() {
		Node[] ok = keys;
		Entry[] ov = vals;
		alloc(bits + 1);
		for (int i = 0; i < ok.length; i++) if (ok[i] != null) insertNew(ok[i], ov[i]);
	}

	private void insertNew(Node n, Entry e) {
		int mask = keys.length - 1;
		int i = slot(n, bits);
		while (keys[i] != null) i = (i + 1) & mask;
		keys[i] = n;
		vals[i] = e;
		count++;
	}

	/** Drops entries of nodes no longer in the tree under root, once the table has grown well past the live set. */
	void compactIfNeeded(Node root) {
		if (count <= compactAt) return;
		Node[] ok = keys;
		Entry[] ov = vals;
		int ob = bits;
		ArrayList<Node> live = new ArrayList<>();
		collect(root, ok, ov, ob, live);
		int b = MIN_BITS;
		while ((1 << b) < live.size() * 4) b++;
		alloc(b);
		for (Node n : live) insertNew(n, find(ok, ov, ob, n));
		compactAt = Math.max(1024, live.size() * 2);
	}

	private static void collect(Node n, Node[] ks, Entry[] vs, int b, ArrayList<Node> out) {
		if (find(ks, vs, b, n) != null) out.add(n);
		for (int i = 0, k = n.children.size(); i < k; i++) collect(n.children.get(i), ks, vs, b, out);
	}
}
