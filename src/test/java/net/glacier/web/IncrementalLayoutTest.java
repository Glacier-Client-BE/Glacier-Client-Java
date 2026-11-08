package net.glacier.web;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Incremental relayout (LayoutCache) must produce the same boxes as a full pass of the same state, for every
 * node that can be painted or hit-tested. Subtrees with display:none keep stale geometry in a full pass and are
 * skipped by the comparison, as WebView skips them.
 */
public class IncrementalLayoutTest {
	private static final float TOL = 1e-3f;

	private static void collect(Node n, boolean unseen, StringBuilder sb) {
		unseen = unseen || "none".equals(n.display) || !n.visible;
		if (!unseen) {
			sb.append(n.isText() ? "#text " : n.tag + " ")
					.append(n.x).append(' ').append(n.y).append(' ').append(n.w).append(' ').append(n.h).append('\n');
		}
		for (Node c : n.children) collect(c, unseen, sb);
	}

	/** Fraction-free box dump; coordinates compared with a tolerance by the caller. */
	private static float[] boxes(Node root) {
		StringBuilder sb = new StringBuilder();
		collect(root, false, sb);
		String[] lines = sb.toString().split("\n");
		float[] out = new float[lines.length * 4];
		for (int i = 0; i < lines.length; i++) {
			String[] p = lines[i].split(" ");
			for (int k = 0; k < 4; k++) out[i * 4 + k] = Float.parseFloat(p[p.length - 4 + k]);
		}
		return out;
	}

	private static void assertSameBoxes(Node inc, Node full, String what) {
		float[] a = boxes(inc), b = boxes(full);
		assertEquals(b.length, a.length, what + ": box count");
		for (int i = 0; i < a.length; i++) {
			assertEquals(b[i], a[i], TOL, what + ": value " + i);
		}
	}

	private static Node firstWith(Node n, String cls) {
		if (!n.isText() && n.classes.contains(cls)) return n;
		for (Node c : n.children) {
			Node r = firstWith(c, cls);
			if (r != null) return r;
		}
		return null;
	}

	private static Node firstText(Node n) {
		if (n.isText()) return n;
		for (Node c : n.children) {
			Node r = firstText(c);
			if (r != null) return r;
		}
		return null;
	}

	@Test
	public void restyleAndTextChangesMatchFullLayout() {
		FakeTextMeasurer m = FakeTextMeasurer.INSTANCE;
		for (String scenario : new String[] {"menu", "large"}) {
			for (int[] vp : new int[][] {{960, 540}, {1920, 1080}}) {
				int w = vp[0], h = vp[1];
				String html = GoldenDump.html(scenario);
				Node inc = HtmlParser.parse(html);
				List<Css.Stylesheet> sheets = GoldenDump.sheets(scenario, inc);
				Css.apply(inc, sheets);
				LayoutCache cache = new LayoutCache();
				Layout.layout(inc, w, h, m, cache);

				Node card = firstWith(inc, "mcard");
				if (card == null) card = firstWith(inc, "button");
				Node text = firstText(card == null ? inc : card);

				for (int step = 0; step < 4; step++) {
					if (card != null) {
						card.setAttr("style", step % 2 == 0 ? "padding:4px" : "padding:9px 2px");
						Css.applySubtree(card, sheets);
						Layout.markLayoutDirty(card);
					}
					if (text != null) {
						text.text = step % 2 == 0 ? "Changed text" : "Module";
						Layout.markLayoutDirty(text);
					}
					Layout.layout(inc, w, h, m, cache);
					float[] incBoxes = boxes(inc);

					// Same state, laid out in full by a cache-free pass; the mutated tree is compared with itself
					// only after its boxes have been copied out above.
					Layout.layout(inc, w, h, m);
					float[] fullBoxes = boxes(inc);
					assertEquals(fullBoxes.length, incBoxes.length, scenario + " " + w + "x" + h + " step " + step + ": box count");
					for (int i = 0; i < fullBoxes.length; i++) {
						assertEquals(fullBoxes[i], incBoxes[i], TOL, scenario + " " + w + "x" + h + " step " + step + ": value " + i);
					}
				}
			}
		}
	}
}
