package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;

import java.util.Locale;

/** "Fall: 2.4 m" while falling, green under 3 blocks, yellow under 8, red past that (fall damage). */
public class FallDistanceHud extends CachedRowsHud {
	private final ToggleSetting label = add(new ToggleSetting("Show Label", true));

	public FallDistanceHud() {
		super("Fall Distance", "Shows how far you have fallen since you last touched the ground.", "Fall: --");
		stackTopLeft(19);
	}

	@Override
	protected boolean pill() {
		return true;
	}

	@Override
	public void onTick() {
		if (mc.player == null) {
			setRows(single(part("Fall: --", TEXT)));
			return;
		}
		double fall = mc.player.fallDistance;
		int color = goodLow(fall, 3, 8);
		String value = String.format(Locale.ROOT, "%.1f m", Math.max(0.0, fall));
		if (label.on()) setRows(single(part("Fall: ", TEXT), part(value, color)));
		else setRows(single(part(value, color)));
	}
}
