package net.glacier.web;

import static net.glacier.web.CssValues.boxInto;
import static net.glacier.web.CssValues.firstInt;
import static net.glacier.web.CssValues.isNumber;
import static net.glacier.web.CssValues.isPct;
import static net.glacier.web.CssValues.orDef;
import static net.glacier.web.CssValues.parseNumber;
import static net.glacier.web.CssValues.pct;
import static net.glacier.web.CssValues.px;
import static net.glacier.web.CssValues.tokens;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The cascade. One applier handles one full or partial pass over a tree; it owns scratch arrays that are
 * reused for every node, so styling a node allocates nothing beyond what the declarations themselves need.
 *
 * <p>Per node: candidate selectors come from the {@link CssIndex} buckets (filtered by the ancestor keys),
 * matched rules are sorted by (specificity, cascade order) as primitive longs, declarations are applied in
 * three passes (font-size, color, rest) with int dispatch on {@link CssProps} ids.
 */
final class CssApply {
	/** Elements that default to inline (text-flow) display. */
	static final Set<String> INLINE_TAGS = new HashSet<>(Arrays.asList(
			"span", "a", "b", "i", "em", "strong", "small", "label", "code", "sup", "sub",
			"u", "s", "kbd", "abbr", "mark", "img", "input", "button", "select", "textarea"));

	private static final int[] SIDES_ALL = {0, 1, 2, 3};
	private static final int[] SIDE_TOP = {0}, SIDE_RIGHT = {1}, SIDE_BOTTOM = {2}, SIDE_LEFT = {3};

	private static volatile boolean sharingEnabled = true;

	/** Parsed inline style attributes, keyed by the attribute string (bounded, least recently used out). */
	private static final int INLINE_CACHE_MAX = 256;
	private static final Map<String, Css.Decl[]> INLINE_CACHE = new LinkedHashMap<String, Css.Decl[]>(64, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Css.Decl[]> eldest) {
			return size() > INLINE_CACHE_MAX;
		}
	};

	static void setStyleSharing(boolean on) {
		sharingEnabled = on;
	}

	/* ------------------------------------------------------------ entry points */

	static void apply(Node root, List<Css.Stylesheet> sheets) {
		CssIndex idx = CssIndex.of(sheets);
		CssApply a = new CssApply(idx, sharingEnabled);
		a.vars = idx.initialVars;
		a.styleNode(root);
		a.applyChildren(root);
	}

	static void applySubtree(Node n, List<Css.Stylesheet> sheets) {
		if (n.isText()) return;
		CssIndex idx = CssIndex.of(sheets);
		CssApply a = new CssApply(idx, sharingEnabled);
		a.vars = n.cssVars != null ? n.cssVars : idx.initialVars;
		for (Node p = n.parent; p != null; p = p.parent) a.addKeys(p, 1);
		a.styleNode(n);
		a.applyChildren(n);
	}

	static int restyleState(Node root, List<Css.Stylesheet> sheets, List<Node> changed) {
		if (changed == null || changed.isEmpty()) return 0;
		CssIndex idx = CssIndex.of(sheets);
		if (idx.stateVars) {
			apply(root, sheets);
			return Css.STYLE_CHANGED | Css.LAYOUT_CHANGED;
		}
		for (Node c : changed) applySubtree(c, sheets);
		return Css.STYLE_CHANGED | (idx.stateLayout ? Css.LAYOUT_CHANGED : Css.PAINT_ONLY);
	}

	/* ------------------------------------------------------------ applier state */

	private final CssIndex idx;
	private final boolean sharing;
	/** Custom properties in effect. Copy-on-write: a map referenced by Node.cssVars is never mutated. */
	private Map<String, String> vars;
	/** Ancestor filter: how many ancestors of the current node carry each key bucket. */
	private final int[] counts = new int[256];
	private final int[] stamp, best;
	private int gen;
	private int matched;
	private int[] ruleBuf = new int[64];
	private long[] keys = new long[64];
	private Css.Decl[] all = new Css.Decl[64];
	private int allLen;
	private String[] vals = new String[64];
	private final float[] box4 = new float[4];

	private CssApply(CssIndex idx, boolean sharing) {
		this.idx = idx;
		this.sharing = sharing && idx.sharingAllowed;
		this.stamp = new int[idx.rules.length];
		this.best = new int[idx.rules.length];
	}

	/* ------------------------------------------------------------ tree walk */

	private void applyChildren(Node n) {
		if (n.children.isEmpty()) return;
		addKeys(n, 1);
		Node prev = null;
		for (Node c : n.children) {
			if (c.isText()) continue;
			if (sharing && prev != null && prev.shareable && canShare(prev, c)) {
				c.copyStyleFrom(prev);
				c.cssVars = vars;
				c.shareable = true;
			} else {
				styleNode(c);
			}
			prev = c;
			applyChildren(c);
		}
		addKeys(n, -1);
	}

	/** Siblings with the same selector-visible identity get the same computed style (no state, no customs). */
	private static boolean canShare(Node a, Node b) {
		return !b.hovered && !b.active && a.tag.equals(b.tag) && a.id.equals(b.id)
				&& a.classes.equals(b.classes) && a.attrs.equals(b.attrs);
	}

	private void addKeys(Node n, int delta) {
		counts[CssIndex.tagKey(n.tag)] += delta;
		if (!n.id.isEmpty()) counts[CssIndex.idKey(n.id)] += delta;
		for (String c : n.classes) counts[CssIndex.classKey(c)] += delta;
	}

	/* ------------------------------------------------------------ one node */

	private void styleNode(Node n) {
		n.resetStyle();
		if (!n.style.isEmpty()) n.style.clear();
		if (INLINE_TAGS.contains(n.tag)) {
			n.display = "inline";
			n.displayBits = Node.DISPLAY_INLINE;
		}
		Node p = n.parent;
		if (p != null) {
			n.fontSize = p.fontSize;
			n.color = p.color;
			n.bold = p.bold;
			n.textAlign = p.textAlign;
			n.textAlignId = p.textAlignId;
			n.lineHeight = p.lineHeight;
			n.letterSpacing = p.letterSpacing;
		}
		n.cssVars = vars;

		int mc = collect(n);
		allLen = 0;
		for (int k = 0; k < mc; k++) {
			Css.Rule r = idx.rules[(int) (keys[k] & 0xFFFFFFFFL)];
			for (Css.Decl d : r.decls) push(d);
		}
		String inline = n.attr("style");
		boolean hasInline = !inline.isEmpty();
		if (hasInline) for (Css.Decl d : inlineDecls(inline)) push(d);

		boolean customs = false, usesVar = false;
		for (int i = 0; i < allLen; i++) {
			Css.Decl d = all[i];
			if (!d.custom) continue;
			customs = true;
			if (!d.value.equals(vars.get(d.prop))) {
				vars = new HashMap<>(vars);
				vars.put(d.prop, d.value);
			}
		}

		if (vals.length < allLen) vals = new String[Math.max(allLen, vals.length * 2)];
		for (int i = 0; i < allLen; i++) {
			Css.Decl d = all[i];
			if (d.custom) continue;
			String v = d.value;
			if (d.hasVar) {
				v = CssValues.resolveVars(v, vars);
				usesVar = true;
			}
			vals[i] = v;
			if (d.id == CssProps.OTHER) n.style.put(d.prop, v);
		}
		for (int pass = 1; pass <= 3; pass++) {
			for (int i = 0; i < allLen; i++) {
				Css.Decl d = all[i];
				if (d.custom || d.pass != pass) continue;
				setProp(n, d, vals[i]);
			}
		}
		n.shareable = sharing && !hasInline && !customs && !usesVar && !n.hovered && !n.active;
	}

	private void push(Css.Decl d) {
		if (allLen == all.length) all = Arrays.copyOf(all, allLen * 2);
		all[allLen++] = d;
	}

	private static Css.Decl[] inlineDecls(String style) {
		synchronized (INLINE_CACHE) {
			Css.Decl[] cached = INLINE_CACHE.get(style);
			if (cached != null) return cached;
		}
		Css.Decl[] parsed = CssParser.parseDeclarations(style).toArray(new Css.Decl[0]);
		synchronized (INLINE_CACHE) {
			INLINE_CACHE.put(style, parsed);
		}
		return parsed;
	}

	/* ------------------------------------------------------------ rule candidates */

	/**
	 * Gathers the rules with a selector matching n, each once with its highest matching specificity,
	 * into {@code keys} sorted by (specificity, cascade order). Returns the count.
	 */
	private int collect(Node n) {
		if (++gen == Integer.MAX_VALUE) {
			Arrays.fill(stamp, 0);
			gen = 1;
		}
		matched = 0;
		if (!n.id.isEmpty()) scan(idx.byId.get(n.id), n);
		for (String c : n.classes) scan(idx.byClass.get(c), n);
		scan(idx.byTag.get(n.tag), n);
		scan(idx.universal, n);
		for (int k = 0; k < matched; k++) {
			int r = ruleBuf[k];
			keys[k] = ((long) best[r] << 32) | r;
		}
		Arrays.sort(keys, 0, matched);
		return matched;
	}

	private void scan(int[] entries, Node n) {
		if (entries == null) return;
		for (int e : entries) {
			int[] anc = idx.selAnc[e];
			if (anc != null && !ancestorsPresent(anc)) continue;
			Css.Selector s = idx.selectors[e];
			if (!CssSelectorMatch.selector(s, n)) continue;
			int r = idx.selRule[e];
			if (stamp[r] != gen) {
				stamp[r] = gen;
				best[r] = s.spec;
				if (matched == ruleBuf.length) {
					ruleBuf = Arrays.copyOf(ruleBuf, matched * 2);
					keys = Arrays.copyOf(keys, matched * 2);
				}
				ruleBuf[matched++] = r;
			} else if (s.spec > best[r]) {
				best[r] = s.spec;
			}
		}
	}

	private boolean ancestorsPresent(int[] anc) {
		for (int k : anc) if (counts[k] == 0) return false;
		return true;
	}

	/* ------------------------------------------------------------ declarations */

	private static float lenPx(Css.Decl d, String v, float em) {
		return Float.isNaN(d.pre) ? px(v, em) : d.pre;
	}

	private void setProp(Node n, Css.Decl d, String v) {
		if (v == null || v.isEmpty()) return;
		float em = n.fontSize;
		switch (d.id) {
			case CssProps.FONT_SIZE -> {
				float base = n.fontSize;
				float f = isPct(v) ? pct(v) * base : px(v, base);
				if (!Float.isNaN(f)) n.fontSize = f;
			}
			case CssProps.FONT -> setFont(n, v);
			case CssProps.FONT_WEIGHT -> n.bold = weight(v, n.bold);
			case CssProps.LINE_HEIGHT -> {
				if (v.equals("normal")) n.lineHeight = 0;
				else if (isNumber(v)) n.lineHeight = Float.parseFloat(v) * n.fontSize;
				else n.lineHeight = orDef(isPct(v) ? pct(v) * n.fontSize : px(v, em), 0);
			}
			case CssProps.LETTER_SPACING -> n.letterSpacing = v.equals("normal") ? 0 : orDef(px(v, em), 0);
			case CssProps.TEXT_ALIGN -> {
				String lv = v.toLowerCase(Locale.ROOT);
				n.textAlign = lv;
				n.textAlignId = Node.keywordId(lv, Node.TEXT_ALIGN_VALUES);
			}
			case CssProps.WHITE_SPACE -> n.nowrap = v.equals("nowrap") || v.equals("pre");
			case CssProps.TEXT_OVERFLOW -> n.ellipsis = v.equals("ellipsis");
			case CssProps.BOX_SHADOW -> n.boxShadow = v;
			case CssProps.COLOR -> {
				Integer c = CssValues.parseColor(v);
				if (c != null) n.color = c;
			}
			case CssProps.BACKGROUND, CssProps.BACKGROUND_COLOR -> setBackground(n, v);
			case CssProps.OPACITY -> {
				float f = parseNumber(v);
				if (!Float.isNaN(f)) n.opacity = Math.max(0f, Math.min(1f, f));
			}
			case CssProps.DISPLAY -> {
				String lv = v.toLowerCase(Locale.ROOT);
				n.display = lv;
				n.displayBits = Node.displayBitsOf(lv);
			}
			case CssProps.POSITION -> {
				String pv = v.toLowerCase(Locale.ROOT);
				n.position = pv.equals("fixed") ? "absolute" : pv;
				n.positionId = Node.positionIdOf(n.position);
			}
			case CssProps.TOP -> n.inset[0] = lenPx(d, v, em);
			case CssProps.RIGHT -> n.inset[1] = lenPx(d, v, em);
			case CssProps.BOTTOM -> n.inset[2] = lenPx(d, v, em);
			case CssProps.LEFT -> n.inset[3] = lenPx(d, v, em);
			case CssProps.INSET -> {
				if (boxInto(v, em, box4)) System.arraycopy(box4, 0, n.inset, 0, 4);
			}
			case CssProps.BOX_SIZING -> n.borderBox = v.equalsIgnoreCase("border-box");
			case CssProps.OVERFLOW -> {
				String lv = v.toLowerCase(Locale.ROOT);
				n.overflow = lv;
				n.overflowId = Node.overflowIdOf(lv);
			}
			case CssProps.WIDTH -> {
				n.percentWidth = isPct(v);
				n.width = n.percentWidth ? pct(v) : orDef(px(v, em), -1);
			}
			case CssProps.HEIGHT -> {
				n.percentHeight = isPct(v);
				n.height = n.percentHeight ? pct(v) : orDef(px(v, em), -1);
			}
			case CssProps.MIN_WIDTH -> {
				n.minWidth = orDef(px(v, em), 0);
				n.minWidthSet = !v.trim().equalsIgnoreCase("auto");
			}
			case CssProps.MIN_HEIGHT -> n.minHeight = orDef(px(v, em), 0);
			case CssProps.MAX_WIDTH -> {
				n.percentMaxWidth = isPct(v);
				n.maxWidth = n.percentMaxWidth ? orDef(pct(v), -1) : orDef(px(v, em), -1);
			}
			case CssProps.MAX_HEIGHT -> {
				n.percentMaxHeight = isPct(v);
				n.maxHeight = n.percentMaxHeight ? orDef(pct(v), -1) : orDef(px(v, em), -1);
			}
			case CssProps.MARGIN -> {
				if (boxInto(v, em, box4)) System.arraycopy(box4, 0, n.margin, 0, 4);
			}
			case CssProps.MARGIN_TOP -> n.margin[0] = lenPx(d, v, em);
			case CssProps.MARGIN_RIGHT -> n.margin[1] = lenPx(d, v, em);
			case CssProps.MARGIN_BOTTOM -> n.margin[2] = lenPx(d, v, em);
			case CssProps.MARGIN_LEFT -> n.margin[3] = lenPx(d, v, em);
			case CssProps.PADDING -> {
				if (boxInto(v, em, box4)) for (int i = 0; i < 4; i++) n.padding[i] = orDef(box4[i], 0);
			}
			case CssProps.PADDING_TOP -> n.padding[0] = orDef(lenPx(d, v, em), 0);
			case CssProps.PADDING_RIGHT -> n.padding[1] = orDef(lenPx(d, v, em), 0);
			case CssProps.PADDING_BOTTOM -> n.padding[2] = orDef(lenPx(d, v, em), 0);
			case CssProps.PADDING_LEFT -> n.padding[3] = orDef(lenPx(d, v, em), 0);
			case CssProps.FLEX_DIRECTION -> {
				String lv = v.toLowerCase(Locale.ROOT);
				n.flexDirection = lv;
				n.flexDirId = Node.flexDirIdOf(lv);
			}
			case CssProps.FLEX_WRAP -> n.flexWrap = v.toLowerCase(Locale.ROOT).startsWith("wrap");
			case CssProps.FLEX_GROW -> n.flexGrow = orDef(parseNumber(v), 0);
			case CssProps.FLEX_SHRINK -> n.flexShrink = orDef(parseNumber(v), 1);
			case CssProps.FLEX_BASIS -> n.flexBasis = isPct(v) ? -1 : orDef(px(v, em), -1);
			case CssProps.FLEX -> setFlex(n, v);
			case CssProps.JUSTIFY_CONTENT -> {
				String lv = v.toLowerCase(Locale.ROOT);
				n.justifyContent = lv;
				n.justifyId = Node.keywordId(lv, Node.JUSTIFY_VALUES);
			}
			case CssProps.ALIGN_ITEMS -> {
				String lv = v.toLowerCase(Locale.ROOT);
				n.alignItems = lv;
				n.alignItemsId = Node.keywordId(lv, Node.ALIGN_ITEMS_VALUES);
			}
			case CssProps.ALIGN_SELF -> {
				String lv = v.toLowerCase(Locale.ROOT);
				n.alignSelf = lv;
				n.alignSelfId = Node.keywordId(lv, Node.ALIGN_SELF_VALUES);
			}
			case CssProps.GAP -> {
				List<String> t = tokens(v);
				if (!t.isEmpty()) {
					float a = orDef(px(t.get(0), em), 0);
					float b = t.size() > 1 ? orDef(px(t.get(1), em), 0) : a;
					n.rowGap = a;
					n.columnGap = b;
				}
			}
			case CssProps.ROW_GAP -> n.rowGap = orDef(lenPx(d, v, em), 0);
			case CssProps.COLUMN_GAP -> n.columnGap = orDef(lenPx(d, v, em), 0);
			case CssProps.GRID_TEMPLATE_COLUMNS -> n.gridColumns = v;
			case CssProps.GRID_COLUMN -> {
				int s = v.indexOf("span");
				if (s >= 0) n.gridColumnSpan = Math.max(1, firstInt(v.substring(s + 4), 1));
			}
			case CssProps.BORDER_RADIUS -> {
				// box order is top, right, bottom, left; radius order is tl, tr, br, bl
				if (boxInto(v, em, box4)) {
					n.radius[0] = orDef(box4[0], 0);
					n.radius[1] = orDef(box4[1], 0);
					n.radius[2] = orDef(box4[2], 0);
					n.radius[3] = orDef(box4[3], 0);
				}
			}
			case CssProps.BORDER -> setBorder(n, SIDES_ALL, v);
			case CssProps.BORDER_TOP -> setBorder(n, SIDE_TOP, v);
			case CssProps.BORDER_RIGHT -> setBorder(n, SIDE_RIGHT, v);
			case CssProps.BORDER_BOTTOM -> setBorder(n, SIDE_BOTTOM, v);
			case CssProps.BORDER_LEFT -> setBorder(n, SIDE_LEFT, v);
			case CssProps.BORDER_WIDTH -> {
				if (boxInto(v, em, box4)) for (int i = 0; i < 4; i++) n.borderWidth[i] = orDef(box4[i], 0);
			}
			case CssProps.BORDER_STYLE -> n.borderStyle = v.toLowerCase(Locale.ROOT);
			case CssProps.BORDER_COLOR -> {
				Integer c = CssValues.parseColor(v);
				if (c != null) Arrays.fill(n.borderColor, c);
			}
			case CssProps.TRANSFORM -> setTransform(n, v);
			default -> { /* cursor, font-family, text-decoration, transition, ...: kept in style map only */ }
		}
	}

	private static void setFont(Node n, String v) {
		float base = n.fontSize;
		for (String t : tokens(v)) {
			int slash = t.indexOf('/');
			String size = slash < 0 ? t : t.substring(0, slash);
			if (size.equals("normal")) continue;
			if (isNumber(size) && Float.parseFloat(size) >= 100) {
				n.bold = weight(size, n.bold);
			} else if (size.equals("bold") || size.equals("bolder") || size.equals("normal") || size.equals("lighter")) {
				n.bold = weight(size, n.bold);
			} else {
				float f = px(size, base);
				if (!Float.isNaN(f)) n.fontSize = f;
			}
			if (slash >= 0) {
				float lh = px(t.substring(slash + 1), n.fontSize);
				if (!Float.isNaN(lh)) n.lineHeight = lh;
			}
		}
	}

	private static boolean weight(String v, boolean def) {
		return switch (v.toLowerCase(Locale.ROOT)) {
			case "bold", "bolder" -> true;
			case "normal", "lighter" -> false;
			default -> isNumber(v) ? Float.parseFloat(v) >= 700 : def;
		};
	}

	private static void setBackground(Node n, String v) {
		String lv = v.toLowerCase(Locale.ROOT);
		if (lv.contains("gradient(")) {
			Css.Gradient g = CssValues.gradient(v);
			if (g != null) {
				n.gradient = g;
				n.background = 0;
			}
			return;
		}
		if (lv.equals("none")) {
			n.background = 0;
			n.gradient = null;
			return;
		}
		List<String> t = tokens(v);
		Integer c = t.isEmpty() ? null : CssValues.parseColor(t.get(0));
		if (c != null) {
			n.background = c;
			n.gradient = null;
		}
	}

	private static void setBorder(Node n, int[] sides, String v) {
		float w = 0;
		Integer col = null;
		String style = null;
		for (String t : tokens(v)) {
			String lt = t.toLowerCase(Locale.ROOT);
			if (isBorderStyle(lt)) {
				style = lt;
				continue;
			}
			float f = px(t, n.fontSize);
			if (!Float.isNaN(f)) {
				w = f;
				continue;
			}
			Integer c = CssValues.parseColor(t);
			if (c != null) col = c;
		}
		if (style != null && (style.equals("none") || style.equals("hidden"))) w = 0;
		for (int s : sides) {
			n.borderWidth[s] = w;
			n.borderColor[s] = col != null ? col : n.color;
		}
		if (style != null) n.borderStyle = style;
	}

	private static boolean isBorderStyle(String s) {
		return switch (s) {
			case "none", "hidden", "solid", "dashed", "dotted", "double", "groove", "ridge", "inset", "outset" -> true;
			default -> false;
		};
	}

	private static void setFlex(Node n, String v) {
		String lv = v.trim().toLowerCase(Locale.ROOT);
		n.flexGrow = 0;
		n.flexShrink = 1;
		n.flexBasis = -1;
		if (lv.equals("none")) { n.flexShrink = 0; return; }
		if (lv.equals("auto")) { n.flexGrow = 1; return; }
		int numbers = 0;
		boolean basisSet = false;
		for (String t : tokens(lv)) {
			if (isNumber(t)) {
				float f = Float.parseFloat(t);
				if (numbers == 0) n.flexGrow = f;
				else if (numbers == 1) n.flexShrink = f;
				else { n.flexBasis = f; basisSet = true; }
				numbers++;
			} else if (!isPct(t)) {
				float f = px(t, n.fontSize);
				if (!Float.isNaN(f)) { n.flexBasis = f; basisSet = true; }
			}
		}
		if (numbers == 1 && !basisSet) n.flexBasis = 0;
	}

	private static void setTransform(Node n, String v) {
		String lv = v.toLowerCase(Locale.ROOT);
		n.translateX = n.translateY = n.translateXPct = n.translateYPct = 0;
		int i;
		if ((i = lv.indexOf("translatex(")) >= 0) setTranslate(n, lv, i + 11, 'x');
		if ((i = lv.indexOf("translatey(")) >= 0) setTranslate(n, lv, i + 11, 'y');
		if ((i = lv.indexOf("translate(")) >= 0) {
			int close = lv.indexOf(')', i);
			if (close > 0) {
				List<String> parts = CssValues.splitTop(lv.substring(i + 10, close), ',');
				setAxis(n, parts.get(0).trim(), 'x');
				if (parts.size() > 1) setAxis(n, parts.get(1).trim(), 'y');
			}
		}
	}

	private static void setTranslate(Node n, String lv, int from, char axis) {
		int close = lv.indexOf(')', from);
		if (close > from) setAxis(n, lv.substring(from, close).trim(), axis);
	}

	private static void setAxis(Node n, String arg, char axis) {
		float px = px(arg, n.fontSize);
		float pc = isPct(arg) ? pct(arg) : Float.NaN;
		if (axis == 'x') {
			if (!Float.isNaN(px)) n.translateX = px;
			if (!Float.isNaN(pc)) n.translateXPct = pc;
		} else {
			if (!Float.isNaN(px)) n.translateY = px;
			if (!Float.isNaN(pc)) n.translateYPct = pc;
		}
	}
}
