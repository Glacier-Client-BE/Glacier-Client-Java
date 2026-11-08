package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;

/** "Entities: 214", the entities loaded in your world. Refreshed once per second. */
public class EntityCountHud extends CachedRowsHud {
	private final ToggleSetting label = add(new ToggleSetting("Show Label", true));
	private int ticks;

	public EntityCountHud() {
		super("Entity Count", "Shows how many entities are loaded in your world.", "Entities: -");
		stackTopLeft(22);
	}

	@Override
	protected boolean pill() {
		return true;
	}

	@Override
	public void onTick() {
		if (ticks-- > 0) return;
		ticks = 19;
		if (mc.level == null) {
			setRows(single(part("Entities: -", TEXT)));
			return;
		}
		String n = String.valueOf(mc.level.getEntityCount());
		if (label.on()) setRows(single(part("Entities: ", TEXT), part(n, TEXT)));
		else setRows(single(part(n, TEXT)));
	}
}
