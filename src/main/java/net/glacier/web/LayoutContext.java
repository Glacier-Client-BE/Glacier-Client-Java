package net.glacier.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.ToIntFunction;

/**
 * Per-pass state for the layout files: the text measurer, the memo cache, the pass id and stack-disciplined
 * scratch pools, so the hot paths do not allocate. Borrow with
 * {@code int m = ctx.listMark(); ArrayList<Node> l = ctx.borrowList(); ... ctx.releaseLists(m);} and the same
 * pair for float arrays. A borrowed array is only valid until its matching release.
 *
 * <p>Keyword values are read through int kinds derived from the {@link Node} keyword tables, so no String
 * comparison happens in the layout hot paths.
 */
final class LayoutContext {
	/** Alignment kinds (align-items / align-self, reduced to what layout tests for). */
	static final int AL_OTHER = 0, AL_STRETCH = 1, AL_CENTER = 2, AL_END = 3;
	/** text-align kinds. */
	static final int TA_LEFT = 0, TA_RIGHT = 1, TA_CENTER = 2;
	/** justify-content kinds. */
	static final int JC_START = 0, JC_END = 1, JC_CENTER = 2, JC_SPACE_BETWEEN = 3, JC_SPACE_AROUND = 4,
			JC_SPACE_EVENLY = 5;

	private static final int[] ITEMS_ALIGN = kinds(Node.ALIGN_ITEMS_VALUES, LayoutContext::alignKindOf);
	private static final int[] SELF_ALIGN = kinds(Node.ALIGN_SELF_VALUES, LayoutContext::alignKindOf);
	private static final int[] JUSTIFY = kinds(Node.JUSTIFY_VALUES, LayoutContext::justifyKindOf);
	private static final int[] TEXT_ALIGN = kinds(Node.TEXT_ALIGN_VALUES, LayoutContext::textAlignKindOf);

	/** Measurer for this pass. */
	Layout.TextMeasurer m;
	/** Memo cache for this pass (the shared scratch cache when the pass is not incremental). */
	LayoutCache cache;
	/** True when box results may be reused from the cache (incremental pass). */
	boolean memo;
	/** Unique id of this pass; per-pass memos compare against it. */
	int pass;

	/** Result of the last {@link #justify} call. */
	float jStart, jBetween;

	private final ArrayList<ArrayList<Node>> lists = new ArrayList<>();
	private int listTop;
	private float[][] floats = new float[32][];
	private int floatTop;

	void begin(Layout.TextMeasurer m, LayoutCache cache, boolean memo, int pass) {
		this.m = m;
		this.cache = cache;
		this.memo = memo;
		this.pass = pass;
		listTop = 0;
		floatTop = 0;
	}

	void end() {
		m = null;
		cache = null;
		for (ArrayList<Node> l : lists) l.clear();
		listTop = 0;
		floatTop = 0;
	}

	int listMark() {
		return listTop;
	}

	/** Borrows an empty node list; release it with {@link #releaseLists}. */
	ArrayList<Node> borrowList() {
		if (listTop == lists.size()) lists.add(new ArrayList<>());
		ArrayList<Node> l = lists.get(listTop++);
		l.clear();
		return l;
	}

	/** Releases every list borrowed since {@code mark}. */
	void releaseLists(int mark) {
		for (int i = mark; i < listTop; i++) lists.get(i).clear();
		listTop = mark;
	}

	int floatMark() {
		return floatTop;
	}

	/** Borrows a zero-filled float array of at least n elements; only indices below n may be used. */
	float[] borrowFloats(int n) {
		if (floatTop == floats.length) floats = Arrays.copyOf(floats, floats.length * 2);
		float[] a = floats[floatTop];
		if (a == null || a.length < n) {
			a = new float[Math.max(n, 8)];
			floats[floatTop] = a;
		} else {
			Arrays.fill(a, 0, n, 0f);
		}
		floatTop++;
		return a;
	}

	/** Releases every float array borrowed since {@code mark}. */
	void releaseFloats(int mark) {
		floatTop = mark;
	}

	/** Sets {@link #jStart} and {@link #jBetween} for justify-content kind id, rem free space and k items. */
	void justify(int id, float rem, int k) {
		int kind = id < JUSTIFY.length ? JUSTIFY[id] : JC_START;
		float start = 0, between = 0;
		switch (kind) {
			case JC_CENTER -> start = rem / 2f;
			case JC_END -> start = rem;
			case JC_SPACE_BETWEEN -> {
				if (k > 1) between = rem / (k - 1);
			}
			case JC_SPACE_AROUND -> {
				if (k > 0) {
					start = rem / (2f * k);
					between = rem / k;
				}
			}
			case JC_SPACE_EVENLY -> {
				if (k > 0) {
					start = rem / (k + 1);
					between = rem / (k + 1);
				}
			}
			default -> { }
		}
		jStart = start;
		jBetween = between;
	}

	float lineHeight(Node p) {
		return p.lineHeight > 0 ? p.lineHeight : m.lineHeight(p.fontSize);
	}

	/* ---------------------------------------------------------- static helpers */

	/** Alignment kind of c inside container: its own align-self, or the container's align-items for auto. */
	static int alignKind(Node c, Node container) {
		int sid = c.alignSelfId;
		if (sid == 0) return ITEMS_ALIGN[container.alignItemsId];
		return sid < SELF_ALIGN.length ? SELF_ALIGN[sid] : AL_OTHER;
	}

	static int textAlignKind(Node p) {
		return p.textAlignId < TEXT_ALIGN.length ? TEXT_ALIGN[p.textAlignId] : TA_LEFT;
	}

	static boolean isFlex(Node n) {
		return (n.displayBits & Node.DISPLAY_FLEX) != 0;
	}

	static boolean isGrid(Node n) {
		return (n.displayBits & Node.DISPLAY_GRID) != 0;
	}

	/** True for inline-level element boxes (text nodes are not elements). */
	static boolean isInline(Node c) {
		return !c.isText() && (c.displayBits & Node.DISPLAY_INLINE) != 0;
	}

	static boolean isNone(Node c) {
		return (c.displayBits & Node.DISPLAY_NONE) != 0;
	}

	static boolean isAbsolute(Node c) {
		return c.positionId == Node.POSITION_ABSOLUTE;
	}

	static float nz(float f) {
		return Float.isNaN(f) ? 0 : f;
	}

	/** Maps each keyword to its kind; the extra last slot (any other value) is kind 0. */
	private static int[] kinds(String[] names, ToIntFunction<String> kind) {
		int[] out = new int[names.length + 1];
		for (int i = 0; i < names.length; i++) out[i] = kind.applyAsInt(names[i]);
		return out;
	}

	private static int alignKindOf(String v) {
		return switch (v) {
			case "stretch" -> AL_STRETCH;
			case "center" -> AL_CENTER;
			case "flex-end", "end" -> AL_END;
			default -> AL_OTHER;
		};
	}

	private static int justifyKindOf(String v) {
		return switch (v) {
			case "center" -> JC_CENTER;
			case "flex-end", "end" -> JC_END;
			case "space-between" -> JC_SPACE_BETWEEN;
			case "space-around" -> JC_SPACE_AROUND;
			case "space-evenly" -> JC_SPACE_EVENLY;
			default -> JC_START;
		};
	}

	private static int textAlignKindOf(String v) {
		return switch (v) {
			case "center" -> TA_CENTER;
			case "right", "end" -> TA_RIGHT;
			default -> TA_LEFT;
		};
	}
}
