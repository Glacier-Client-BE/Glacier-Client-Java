package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;

/** "Light: 12" on the block you stand in. Below 8, hostile mobs can spawn there. */
public class LightLevelHud extends CachedRowsHud {
	private final ToggleSetting label = add(new ToggleSetting("Show Label", true));

	public LightLevelHud() {
		super("Light Level", "Shows the light level of the block you are standing in.", "Light: --");
		stackTopLeft(18);
	}

	@Override
	protected boolean pill() {
		return true;
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.level == null) {
			setRows(single(part("Light: --", TEXT)));
			return;
		}
		int light = mc.level.getMaxLocalRawBrightness(mc.player.blockPosition());
		int color = goodHigh(light, 8, 1);
		if (label.on()) setRows(single(part("Light: ", TEXT), part(String.valueOf(light), color)));
		else setRows(single(part(String.valueOf(light), color)));
	}
}
