package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Theme;

/** "Weather: Rain", coloured by state: clear green, rain yellow, thunder red. */
public class WeatherHud extends CachedRowsHud {
	private final ToggleSetting label = add(new ToggleSetting("Show Label", true));

	public WeatherHud() {
		super("Weather", "Shows whether it is clear, raining or thundering in your world.", "Weather: -");
		stackTopLeft(23);
	}

	@Override
	protected boolean pill() {
		return true;
	}

	@Override
	public void onTick() {
		if (mc.level == null) {
			setRows(single(part("Weather: -", TEXT)));
			return;
		}
		String state;
		int color;
		if (mc.level.isThundering()) {
			state = "Thunder";
			color = Theme.HUD_BAD;
		} else if (mc.level.isRaining()) {
			state = "Rain";
			color = Theme.HUD_WARN;
		} else {
			state = "Clear";
			color = Theme.HUD_GOOD;
		}
		if (label.on()) setRows(single(part("Weather: ", TEXT), part(state, color)));
		else setRows(single(part(state, color)));
	}
}
