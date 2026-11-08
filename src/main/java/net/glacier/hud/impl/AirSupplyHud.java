package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;

import java.util.Locale;

/** "Air: 12.4 s" of breath left, or "Air: full" when none is in use. Green above half, red below a quarter. */
public class AirSupplyHud extends CachedRowsHud {
	private final ToggleSetting label = add(new ToggleSetting("Show Label", true));

	public AirSupplyHud() {
		super("Air Supply", "Shows your remaining breath in seconds while underwater.", "Air: --");
		stackTopLeft(26);
	}

	@Override
	protected boolean pill() {
		return true;
	}

	@Override
	public void onTick() {
		if (mc.player == null) {
			setRows(single(part("Air: --", TEXT)));
			return;
		}
		int air = mc.player.getAirSupply();
		int max = Math.max(1, mc.player.getMaxAirSupply());
		String value = air >= max ? "full" : String.format(Locale.ROOT, "%.1f s", Math.max(0, air) / 20.0);
		int color = goodHigh(air * 100.0 / max, 50, 25);
		if (label.on()) setRows(single(part("Air: ", TEXT), part(value, color)));
		else setRows(single(part(value, color)));
	}
}
