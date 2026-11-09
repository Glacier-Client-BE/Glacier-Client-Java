package net.glacier.hud.impl;

import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.ToggleSetting;
import net.glacier.compat.GLFW;

/** Clicks per second for the left and right mouse buttons, counted over a sliding one second window. */
public class CpsHud extends CachedRowsHud {
	private final ToggleSetting showRight = add(new ToggleSetting("Show Right Click", true));
	private final ToggleSetting label = add(new ToggleSetting("Show Label", true));
	private final ChoiceSetting separator = add(new ChoiceSetting("Separator", "|", "|", "/", "-", "+"));
	private final long[] left = new long[64];
	private final long[] right = new long[64];
	private int li, ri;
	private boolean lDown, rDown;
	private int lastL = -1;
	private int lastR = -1;
	private int lastKey = -1;

	public CpsHud() {
		super("CPS", "Shows your clicks per second.", "CPS: 0");
		stackTopLeft(25);
	}

	@Override
	protected boolean pill() {
		return true;
	}

	private static int count(long[] a, long now) {
		int n = 0;
		for (long t : a) if (t != 0 && now - t < 1000) n++;
		return n;
	}

	@Override
	public void onTick() {
		if (mc.getWindow() == null) return;
		long now = System.currentTimeMillis();
		boolean l = mc.gui.screen() == null && mc.mouseHandler.isLeftPressed();
		boolean r = mc.gui.screen() == null && mc.mouseHandler.isRightPressed();
		if (l && !lDown) left[li++ & 63] = now;
		if (r && !rDown) right[ri++ & 63] = now;
		lDown = l;
		rDown = r;
		int cl = count(left, now);
		int cr = showRight.on() ? count(right, now) : -1;
		int key = (label.on() ? 1 : 0) + separator.index() * 2;
		if (cl == lastL && cr == lastR && key == lastKey) return;
		lastL = cl;
		lastR = cr;
		lastKey = key;
		String v = cr < 0 ? String.valueOf(cl) : cl + " " + separator.get() + " " + cr;
		setReadout(label.on(), "CPS", v, TEXT, "");
	}
}
