package net.glacier.hud.impl;

import net.glacier.hud.RowsHudModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/**
 * Rows rebuilt on tick and cached, so rendering never allocates. Subclasses call {@link #setRows} or the
 * {@link #setReadout} shortcut, and may hide the whole element through {@link #hidden()}.
 */
abstract class CachedRowsHud extends RowsHudModule {
	private List<Part[]> rows;

	CachedRowsHud(String name, String desc, String placeholder) {
		super(name, desc);
		rows = single(part(placeholder, TEXT));
	}

	static List<Part[]> single(Part... row) {
		return List.<Part[]>of(row);
	}

	final void setRows(List<Part[]> rows) {
		this.rows = rows;
	}

	/**
	 * Shows one "Label: value suffix" row, or just the value when the label is off. The suffix keeps the text colour
	 * when labelled and takes the value colour when not. Allocates, so call it only when the text changed.
	 */
	final void setReadout(boolean showLabel, String label, String value, int valueColor, String suffix) {
		if (!showLabel) setRows(single(part(suffix.isEmpty() ? value : value + suffix, valueColor)));
		else if (suffix.isEmpty()) setRows(single(part(label + ": ", TEXT), part(value, valueColor)));
		else setRows(single(part(label + ": ", TEXT), part(value, valueColor), part(suffix, TEXT)));
	}

	/** Turns a registry path such as "dark_forest" into "Dark Forest". */
	static String pretty(String id) {
		StringBuilder sb = new StringBuilder(id.length());
		for (String word : id.replace('_', ' ').split(" ")) {
			if (word.isEmpty()) continue;
			if (sb.length() > 0) sb.append(' ');
			sb.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
		}
		return sb.toString();
	}

	/** True to skip drawing the element for now, for example when it has nothing to list. */
	protected boolean hidden() {
		return false;
	}

	@Override
	public void render(GuiGraphicsExtractor g, int sw, int sh) {
		if (!hidden()) super.render(g, sw, sh);
	}

	@Override
	protected final List<Part[]> rows() {
		return rows;
	}
}
