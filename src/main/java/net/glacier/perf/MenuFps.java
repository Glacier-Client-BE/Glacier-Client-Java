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
		var screen = mc.screen;
		// Glacier's own interactive screens (mod menu, HUD editor, quick bar) stay at the full rate so scrolling and animations are smooth
		if (screen instanceof net.glacier.ui.screen.NativeMenuScreen || screen instanceof net.glacier.ui.screen.HudEditorScreen
				|| screen instanceof net.glacier.ui.screen.NativeQuickBar || screen instanceof net.glacier.ui.screen.HudQuickScreen) return vanilla;
		boolean inMenu = mc.level == null || (screen != null && !(screen instanceof PauseScreen));
		return inMenu ? cap : vanilla;
	}
}
