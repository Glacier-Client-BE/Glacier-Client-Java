package net.glacier.hud.impl;

import net.glacier.hud.TextHudModule;
import net.glacier.setting.ChoiceSetting;

import java.util.List;
import java.util.Locale;

/** "Speed: 7.8 b/s", smoothed horizontal speed. */
public class Speedometer extends TextHudModule {
	/** Horizontal moves longer than this in one tick are teleports or ender pearls and are ignored. */
	private static final double MAX_TICK_STEP = 10.0;

	private final ChoiceSetting unit = add(new ChoiceSetting("Unit", "Blocks/s", "Blocks/s", "km/h", "m/s"));

	private double lastX;
	private double lastZ;
	private boolean primed;
	private double speedBps;
	/** The line shown, rebuilt only when the speed or the unit changes. */
	private List<String> line = List.of();
	private double lineBps = Double.NaN;
	private int lineUnit = -1;

	public Speedometer() {
		super("Speedometer", "Shows your horizontal movement speed.");
		stackTopLeft(3);
	}

	@Override
	protected boolean pill() {
		return true;
	}

	@Override
	protected void onEnable() {
		primed = false;
		speedBps = 0;
		refreshLine();
	}

	@Override
	public void onTick() {
		if (mc.player == null) {
			primed = false;
			speedBps = 0;
			refreshLine();
			return;
		}
		double x = mc.player.getX();
		double z = mc.player.getZ();
		if (primed) {
			double dx = x - lastX;
			double dz = z - lastZ;
			double step = Math.sqrt(dx * dx + dz * dz);
			// A one-tick jump larger than 10 blocks is a teleport or pearl, not walking: skip that sample.
			if (step <= MAX_TICK_STEP) {
				double bps = step * 20.0;
				speedBps = speedBps * 0.6 + bps * 0.4;
			}
		}
		lastX = x;
		lastZ = z;
		primed = true;
		refreshLine();
	}

	@Override
	protected List<String> lines() {
		refreshLine();
		return line;
	}

	/** Formats the line once per tick at most, and only when the speed or the unit has changed. */
	private void refreshLine() {
		int u = unit.is("km/h") ? 1 : unit.is("m/s") ? 2 : 0;
		double v = speedBps;
		if (u == lineUnit && v == lineBps) return;
		lineUnit = u;
		lineBps = v;
		String value;
		if (u == 1) value = String.format(Locale.ROOT, "%.1f km/h", v * 3.6);
		else if (u == 2) value = String.format(Locale.ROOT, "%.1f m/s", v);
		else value = String.format(Locale.ROOT, "%.1f b/s", v);
		line = List.of("Speed: " + value);
	}
}
