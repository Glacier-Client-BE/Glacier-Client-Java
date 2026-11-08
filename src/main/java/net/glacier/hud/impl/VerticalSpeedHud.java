package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Theme;

import java.util.Locale;

/** "Vert: -2.31 b/s", your vertical velocity. Negative is falling, and falls faster than 3 b/s turn yellow. */
public class VerticalSpeedHud extends CachedRowsHud {
	private final ToggleSetting label = add(new ToggleSetting("Show Label", true));

	public VerticalSpeedHud() {
		super("Vertical Speed", "Shows how fast you are rising or falling.", "Vert: --");
		stackTopLeft(25);
	}

	@Override
	protected boolean pill() {
		return true;
	}

	@Override
	public void onTick() {
		if (mc.player == null) {
			setRows(single(part("Vert: --", TEXT)));
			return;
		}
		double vy = mc.player.getDeltaMovement().y * 20.0;
		int color = vy < -3.0 ? Theme.HUD_WARN : TEXT;
		String value = String.format(Locale.ROOT, "%+.2f b/s", vy);
		if (label.on()) setRows(single(part("Vert: ", TEXT), part(value, color)));
		else setRows(single(part(value, color)));
	}
}
