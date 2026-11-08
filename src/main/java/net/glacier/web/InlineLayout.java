package net.glacier.web;

import java.util.ArrayList;
import java.util.List;

/**
 * Inline formatting: text runs with line wrapping, inline-level element boxes, line boxes and text-align,
 * plus the text-node caches (collapsed text, per-pass width, wrapped lines).
 */
final class InlineLayout {
	private InlineLayout() {}

	/**
	 * Places a run of text and inline-level children left to right inside the content box at (cx, cy) of
	 * width cw, wrapping where needed. Returns the height of the run.
	 */
	static float inlineRun(LayoutContext ctx, Node p, ArrayList<Node> run, float cx, float cy, float cw) {
		float lh = ctx.lineHeight(p);
		float lx = 0, ly = 0, lineH = 0;
		int mark = ctx.listMark();
		ArrayList<Node> line = ctx.borrowList();
		int align = LayoutContext.textAlignKind(p);
		boolean nowrap = p.nowrap;
		for (int i = 0, k = run.size(); i < k; i++) {
			Node c = run.get(i);
			if (c.isText()) {
				String t = norm(c);
				float tw = textW(ctx, c);
				if (nowrap || lx + tw <= cw + 0.5f) {
					setSingleLine(ctx, c, t);
					c.x = cx + lx;
					c.y = cy + ly;
					c.w = tw;
					c.h = lh;
					line.add(c);
					lx += tw;
					lineH = Math.max(lineH, lh);
				} else {
					if (lx > 0) {
						ly += lineH;
						flushLine(ctx, line, lx, cw, align);
						line.clear();
						lx = 0;
						lineH = 0;
					}
					LayoutCache.Entry e = wrapInto(ctx, c, t, p.bold, p.fontSize, cw);
					c.x = cx;
					c.y = cy + ly;
					c.w = e.wrapLineW;
					c.h = c.lines.size() * lh;
					ly += (c.lines.size() - 1) * lh;
					lx = e.wrapLastW;
					lineH = lh;
				}
			} else {
				float iw = LayoutSizes.intrinsic(ctx, c);
				if (!nowrap && lx > 0 && lx + iw > cw) {
					ly += lineH;
					flushLine(ctx, line, lx, cw, align);
					line.clear();
					lx = 0;
					lineH = 0;
				}
				float h = Layout.box(ctx, c, cx + lx, cy + ly, cw, -1, iw, -1);
				line.add(c);
				lx += iw;
				lineH = Math.max(lineH, h);
			}
		}
		flushLine(ctx, line, lx, cw, align);
		ctx.releaseLists(mark);
		return ly + lineH;
	}

	/** Lays out a text node as a block of wrapped lines inside width w. Returns its height. */
	static float textItem(LayoutContext ctx, Node c, float x, float y, float w) {
		Node p = c.parent;
		String t = norm(c);
		if (p.nowrap || textW(ctx, c) <= w + 0.5f) setSingleLine(ctx, c, t);
		else wrapInto(ctx, c, t, p.bold, p.fontSize, w);
		c.x = x;
		c.y = y;
		c.w = w;
		c.h = c.lines.size() * ctx.lineHeight(p);
		return c.h;
	}

	/** Shifts the line's items so that text-align applies to the line of width width. */
	private static void flushLine(LayoutContext ctx, ArrayList<Node> line, float width, float cw, int align) {
		float dx = align == LayoutContext.TA_CENTER ? (cw - width) / 2f
				: align == LayoutContext.TA_RIGHT ? cw - width
				: 0f;
		if (dx > 0) for (int i = 0, k = line.size(); i < k; i++) Layout.shift(ctx, line.get(i), dx, 0);
	}

	/** Puts the one-line text t into c.lines and clears any wrap memo that described c.lines. */
	private static void setSingleLine(LayoutContext ctx, Node c, String t) {
		c.lines.clear();
		c.lines.add(t);
		LayoutCache.Entry e = ctx.cache.get(c);
		if (e != null) e.wrapOk = false;
	}

	/**
	 * Wraps text t into c.lines at maxW (words separated by single spaces), memoised per text node. Returns the
	 * entry holding the widest line and the width of the last line.
	 */
	private static LayoutCache.Entry wrapInto(LayoutContext ctx, Node c, String t, boolean bold, float size, float maxW) {
		LayoutCache.Entry e = ctx.cache.entry(c);
		if (e.wrapOk && e.wrapSrc == t && e.wrapW == maxW && e.wrapSize == size && e.wrapBold == bold) return e;
		Layout.TextMeasurer m = ctx.m;
		List<String> out = c.lines;
		out.clear();
		if (t.indexOf("  ") >= 0) wrapWords(t, maxW, bold, size, m, out);
		else wrapSlices(t, maxW, bold, size, m, out);
		float maxLine = 0;
		for (int j = 0, k = out.size(); j < k; j++) maxLine = Math.max(maxLine, m.width(out.get(j), bold, size));
		e.wrapOk = true;
		e.wrapSrc = t;
		e.wrapW = maxW;
		e.wrapSize = size;
		e.wrapBold = bold;
		e.wrapLineW = maxLine;
		e.wrapLastW = m.width(out.get(out.size() - 1), bold, size);
		return e;
	}

	/**
	 * Greedy line breaking on single-space text: every candidate line is a substring of t. The collapse helper can
	 * leave a double space in the text, so text that has one takes {@link #wrapWords}, which rebuilds lines from
	 * their words exactly as before.
	 */
	private static void wrapSlices(String t, float maxW, boolean bold, float size, Layout.TextMeasurer m, List<String> out) {
		int len = t.length();
		int curS = 0, curE = 0, i = 0;
		boolean has = false;
		while (true) {
			while (i < len && t.charAt(i) == ' ') i++;
			if (i >= len) break;
			int ws = i;
			while (i < len && t.charAt(i) != ' ') i++;
			int we = i;
			if (!has) {
				curS = ws;
				curE = we;
				has = true;
			} else if (m.width(t.substring(curS, we), bold, size) > maxW) {
				out.add(t.substring(curS, curE));
				curS = ws;
				curE = we;
			} else {
				curE = we;
			}
		}
		out.add(has ? t.substring(curS, curE) : "");
	}

	private static void wrapWords(String t, float maxW, boolean bold, float size, Layout.TextMeasurer m, List<String> out) {
		StringBuilder cur = new StringBuilder();
		for (String word : t.split(" ")) {
			if (word.isEmpty()) continue;
			if (cur.length() == 0) {
				cur.append(word);
				continue;
			}
			String trial = cur + " " + word;
			if (m.width(trial, bold, size) > maxW) {
				out.add(cur.toString());
				cur.setLength(0);
				cur.append(word);
			} else {
				cur.setLength(0);
				cur.append(trial);
			}
		}
		out.add(cur.toString());
	}

	/** Whitespace-collapsed text of a text node, cached until its text changes. */
	static String norm(Node c) {
		if (c.normSrc != c.text) {
			c.normSrc = c.text;
			c.normText = collapse(c.text);
		}
		return c.normText;
	}

	private static String collapse(String s) {
		String t = s.trim();
		StringBuilder b = null;
		boolean ws = false;
		for (int i = 0; i < t.length(); i++) {
			char ch = t.charAt(i);
			if (Character.isWhitespace(ch)) {
				if ((ws || ch != ' ') && b == null) b = new StringBuilder(t.length()).append(t, 0, i);
				ws = true;
				continue;
			}
			if (b != null) {
				if (ws) b.append(' ');
				b.append(ch);
			}
			ws = false;
		}
		return b == null ? t : b.toString();
	}

	/** Width of a text node's collapsed text, measured once per layout pass. */
	static float textW(LayoutContext ctx, Node c) {
		if (c.measPass != ctx.pass) {
			c.measW = ctx.m.width(norm(c), c.parent.bold, c.parent.fontSize);
			c.measPass = ctx.pass;
		}
		return c.measW;
	}
}
