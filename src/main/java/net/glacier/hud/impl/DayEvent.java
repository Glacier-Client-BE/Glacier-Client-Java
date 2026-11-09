package net.glacier.hud.impl;

/** In-game day, clock time (24 h) and moon phase, from the world time. */
public class DayEvent extends CachedRowsHud {
	private static final String[] PHASES = {
			"Full Moon", "Waning Gibbous", "Third Quarter", "Waning Crescent",
			"New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous"
	};
	private String last = "";

	public DayEvent() {
		super("Day Event", "Shows the in-game day, the time of day and the moon phase.", "Day 0");
		stackTopLeft(6);
	}

	@Override
	public void onTick() {
		if (mc.level == null) return;
		long time = mc.level.getOverworldClockTime();
		long day = time / 24000L;
		long tick = time % 24000L;
		int hours = (int) ((tick / 1000L + 6) % 24);
		int minutes = (int) ((tick % 1000L) * 60 / 1000L);
		String s = "Day " + day + "  " + pad2(hours) + ":" + pad2(minutes) + "  " + moonPhase(day);
		if (s.equals(last)) return;
		last = s;
		setRows(single(part(s, TEXT)));
	}

	/** Moon phase name for an in-game day. */
	static String moonPhase(long day) {
		return PHASES[(int) (day % 8)];
	}

	private static String pad2(int v) {
		return (v < 10 ? "0" : "") + v;
	}
}
