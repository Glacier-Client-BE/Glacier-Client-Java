package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;

/** Removes the blur drawn behind menus and inventories (see Nr2ClearBackground); the dim overlay stays. */
public class ClearBackground extends Module {
	private static ClearBackground instance;

	public ClearBackground() {
		super("Clear Background", "Disables the blur behind menus so the world stays sharp.", Category.RENDER);
		instance = this;
	}

	public static boolean active() {
		ClearBackground c = instance;
		return c != null && c.isEnabled();
	}
}
