package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;

/**
 * "Avg: 144  Low: 61" over the last minute. The frame rate is sampled once a second, so "Low" is the worst second
 * in that window, not a per-frame 1% low.
 */
public class FpsStatsHud extends CachedRowsHud {
	private static final int WINDOW = 60;
	private static final int TICKS_PER_SAMPLE = 20;

	private final ToggleSetting label = add(new ToggleSetting("Show Label", true));
	private final int[] samples = new int[WINDOW];
	private int count;
	private int head;
	private int ticks;

	public FpsStatsHud() {
		super("FPS Stats", "Average and lowest frame rate over the last minute, sampled each second.", "Avg: -");
		stackTopLeft(29);
	}

	@Override
	protected boolean pill() {
		return true;
	}

	@Override
	public void onTick() {
		if (++ticks < TICKS_PER_SAMPLE) return;
		ticks = 0;
		samples[head] = mc.getFps();
		head = (head + 1) % WINDOW;
		if (count < WINDOW) count++;
		long sum = 0;
		int low = Integer.MAX_VALUE;
		for (int i = 0; i < count; i++) {
			sum += samples[i];
			low = Math.min(low, samples[i]);
		}
		int avg = (int) (sum / count);
		Part avgPart = part(String.valueOf(avg), goodHigh(avg, 60, 30));
		Part lowPart = part(String.valueOf(low), goodHigh(low, 60, 30));
		if (label.on()) setRows(single(part("Avg: ", TEXT), avgPart, part("  Low: ", TEXT), lowPart));
		else setRows(single(avgPart, part("  ", TEXT), lowPart));
	}
}
