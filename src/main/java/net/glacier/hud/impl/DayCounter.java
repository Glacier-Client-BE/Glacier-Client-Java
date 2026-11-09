package net.glacier.hud.impl;

import net.glacier.setting.ChoiceSetting;

/** The current in-game day, as "Day 12", a bare number, or "Day 12  06:30" with the time of day. */
public class DayCounter extends CachedRowsHud {
	private static final long DAY_TICKS = 24000L;
	/** The world clock starts at 6:00 on tick 0, and one in-game hour is 1000 ticks. */
	private static final int START_HOUR = 6;

	private final ChoiceSetting style = add(new ChoiceSetting("Style", "Day N", "Day N", "Number Only", "Day N + Time"));
	private long lastKey = -1;

	public DayCounter() {
		super("Day Counter", "Shows the current in-game day.", "Day 0");
		stackTopLeft(11);
	}

	@Override
	public void onTick() {
		if (mc.level == null) return;
		long time = mc.level.getOverworldClockTime();
		long day = time / DAY_TICKS;
		boolean withTime = style.is("Day N + Time");
		long minuteOfDay = withTime ? ((time % DAY_TICKS) * 60L / 1000L + START_HOUR * 60L) % (24 * 60L) : 0;
		long key = day * 8192 + minuteOfDay * 4 + style.index();
		if (key == lastKey) return;
		lastKey = key;
		String text = style.is("Number Only") ? String.valueOf(day) : "Day " + day;
		if (withTime) text += "  " + two(minuteOfDay / 60) + ":" + two(minuteOfDay % 60);
		setRows(single(part(text, TEXT)));
	}

	private static String two(long n) {
		return n < 10 ? "0" + n : String.valueOf(n);
	}
}
