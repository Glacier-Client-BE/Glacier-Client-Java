package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;

/** Horizontal blocks walked this session (teleports and riding ignored). */
public class WalkDistance extends CachedRowsHud {
	private final ToggleSetting hideLabel = add(new ToggleSetting("Hide Label", false));
	private double total;
	private double lastX, lastZ;
	private boolean hasLast;
	private long shown = -1;
	private boolean shownHideLabel;

	public WalkDistance() {
		super("Walk Distance", "Shows how far you walked this session.", "Walked: 0 m");
		stackTopLeft(15);
	}

	@Override
	protected void onEnable() {
		hasLast = false;
	}

	@Override
	public void onTick() {
		if (mc.player == null) {
			hasLast = false;
			return;
		}
		double x = mc.player.getX();
		double z = mc.player.getZ();
		if (hasLast && !mc.player.isPassenger()) {
			double ddx = x - lastX, ddz = z - lastZ;
			double d2 = ddx * ddx + ddz * ddz;
			if (d2 < 100) total += Math.sqrt(d2);
		}
		lastX = x;
		lastZ = z;
		hasLast = true;
		long m = (long) total;
		boolean hide = hideLabel.on();
		if (m == shown && hide == shownHideLabel) return;
		shown = m;
		shownHideLabel = hide;
		setRows(single(part(hide ? m + " m" : "Walked: " + m + " m", TEXT)));
	}
}
