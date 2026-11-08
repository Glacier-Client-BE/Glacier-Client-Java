package net.glacier.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** CSS grid with fixed, fr and auto column tracks, column spans and row wrapping. */
final class GridLayout {
	private GridLayout() {}

	/** Lays out the in-flow children of grid container n. Returns the natural content height. */
	static float grid(LayoutContext ctx, Node n, float cx, float cy, float cw) {
		List<String> spec = expandTracks(n.gridColumns);
		if (spec.isEmpty()) spec = List.of("1fr");
		int cols = spec.size();
		int fm = ctx.floatMark();
		float[] tw = ctx.borrowFloats(cols);
		float[] wt = ctx.borrowFloats(cols);
		float fixed = 0, frSum = 0;
		for (int i = 0; i < cols; i++) {
			String t = spec.get(i);
			if (t.endsWith("fr")) {
				wt[i] = weight(t.substring(0, t.length() - 2));
				frSum += wt[i];
			} else if (t.equals("auto")) {
				wt[i] = 1;
				frSum += 1;
			} else {
				tw[i] = Math.max(0, orZero(Css.px(t, n.fontSize)));
				fixed += tw[i];
			}
		}
		float free = Math.max(0, cw - fixed - n.columnGap * (cols - 1));
		for (int i = 0; i < cols; i++) {
			if (wt[i] > 0) tw[i] = frSum > 0 ? free * wt[i] / frSum : 0;
		}

		float y = cy, rowH = 0;
		int col = 0;
		List<Node> kids = n.children;
		for (int i = 0, kc = kids.size(); i < kc; i++) {
			Node c = kids.get(i);
			if (!c.isText() && LayoutContext.isAbsolute(c)) continue;
			if (!c.isText() && LayoutContext.isNone(c)) {
				Layout.hide(ctx, c);
				continue;
			}
			int span = Math.min(cols, c.isText() ? 1 : c.gridColumnSpan);
			if (col + span > cols) {
				y += rowH + n.rowGap;
				rowH = 0;
				col = 0;
			}
			float x = cx, sw = 0;
			for (int k = 0; k < col; k++) x += tw[k] + n.columnGap;
			for (int k = col; k < col + span; k++) sw += tw[k];
			sw += n.columnGap * (span - 1);
			float ml = LayoutContext.nz(c.margin[3]), mr = LayoutContext.nz(c.margin[1]);
			float mt = LayoutContext.nz(c.margin[0]), mb = LayoutContext.nz(c.margin[2]);
			float fw = Math.max(0, sw - ml - mr);
			if (c.isText()) InlineLayout.textItem(ctx, c, x + ml, y + mt, fw);
			else Layout.box(ctx, c, x + ml, y + mt, fw, -1, fw, -1);
			rowH = Math.max(rowH, c.h + mt + mb);
			col += span;
		}
		ctx.releaseFloats(fm);
		return y + rowH - cy;
	}

	private static float weight(String s) {
		try {
			return Float.parseFloat(s);
		} catch (NumberFormatException e) {
			return 1f;
		}
	}

	private static float orZero(float f) {
		return Float.isNaN(f) ? 0 : f;
	}

	/** Expands repeat(n, X) and minmax(a, b) (taken as b) into a flat track list. */
	private static List<String> expandTracks(String s) {
		List<String> out = new ArrayList<>();
		for (String t : Css.tokens(s)) {
			String tl = t.trim().toLowerCase(Locale.ROOT);
			if (tl.startsWith("repeat(") && tl.endsWith(")")) {
				String in = tl.substring(7, tl.length() - 1);
				int comma = in.indexOf(',');
				if (comma < 0) continue;
				int count;
				try {
					count = Integer.parseInt(in.substring(0, comma).trim());
				} catch (NumberFormatException e) {
					count = 1;
				}
				String inner = minmaxMax(in.substring(comma + 1).trim());
				for (int i = 0; i < Math.min(64, Math.max(1, count)); i++) out.add(inner);
			} else {
				out.add(minmaxMax(tl));
			}
		}
		return out;
	}

	private static String minmaxMax(String t) {
		if (!t.startsWith("minmax(") || !t.endsWith(")")) return t;
		List<String> parts = Css.splitTop(t.substring(7, t.length() - 1), ',');
		return parts.get(parts.size() - 1).trim();
	}
}
