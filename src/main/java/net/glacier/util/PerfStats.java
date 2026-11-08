package net.glacier.util;

import net.minecraft.client.Minecraft;

/** Rolling FPS history for the Performance page: 48 samples, one every 5 client ticks (about 12 seconds). */
public final class PerfStats {
	public static final int SAMPLES = 48;
	private static final int TICKS_PER_SAMPLE = 5;

	private static final int[] HISTORY = new int[SAMPLES];
	private static int filled;
	private static int ticks;
	private static final long START_MS = System.currentTimeMillis();

	private PerfStats() {
	}

	public static void tick() {
		if (++ticks % TICKS_PER_SAMPLE != 0) return;
		int fps = Minecraft.getInstance().getFps();
		System.arraycopy(HISTORY, 1, HISTORY, 0, SAMPLES - 1);
		HISTORY[SAMPLES - 1] = fps;
		if (filled < SAMPLES) filled++;
	}

	/** Copy of the samples, oldest first. Slots before the first sample are 0. */
	public static int[] history() {
		return HISTORY.clone();
	}

	public static int current() {
		return HISTORY[SAMPLES - 1];
	}

	/** Lowest sample recorded so far, or 0 when there is no data yet. */
	public static int worst() {
		if (filled == 0) return 0;
		int min = Integer.MAX_VALUE;
		for (int i = SAMPLES - filled; i < SAMPLES; i++) min = Math.min(min, HISTORY[i]);
		return min;
	}

	private static long sessionSec = -1;
	private static String sessionText = "0:00";

	/** Time since the stats started, formatted as m:ss. Rebuilt only when the second changes. */
	public static String session() {
		long s = (System.currentTimeMillis() - START_MS) / 1000;
		if (s != sessionSec) {
			sessionSec = s;
			if (s < 0) {
				sessionText = String.format("%d:%02d", s / 60, s % 60);
			} else {
				long sec = s % 60;
				sessionText = (s / 60) + (sec < 10 ? ":0" : ":") + sec;
			}
		}
		return sessionText;
	}
}
