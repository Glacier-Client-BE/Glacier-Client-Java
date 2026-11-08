package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Theme;

/**
 * Reusable template for single-line "Label: value" readouts. Subclasses only supply {@link #value()}; the
 * text is rebuilt on tick and only when it or its colours changed, so rendering never allocates.
 */
abstract class LabelValueHud extends CachedRowsHud {
	private final String label;
	/** Shared by every label/value readout: turn off to show just the value. */
	private final ToggleSetting showLabel = add(new ToggleSetting("Show Label", true));
	private boolean lastShowLabel = true;
	private String last = "";
	private int lastValueColor;
	private int lastLabelColor;

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

	/** Colour of the label part; default is muted grey. */
	protected int labelColor() {
		return Theme.HUD_MUTED;
	}

	@Override
	protected boolean pill() {
		return true;
	}

	@Override
	public void onTick() {
		String v = value();
		if (v == null) return;
		int vc = valueColor();
		int lc = labelColor();
		boolean sl = showLabel.on();
		if (v.equals(last) && vc == lastValueColor && lc == lastLabelColor && sl == lastShowLabel) return;
		last = v;
		lastShowLabel = sl;
		lastValueColor = vc;
		lastLabelColor = lc;
		setRows(sl ? single(part(label + ": ", lc), part(v, vc)) : single(part(v, vc)));
	}
}
