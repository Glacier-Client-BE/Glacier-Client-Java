package net.glacier.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A forgiving HTML parser producing a {@link Node} tree. Text of every
 * {@code <style>} element is collected into the root's {@code styleText}; the
 * style and script elements themselves are not kept in the tree.
 */
public final class HtmlParser {
	private static final Set<String> VOID = Set.of(
			"area", "base", "br", "col", "embed", "hr", "img", "input",
			"link", "meta", "param", "source", "track", "wbr");
	/** Elements dropped from the tree; their children are still parsed (for nested styles). */
	private static final Set<String> DROP = Set.of(
			"head", "title", "meta", "link", "base", "template", "noscript");
	private static final Map<String, String> ENTITIES = Map.ofEntries(
			Map.entry("amp", "&"), Map.entry("lt", "<"), Map.entry("gt", ">"),
			Map.entry("quot", "\""), Map.entry("apos", "'"), Map.entry("nbsp", " "),
			Map.entry("times", "×"), Map.entry("middot", "·"), Map.entry("copy", "©"),
			Map.entry("hellip", "…"), Map.entry("mdash", "—"), Map.entry("ndash", "–"),
			Map.entry("rarr", "→"), Map.entry("larr", "←"), Map.entry("check", "✓"),
			Map.entry("bull", "•"));

	private final String src;
	private final int len;
	private final StringBuilder styleBuf = new StringBuilder();
	private final StringBuilder text = new StringBuilder();
	private final List<String> open = new ArrayList<>();
	private int i;

	private HtmlParser(String src) {
		this.src = src;
		this.len = src.length();
	}

	/** Parses an HTML document or fragment. The returned root has tag "#root". */
	public static Node parse(String html) {
		HtmlParser p = new HtmlParser(html == null ? "" : html);
		Node root = new Node("#root");
		p.parseChildren(root);
		root.styleText = p.styleBuf.toString();
		return root;
	}

	private void parseChildren(Node parent) {
		// Shared text buffer: every path flushes it before recursing or returning, so it is empty at each nesting level.
		StringBuilder buf = text;
		while (i < len) {
			char c = src.charAt(i);
			if (c == '<' && i + 1 < len) {
				char d = src.charAt(i + 1);
				if (src.startsWith("<!--", i)) {
					flush(parent, buf);
					i = skipPast("-->", i + 4);
					continue;
				}
				if (d == '!' || d == '?') {
					flush(parent, buf);
					i = skipPast(">", i + 1);
					continue;
				}
				if (d == '/') {
					flush(parent, buf);
					int end = src.indexOf('>', i);
					String name = end < 0 ? "" : src.substring(i + 2, end).trim().toLowerCase(Locale.ROOT);
					if (parent.tag.equals(name)) {
						i = end < 0 ? len : end + 1;
						return;
					}
					if (open.contains(name)) return;   // closes an ancestor; leave the tag for it
					i = end < 0 ? len : end + 1;        // stray close tag
					continue;
				}
				if (Character.isLetter(d)) {
					flush(parent, buf);
					Node el = parseElement();
					if (el != null) parent.add(el);
					continue;
				}
			}
			if (c != '<') {
				// Plain text up to the next '<' in one copy.
				int next = src.indexOf('<', i);
				int end = next < 0 ? len : next;
				buf.append(src, i, end);
				i = end;
			} else {
				buf.append(c);
				i++;
			}
		}
		flush(parent, buf);
	}

	private static void flush(Node parent, StringBuilder buf) {
		if (buf.length() == 0) return;
		String t = decode(collapseSpaces(buf));
		buf.setLength(0);
		if (!t.isBlank()) parent.add(Node.text(t));
	}

	/** Same result as {@code s.replaceAll("\\s+", " ")} (regex whitespace: space, tab, LF, VT, FF, CR), without a regex. */
	static String collapseSpaces(CharSequence s) {
		StringBuilder out = new StringBuilder(s.length());
		boolean ws = false;
		for (int k = 0; k < s.length(); k++) {
			char c = s.charAt(k);
			if (c == ' ' || c == '\t' || c == '\n' || c == '\u000B' || c == '\f' || c == '\r') {
				if (!ws) out.append(' ');
				ws = true;
			} else {
				out.append(c);
				ws = false;
			}
		}
		return out.toString();
	}

	/** Decodes named and numeric character references; unknown ones are kept verbatim. */
	public static String decode(String s) {
		if (s.indexOf('&') < 0) return s;
		StringBuilder out = new StringBuilder(s.length());
		int k = 0;
		while (k < s.length()) {
			char c = s.charAt(k);
			if (c == '&') {
				int semi = s.indexOf(';', k);
				if (semi > k && semi - k <= 12) {
					String rep = entity(s.substring(k + 1, semi));
					if (rep != null) {
						out.append(rep);
						k = semi + 1;
						continue;
					}
				}
			}
			out.append(c);
			k++;
		}
		return out.toString();
	}

	private static String entity(String ent) {
		if (ent.startsWith("#")) {
			try {
				int cp = ent.startsWith("#x") || ent.startsWith("#X")
						? Integer.parseInt(ent.substring(2), 16)
						: Integer.parseInt(ent.substring(1));
				return new String(Character.toChars(cp));
			} catch (RuntimeException e) {
				return null;
			}
		}
		return ENTITIES.get(ent);
	}

	/** Parses one element starting at '<'. Returns null when the element is dropped. */
	private Node parseElement() {
		i++; // '<'
		int start = i;
		while (i < len && isNameChar(src.charAt(i))) i++;
		String tag = src.substring(start, i).toLowerCase(Locale.ROOT);
		Node el = new Node(tag);
		parseAttributes(el);
		boolean selfClose = false;
		if (i < len && src.charAt(i) == '/') {
			selfClose = true;
			i++;
		}
		if (i < len && src.charAt(i) == '>') i++;
		boolean drop = DROP.contains(tag);

		if (selfClose || VOID.contains(tag)) return drop ? null : el;

		if (tag.equals("style") || tag.equals("script")) {
			int end = indexOfIgnoreCase("</" + tag, i);
			String raw = end < 0 ? src.substring(i) : src.substring(i, end);
			if (tag.equals("style")) styleBuf.append(raw).append('\n');
			i = end < 0 ? len : skipPast(">", end);
			return null;
		}
		if (tag.equals("svg")) {
			// Icons are not rendered: keep the element for its size attributes, drop its contents.
			int end = indexOfIgnoreCase("</svg", i);
			i = end < 0 ? len : skipPast(">", end);
			return el;
		}

		open.add(tag);
		parseChildren(el);
		open.remove(open.size() - 1);
		return drop ? null : el;
	}

	private void parseAttributes(Node el) {
		while (i < len) {
			skipWs();
			if (i >= len) return;
			char c = src.charAt(i);
			if (c == '>' || c == '/') return;
			int start = i;
			while (i < len && isAttrNameChar(src.charAt(i))) i++;
			if (i == start) {
				i++;
				continue;
			}
			String name = src.substring(start, i).toLowerCase(Locale.ROOT);
			skipWs();
			String value = "";
			if (i < len && src.charAt(i) == '=') {
				i++;
				skipWs();
				if (i < len && (src.charAt(i) == '"' || src.charAt(i) == '\'')) {
					char q = src.charAt(i++);
					int vs = i;
					while (i < len && src.charAt(i) != q) i++;
					value = src.substring(vs, i);
					if (i < len) i++;
				} else {
					int vs = i;
					while (i < len && !Character.isWhitespace(src.charAt(i)) && src.charAt(i) != '>') i++;
					value = src.substring(vs, i);
				}
			}
			el.setAttr(name, decode(value));
		}
	}

	private void skipWs() {
		while (i < len && Character.isWhitespace(src.charAt(i))) i++;
	}

	/** Index just past the next occurrence of {@code marker} at or after {@code from}, or len. */
	private int skipPast(String marker, int from) {
		int idx = src.indexOf(marker, from);
		return idx < 0 ? len : idx + marker.length();
	}

	private static boolean isNameChar(char c) {
		return Character.isLetterOrDigit(c) || c == '-' || c == '_';
	}

	private static boolean isAttrNameChar(char c) {
		return !Character.isWhitespace(c) && c != '=' && c != '>' && c != '/' && c != '<' && c != '"' && c != '\'';
	}

	private int indexOfIgnoreCase(String needle, int from) {
		int n = needle.length();
		for (int k = from; k + n <= len; k++) {
			if (src.regionMatches(true, k, needle, 0, n)) return k;
		}
		return -1;
	}
}
