package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;

/** Lowers the fire overlay so it stops covering your view (see MixinLowFire). */
public class LowFire extends Module {
	private static LowFire instance;
	private final SliderSetting drop = add(new SliderSetting("Drop", 0.3, 0.0, 0.6, 0.05));
	private final ToggleSetting hide = add(new ToggleSetting("Hide Completely", false));

	public LowFire() {
		super("Low Fire", "Lowers the on-screen fire overlay.", Category.RENDER);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	public static boolean hide() {
		return instance.hide.on();
	}

	public static float drop() {
		return instance.drop.f();
	}
}
