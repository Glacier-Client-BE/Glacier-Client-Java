package net.glacier.hud.impl;

import net.glacier.ui.Theme;

/**
 * Reusable template for single-line "Label: value" readouts. Subclasses only supply {@link #value()}; the
 * text is rebuilt on tick and only when it changed, so rendering never allocates.
 */
abstract class LabelValueHud extends CachedRowsHud {
	private final String label;
	private String last = "";

	LabelValueHud(String name, String desc, String label, int order) {
		super(name, desc, label + ": -");
		this.label = label;
		stackTopLeft(order);
	}

	/** Current value text, or null to keep showing the previous one. */
	protected abstract String value();

	/** Colour of the value part; default is the configured text colour. */
	protected int valueColor() {
		return TEXT;
	}

	@Override
	protected boolean pill() {
		return true;
	}

	@Override
	public void onTick() {
		String v = value();
		if (v == null || v.equals(last)) return;
		last = v;
		setRows(single(part(label + ": ", Theme.HUD_MUTED), part(v, valueColor())));
	}
}
