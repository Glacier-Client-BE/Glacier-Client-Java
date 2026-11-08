package net.glacier.web;

import java.util.ArrayList;

/**
 * Flexbox: row lines (with wrap) and column flow, main-axis grow and shrink, justify-content and
 * cross-axis alignment. Per-line scratch arrays come from {@link LayoutContext}, so a line does not allocate.
 */
final class FlexLayout {
	private FlexLayout() {}

	/** Lays out the in-flow children of flex container n. Returns the natural content height. */
	static float flex(LayoutContext ctx, Node n, float cx, float cy, float cw, float ch) {
		int mark = ctx.listMark();
		ArrayList<Node> items = ctx.borrowList();
		for (int i = 0, k = n.children.size(); i < k; i++) {
			Node c = n.children.get(i);
			if (!c.isText() && LayoutContext.isAbsolute(c)) continue;
			if (!c.isText() && LayoutContext.isNone(c)) {
				Layout.hide(ctx, c);
				continue;
			}
			items.add(c);
		}
		float r = n.flexDirId == Node.FLEX_DIR_COLUMN
				? flexCol(ctx, n, items, cx, cy, cw, ch)
				: flexRow(ctx, n, items, cx, cy, cw, ch);
		ctx.releaseLists(mark);
		return r;
	}

	private static float flexRow(LayoutContext ctx, Node n, ArrayList<Node> items, float cx, float cy, float cw, float ch) {
		int total = items.size();
		int fm = ctx.floatMark();
		float[] main = ctx.borrowFloats(total);
		for (int i = 0; i < total; i++) main[i] = LayoutSizes.basisW(ctx, items.get(i), cw);
		float[] outer = ctx.borrowFloats(total);
		float[] shrinkW = ctx.borrowFloats(total);

		float y = cy;
		int start = 0, li = 0;
		while (start < total) {
			int end = start;
			float used = 0;
			while (end < total) {
				Node c = items.get(end);
				float need = used + main[end] + LayoutContext.nz(c.margin[1]) + LayoutContext.nz(c.margin[3])
						+ (end == start ? 0 : n.columnGap);
				if (n.flexWrap && end > start && need > cw) break;
				used = need;
				end++;
			}
			boolean single = start == 0 && end == total;
			if (li++ > 0) y += n.rowGap;
			int k = end - start;
			float sumBase = 0, sumGrow = 0;
			int autos = 0;
			for (int i = start; i < end; i++) {
				Node c = items.get(i);
				sumBase += main[i] + LayoutContext.nz(c.margin[1]) + LayoutContext.nz(c.margin[3]);
				if (!c.isText()) sumGrow += c.flexGrow;
				if (!c.isText() && Float.isNaN(c.margin[3])) autos++;
				if (!c.isText() && Float.isNaN(c.margin[1])) autos++;
			}
			float free = cw - sumBase - n.columnGap * (k - 1);
			float rem = 0, autoW = 0;
			if (free > 0 && sumGrow > 0) {
				for (int i = start; i < end; i++) {
					Node c = items.get(i);
					if (!c.isText()) main[i] += free * c.flexGrow / sumGrow;
				}
			} else if (free < 0) {
				shrink(ctx, items, main, shrinkW, outer, start, end, -free);
			} else if (autos > 0) {
				autoW = free / autos;
			} else {
				rem = free;
			}

			ctx.justify(n.justifyId, rem, k);
			float x = cx + ctx.jStart;
			float between = n.columnGap + ctx.jBetween;
			float lineCross = 0;
			for (int i = start; i < end; i++) {
				Node c = items.get(i);
				float ml = Float.isNaN(c.margin[3]) ? autoW : c.margin[3];
				float mr = Float.isNaN(c.margin[1]) ? autoW : c.margin[1];
				float mt = LayoutContext.nz(c.margin[0]), mb = LayoutContext.nz(c.margin[2]);
				float fw = Math.max(0, main[i]);
				if (c.isText()) InlineLayout.textItem(ctx, c, x + ml, y + mt, fw);
				else Layout.box(ctx, c, x + ml, y + mt, fw, ch, fw, -1);
				outer[i] = c.h + mt + mb;
				lineCross = Math.max(lineCross, outer[i]);
				x += fw + ml + mr + between;
			}
			if (single && ch >= 0) lineCross = ch;

			for (int i = start; i < end; i++) {
				Node c = items.get(i);
				int al = LayoutContext.alignKind(c, n);
				float mt = LayoutContext.nz(c.margin[0]), mb = LayoutContext.nz(c.margin[2]);
				float off = 0;
				if (al == LayoutContext.AL_STRETCH && !c.isText() && c.height < 0) {
					float sh = Math.max(0, lineCross - mt - mb);
					if (Math.abs(sh - c.h) > 0.01f && needsDefiniteHeight(c)) Layout.box(ctx, c, c.x, c.y, c.w, ch, c.w, sh);
					else c.h = sh;
				} else {
					float slack = lineCross - outer[i];
					if (al == LayoutContext.AL_CENTER) off = slack / 2f;
					else if (al == LayoutContext.AL_END) off = slack;
				}
				Layout.shift(ctx, c, 0, off);
			}
			y += lineCross;
			start = end;
		}
		ctx.releaseFloats(fm);
		return y - cy;
	}

	/** Shrinks main sizes in [start,end) by deficit, weighted by flex-shrink x base, clamped at min sizes. */
	private static void shrink(LayoutContext ctx, ArrayList<Node> items, float[] main, float[] w, float[] mins,
			int start, int end, float deficit) {
		for (int i = start; i < end; i++) mins[i] = LayoutSizes.minMain(ctx, items.get(i));
		for (int it = 0; it < 4 && deficit > 0.01f; it++) {
			float sum = 0;
			for (int i = start; i < end; i++) {
				Node c = items.get(i);
				w[i] = c.isText() || main[i] <= mins[i] ? 0 : c.flexShrink * main[i];
				sum += w[i];
			}
			if (sum <= 0) return;
			float taken = 0;
			for (int i = start; i < end; i++) {
				if (w[i] == 0) continue;
				float nv = Math.max(mins[i], main[i] - deficit * w[i] / sum);
				taken += main[i] - nv;
				main[i] = nv;
			}
			deficit -= taken;
		}
	}

	/** True when a box whose height is imposed by flex must be laid out again so its children see that height. */
	static boolean needsDefiniteHeight(Node c) {
		if (c.translateX != 0 || c.translateY != 0 || c.translateXPct != 0 || c.translateYPct != 0) return false;
		if (c.positionId == Node.POSITION_RELATIVE) return false;
		return (LayoutContext.isFlex(c) && c.flexDirId == Node.FLEX_DIR_COLUMN)
				|| c.overflowId == Node.OVERFLOW_AUTO || c.overflowId == Node.OVERFLOW_SCROLL;
	}

	private static float flexCol(LayoutContext ctx, Node n, ArrayList<Node> items, float cx, float cy, float cw, float ch) {
		int k = items.size();
		int fm = ctx.floatMark();
		float[] nat = ctx.borrowFloats(k);
		float sum = 0, sumGrow = 0;
		for (int i = 0; i < k; i++) {
			Node c = items.get(i);
			float ml = LayoutContext.nz(c.margin[3]), mr = LayoutContext.nz(c.margin[1]);
			float mt = LayoutContext.nz(c.margin[0]), mb = LayoutContext.nz(c.margin[2]);
			float avail = Math.max(0, cw - ml - mr);
			int al = LayoutContext.alignKind(c, n);
			float fw = c.width >= 0 && !c.isText() ? -1
					: al == LayoutContext.AL_STRETCH ? avail : Math.min(avail, LayoutSizes.intrinsic(ctx, c));
			float fh = c.flexBasis >= 0 ? c.flexBasis : -1;
			if (c.isText()) InlineLayout.textItem(ctx, c, cx + ml, cy, fw);
			else Layout.box(ctx, c, cx + ml, cy, avail, ch, fw, fh);
			float slack = avail - c.w;
			boolean autoL = !c.isText() && Float.isNaN(c.margin[3]);
			boolean autoR = !c.isText() && Float.isNaN(c.margin[1]);
			float off = autoL && autoR ? slack / 2f : autoL ? slack : autoR ? 0
					: al == LayoutContext.AL_CENTER ? slack / 2f
					: al == LayoutContext.AL_END ? slack : 0;
			if (off != 0) Layout.shift(ctx, c, off, 0);
			nat[i] = c.h;
			sum += c.h + mt + mb;
			if (!c.isText()) sumGrow += c.flexGrow;
		}
		float gaps = n.rowGap * Math.max(0, k - 1);
		float free = ch >= 0 ? ch - sum - gaps : 0;
		float[] size = ctx.borrowFloats(k);
		System.arraycopy(nat, 0, size, 0, k);
		boolean resized = false;
		if (free > 0 && sumGrow > 0) {
			for (int i = 0; i < k; i++) {
				Node c = items.get(i);
				if (!c.isText() && c.flexGrow > 0) {
					size[i] += free * c.flexGrow / sumGrow;
					resized = true;
				}
			}
		} else if (free < 0) {
			// only boxes that may go below their content shrink: clipped/scrolling overflow or columns holding one
			float deficit = -free;
			for (int it = 0; it < 4 && deficit > 0.01f; it++) {
				float wsum = 0;
				for (int i = 0; i < k; i++) {
					Node c = items.get(i);
					if (colShrinkable(c) && size[i] > c.minHeight) wsum += c.flexShrink * size[i];
				}
				if (wsum <= 0) break;
				float taken = 0;
				for (int i = 0; i < k; i++) {
					Node c = items.get(i);
					if (!colShrinkable(c) || size[i] <= c.minHeight) continue;
					float nv = Math.max(c.minHeight, size[i] - deficit * c.flexShrink * size[i] / wsum);
					taken += size[i] - nv;
					size[i] = nv;
					resized = true;
				}
				deficit -= taken;
			}
		}
		float used = gaps;
		for (int i = 0; i < k; i++) {
			Node c = items.get(i);
			used += size[i] + LayoutContext.nz(c.margin[0]) + LayoutContext.nz(c.margin[2]);
		}
		float rem = ch >= 0 && !resized ? Math.max(0, ch - used) : 0;
		ctx.justify(n.justifyId, rem, k);
		float yCur = cy + ctx.jStart;
		float between = n.rowGap + ctx.jBetween;
		for (int i = 0; i < k; i++) {
			Node c = items.get(i);
			float mt = LayoutContext.nz(c.margin[0]), mb = LayoutContext.nz(c.margin[2]);
			if (size[i] != nat[i] && needsDefiniteHeight(c)) {
				Layout.shift(ctx, c, 0, yCur + mt - c.y);
				Layout.box(ctx, c, c.x, c.y, c.w, ch, c.w, size[i]);
			} else {
				if (size[i] != nat[i]) c.h = size[i];
				Layout.shift(ctx, c, 0, yCur + mt - c.y);
			}
			yCur += size[i] + mt + mb + between;
		}
		ctx.releaseFloats(fm);
		return used;
	}

	private static boolean colShrinkable(Node c) {
		if (c.isText() || c.flexShrink <= 0 || c.height >= 0) return false;
		if (c.overflowId != Node.OVERFLOW_VISIBLE) return true;
		if (!LayoutContext.isFlex(c) || c.flexDirId != Node.FLEX_DIR_COLUMN) return false;
		for (int i = 0, k = c.children.size(); i < k; i++) if (colShrinkable(c.children.get(i))) return true;
		return false;
	}
}
