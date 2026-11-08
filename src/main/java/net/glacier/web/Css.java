package net.glacier.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Public facade of the CSS engine. The work is split across:
 * <ul>
 *   <li>{@link CssParser}: stylesheet, selector and declaration parsing;</li>
 *   <li>{@link CssValues}: colour, length and token helpers;</li>
 *   <li>{@link CssProps}: property name to int id;</li>
 *   <li>{@link CssSelectorMatch}: selector matching;</li>
 *   <li>{@link CssIndex}: per-stylesheet-list rule index (rightmost-compound buckets);</li>
 *   <li>{@link CssApply}: the cascade, style sharing and incremental restyle.</li>
 * </ul>
 */
public final class Css {
	private Css() {}

	/** {@link #restyleState} result bits. */
	public static final int STYLE_CHANGED = 1, LAYOUT_CHANGED = 2, PAINT_ONLY = 4;

	/* ----------------------------------------------------------------- model */

	public static final class Gradient {
		public float angleDeg = 180f;                              // 0 = to top, 90 = to right, 180 = to bottom
		public final List<Integer> colors = new ArrayList<>();     // ARGB
		public final List<Float> stops = new ArrayList<>();        // 0..1, NaN = evenly spaced
	}

	public static final class Stylesheet {
		final List<Rule> rules = new ArrayList<>();
		final Map<String, String> vars = new java.util.HashMap<>();
	}

	/** One declaration, with its property id and pre-parsed value (see {@link CssProps}). */
	static final class Decl {
		final String prop, value;
		/** {@link CssProps} id of {@link #prop}. */
		final int id;
		/** Cascade pass 1..3. */
		final int pass;
		/** True for --custom properties. */
		final boolean custom;
		/** True when the value contains var(); such values are resolved at apply time. */
		final boolean hasVar;
		/** Pre-parsed px value for simple lengths (no var(), no em base), otherwise NaN. */
		final float pre;

		Decl(String prop, String value) {
			this.prop = prop;
			this.value = value;
			this.id = CssProps.id(prop);
			this.custom = id == CssProps.CUSTOM;
			this.pass = CssProps.pass(id);
			this.hasVar = value.contains("var(");
			this.pre = !custom && !hasVar && CssProps.preParsedLength(id) ? CssValues.emFreePx(value) : Float.NaN;
		}
	}

	static final class Rule {
		final List<Selector> selectors = new ArrayList<>();
		final List<Decl> decls = new ArrayList<>();
	}

	/** One compound selector (tag#id.class[attr]:pseudo) plus its combinator to the previous part. */
	static final class Compound {
		char combinator = ' ';                       // ' ' descendant, '>' child
		String tag = "";                             // "" = any
		String id = "";
		final List<String> classes = new ArrayList<>();
		final List<String[]> attrs = new ArrayList<>();   // {name, value-or-null}
		final List<String> pseudos = new ArrayList<>();
		boolean never;                               // unsupported part: never matches

		boolean matches(Node n) {
			return CssSelectorMatch.compound(this, n);
		}
	}

	static final class Selector {
		final List<Compound> parts = new ArrayList<>();
		int spec;

		boolean matches(Node n) {
			return CssSelectorMatch.selector(this, n);
		}
	}

	/* ---------------------------------------------------------------- public API */

	/** Parses a stylesheet. @media/@keyframes/@font-face blocks are ignored. */
	public static Stylesheet parseSheet(String css) {
		return CssParser.parseSheet(css);
	}

	/**
	 * Recomputes the computed style of every element under root. Call after any DOM
	 * or class change. Sheets are matched in order; later rules win at equal specificity.
	 */
	public static void apply(Node root, List<Stylesheet> sheets) {
		CssApply.apply(root, sheets);
	}

	/**
	 * Recomputes the style of one element and its descendants, leaving the rest of the tree alone.
	 * The element's parent must already be styled (as after {@link #apply}).
	 */
	public static void applySubtree(Node n, List<Stylesheet> sheets) {
		CssApply.applySubtree(n, sheets);
	}

	/**
	 * Restyles after the hovered/active flags of {@code changed} nodes changed. Only the changed nodes'
	 * subtrees are recomputed, which is exact because state selectors can only match a node or its
	 * descendants. Returns {@link #STYLE_CHANGED} plus either {@link #PAINT_ONLY} (every :hover/:active rule
	 * sets only paint properties, so layout can be skipped) or {@link #LAYOUT_CHANGED}. Returns 0 when
	 * nothing changed. When a state rule declares a custom property, falls back to a full {@link #apply}.
	 */
	public static int restyleState(Node root, List<Stylesheet> sheets, List<Node> changed) {
		return CssApply.restyleState(root, sheets, changed);
	}

	/** Enables or disables style sharing between identical siblings (on by default). */
	public static void setStyleSharing(boolean on) {
		CssApply.setStyleSharing(on);
	}

	/** Parses a CSS color to ARGB, or null when it is not a color (e.g. currentColor). */
	public static Integer parseColor(String v) {
		return CssValues.parseColor(v);
	}

	public static int argb(int a, int r, int g, int b) {
		return CssValues.argb(a, r, g, b);
	}

	/** Length in px. em uses {@code emBase}; NaN for auto, percentages, none or unparseable values. */
	public static float px(String v, float emBase) {
		return CssValues.px(v, emBase);
	}

	/** Resolves var(--x[, fallback]) references against the variable map (recursive). */
	public static String resolveVars(String value, Map<String, String> vars) {
		return CssValues.resolveVars(value, vars);
	}

	/** Splits on whitespace that is not inside parentheses or quotes. */
	static List<String> tokens(String v) {
		return CssValues.tokens(v);
	}

	/** Splits on a separator that is not inside parentheses. */
	static List<String> splitTop(String v, char sep) {
		return CssValues.splitTop(v, sep);
	}
}
