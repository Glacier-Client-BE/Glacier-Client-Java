package net.glacier.web;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Stylesheet, selector and declaration parsing. Stateless; every method is static. */
final class CssParser {
	private CssParser() {}

	private static final Set<String> SUPPORTED_PSEUDO = new HashSet<>(Arrays.asList(
			"root", "first-child", "last-child", "hover", "active"));

	/** Parses a stylesheet. @media/@keyframes/@font-face blocks are ignored. */
	static Css.Stylesheet parseSheet(String css) {
		Css.Stylesheet sheet = new Css.Stylesheet();
		if (css == null) return sheet;
		css = stripComments(css);
		int i = 0, n = css.length();
		while (i < n) {
			int brace = css.indexOf('{', i);
			int semi = css.indexOf(';', i);
			if (semi >= 0 && (brace < 0 || semi < brace)) { i = semi + 1; continue; } // @import x;
			if (brace < 0) break;
			String prelude = css.substring(i, brace).trim();
			int end = matchBrace(css, brace);
			String body = css.substring(brace + 1, end);
			i = end + 1;
			if (prelude.startsWith("@")) continue;

			List<Css.Decl> decls = parseDeclarations(body);
			for (Css.Decl d : decls) if (d.custom) sheet.vars.put(d.prop, d.value);

			Css.Rule rule = new Css.Rule();
			rule.decls.addAll(decls);
			for (String sel : prelude.split(",")) {
				Css.Selector s = parseSelector(sel.trim());
				if (s != null) rule.selectors.add(s);
			}
			if (!rule.selectors.isEmpty()) sheet.rules.add(rule);
		}
		return sheet;
	}

	/** Index of the '}' matching the '{' at {@code open}, or css.length(). */
	private static int matchBrace(String css, int open) {
		int depth = 0;
		for (int k = open; k < css.length(); k++) {
			char c = css.charAt(k);
			if (c == '{') depth++;
			else if (c == '}' && --depth == 0) return k;
		}
		return css.length();
	}

	static List<Css.Decl> parseDeclarations(String body) {
		List<Css.Decl> out = new java.util.ArrayList<>();
		for (String decl : body.split(";")) {
			int colon = decl.indexOf(':');
			if (colon < 0) continue;
			String prop = decl.substring(0, colon).trim().toLowerCase(Locale.ROOT);
			String val = decl.substring(colon + 1).replace("!important", "").trim();
			if (!prop.isEmpty() && !val.isEmpty()) out.add(new Css.Decl(prop, val));
		}
		return out;
	}

	private static Css.Selector parseSelector(String s) {
		Css.Selector sel = new Css.Selector();
		int i = 0, n = s.length();
		char comb = ' ';
		while (i < n) {
			char c = s.charAt(i);
			if (Character.isWhitespace(c)) { i++; continue; }
			if (c == '>') { comb = '>'; i++; continue; }
			Css.Compound cp = new Css.Compound();
			cp.combinator = sel.parts.isEmpty() ? ' ' : comb;
			comb = ' ';
			while (i < n) {
				c = s.charAt(i);
				if (Character.isWhitespace(c) || c == '>') break;
				if (c == '.' || c == '#') {
					int st = ++i;
					while (i < n && isIdent(s.charAt(i))) i++;
					String name = s.substring(st, i);
					if (name.isEmpty()) return null;
					if (c == '.') cp.classes.add(name); else cp.id = name;
				} else if (c == '[') {
					int end = s.indexOf(']', i);
					if (end < 0) return null;
					String body = s.substring(i + 1, end).trim();
					i = end + 1;
					int eq = body.indexOf('=');
					String an = (eq < 0 ? body : body.substring(0, eq)).trim().toLowerCase(Locale.ROOT);
					String av = eq < 0 ? null : body.substring(eq + 1).trim().replace("\"", "").replace("'", "");
					if (an.isEmpty()) return null;
					cp.attrs.add(new String[]{an, av});
				} else if (c == ':') {
					int st = ++i;
					while (i < n && (isIdent(s.charAt(i)) || s.charAt(i) == ':')) i++;
					String name = s.substring(st, i).toLowerCase(Locale.ROOT);
					if (i < n && s.charAt(i) == '(') {
						int close = s.indexOf(')', i);
						if (close < 0) return null;
						i = close + 1;
						cp.never = true;                       // :not(), :is() etc. unsupported
					} else if (name.startsWith(":") || !SUPPORTED_PSEUDO.contains(name)) {
						cp.never = true;                       // pseudo-elements, :focus, ...
					} else {
						cp.pseudos.add(name);
					}
				} else if (c == '*') {
					i++;
				} else if (isIdent(c)) {
					int st = i;
					while (i < n && isIdent(s.charAt(i))) i++;
					cp.tag = s.substring(st, i).toLowerCase(Locale.ROOT);
				} else {
					return null;                               // + ~ " etc.
				}
			}
			sel.parts.add(cp);
		}
		if (sel.parts.isEmpty()) return null;
		int spec = 0;
		for (Css.Compound cp : sel.parts) {
			spec += (cp.id.isEmpty() ? 0 : 10000)
					+ (cp.classes.size() + cp.attrs.size() + cp.pseudos.size()) * 100
					+ (cp.tag.isEmpty() ? 0 : 1);
		}
		sel.spec = spec;
		return sel;
	}

	private static boolean isIdent(char c) {
		return Character.isLetterOrDigit(c) || c == '-' || c == '_';
	}

	private static String stripComments(String css) {
		if (css.indexOf("/*") < 0) return css;
		StringBuilder sb = new StringBuilder(css.length());
		int i = 0;
		while (i < css.length()) {
			if (css.startsWith("/*", i)) {
				int end = css.indexOf("*/", i + 2);
				i = end < 0 ? css.length() : end + 2;
			} else {
				sb.append(css.charAt(i++));
			}
		}
		return sb.toString();
	}
}
