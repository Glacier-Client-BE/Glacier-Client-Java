package net.glacier.web;

/**
 * Content-based sizes: max-content (intrinsic) and min-content widths, flex basis and min sizes.
 * Results depend only on styles and subtree content, so they are memoised per pass and, in incremental
 * passes, across passes while the node and its subtree stay clean.
 */
final class LayoutSizes {
	private LayoutSizes() {}

	/** Max-content width of c (border-box). */
	static float intrinsic(LayoutContext ctx, Node c) {
		if (c.isText()) return InlineLayout.textW(ctx, c);
		if (LayoutContext.isNone(c)) return 0;
		LayoutCache.Entry e = ctx.cache.entry(c);
		boolean clean = !c.needsLayout && !c.subtreeDirty;
		if (e.intrPass == ctx.pass || (ctx.memo && e.intrOk && clean)) return e.intr;
		float v = intrinsicOf(ctx, c);
		e.intrPass = ctx.pass;
		e.intr = v;
		e.intrOk = clean;
		return v;
	}

	private static float intrinsicOf(LayoutContext ctx, Node c) {
		float pads = c.borderWidth[1] + c.borderWidth[3] + c.padding[1] + c.padding[3];
		float extra = c.borderBox ? 0 : pads;
		if (c.width >= 0 && !c.percentWidth) return Math.max(c.width + extra, c.minWidth + extra);
		boolean row = (c.displayBits & Node.DISPLAY_INLINE) != 0
				|| (LayoutContext.isFlex(c) && c.flexDirId == Node.FLEX_DIR_ROW);
		float sum = 0, max = 0;
		int count = 0;
		for (int i = 0, n = c.children.size(); i < n; i++) {
			Node k = c.children.get(i);
			if (LayoutContext.isAbsolute(k) || LayoutContext.isNone(k)) continue;
			float w = intrinsic(ctx, k) + LayoutContext.nz(k.margin[1]) + LayoutContext.nz(k.margin[3]);
			sum += w;
			max = Math.max(max, w);
			count++;
		}
		float gaps = row && count > 1 ? c.columnGap * (count - 1) : 0;
		float w = (row ? sum + gaps : max) + pads;
		if (c.maxWidth >= 0 && !c.percentMaxWidth) w = Math.min(w, c.maxWidth + extra);
		return Math.max(w, c.minWidth + extra);
	}

	/** Approximate min-content width: longest word for text, sum for rows, max for everything else. */
	static float minContent(LayoutContext ctx, Node c) {
		if (c.isText()) return minContentText(ctx, c);
		if (LayoutContext.isNone(c) || LayoutContext.isAbsolute(c)) return 0;
		LayoutCache.Entry e = ctx.cache.entry(c);
		boolean clean = !c.needsLayout && !c.subtreeDirty;
		if (e.minPass == ctx.pass || (ctx.memo && e.minOk && clean)) return e.minC;
		float v = minContentOf(ctx, c);
		e.minPass = ctx.pass;
		e.minC = v;
		e.minOk = clean;
		return v;
	}

	private static float minContentText(LayoutContext ctx, Node c) {
		Node p = c.parent;
		if (p.nowrap) return InlineLayout.textW(ctx, c);
		float best = 0;
		String t = InlineLayout.norm(c);
		int s = 0;
		for (int i = 0; i <= t.length(); i++) {
			if (i == t.length() || t.charAt(i) == ' ') {
				if (i > s) best = Math.max(best, ctx.m.width(t.substring(s, i), p.bold, p.fontSize));
				s = i + 1;
			}
		}
		return best;
	}

	private static float minContentOf(LayoutContext ctx, Node c) {
		float pads = c.borderWidth[1] + c.borderWidth[3] + c.padding[1] + c.padding[3];
		if (c.width >= 0 && !c.percentWidth) return c.width + (c.borderBox ? 0 : pads);
		if (c.minWidthSet || c.overflowId != Node.OVERFLOW_VISIBLE) return c.minWidth + (c.borderBox ? 0 : pads);
		boolean row = LayoutContext.isFlex(c) && c.flexDirId == Node.FLEX_DIR_ROW && !c.flexWrap;
		float sum = 0, max = 0;
		int count = 0;
		for (int i = 0, n = c.children.size(); i < n; i++) {
			Node k = c.children.get(i);
			if (!k.isText() && (LayoutContext.isAbsolute(k) || LayoutContext.isNone(k))) continue;
			float w = minContent(ctx, k) + LayoutContext.nz(k.margin[1]) + LayoutContext.nz(k.margin[3]);
			sum += w;
			max = Math.max(max, w);
			count++;
		}
		return (row ? sum + (count > 1 ? c.columnGap * (count - 1) : 0) : max) + pads;
	}

	/** Smallest border-box main size a flex item may shrink to. */
	static float minMain(LayoutContext ctx, Node c) {
		if (c.isText()) return 0;
		float extra = c.borderBox ? 0 : c.borderWidth[1] + c.borderWidth[3] + c.padding[1] + c.padding[3];
		float min = c.minWidth + extra;
		// min-width:auto keeps min-content unless the item clips its overflow
		if (!c.minWidthSet && c.overflowId == Node.OVERFLOW_VISIBLE) min = Math.max(min, minContent(ctx, c));
		return min;
	}

	/** Flex base size along the main (row) axis, excluding margins. */
	static float basisW(LayoutContext ctx, Node c, float cw) {
		if (c.isText()) return intrinsic(ctx, c);
		float extra = c.borderBox ? 0 : c.borderWidth[1] + c.borderWidth[3] + c.padding[1] + c.padding[3];
		float b;
		if (c.flexBasis >= 0) b = c.flexBasis;
		else if (c.width >= 0 && c.percentWidth) b = c.width * cw + extra;
		else return intrinsic(ctx, c);
		float mx = maxW(c, cw);
		if (mx >= 0) b = Math.min(b, mx);
		return Math.max(b, c.minWidth + extra);
	}

	/** Resolved border-box max width, or -1 when unbounded. */
	static float maxW(Node n, float availW) {
		if (n.maxWidth < 0) return -1;
		float extra = n.borderBox ? 0 : n.borderWidth[1] + n.borderWidth[3] + n.padding[1] + n.padding[3];
		return (n.percentMaxWidth ? n.maxWidth * availW : n.maxWidth) + extra;
	}
}
