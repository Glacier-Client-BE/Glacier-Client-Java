package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.SliderSetting;

/** Lowers the frame cap on the title screen and in menus (not the pause menu). Applied via net.glacier.perf.MenuFps. */
public class MenuFpsLimit extends Module {
	private static MenuFpsLimit instance;

	private final SliderSetting cap = add(new SliderSetting("Menu FPS", 30, 20, 60, 5));

	public MenuFpsLimit() {
		super("Menu FPS Limit", "Caps the frame rate in menus to save power.", Category.RENDER);
		instance = this;
	}

	/** The configured cap, or 0 when the module is off. */
	public static int capValue() {
		MenuFpsLimit m = instance;
		return m != null && m.isEnabled() ? m.cap.i() : 0;
	}
}
