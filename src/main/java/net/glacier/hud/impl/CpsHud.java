package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;
import org.lwjgl.glfw.GLFW;

/** Clicks per second for the left and right mouse buttons, counted over a sliding one second window. */
public class CpsHud extends CachedRowsHud {
	private final ToggleSetting showRight = add(new ToggleSetting("Show Right Click", true));
	private final long[] left = new long[64];
	private final long[] right = new long[64];
	private int li, ri;
	private boolean lDown, rDown;

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
		boolean l = mc.screen == null && GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean r = mc.screen == null && GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		if (l && !lDown) left[li++ & 63] = now;
		if (r && !rDown) right[ri++ & 63] = now;
		lDown = l;
		rDown = r;
		String text = "CPS: " + count(left, now) + (showRight.on() ? " | " + count(right, now) : "");
		setRows(single(part(text, TEXT)));
	}
}
