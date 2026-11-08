package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;

/** "FPS: 144", the value green at 60+, yellow at 30+, red below. Rows are rebuilt only when the value changes. */
public class FpsCounter extends CachedRowsHud {
	private final ToggleSetting label = add(new ToggleSetting("Show Label", true));
	private int lastFps = -1;
	private boolean lastLabel;

	public FpsCounter() {
		super("FPS Counter", "Displays your current frames per second.", "FPS: 0");
		stackTopLeft(0);
	}

	@Override
	protected boolean pill() {
		return true;
	}

	@Override
	public void onTick() {
		int fps = mc.getFps();
		boolean withLabel = label.on();
		if (fps == lastFps && withLabel == lastLabel) return;
		lastFps = fps;
		lastLabel = withLabel;
		int color = goodHigh(fps, 60, 30);
		Part value = part(String.valueOf(fps), color);
		if (!withLabel) setRows(single(value));
		else setRows(single(part("FPS: ", TEXT), value));
	}
}
