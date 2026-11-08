package net.glacier.web;

/** Selector matching against a node. Stateless; the rule index only narrows which selectors are tried. */
final class CssSelectorMatch {
	private CssSelectorMatch() {}

	/** True when the compound part matches the element (text nodes never match). */
	static boolean compound(Css.Compound c, Node n) {
		if (n.isText() || c.never) return false;
		if (!c.tag.isEmpty() && !c.tag.equals(n.tag)) return false;
		if (!c.id.isEmpty() && !c.id.equals(n.id)) return false;
		for (String cl : c.classes) if (!n.hasClass(cl)) return false;
		for (String[] a : c.attrs) {
			if (!n.hasAttr(a[0])) return false;
			if (a[1] != null && !a[1].equals(n.attr(a[0]))) return false;
		}
		for (String p : c.pseudos) if (!pseudo(p, n)) return false;
		return true;
	}

	/** True when the whole selector matches the element. */
	static boolean selector(Css.Selector s, Node n) {
		return !s.parts.isEmpty() && matchFrom(s, s.parts.size() - 1, n);
	}

	private static boolean matchFrom(Css.Selector s, int idx, Node n) {
		Css.Compound c = s.parts.get(idx);
		if (!compound(c, n)) return false;
		if (idx == 0) return true;
		if (c.combinator == '>') return n.parent != null && matchFrom(s, idx - 1, n.parent);
		for (Node p = n.parent; p != null; p = p.parent) {
			if (matchFrom(s, idx - 1, p)) return true;
		}
		return false;
	}

	private static boolean pseudo(String p, Node n) {
		return switch (p) {
			case "root" -> n.parent == null || "#root".equals(n.parent.tag);
			case "first-child" -> n.parent != null && n.parent.firstElement() == n;
			case "last-child" -> n.parent != null && lastElement(n.parent) == n;
			case "hover" -> n.hovered;
			case "active" -> n.active;
			default -> false;
		};
	}

	private static Node lastElement(Node p) {
		for (int i = p.children.size() - 1; i >= 0; i--) {
			Node c = p.children.get(i);
			if (!c.isText()) return c;
		}
		return null;
	}
}
