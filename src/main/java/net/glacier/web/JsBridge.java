package net.glacier.web;

import net.glacier.GlacierClient;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Rhino scripting layer for a {@link WebView}. Installs a small DOM ({@code document}, element wrappers with
 * {@code innerHTML}, {@code classList}, {@code style}, {@code dataset}, listeners and selectors), timers driven by
 * {@link #tick()}, and {@code console}. Events from the view are delivered to JS listeners with a DOM-like event
 * object; {@code stopPropagation} is honoured because the view fires one call per ancestor.
 *
 * <p>Page scripts are compiled once per asset name and the {@link Script} is reused by every bridge. Bytecode
 * generation (optimisation 9) is used when it works; if it fails with a linkage or verification error the bridge
 * falls back to interpreted mode (-1) for the rest of the session. Every entry point enters the Context on the
 * calling thread and exits it again.
 *
 * <p>Element wrappers are cached per {@link Node} in an identity map. Entries are dropped when a subtree is replaced
 * (innerHTML, textContent), so the map never outlives the tree it mirrors. A wrapper must not be cached in a
 * {@code WeakHashMap}: the wrapper references its node, which would keep the key alive.
 */
public final class JsBridge implements WebView.WebEvents {

	private static final String ASSET_DIR = "/assets/glacier/web/";
	private static final Object[] NO_ARGS = new Object[0];
	private static final int SELECTOR_CACHE_MAX = 64;
	private static final int COMPILED_LEVEL = 9;
	private static final int INTERPRETED_LEVEL = -1;
	private static final Set<String> VOID_TAGS = Set.of("area", "base", "br", "col", "embed", "hr", "img",
			"input", "link", "meta", "source", "track", "wbr");
	private static final Set<String> UNITLESS = Set.of("opacity", "z-index", "flex", "flex-grow", "flex-shrink",
			"order", "font-weight", "line-height");
	private static final Set<String> OBJECT_NAMES = Set.of("toString", "valueOf", "constructor", "hasOwnProperty",
			"isPrototypeOf", "propertyIsEnumerable", "toLocaleString", "__proto__");

	/* Element property dispatch: one hash lookup maps a name to a code, then an int switch. */
	private static final int K_INNER_HTML = 1, K_TEXT_CONTENT = 2, K_CLASS_NAME = 3, K_ID = 4, K_VALUE = 5,
			K_CHECKED = 6, K_TAG_NAME = 7, K_NODE_NAME = 8, K_PARENT = 9, K_CHILDREN = 10, K_STYLE = 11,
			K_CLASS_LIST = 12, K_DATASET = 13;
	private static final String[] METHOD_NAMES = {"addEventListener", "removeEventListener", "setAttribute",
			"getAttribute", "hasAttribute", "removeAttribute", "closest", "matches", "querySelector",
			"querySelectorAll"};
	private static final int M_BASE = 100;
	/** Event types with an onX property; the property name is "on" + type. */
	private static final String[] HANDLER_TYPES = {"click", "mousedown", "mouseup", "keydown", "input", "change",
			"mouseover", "mouseout"};
	private static final int H_BASE = 200;
	private static final Map<String, Integer> ELEMENT_KEYS;

	static {
		Map<String, Integer> m = new HashMap<>();
		m.put("innerHTML", K_INNER_HTML);
		m.put("textContent", K_TEXT_CONTENT);
		m.put("className", K_CLASS_NAME);
		m.put("id", K_ID);
		m.put("value", K_VALUE);
		m.put("checked", K_CHECKED);
		m.put("tagName", K_TAG_NAME);
		m.put("nodeName", K_NODE_NAME);
		m.put("parentElement", K_PARENT);
		m.put("children", K_CHILDREN);
		m.put("style", K_STYLE);
		m.put("classList", K_CLASS_LIST);
		m.put("dataset", K_DATASET);
		for (int i = 0; i < METHOD_NAMES.length; i++) m.put(METHOD_NAMES[i], M_BASE + i);
		for (int i = 0; i < HANDLER_TYPES.length; i++) m.put("on" + HANDLER_TYPES[i], H_BASE + i);
		ELEMENT_KEYS = Map.copyOf(m);
	}

	/** Optimisation level for new contexts. Drops to -1 for the session if bytecode generation fails. */
	private static volatile int optLevel = COMPILED_LEVEL;
	/** Compiled page scripts by asset name. Only touched on the thread that builds and renders screens. */
	private static final Map<String, Script> SCRIPTS = new HashMap<>();

	/** A Java implementation of a JavaScript-callable function. Returns a Java value, Undefined or null. */
	public interface Fn {
		Object call(Object[] args);
	}

	private static final ContextFactory FACTORY = new ContextFactory() {
		@Override
		protected Context makeContext() {
			Context cx = super.makeContext();
			cx.setOptimizationLevel(optLevel);
			cx.setLanguageVersion(Context.VERSION_ES6);
			return cx;
		}
	};

	private static final class Timer {
		final int id;
		final Callable fn;
		final boolean repeat;
		final long periodNanos;
		long due;
		boolean cancelled;

		Timer(int id, Callable fn, boolean repeat, long periodNanos) {
			this.id = id;
			this.fn = fn;
			this.repeat = repeat;
			this.periodNanos = periodNanos;
		}
	}

	private final WebView view;
	private final ScriptableObject scope;
	private final Map<Node, Element> wrappers = new IdentityHashMap<>();
	/** Bounded LRU of compiled selector lists, keyed by selector text. */
	private final Map<String, List<Css.Selector>> selectorCache = new LinkedHashMap<>(16, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, List<Css.Selector>> e) {
			return size() > SELECTOR_CACHE_MAX;
		}
	};
	private final List<Timer> timers = new ArrayList<>();
	private int nextTimerId = 1;
	/** Set by stopPropagation; cleared when the next event starts at its origin. */
	private boolean stopped;
	/** Set by stopImmediatePropagation during the listener loop of the current event. */
	private boolean immediateStop;
	/** Bumped by every subtree replacement and every id change; cached document lookups compare against it. */
	private int structGen;
	private Node bodyNode;
	private int bodyGen = -1;
	private Map<String, Node> idIndex;
	private int idIndexGen = -1;
	/** Shared by every event object: they read the bridge flags, so one function of each kind is enough. */
	private final BaseFunction preventDefaultFn, stopPropagationFn, stopImmediateFn;

	/** Creates the scope and installs document, console, window and the timer functions. Sets the view's sink. */
	public JsBridge(WebView view) {
		this.view = view;
		this.scope = newScope();
		this.preventDefaultFn = fn("preventDefault", a -> Undefined.instance);
		this.stopPropagationFn = fn("stopPropagation", a -> {
			stopped = true;
			return Undefined.instance;
		});
		this.stopImmediateFn = fn("stopImmediatePropagation", a -> {
			stopped = true;
			immediateStop = true;
			return Undefined.instance;
		});
		view.setEventSink(this);
		scope.put("window", scope, scope);
		scope.put("document", scope, new Document());
		scope.put("console", scope, console());
		global("setTimeout", a -> addTimer(a, false));
		global("setInterval", a -> addTimer(a, true));
		global("requestAnimationFrame", a -> addTimer(new Object[]{arg(a, 0), 0.0}, false));
		global("clearTimeout", a -> clearTimerFn(a));
		global("clearInterval", a -> clearTimerFn(a));
		global("cancelAnimationFrame", a -> clearTimerFn(a));
	}

	/* ------------------------------------------------------------ public API */

	/** The global scope that scripts run in. */
	public Scriptable scope() {
		return scope;
	}

	/** A new plain JS object. */
	public Scriptable newObject() {
		Context cx = enter();
		try {
			return cx.newObject(scope);
		} finally {
			Context.exit();
		}
	}

	/** A new JS array holding the given values (Double, String, Boolean, Scriptable or null). */
	public Scriptable newArray(Collection<?> items) {
		Object[] arr = items.toArray();
		Context cx = enter();
		try {
			return cx.newArray(scope, arr);
		} finally {
			Context.exit();
		}
	}

	/** Wraps a Java function as a JS function. Exceptions thrown by fn are logged and yield undefined. */
	public BaseFunction fn(String name, Fn fn) {
		return new NativeFn(name, fn);
	}

	/** Argument i, or undefined when the call supplied fewer arguments. */
	public static Object arg(Object[] a, int i) {
		return i < a.length ? a[i] : Undefined.instance;
	}

	public static String argString(Object[] a, int i) {
		return Context.toString(arg(a, i));
	}

	public static double argNumber(Object[] a, int i) {
		return Context.toNumber(arg(a, i));
	}

	/** True for JS undefined and null. */
	public static boolean isMissing(Object v) {
		return v == null || v == Undefined.instance;
	}

	/** Reads a classpath resource under assets/glacier/web/, or returns null when it does not exist. */
	public static String readAsset(String name) {
		try (InputStream in = JsBridge.class.getResourceAsStream(ASSET_DIR + name)) {
			if (in == null) return null;
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			GlacierClient.LOGGER.error("[ui] cannot read {}", name, e);
			return null;
		}
	}

	/**
	 * Runs the named scripts, in order, from assets/glacier/web. Each script is compiled once per process and the
	 * compiled form is reused. A failing script is logged; the rest still run.
	 */
	public void loadScripts(String... names) {
		for (String name : names) {
			Script s = cachedScript(name);
			if (s == null) continue;
			Context cx = enter();
			try {
				s.exec(cx, scope);
			} catch (RuntimeException e) {
				GlacierClient.LOGGER.error("[ui] script {} failed: {}", name, e.getMessage());
			} finally {
				Context.exit();
			}
		}
	}

	/** Evaluates source in the global scope. Returns false when it failed to compile or threw. */
	public boolean eval(String source, String name) {
		Context cx = enter();
		try {
			compile(cx, source, name).exec(cx, scope);
			return true;
		} catch (RuntimeException e) {
			GlacierClient.LOGGER.error("[ui] script {} failed: {}", name, e.getMessage());
			return false;
		} finally {
			Context.exit();
		}
	}

	/** Fires timers that are due. Call once per frame. */
	public void tick() {
		if (timers.isEmpty()) return;
		long now = System.nanoTime();
		List<Timer> due = null;
		for (Timer t : timers) {
			if (t.due <= now) {
				if (due == null) due = new ArrayList<>();
				due.add(t);
			}
		}
		if (due == null) return;
		Context cx = enter();
		try {
			for (Timer t : due) {
				if (t.cancelled) continue;
				if (t.repeat) {
					t.due = now + t.periodNanos;
				} else {
					t.cancelled = true;
					timers.remove(t);
				}
				try {
					t.fn.call(cx, scope, scope, NO_ARGS);
				} catch (RuntimeException e) {
					GlacierClient.LOGGER.error("[ui] timer callback failed", e);
				}
			}
		} finally {
			Context.exit();
		}
	}

	/* ------------------------------------------------------------ events */

	@Override
	public void fire(Node target, String type, Map<String, Object> detail) {
		Node origin = target;
		if (detail != null && detail.get("origin") instanceof Node o) origin = o;
		if (target == origin) {
			stopped = false;
		} else if (stopped) {
			return;
		}
		Element el = wrappers.get(target);
		if (el == null || !el.hasListeners(type)) return;
		dispatch(el, origin, type, detail);
	}

	private void dispatch(Element el, Node origin, String type, Map<String, Object> detail) {
		Context cx = enter();
		try {
			immediateStop = false;
			Scriptable ev = event(cx, type, origin, el, detail);
			for (Callable c : el.listenersFor(type)) {
				try {
					c.call(cx, scope, el, new Object[]{ev});
				} catch (RuntimeException e) {
					GlacierClient.LOGGER.error("[ui] {} listener failed", type, e);
				}
				if (immediateStop) break;
			}
		} finally {
			Context.exit();
		}
	}

	private Scriptable event(Context cx, String type, Node origin, Element current, Map<String, Object> detail) {
		Scriptable ev = cx.newObject(scope);
		ev.put("type", ev, type);
		ev.put("target", ev, wrap(origin));
		ev.put("currentTarget", ev, current);
		if (detail != null) {
			for (Map.Entry<String, Object> e : detail.entrySet()) {
				if ("origin".equals(e.getKey()) || e.getValue() instanceof Node) continue;
				ev.put(e.getKey(), ev, Context.javaToJS(e.getValue(), scope));
			}
		}
		ev.put("preventDefault", ev, preventDefaultFn);
		ev.put("stopPropagation", ev, stopPropagationFn);
		ev.put("stopImmediatePropagation", ev, stopImmediateFn);
		return ev;
	}

	/* ------------------------------------------------------------ internals */

	private static Context enter() {
		return FACTORY.enterContext();
	}

	private static ScriptableObject newScope() {
		Context cx = enter();
		try {
			return cx.initStandardObjects();
		} finally {
			Context.exit();
		}
	}

	/**
	 * Compiles source at the current optimisation level. When bytecode generation fails for a reason other than a
	 * script error, the same source is compiled interpreted and the bridge stays interpreted from then on.
	 */
	private static Script compile(Context cx, String source, String name) {
		if (optLevel != COMPILED_LEVEL) return cx.compileString(source, name, 1, null);
		try {
			return cx.compileString(source, name, 1, null);
		} catch (LinkageError | RuntimeException e) {
			cx.setOptimizationLevel(INTERPRETED_LEVEL);
			Script s = cx.compileString(source, name, 1, null);
			GlacierClient.LOGGER.warn("[ui] bytecode scripts unavailable, using the interpreter: {}", e.toString());
			optLevel = INTERPRETED_LEVEL;
			return s;
		}
	}

	/** Compiled form of an asset, cached by name. Null when the asset is missing or does not compile. */
	private Script cachedScript(String name) {
		Script s = SCRIPTS.get(name);
		if (s != null) return s;
		String src = readAsset(name);
		if (src == null) {
			GlacierClient.LOGGER.error("[ui] missing script {}", name);
			return null;
		}
		Context cx = enter();
		try {
			s = compile(cx, src, name);
		} catch (RuntimeException e) {
			GlacierClient.LOGGER.error("[ui] script {} failed to compile: {}", name, e.getMessage());
			return null;
		} finally {
			Context.exit();
		}
		SCRIPTS.put(name, s);
		return s;
	}

	private void global(String name, Fn f) {
		scope.put(name, scope, fn(name, f));
	}

	private Scriptable console() {
		Scriptable c = newObject();
		c.put("log", c, fn("log", a -> logAt(0, a)));
		c.put("info", c, fn("info", a -> logAt(0, a)));
		c.put("debug", c, fn("debug", a -> logAt(0, a)));
		c.put("warn", c, fn("warn", a -> logAt(1, a)));
		c.put("error", c, fn("error", a -> logAt(2, a)));
		return c;
	}

	private static Object logAt(int level, Object[] a) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < a.length; i++) {
			if (i > 0) sb.append(' ');
			sb.append(Context.toString(a[i]));
		}
		if (level == 2) GlacierClient.LOGGER.error("[ui] {}", sb);
		else if (level == 1) GlacierClient.LOGGER.warn("[ui] {}", sb);
		else GlacierClient.LOGGER.info("[ui] {}", sb);
		return Undefined.instance;
	}

	private Object addTimer(Object[] a, boolean repeat) {
		Object f = arg(a, 0);
		if (!(f instanceof Callable c)) return 0.0;
		double ms = argNumber(a, 1);
		long millis = Double.isNaN(ms) ? 0 : (long) Math.max(0, ms);
		if (repeat) millis = Math.max(1, millis);
		int id = nextTimerId++;
		Timer t = new Timer(id, c, repeat, millis * 1_000_000L);
		t.due = System.nanoTime() + t.periodNanos;
		timers.add(t);
		return (double) id;
	}

	private Object clearTimerFn(Object[] a) {
		int id = (int) argNumber(a, 0);
		for (Iterator<Timer> it = timers.iterator(); it.hasNext(); ) {
			Timer t = it.next();
			if (t.id == id) {
				t.cancelled = true;
				it.remove();
			}
		}
		return Undefined.instance;
	}

	private Element wrap(Node n) {
		if (n == null) return null;
		Element e = wrappers.get(n);
		if (e == null) {
			e = new Element(n);
			wrappers.put(n, e);
		}
		return e;
	}

	/** A style or class change: the view re-runs its cascade and layout once on the next render. */
	private void changed() {
		view.invalidate();
	}

	/** A subtree was replaced or an id changed: lookups keyed on the tree are stale. */
	private void treeChanged() {
		structGen++;
		changed();
	}

	private List<Css.Selector> compile(String sel) {
		return selectorCache.computeIfAbsent(sel, JsBridge::parseSelectors);
	}

	private static List<Css.Selector> parseSelectors(String sel) {
		List<Css.Selector> out = new ArrayList<>();
		try {
			Css.Stylesheet sheet = Css.parseSheet(sel + " {}");
			for (Css.Rule r : sheet.rules) out.addAll(r.selectors);
		} catch (RuntimeException e) {
			GlacierClient.LOGGER.warn("[ui] bad selector {}", sel, e);
		}
		return out;
	}

	private static boolean matchesAny(List<Css.Selector> sels, Node n) {
		if (n.isText()) return false;
		for (Css.Selector s : sels) {
			if (s.matches(n)) return true;
		}
		return false;
	}

	private static Node findFirst(Node root, List<Css.Selector> sels) {
		for (Node c : root.children) {
			if (c.isText()) continue;
			if (matchesAny(sels, c)) return c;
			Node f = findFirst(c, sels);
			if (f != null) return f;
		}
		return null;
	}

	private void collect(Node root, List<Css.Selector> sels, List<Object> out) {
		for (Node c : root.children) {
			if (c.isText()) continue;
			if (matchesAny(sels, c)) out.add(wrap(c));
			collect(c, sels, out);
		}
	}

	/** First element in document order with this id, through the id index rebuilt after each structural change. */
	private Node findIdFast(String id) {
		if (idIndex == null || idIndexGen != structGen) {
			Map<String, Node> m = new HashMap<>();
			indexIds(view.root(), m);
			idIndex = m;
			idIndexGen = structGen;
		}
		return idIndex.get(id);
	}

	private static void indexIds(Node root, Map<String, Node> m) {
		for (Node c : root.children) {
			if (c.isText()) continue;
			if (!c.id.isEmpty()) m.putIfAbsent(c.id, c);
			indexIds(c, m);
		}
	}

	private static Node findTag(Node root, String tag) {
		for (Node c : root.children) {
			if (c.isText()) continue;
			if (tag.equals(c.tag)) return c;
			Node f = findTag(c, tag);
			if (f != null) return f;
		}
		return null;
	}

	private Node findBody() {
		Node body = findTag(view.root(), "body");
		if (body != null) return body;
		Node first = view.root().firstElement();
		return first != null ? first : view.root();
	}

	private Node body() {
		if (bodyNode == null || bodyGen != structGen) {
			bodyNode = findBody();
			bodyGen = structGen;
		}
		return bodyNode;
	}

	private void forgetChildren(Node target) {
		for (Node c : target.children) c.walk(n -> wrappers.remove(n));
		target.children.clear();
	}

	private void setInnerHtml(Node target, String html) {
		forgetChildren(target);
		Node frag = HtmlParser.parse(html);
		for (Node c : frag.children) target.add(c);
		treeChanged();
	}

	private void setText(Node target, String text) {
		forgetChildren(target);
		if (!text.isEmpty()) target.add(Node.text(text));
		treeChanged();
	}

	private void removeAttr(Node n, String name) {
		n.attrs.remove(name);
		if ("id".equals(name)) n.id = "";
		if ("class".equals(name)) n.classes.clear();
		if ("id".equals(name)) treeChanged();
		else changed();
	}

	private Scriptable classList(Node n) {
		Scriptable o = newObject();
		o.put("add", o, fn("add", a -> {
			for (String t : tokens(a)) n.addClass(t);
			changed();
			return Undefined.instance;
		}));
		o.put("remove", o, fn("remove", a -> {
			for (String t : tokens(a)) n.removeClass(t);
			changed();
			return Undefined.instance;
		}));
		o.put("toggle", o, fn("toggle", a -> {
			String t = argString(a, 0);
			boolean want = isMissing(arg(a, 1)) ? !n.hasClass(t) : Context.toBoolean(arg(a, 1));
			if (want) n.addClass(t);
			else n.removeClass(t);
			changed();
			return want;
		}));
		o.put("contains", o, fn("contains", a -> n.hasClass(argString(a, 0))));
		return o;
	}

	private static List<String> tokens(Object[] a) {
		List<String> out = new ArrayList<>();
		for (Object v : a) {
			for (String t : Context.toString(v).trim().split("\\s+")) {
				if (!t.isEmpty()) out.add(t);
			}
		}
		return out;
	}

	private static void serialize(Node n, StringBuilder sb) {
		if (n.isText()) {
			escapeTo(sb, n.text, false);
			return;
		}
		sb.append('<').append(n.tag);
		for (Map.Entry<String, String> e : n.attrs.entrySet()) {
			sb.append(' ').append(e.getKey()).append("=\"");
			escapeTo(sb, e.getValue(), true);
			sb.append('"');
		}
		sb.append('>');
		if (VOID_TAGS.contains(n.tag)) return;
		for (Node c : n.children) serialize(c, sb);
		sb.append("</").append(n.tag).append('>');
	}

	/** Appends s with &amp; &lt; &gt; escaped, and quotes too when attr. Strings with nothing to escape are copied. */
	private static void escapeTo(StringBuilder sb, String s, boolean attr) {
		int n = s.length();
		int i = 0;
		while (i < n) {
			char c = s.charAt(i);
			if (c == '&' || c == '<' || c == '>' || (attr && c == '"')) break;
			i++;
		}
		sb.append(s, 0, i);
		for (; i < n; i++) {
			char c = s.charAt(i);
			switch (c) {
				case '&' -> sb.append("&amp;");
				case '<' -> sb.append("&lt;");
				case '>' -> sb.append("&gt;");
				case '"' -> sb.append(attr ? "&quot;" : "\"");
				default -> sb.append(c);
			}
		}
	}

	/** camelCase to kebab-case; names with a hyphen or no capitals are returned unchanged (no allocation). */
	private static String kebab(String name) {
		if (name.indexOf('-') >= 0) return name;
		int first = 0;
		while (first < name.length() && !Character.isUpperCase(name.charAt(first))) first++;
		if (first == name.length()) return name;
		StringBuilder sb = new StringBuilder(name.length() + 4).append(name, 0, first);
		for (int i = first; i < name.length(); i++) {
			char ch = name.charAt(i);
			if (Character.isUpperCase(ch)) sb.append('-').append(Character.toLowerCase(ch));
			else sb.append(ch);
		}
		return sb.toString();
	}

	/** The value of one property in an inline style string; the last non-empty declaration wins. Null when absent. */
	private static String styleGet(String s, String prop) {
		String found = null;
		for (String part : s.split(";")) {
			int colon = part.indexOf(':');
			if (colon <= 0) continue;
			if (!part.substring(0, colon).trim().toLowerCase(Locale.ROOT).equals(prop)) continue;
			String v = part.substring(colon + 1).trim();
			if (!v.isEmpty()) found = v;
		}
		return found;
	}

	private static Map<String, String> parseStyle(String s) {
		Map<String, String> m = new LinkedHashMap<>();
		for (String part : s.split(";")) {
			int colon = part.indexOf(':');
			if (colon <= 0) continue;
			String k = part.substring(0, colon).trim().toLowerCase(Locale.ROOT);
			String v = part.substring(colon + 1).trim();
			if (!k.isEmpty() && !v.isEmpty()) m.put(k, v);
		}
		return m;
	}

	private static String writeStyle(Map<String, String> m) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> e : m.entrySet()) {
			if (sb.length() > 0) sb.append("; ");
			sb.append(e.getKey()).append(": ").append(e.getValue());
		}
		return sb.toString();
	}

	private static String styleValue(String prop, Object value) {
		if (isMissing(value)) return "";
		if (value instanceof Number n) {
			double d = n.doubleValue();
			String num = d == Math.rint(d) ? Long.toString((long) d) : Double.toString(d);
			return d == 0 || UNITLESS.contains(prop) ? num : num + "px";
		}
		return Context.toString(value).trim();
	}

	/* ------------------------------------------------------------ Java-side JS classes */

	/** A JS function backed by an Fn. Java exceptions are logged and the call returns undefined. */
	private final class NativeFn extends BaseFunction {
		private final String fnName;
		private final Fn fn;

		NativeFn(String fnName, Fn fn) {
			this.fnName = fnName;
			this.fn = fn;
			setParentScope(JsBridge.this.scope);
			setPrototype(getFunctionPrototype(JsBridge.this.scope));
		}

		@Override
		public String getFunctionName() {
			return fnName;
		}

		@Override
		public Object call(Context cx, Scriptable s, Scriptable thisObj, Object[] args) {
			try {
				return fn.call(args);
			} catch (RuntimeException e) {
				GlacierClient.LOGGER.error("[ui] {} failed", fnName, e);
				return Undefined.instance;
			}
		}
	}

	/** Wrapper for one DOM node. Virtual properties are handled in get/put; everything else is an expando. */
	private final class Element extends ScriptableObject {
		final Node node;
		private Object[] methods;
		private Map<String, Callable> handlers;
		private Map<String, List<Callable>> listeners;
		private Scriptable style;
		private Scriptable classList;
		private Scriptable dataset;

		Element(Node node) {
			this.node = node;
			setParentScope(JsBridge.this.scope);
			setPrototype(getObjectPrototype(JsBridge.this.scope));
		}

		@Override
		public String getClassName() {
			return "HTMLElement";
		}

		@Override
		public Object get(String name, Scriptable start) {
			Integer key = ELEMENT_KEYS.get(name);
			if (key == null) return super.get(name, start);
			int k = key;
			if (k >= H_BASE) return handlers == null ? null : handlers.get(HANDLER_TYPES[k - H_BASE]);
			if (k >= M_BASE) return method(k - M_BASE);
			switch (k) {
				case K_INNER_HTML: {
					StringBuilder sb = new StringBuilder();
					for (Node c : node.children) serialize(c, sb);
					return sb.toString();
				}
				case K_TEXT_CONTENT:
					return node.innerText();
				case K_CLASS_NAME:
					return String.join(" ", node.classes);
				case K_ID:
					return node.id;
				case K_VALUE:
					return node.attr("value");
				case K_CHECKED:
					return node.hasAttr("checked");
				case K_TAG_NAME:
				case K_NODE_NAME:
					return node.tag.toUpperCase(Locale.ROOT);
				case K_PARENT:
					return node.parent == null || "#root".equals(node.parent.tag) ? null : wrap(node.parent);
				case K_CHILDREN:
					return childElements();
				case K_STYLE:
					if (style == null) style = new StyleObj(node);
					return style;
				case K_CLASS_LIST:
					if (classList == null) classList = classList(node);
					return classList;
				case K_DATASET:
					if (dataset == null) dataset = new Dataset(node);
					return dataset;
				default:
					return super.get(name, start);
			}
		}

		@Override
		public void put(String name, Scriptable start, Object value) {
			Integer key = ELEMENT_KEYS.get(name);
			if (key == null || (key > K_DATASET && key < H_BASE)) {
				super.put(name, start, value);
				return;
			}
			int k = key;
			if (k >= H_BASE) {
				setHandler(HANDLER_TYPES[k - H_BASE], value instanceof Callable c ? c : null);
				return;
			}
			switch (k) {
				case K_INNER_HTML:
					setInnerHtml(node, Context.toString(value));
					return;
				case K_TEXT_CONTENT:
					setText(node, Context.toString(value));
					return;
				case K_CLASS_NAME:
					node.setAttr("class", Context.toString(value));
					changed();
					return;
				case K_ID:
					node.setAttr("id", Context.toString(value));
					treeChanged();
					return;
				case K_VALUE:
					node.setAttr("value", Context.toString(value));
					changed();
					return;
				case K_CHECKED:
					if (Context.toBoolean(value)) node.setAttr("checked", "");
					else node.attrs.remove("checked");
					changed();
					return;
				case K_STYLE:
					node.setAttr("style", Context.toString(value));
					changed();
					return;
				default:
					super.put(name, start, value);
			}
		}

		private void setHandler(String type, Callable c) {
			if (handlers == null) handlers = new HashMap<>();
			if (c == null) handlers.remove(type);
			else handlers.put(type, c);
		}

		/** Cheap pre-check for fire(): no event object is built unless a handler or listener exists. */
		boolean hasListeners(String type) {
			if (handlers != null && handlers.containsKey(type)) return true;
			if (listeners == null) return false;
			List<Callable> l = listeners.get(type);
			return l != null && !l.isEmpty();
		}

		/** A snapshot of the handlers for type, so listeners may add or remove listeners while running. */
		List<Callable> listenersFor(String type) {
			List<Callable> out = new ArrayList<>(2);
			if (handlers != null) {
				Callable h = handlers.get(type);
				if (h != null) out.add(h);
			}
			if (listeners != null) {
				List<Callable> l = listeners.get(type);
				if (l != null) out.addAll(l);
			}
			return out;
		}

		private void addListener(String type, Object fnValue) {
			if (!(fnValue instanceof Callable c)) return;
			if (listeners == null) listeners = new HashMap<>();
			List<Callable> l = listeners.computeIfAbsent(type, k -> new ArrayList<>());
			if (!l.contains(c)) l.add(c);
		}

		private void removeListener(String type, Object fnValue) {
			if (listeners == null || !(fnValue instanceof Callable c)) return;
			List<Callable> l = listeners.get(type);
			if (l != null) l.remove(c);
		}

		private Scriptable childElements() {
			List<Object> out = new ArrayList<>();
			for (Node c : node.children) {
				if (!c.isText()) out.add(wrap(c));
			}
			return newArray(out);
		}

		private Object method(int idx) {
			if (methods == null) methods = new Object[METHOD_NAMES.length];
			Object m = methods[idx];
			if (m == null) {
				m = fn(METHOD_NAMES[idx], methodImpl(METHOD_NAMES[idx]));
				methods[idx] = m;
			}
			return m;
		}

		private Fn methodImpl(String name) {
			switch (name) {
				case "addEventListener":
					return a -> {
						addListener(argString(a, 0), arg(a, 1));
						return Undefined.instance;
					};
				case "removeEventListener":
					return a -> {
						removeListener(argString(a, 0), arg(a, 1));
						return Undefined.instance;
					};
				case "setAttribute":
					return a -> {
						String attr = argString(a, 0);
						node.setAttr(attr, argString(a, 1));
						if ("id".equals(attr)) treeChanged();
						else changed();
						return Undefined.instance;
					};
				case "getAttribute":
					return a -> node.hasAttr(argString(a, 0)) ? node.attr(argString(a, 0)) : null;
				case "hasAttribute":
					return a -> node.hasAttr(argString(a, 0));
				case "removeAttribute":
					return a -> {
						removeAttr(node, argString(a, 0));
						return Undefined.instance;
					};
				case "closest":
					return a -> closest(argString(a, 0));
				case "matches":
					return a -> matchesAny(compile(argString(a, 0)), node);
				case "querySelector":
					return a -> wrap(findFirst(node, compile(argString(a, 0))));
				case "querySelectorAll":
					return a -> {
						List<Object> out = new ArrayList<>();
						collect(node, compile(argString(a, 0)), out);
						return newArray(out);
					};
				default:
					return a -> Undefined.instance;
			}
		}

		private Element closest(String sel) {
			List<Css.Selector> sels = compile(sel);
			for (Node n = node; n != null && !"#root".equals(n.tag); n = n.parent) {
				if (matchesAny(sels, n)) return wrap(n);
			}
			return null;
		}
	}

	/** Backs element.style: reads and writes the inline style attribute. */
	private final class StyleObj extends ScriptableObject {
		private final Node node;

		StyleObj(Node node) {
			this.node = node;
			setParentScope(JsBridge.this.scope);
			setPrototype(getObjectPrototype(JsBridge.this.scope));
		}

		@Override
		public String getClassName() {
			return "CSSStyleDeclaration";
		}

		@Override
		public Object get(String name, Scriptable start) {
			if (OBJECT_NAMES.contains(name)) return super.get(name, start);
			if ("cssText".equals(name)) return node.attr("style");
			String v = styleGet(node.attr("style"), kebab(name));
			return v == null ? "" : v;
		}

		@Override
		public void put(String name, Scriptable start, Object value) {
			if ("cssText".equals(name)) {
				node.setAttr("style", Context.toString(value));
				changed();
				return;
			}
			Map<String, String> decls = parseStyle(node.attr("style"));
			String prop = kebab(name);
			String v = styleValue(prop, value);
			if (v.isEmpty()) decls.remove(prop);
			else decls.put(prop, v);
			node.setAttr("style", writeStyle(decls));
			changed();
		}
	}

	/** Backs element.dataset: data-* attributes with camelCase keys. */
	private final class Dataset extends ScriptableObject {
		private final Node node;

		Dataset(Node node) {
			this.node = node;
			setParentScope(JsBridge.this.scope);
			setPrototype(getObjectPrototype(JsBridge.this.scope));
		}

		@Override
		public String getClassName() {
			return "DOMStringMap";
		}

		@Override
		public Object get(String name, Scriptable start) {
			String attr = "data-" + kebab(name);
			return node.hasAttr(attr) ? node.attr(attr) : super.get(name, start);
		}

		@Override
		public void put(String name, Scriptable start, Object value) {
			node.setAttr("data-" + kebab(name), Context.toString(value));
			changed();
		}
	}

	/** The global document object. */
	private final class Document extends ScriptableObject {
		private final Object getById;
		private final Object query;
		private final Object queryAll;

		Document() {
			setParentScope(JsBridge.this.scope);
			setPrototype(getObjectPrototype(JsBridge.this.scope));
			getById = fn("getElementById", a -> {
				String id = argString(a, 0);
				return id.isEmpty() ? null : wrap(findIdFast(id));
			});
			query = fn("querySelector", a -> wrap(findFirst(view.root(), compile(argString(a, 0)))));
			queryAll = fn("querySelectorAll", a -> {
				List<Object> out = new ArrayList<>();
				collect(view.root(), compile(argString(a, 0)), out);
				return newArray(out);
			});
		}

		@Override
		public String getClassName() {
			return "HTMLDocument";
		}

		@Override
		public Object get(String name, Scriptable start) {
			switch (name) {
				case "getElementById":
					return getById;
				case "querySelector":
					return query;
				case "querySelectorAll":
					return queryAll;
				case "body":
					return wrap(body());
				case "documentElement":
					return wrap(view.root().firstElement());
				default:
					return super.get(name, start);
			}
		}
	}
}
