package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;

/** Removes the camera tilt when taking damage (cancels GameRenderer#bobHurt, see MixinHurtCam). */
public class NoHurtCam extends Module {
	private static NoHurtCam instance;

	public NoHurtCam() {
		super("No Hurt Cam", "Removes the camera shake when you take damage.", Category.RENDER);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}
}
