package net.glacier.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rule index for one list of stylesheets (in cascade order). Each selector is filed under the key of its
 * rightmost compound (id, else first class, else tag, else universal), so an element only tests the
 * selectors whose key it carries. Also holds the ancestor-filter keys of every selector, the initial
 * custom properties, and the static facts style sharing and incremental restyle need.
 *
 * <p>Built once per stylesheet list and cached (the sheets are immutable after parsing).
 */
final class CssIndex {
	/** Properties a :hover/:active rule may set without changing layout (mirrors WebView's list). */
	static final Set<String> PAINT_ONLY_PROPS = Set.of("background", "background-color", "background-image",
			"color", "border-color", "border-top-color", "border-right-color", "border-bottom-color",
			"border-left-color", "box-shadow", "opacity", "cursor", "transition", "accent-color", "outline",
			"outline-color");

	/** Rules in cascade order (sheet order, then rule order). */
	final Css.Rule[] rules;
	/** Selector entries, each filed under exactly one bucket. */
	final Css.Selector[] selectors;
	/** Global rule index of each selector entry. */
	final int[] selRule;
	/** Ancestor-filter bucket keys required by each selector entry (null when none). */
	final int[][] selAnc;
	final Map<String, int[]> byId = new HashMap<>();
	final Map<String, int[]> byClass = new HashMap<>();
	final Map<String, int[]> byTag = new HashMap<>();
	final int[] universal;
	/** Custom properties of all sheets merged in order. Read-only: the cascade copies before writing. */
	final Map<String, String> initialVars;
	/** False when some selector uses :first-child or :last-child (siblings can differ then). */
	final boolean sharingAllowed;
	/** True when some :hover/:active rule sets a property outside {@link #PAINT_ONLY_PROPS}. */
	final boolean stateLayout;
	/** True when some :hover/:active rule declares a custom property. */
	final boolean stateVars;

	private final Css.Stylesheet[] key;

	private CssIndex(List<Css.Stylesheet> sheets) {
		key = sheets.toArray(new Css.Stylesheet[0]);
		int nRules = 0, nSel = 0;
		for (Css.Stylesheet s : key) {
			for (Css.Rule r : s.rules) {
				nRules++;
				nSel += r.selectors.size();
			}
		}
		rules = new Css.Rule[nRules];
		selectors = new Css.Selector[nSel];
		selRule = new int[nSel];
		selAnc = new int[nSel][];
		initialVars = new HashMap<>();

		Map<String, List<Integer>> ids = new HashMap<>(), classes = new HashMap<>(), tags = new HashMap<>();
		List<Integer> uni = new ArrayList<>();
		boolean share = true, layout = false, vars = false;
		int g = 0, e = 0;
		for (Css.Stylesheet s : key) {
			initialVars.putAll(s.vars);
			for (Css.Rule r : s.rules) {
				rules[g] = r;
				boolean stateRule = false;
				for (Css.Selector sel : r.selectors) {
					selectors[e] = sel;
					selRule[e] = g;
					Css.Compound right = sel.parts.get(sel.parts.size() - 1);
					if (!right.id.isEmpty()) {
						add(ids, right.id, e);
					} else if (!right.classes.isEmpty()) {
						add(classes, right.classes.get(0), e);
					} else if (!right.tag.isEmpty()) {
						add(tags, right.tag, e);
					} else {
						uni.add(e);
					}
					selAnc[e] = ancestorKeys(sel);
					for (Css.Compound c : sel.parts) {
						for (String p : c.pseudos) {
							if (p.equals("first-child") || p.equals("last-child")) share = false;
							if (p.equals("hover") || p.equals("active")) stateRule = true;
						}
					}
					e++;
				}
				if (stateRule) {
					for (Css.Decl d : r.decls) {
						if (d.custom) vars = true;
						if (!PAINT_ONLY_PROPS.contains(d.prop)) layout = true;
					}
				}
				g++;
			}
		}
		for (Map.Entry<String, List<Integer>> en : ids.entrySet()) byId.put(en.getKey(), toArray(en.getValue()));
		for (Map.Entry<String, List<Integer>> en : classes.entrySet()) byClass.put(en.getKey(), toArray(en.getValue()));
		for (Map.Entry<String, List<Integer>> en : tags.entrySet()) byTag.put(en.getKey(), toArray(en.getValue()));
		universal = toArray(uni);
		sharingAllowed = share;
		stateLayout = layout;
		stateVars = vars;
	}

	/** Indexes kept for reuse: a few screens (menu, start, pause, ...) can alternate. */
	private static final int CACHE_MAX = 8;
	private static final List<CssIndex> CACHE = new ArrayList<>();

	/** The index for this stylesheet list, built on first use and reused while the same sheets are passed. */
	static synchronized CssIndex of(List<Css.Stylesheet> sheets) {
		for (int i = 0; i < CACHE.size(); i++) {
			CssIndex c = CACHE.get(i);
			if (c.sameSheets(sheets)) {
				if (i > 0) {
					CACHE.remove(i);
					CACHE.add(0, c);
				}
				return c;
			}
		}
		CssIndex c = new CssIndex(sheets);
		CACHE.add(0, c);
		if (CACHE.size() > CACHE_MAX) CACHE.remove(CACHE.size() - 1);
		return c;
	}

	private boolean sameSheets(List<Css.Stylesheet> sheets) {
		if (sheets.size() != key.length) return false;
		int i = 0;
		for (Css.Stylesheet s : sheets) {
			if (s != key[i++]) return false;
		}
		return true;
	}

	/* ---- bucket keys for the ancestor filter (0..255) ---- */

	static int tagKey(String tag) {
		return mix(tag.hashCode());
	}

	static int idKey(String id) {
		return mix(id.hashCode() ^ 0x5bd1e995);
	}

	static int classKey(String cls) {
		return mix(cls.hashCode() * 31 + 7);
	}

	private static int mix(int h) {
		return (h * 0x9E3779B1) >>> 24;
	}

	private static int[] ancestorKeys(Css.Selector sel) {
		int n = sel.parts.size() - 1;
		if (n <= 0) return null;
		int count = 0;
		for (int i = 0; i < n; i++) {
			Css.Compound c = sel.parts.get(i);
			count += (c.tag.isEmpty() ? 0 : 1) + (c.id.isEmpty() ? 0 : 1) + c.classes.size();
		}
		if (count == 0) return null;
		int[] keys = new int[count];
		int k = 0;
		for (int i = 0; i < n; i++) {
			Css.Compound c = sel.parts.get(i);
			if (!c.tag.isEmpty()) keys[k++] = tagKey(c.tag);
			if (!c.id.isEmpty()) keys[k++] = idKey(c.id);
			for (String cl : c.classes) keys[k++] = classKey(cl);
		}
		return keys;
	}

	private static void add(Map<String, List<Integer>> m, String k, int e) {
		m.computeIfAbsent(k, x -> new ArrayList<>()).add(e);
	}

	private static int[] toArray(List<Integer> l) {
		int[] a = new int[l.size()];
		for (int i = 0; i < a.length; i++) a[i] = l.get(i);
		return a;
	}
}
