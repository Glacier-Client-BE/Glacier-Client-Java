package net.glacier.web;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Lays out a styled DOM tree into pixel boxes. Every element gets border-box
 * x/y/w/h (absolute viewport coordinates), contentH and visible; text nodes get
 * x/y/w/h plus {@link Node#lines} with the wrapped lines.
 *
 * <p>The work is split across {@link FlexLayout} (flexbox), {@link GridLayout}, {@link InlineLayout}
 * (text runs and line boxes), {@link AbsoluteLayout} (absolute positioning) and {@link LayoutSizes} (intrinsic
 * sizes). This class holds the box model and block flow. Scratch state lives in {@link LayoutContext}.
 *
 * <p>{@link #layout(Node, float, float, TextMeasurer)} is a full pass. {@link #layout(Node, float, float,
 * TextMeasurer, LayoutCache)} is an incremental pass that reuses the boxes of clean subtrees; see
 * {@link LayoutCache} for the dirty-flag contract.
 */
public final class Layout {

	/** Text metrics supplied by the host. */
	public interface TextMeasurer {
		float width(String text, boolean bold, float fontPx);
		float lineHeight(float fontPx);
	}

	/**
	 * Flags n and every node below it as needing layout, and n's ancestors as having a dirty descendant. Call this
	 * after restyling a subtree: an inherited value (font size, bold, text-align) can change below n without any flag
	 * on those nodes, and a clean node is reused as it was.
	 */
	public static void markLayoutDirty(Node n) {
		markSubtree(n);
		n.markDirty(Node.DIRTY_LAYOUT);
	}

	private static void markSubtree(Node n) {
		n.needsLayout = true;
		n.subtreeDirty = !n.children.isEmpty();
		for (int i = 0, k = n.children.size(); i < k; i++) markSubtree(n.children.get(i));
	}

	private static int passCounter;
	/** Bumped at the end of every pass; a cache is trusted only when it was the last one to see the tree. */
	private static long epoch;
	private static final LayoutCache SCRATCH = new LayoutCache();
	private static final LayoutContext CTX = new LayoutContext();
	private static boolean busy;

	private Layout() {}

	/** Lays out root and its descendants to fill a viewportW x viewportH box at (0,0). */
	public static void layout(Node root, float viewportW, float viewportH, TextMeasurer m) {
		run(root, viewportW, viewportH, m, null);
	}

	/**
	 * Incremental layout: as {@link #layout(Node, float, float, TextMeasurer)}, but a subtree with no dirty flags
	 * whose box inputs are unchanged is reused instead of laid out again. The cache must be used for every layout
	 * of the same tree; the first call (or any call after another layout ran) lays out everything.
	 */
	public static void layout(Node root, float viewportW, float viewportH, TextMeasurer m, LayoutCache cache) {
		run(root, viewportW, viewportH, m, Objects.requireNonNull(cache, "cache"));
	}

	private static void run(Node root, float viewportW, float viewportH, TextMeasurer m, LayoutCache cache) {
		if (busy) throw new IllegalStateException("Layout is not re-entrant");
		LayoutCache c = cache != null ? cache : SCRATCH;
		if (c.epoch != epoch || c.owner != m) {
			c.clear();
			c.owner = m;
		}
		LayoutContext ctx = CTX;
		ctx.begin(m, c, cache != null, ++passCounter);
		busy = true;
		boolean ok = false;
		try {
			box(ctx, root, 0, 0, viewportW, viewportH, viewportW, viewportH);
			ok = true;
		} finally {
			busy = false;
			ctx.end();
			if (!ok) c.clear();
			c.epoch = ++epoch;
		}
		c.compactIfNeeded(root);
	}

	/* ------------------------------------------------------------- box model */

	/**
	 * Lays out one box at (x,y). availW/availH are the containing block size.
	 * fW/fH force the border-box size when >= 0. Returns the border-box height.
	 */
	static float box(LayoutContext ctx, Node n, float x, float y, float availW, float availH, float fW, float fH) {
		LayoutCache.Entry e = null;
		boolean wasDirty = n.needsLayout || n.subtreeDirty;
		if (ctx.memo) {
			e = ctx.cache.entry(n);
			if (e.valid && !wasDirty && e.aw == availW && e.ah == availH && e.fw == fW && e.fh == fH) {
				// Clean and same inputs: the subtree is unchanged, so move it to its box origin plus translate.
				n.visible = true;
				shift(ctx, n, x + e.dx - n.x, y + e.dy - n.y);
				n.w = e.w;
				n.h = e.h;
				n.contentH = e.contentH;
				return e.h;
			}
			// A dirty box may have changed its content, so its content-based sizes must be recomputed.
			if (wasDirty) {
				e.intrOk = false;
				e.minOk = false;
			}
		}
		n.x = x;
		n.y = y;
		n.visible = true;
		if (LayoutContext.isNone(n)) {
			hide(ctx, n);
			return 0;
		}
		float bT = n.borderWidth[0], bR = n.borderWidth[1], bB = n.borderWidth[2], bL = n.borderWidth[3];
		float pT = n.padding[0], pR = n.padding[1], pB = n.padding[2], pL = n.padding[3];
		float hPad = bL + bR + pL + pR, vPad = bT + bB + pT + pB;
		float extraW = n.borderBox ? 0 : hPad, extraH = n.borderBox ? 0 : vPad;

		float W;
		if (fW >= 0) W = fW;
		else if (n.width >= 0) W = (n.percentWidth ? n.width * availW : n.width) + extraW;
		else W = availW;
		float maxW = LayoutSizes.maxW(n, availW);
		if (maxW >= 0) W = Math.min(W, maxW);
		W = Math.max(W, n.minWidth + extraW);

		float Hs = -1;
		if (fH >= 0) Hs = fH;
		else if (n.height >= 0 && (!n.percentHeight || availH >= 0)) {
			Hs = (n.percentHeight ? n.height * availH : n.height) + extraH;
		}

		float natural = content(ctx, n, x + bL + pL, y + bT + pT, Math.max(0, W - hPad), Hs >= 0 ? Hs - vPad : -1);
		n.contentH = natural;
		float H = Hs >= 0 ? Hs : natural + vPad;
		float maxH = n.maxHeight < 0 ? -1
				: n.percentMaxHeight ? (availH >= 0 ? n.maxHeight * availH + extraH : -1)
				: n.maxHeight + extraH;
		if (maxH >= 0) H = Math.min(H, maxH);
		H = Math.max(H, n.minHeight + extraH);
		n.w = W;
		n.h = H;

		AbsoluteLayout.layout(ctx, n, W, H);

		float dx = n.translateX + n.translateXPct * W;
		float dy = n.translateY + n.translateYPct * H;
		if (n.positionId == Node.POSITION_RELATIVE) {
			if (!Float.isNaN(n.inset[3])) dx += n.inset[3];
			else if (!Float.isNaN(n.inset[1])) dx -= n.inset[1];
			if (!Float.isNaN(n.inset[0])) dy += n.inset[0];
			else if (!Float.isNaN(n.inset[2])) dy -= n.inset[2];
		}
		shift(ctx, n, dx, dy);

		n.needsLayout = false;
		n.subtreeDirty = false;
		if (e != null) {
			e.valid = true;
			e.aw = availW;
			e.ah = availH;
			e.fw = fW;
			e.fh = fH;
			e.dx = dx;
			e.dy = dy;
			e.w = W;
			e.h = H;
			e.contentH = natural;
		}
		return H;
	}

	/** Lays out the children of n inside its content box; returns the natural content height. */
	private static float content(LayoutContext ctx, Node n, float cx, float cy, float cw, float ch) {
		if (LayoutContext.isFlex(n)) return FlexLayout.flex(ctx, n, cx, cy, cw, ch);
		if (LayoutContext.isGrid(n)) return GridLayout.grid(ctx, n, cx, cy, cw);
		return flow(ctx, n, cx, cy, cw, ch);
	}

	/* ------------------------------------------------------------ block flow */

	private static float flow(LayoutContext ctx, Node n, float cx, float cy, float cw, float ch) {
		float y = cy;
		int mark = ctx.listMark();
		ArrayList<Node> run = ctx.borrowList();
		for (int i = 0, k = n.children.size(); i < k; i++) {
			Node c = n.children.get(i);
			if (!c.isText() && LayoutContext.isAbsolute(c)) continue;
			if (c.isText() || LayoutContext.isInline(c)) {
				run.add(c);
				continue;
			}
			if (!run.isEmpty()) {
				y += InlineLayout.inlineRun(ctx, n, run, cx, y, cw);
				run.clear();
			}
			if (LayoutContext.isNone(c)) {
				hide(ctx, c);
				continue;
			}
			float mT = LayoutContext.nz(c.margin[0]), mR = LayoutContext.nz(c.margin[1]);
			float mB = LayoutContext.nz(c.margin[2]), mL = LayoutContext.nz(c.margin[3]);
			boolean autoX = Float.isNaN(c.margin[3]) && Float.isNaN(c.margin[1]);
			float h = box(ctx, c, cx + mL, y + mT, cw - mL - mR, ch, -1, -1);
			if (autoX) shift(ctx, c, (cw - c.w) / 2f, 0);
			y += mT + h + mB;
		}
		if (!run.isEmpty()) y += InlineLayout.inlineRun(ctx, n, run, cx, y, cw);
		ctx.releaseLists(mark);
		return y - cy;
	}

	/* ---------------------------------------------------------------- helpers */

	/** Moves the subtree rooted at n by (dx, dy). Iterative, so deep trees do not recurse. */
	static void shift(LayoutContext ctx, Node n, float dx, float dy) {
		if (dx == 0 && dy == 0) return;
		n.x += dx;
		n.y += dy;
		if (n.children.isEmpty()) return;
		int mark = ctx.listMark();
		ArrayList<Node> stack = ctx.borrowList();
		stack.addAll(n.children);
		while (!stack.isEmpty()) {
			Node c = stack.remove(stack.size() - 1);
			c.x += dx;
			c.y += dy;
			stack.addAll(c.children);
		}
		ctx.releaseLists(mark);
	}

	/** Hides n and its subtree (display:none): not visible, zero box. */
	static void hide(LayoutContext ctx, Node n) {
		walk(ctx, n, true);
	}

	/** Drops the memo and dirty state of a subtree that is laid out nowhere, without changing its boxes. */
	static void forget(LayoutContext ctx, Node n) {
		walk(ctx, n, false);
	}

	private static void walk(LayoutContext ctx, Node root, boolean hide) {
		int mark = ctx.listMark();
		ArrayList<Node> stack = ctx.borrowList();
		stack.add(root);
		while (!stack.isEmpty()) {
			Node c = stack.remove(stack.size() - 1);
			if (hide) {
				c.visible = false;
				c.w = 0;
				c.h = 0;
			}
			c.needsLayout = false;
			c.subtreeDirty = false;
			if (ctx.memo && !c.isText()) {
				LayoutCache.Entry e = ctx.cache.get(c);
				if (e != null) {
					e.valid = false;
					e.intrOk = false;
					e.minOk = false;
				}
			}
			stack.addAll(c.children);
		}
		ctx.releaseLists(mark);
	}
}
