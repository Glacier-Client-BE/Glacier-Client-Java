package net.glacier.hud.impl;

import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.ToggleSetting;

/** "FPS: 144", the value green at 60+, yellow at 30+, red below. Rows are rebuilt only when the value changes. */
public class FpsCounter extends CachedRowsHud {
	private final ToggleSetting label = add(new ToggleSetting("Show Label", true));
	private final ToggleSetting colored = add(new ToggleSetting("Color By Value", true));
	private final ChoiceSetting labelText = add(new ChoiceSetting("Label Text", "FPS", "FPS", "Frames"));
	private int lastFps = -1;
	private int lastKey = -1;

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
		int key = (label.on() ? 1 : 0) + (colored.on() ? 2 : 0) + labelText.index() * 4;
		if (fps == lastFps && key == lastKey) return;
		lastFps = fps;
		lastKey = key;
		setReadout(label.on(), labelText.get(), String.valueOf(fps), colored.on() ? goodHigh(fps, 60, 30) : TEXT, "");
	}
}
