package net.glacier.hud;

import net.glacier.module.ModuleManager;
import net.glacier.module.impl.EditorsModule;
import net.glacier.ui.Draw;
import net.glacier.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

/** Hotbar and EXP bar decorations from the Editors page, drawn over the vanilla bars. */
public final class HudExtras {
	private static final int HOTBAR_W = 182;
	private static final int SLOT = 20;
	private static final String[] DIGITS = {"1", "2", "3", "4", "5", "6", "7", "8", "9"};
	private static int lastPct = -1;
	private static String pctText = "";

	private HudExtras() {
	}

	public static void draw(GuiGraphics g) {
		EditorsModule editors = ModuleManager.get(EditorsModule.class);
		if (editors == null) return;
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return;

		int w = g.guiWidth();
		int h = g.guiHeight();
		int left = w / 2 - HOTBAR_W / 2;

		int tint = editors.displayTint();
		if ((tint >>> 24) != 0) g.fill(0, 0, w, h, tint);

		if (editors.slotNumbers.on()) {
			for (int i = 0; i < 9; i++) {
				Draw.text(g, DIGITS[i], left + 2 + i * SLOT, h - 21, 0.5f, Theme.DIM);
			}
		}

		if (editors.xpPercent.on()) {
			int pct = Math.round(Math.max(0f, Math.min(1f, player.experienceProgress)) * 100f);
			if (pct != lastPct) {
				lastPct = pct;
				pctText = pct + "%";
			}
			Draw.centered(g, pctText, w / 2, h - 30, 0.6f, Theme.TEXT);
		}
	}
}
