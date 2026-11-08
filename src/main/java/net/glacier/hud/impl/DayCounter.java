package net.glacier.hud.impl;

/** "Day N" from the world time. */
public class DayCounter extends CachedRowsHud {
	private long lastDay = -1;

	public DayCounter() {
		super("Day Counter", "Shows the current in-game day.", "Day 0");
		stackTopLeft(11);
	}

	@Override
	public void onTick() {
		if (mc.level == null) return;
		long day = mc.level.getDayTime() / 24000L;
		if (day == lastDay) return;
		lastDay = day;
		setRows(single(part("Day " + day, TEXT)));
	}
}
