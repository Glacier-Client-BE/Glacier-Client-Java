package net.glacier.perf;

import net.glacier.module.impl.MenuFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;

/** Menu frame cap for MixinFramerateLimit: never raises vanilla's limit, only lowers it in menus. */
public final class MenuFps {
	private MenuFps() {
	}

	public static int apply(int vanilla) {
		int cap = MenuFpsLimit.capValue();
		if (cap <= 0 || cap >= vanilla) return vanilla;
		Minecraft mc = Minecraft.getInstance();
		if (mc == null) return vanilla;
		boolean inMenu = mc.level == null || (mc.screen != null && !(mc.screen instanceof PauseScreen));
		return inMenu ? cap : vanilla;
	}
}
