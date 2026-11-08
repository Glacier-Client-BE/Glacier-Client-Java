package net.glacier.hud.impl;

import net.glacier.hud.TextHudModule;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;

import java.util.List;
import java.util.Locale;

/** "Speed: 7.8 b/s", smoothed horizontal speed, optionally including vertical movement. */
public class Speedometer extends TextHudModule {
	/** Horizontal moves longer than this in one tick are teleports or ender pearls and are ignored. */
	private static final double MAX_TICK_STEP = 10.0;

	private final ChoiceSetting unit = add(new ChoiceSetting("Unit", "Blocks/s", "Blocks/s", "km/h", "m/s"));

	private final ToggleSetting includeY = add(new ToggleSetting("Include Y", false));
	private final SliderSetting decimals = add(new SliderSetting("Decimals", 1, 0, 3, 1));

	private double lastX;
	private double lastY;
	private double lastZ;
	private boolean primed;
	private double speedBps;
	/** The line shown, rebuilt only when the speed or the unit changes. */
	private List<String> line = List.of();
	private double lineBps = Double.NaN;
	private int lineUnit = -1;
	private int lineDecimals = -1;

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
		double y = mc.player.getY();
		double z = mc.player.getZ();
		if (primed) {
			double dx = x - lastX;
			double dy = includeY.on() ? y - lastY : 0;
			double dz = z - lastZ;
			double step = Math.sqrt(dx * dx + dy * dy + dz * dz);
			// A one-tick jump larger than 10 blocks is a teleport or pearl, not walking: skip that sample.
			if (step <= MAX_TICK_STEP) {
				double bps = step * 20.0;
				speedBps = speedBps * 0.6 + bps * 0.4;
			}
		}
		lastX = x;
		lastY = y;
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
		int d = decimals.i();
		if (u == lineUnit && v == lineBps && d == lineDecimals) return;
		lineUnit = u;
		lineBps = v;
		lineDecimals = d;
		String suffix = u == 1 ? " km/h" : u == 2 ? " m/s" : " b/s";
		line = List.of("Speed: " + String.format(Locale.ROOT, "%." + d + "f", u == 1 ? v * 3.6 : v) + suffix);
	}
}
