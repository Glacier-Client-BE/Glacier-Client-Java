package net.glacier.web;

import java.util.List;

/** Absolutely positioned children, placed against the padding box of their containing element. */
final class AbsoluteLayout {
	private AbsoluteLayout() {}

	/** Lays out the absolutely positioned children of n, whose border-box is W x H at n.x, n.y. */
	static void layout(LayoutContext ctx, Node n, float W, float H) {
		float px = n.x + n.borderWidth[3], py = n.y + n.borderWidth[0];
		float pw = W - n.borderWidth[3] - n.borderWidth[1];
		float ph = H - n.borderWidth[0] - n.borderWidth[2];
		List<Node> kids = n.children;
		for (int i = 0, k = kids.size(); i < k; i++) {
			Node c = kids.get(i);
			if (c.isText() || !LayoutContext.isAbsolute(c)) continue;
			if (LayoutContext.isNone(c)) {
				// not laid out and not hidden: its boxes keep their last values
				Layout.forget(ctx, c);
				continue;
			}
			float t = c.inset[0], r = c.inset[1], b = c.inset[2], l = c.inset[3];
			float mT = LayoutContext.nz(c.margin[0]), mR = LayoutContext.nz(c.margin[1]);
			float mB = LayoutContext.nz(c.margin[2]), mL = LayoutContext.nz(c.margin[3]);
			float fW = (!Float.isNaN(l) && !Float.isNaN(r)) ? pw - l - r - mL - mR : -1;
			float fH = (!Float.isNaN(t) && !Float.isNaN(b)) ? ph - t - b - mT - mB : -1;
			Layout.box(ctx, c, px, py, pw, ph, fW, fH);
			float ax = !Float.isNaN(l) ? px + l + mL
					: !Float.isNaN(r) ? px + pw - r - mR - c.w
					: px + mL;
			float ay = !Float.isNaN(t) ? py + t + mT
					: !Float.isNaN(b) ? py + ph - b - mB - c.h
					: py + mT;
			Layout.shift(ctx, c, ax - c.x, ay - c.y);
		}
	}
}
