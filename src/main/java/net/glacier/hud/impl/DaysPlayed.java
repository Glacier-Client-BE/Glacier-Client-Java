package net.glacier.hud.impl;

import net.glacier.setting.SliderSetting;

import java.time.LocalDate;

/**
 * Counts the calendar days on which the client has run. The count and the last counted day are hidden
 * settings, so they persist through config.json.
 */
public class DaysPlayed extends CachedRowsHud {
	private final SliderSetting days = add(new SliderSetting("Days Played Count", 0, 0, 1000000, 1));
	private final SliderSetting lastDay = add(new SliderSetting("Last Counted Day", 0, 0, 1000000000, 1));
	private int shown = -1;

	public DaysPlayed() {
		super("Days Played", "Counts how many days you have played.", "Days Played: 0");
		days.hide();
		lastDay.hide();
		stackTopLeft(5);
	}

	@Override
	public void onTick() {
		int today = (int) LocalDate.now().toEpochDay();
		if (lastDay.i() != today) {
			lastDay.set((double) today);
			days.set((double) (days.i() + 1));
		}
		int n = days.i();
		if (n == shown) return;
		shown = n;
		setRows(single(part("Days Played: " + n, TEXT)));
	}
}
