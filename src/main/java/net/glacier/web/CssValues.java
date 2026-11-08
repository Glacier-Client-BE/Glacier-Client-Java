package net.glacier.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** CSS value helpers: colours, lengths, token splitting and var() substitution. Stateless. */
final class CssValues {
	private CssValues() {}

	/** Resolves var(--x[, fallback]) references against the variable map (recursive). */
	static String resolveVars(String value, Map<String, String> vars) {
		return resolveVars(value, vars, 0);
	}

	private static String resolveVars(String value, Map<String, String> vars, int depth) {
		if (value == null || !value.contains("var(") || depth > 8) return value;
		StringBuilder out = new StringBuilder();
		int i = 0;
		while (i < value.length()) {
			if (value.startsWith("var(", i)) {
				int close = matchParen(value, i + 3);
				if (close < 0) { out.append(value.substring(i)); break; }
				String inner = value.substring(i + 4, close).trim();
				int comma = inner.indexOf(',');
				String name = comma < 0 ? inner : inner.substring(0, comma).trim();
				String fallback = comma < 0 ? "" : inner.substring(comma + 1).trim();
				String v = vars.get(name);
				out.append(resolveVars(v != null ? v : fallback, vars, depth + 1));
				i = close + 1;
			} else {
				out.append(value.charAt(i++));
			}
		}
		return out.toString();
	}

	private static int matchParen(String s, int open) {
		int depth = 0;
		for (int k = open; k < s.length(); k++) {
			char c = s.charAt(k);
			if (c == '(') depth++;
			else if (c == ')' && --depth == 0) return k;
		}
		return -1;
	}

	/** Parses a CSS color to ARGB, or null when it is not a color (e.g. currentColor). */
	static Integer parseColor(String v) {
		if (v == null) return null;
		v = v.trim().toLowerCase(Locale.ROOT);
		if (v.isEmpty()) return null;
		if (v.charAt(0) == '#') return hexColor(v.substring(1));
		int open = v.indexOf('(');
		if (open > 0 && v.endsWith(")")) {
			String fn = v.substring(0, open).trim();
			String[] a = v.substring(open + 1, v.length() - 1).trim().split("[,/\\s]+");
			if (fn.equals("rgb") || fn.equals("rgba")) return rgbColor(a);
			if (fn.equals("hsl") || fn.equals("hsla")) return hslColor(a);
			return null;
		}
		return switch (v) {
			case "transparent" -> 0;
			case "white" -> 0xFFFFFFFF;
			case "black" -> 0xFF000000;
			case "red" -> 0xFFFF0000;
			case "green" -> 0xFF008000;
			case "blue" -> 0xFF0000FF;
			case "yellow" -> 0xFFFFFF00;
			case "orange" -> 0xFFFFA500;
			case "purple" -> 0xFF800080;
			case "gray", "grey" -> 0xFF808080;
			default -> null;
		};
	}

	private static Integer hexColor(String h) {
		try {
			if (h.length() == 3 || h.length() == 4) {
				int r = Integer.parseInt(h.substring(0, 1), 16) * 17;
				int g = Integer.parseInt(h.substring(1, 2), 16) * 17;
				int b = Integer.parseInt(h.substring(2, 3), 16) * 17;
				int a = h.length() == 4 ? Integer.parseInt(h.substring(3, 4), 16) * 17 : 255;
				return argb(a, r, g, b);
			}
			if (h.length() == 6) return 0xFF000000 | Integer.parseInt(h, 16);
			if (h.length() == 8) {
				long v = Long.parseLong(h, 16);
				return (int) (((v & 0xFF) << 24) | (v >>> 8));
			}
		} catch (NumberFormatException ignored) {
		}
		return null;
	}

	private static Integer rgbColor(String[] a) {
		if (a.length < 3) return null;
		try {
			int r = channel(a[0]), g = channel(a[1]), b = channel(a[2]);
			int alpha = 255;
			if (a.length >= 4) alpha = alphaByte(a[3]);
			return argb(alpha, r, g, b);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int channel(String s) {
		float f = s.endsWith("%") ? Float.parseFloat(s.substring(0, s.length() - 1)) * 2.55f : Float.parseFloat(s);
		return Math.round(Math.max(0, Math.min(255, f)));
	}

	private static int alphaByte(String s) {
		float f = s.endsWith("%") ? Float.parseFloat(s.substring(0, s.length() - 1)) / 100f : Float.parseFloat(s);
		return Math.round(Math.max(0, Math.min(1, f)) * 255);
	}

	private static Integer hslColor(String[] a) {
		if (a.length < 3) return null;
		try {
			float h = Float.parseFloat(a[0].replace("deg", "")) % 360f;
			if (h < 0) h += 360f;
			float s = Math.max(0, Math.min(1, Float.parseFloat(a[1].replace("%", "")) / 100f));
			float l = Math.max(0, Math.min(1, Float.parseFloat(a[2].replace("%", "")) / 100f));
			float c = (1 - Math.abs(2 * l - 1)) * s;
			float x = c * (1 - Math.abs((h / 60f) % 2 - 1));
			float m = l - c / 2f;
			float r, g, b;
			if (h < 60) { r = c; g = x; b = 0; }
			else if (h < 120) { r = x; g = c; b = 0; }
			else if (h < 180) { r = 0; g = c; b = x; }
			else if (h < 240) { r = 0; g = x; b = c; }
			else if (h < 300) { r = x; g = 0; b = c; }
			else { r = c; g = 0; b = x; }
			int alpha = a.length >= 4 ? alphaByte(a[3]) : 255;
			return argb(alpha, Math.round((r + m) * 255), Math.round((g + m) * 255), Math.round((b + m) * 255));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	static int argb(int a, int r, int g, int b) {
		return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
	}

	/** Length in px. em uses {@code emBase}; NaN for auto, percentages, none or unparseable values. */
	static float px(String v, float emBase) {
		if (v == null) return Float.NaN;
		v = v.trim().toLowerCase(Locale.ROOT);
		if (v.isEmpty() || v.equals("auto") || v.equals("none") || v.endsWith("%")) return Float.NaN;
		try {
			if (v.endsWith("px")) return Float.parseFloat(v.substring(0, v.length() - 2));
			if (v.endsWith("rem")) return Float.parseFloat(v.substring(0, v.length() - 3)) * 16f;
			if (v.endsWith("em")) return Float.parseFloat(v.substring(0, v.length() - 2)) * emBase;
			return Float.parseFloat(v);
		} catch (NumberFormatException e) {
			return Float.NaN;
		}
	}

	/**
	 * The px value of a length that does not depend on the em base (plain number, px or rem), or NaN.
	 * Equal to {@code px(v, anyEm)} for those forms; used to pre-parse declarations at sheet-parse time.
	 */
	static float emFreePx(String v) {
		String t = v.trim().toLowerCase(Locale.ROOT);
		if (t.endsWith("px") || t.endsWith("rem") || t.endsWith("em") || t.endsWith("%")) {
			return t.endsWith("em") || t.endsWith("%") ? Float.NaN : px(t, 0f);
		}
		return isNumber(t) ? px(t, 0f) : Float.NaN;
	}

	/** Percentage as a fraction (50% -> 0.5); NaN if not a percentage. */
	static float pct(String v) {
		try {
			return Float.parseFloat(v.trim().substring(0, v.trim().length() - 1)) / 100f;
		} catch (RuntimeException e) {
			return Float.NaN;
		}
	}

	static boolean isPct(String v) {
		return v != null && v.trim().endsWith("%");
	}

	/** Same language as {@code v.matches("[+-]?(\\d+\\.?\\d*|\\.\\d+)")}, without a regex. */
	static boolean isNumber(String v) {
		if (v == null) return false;
		int i = 0, n = v.length();
		if (i < n && (v.charAt(i) == '+' || v.charAt(i) == '-')) i++;
		int digits = 0;
		while (i < n && isDigit(v.charAt(i))) { i++; digits++; }
		if (digits > 0) {
			if (i < n && v.charAt(i) == '.') {
				i++;
				while (i < n && isDigit(v.charAt(i))) i++;
			}
			return i == n;
		}
		if (i < n && v.charAt(i) == '.') {
			i++;
			int frac = 0;
			while (i < n && isDigit(v.charAt(i))) { i++; frac++; }
			return frac > 0 && i == n;
		}
		return false;
	}

	private static boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	static float parseNumber(String v) {
		try {
			return Float.parseFloat(v.trim());
		} catch (RuntimeException e) {
			return Float.NaN;
		}
	}

	static float orDef(float f, float def) {
		return Float.isNaN(f) ? def : f;
	}

	static int firstInt(String s, int def) {
		String t = s.trim();
		int end = 0;
		while (end < t.length() && Character.isDigit(t.charAt(end))) end++;
		try {
			return end == 0 ? def : Integer.parseInt(t.substring(0, end));
		} catch (NumberFormatException e) {
			return def;
		}
	}

	/**
	 * 1-4 value box shorthand written into {@code out} as [top, right, bottom, left]; NaN for auto.
	 * Returns false (and leaves out untouched) when the value is empty or has more than four parts.
	 */
	static boolean boxInto(String v, float em, float[] out) {
		List<String> t = tokens(v);
		if (t.isEmpty() || t.size() > 4) return false;
		float a = px(t.get(0), em);
		float b = t.size() > 1 ? px(t.get(1), em) : a;
		float c = t.size() > 2 ? px(t.get(2), em) : a;
		float d = t.size() > 3 ? px(t.get(3), em) : b;
		out[0] = a;
		out[1] = b;
		out[2] = c;
		out[3] = d;
		return true;
	}

	/** Splits on whitespace that is not inside parentheses or quotes. */
	static List<String> tokens(String v) {
		List<String> out = new ArrayList<>();
		if (v == null) return out;
		int depth = 0;
		char quote = 0;
		StringBuilder cur = new StringBuilder();
		for (int k = 0; k < v.length(); k++) {
			char c = v.charAt(k);
			if (quote != 0) {
				cur.append(c);
				if (c == quote) quote = 0;
				continue;
			}
			if (c == '\'' || c == '"') { quote = c; cur.append(c); continue; }
			if (c == '(') depth++;
			else if (c == ')') depth--;
			if (Character.isWhitespace(c) && depth == 0) {
				if (cur.length() > 0) { out.add(cur.toString()); cur.setLength(0); }
			} else {
				cur.append(c);
			}
		}
		if (cur.length() > 0) out.add(cur.toString());
		return out;
	}

	/** Splits on a separator that is not inside parentheses. */
	static List<String> splitTop(String v, char sep) {
		List<String> out = new ArrayList<>();
		int depth = 0, start = 0;
		for (int i = 0; i < v.length(); i++) {
			char c = v.charAt(i);
			if (c == '(') depth++;
			else if (c == ')') depth--;
			else if (c == sep && depth == 0) {
				out.add(v.substring(start, i));
				start = i + 1;
			}
		}
		out.add(v.substring(start));
		return out;
	}

	static Css.Gradient gradient(String v) {
		int open = v.indexOf('(');
		int close = v.lastIndexOf(')');
		if (open < 0 || close < open) return null;
		List<String> args = splitTop(v.substring(open + 1, close), ',');
		Css.Gradient g = new Css.Gradient();
		int start = 0;
		if (!args.isEmpty()) {
			String a = args.get(0).trim().toLowerCase(Locale.ROOT);
			if (a.endsWith("deg") && isNumber(a.substring(0, a.length() - 3))) {
				g.angleDeg = Float.parseFloat(a.substring(0, a.length() - 3));
				start = 1;
			} else if (a.startsWith("to ")) {
				g.angleDeg = toAngle(a.substring(3));
				start = 1;
			}
		}
		for (int i = start; i < args.size(); i++) {
			List<String> t = tokens(args.get(i));
			if (t.isEmpty()) continue;
			Integer c = parseColor(t.get(0));
			if (c == null) continue;
			float pos = t.size() > 1 && isPct(t.get(1)) ? pct(t.get(1)) : Float.NaN;
			g.colors.add(c);
			g.stops.add(pos);
		}
		return g.colors.isEmpty() ? null : g;
	}

	private static float toAngle(String dir) {
		boolean top = dir.contains("top"), bottom = dir.contains("bottom");
		boolean left = dir.contains("left"), right = dir.contains("right");
		if (top && right) return 45f;
		if (bottom && right) return 135f;
		if (bottom && left) return 225f;
		if (top && left) return 315f;
		if (right) return 90f;
		if (left) return 270f;
		if (top) return 0f;
		return 180f;
	}
}
