package net.glacier.hud.impl;

import net.minecraft.client.multiplayer.ClientLevel;

import java.util.Locale;

/**
 * Estimated server ticks per second, measured as world game-time progress over wall-clock time between two
 * game-time updates at least {@value #WINDOW_MS} ms apart. Green at 18+, yellow at 12+, red below.
 */
public class TpsHud extends LabelValueHud {
	private static final long WINDOW_MS = 4000L;
	private static final double MAX_TPS = 20.0;

	private ClientLevel level;
	private long lastGameTime;
	private long anchorGameTime;
	private long anchorNanos;
	private double tps = -1;
	private String text = "-";

	public TpsHud() {
		super("TPS", "Estimates the server's ticks per second from world time updates.", "TPS", 40);
	}

	@Override
	protected void onEnable() {
		reset();
	}

	private void reset() {
		level = null;
		anchorNanos = 0;
		tps = -1;
		text = "-";
	}

	@Override
	protected String value() {
		if (mc.level == null || mc.isPaused()) {
			if (mc.level == null) reset();
			anchorNanos = 0;
			return text;
		}
		long now = System.nanoTime();
		long gt = mc.level.getGameTime();
		if (mc.level != level || gt < lastGameTime) {
			level = mc.level;
			anchorNanos = 0;
			tps = -1;
			text = "-";
		}
		if (gt != lastGameTime || anchorNanos == 0) {
			if (anchorNanos == 0) {
				anchorGameTime = gt;
				anchorNanos = now;
			} else if ((now - anchorNanos) / 1_000_000L >= WINDOW_MS) {
				double seconds = (now - anchorNanos) / 1_000_000_000.0;
				tps = Math.min(MAX_TPS, (gt - anchorGameTime) / seconds);
				text = String.format(Locale.ROOT, "%.1f", tps);
				anchorGameTime = gt;
				anchorNanos = now;
			}
		}
		lastGameTime = gt;
		return text;
	}

	@Override
	protected int valueColor() {
		return tps < 0 ? TEXT : goodHigh(tps, 18, 12);
	}
}
