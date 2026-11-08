package net.glacier.hud.impl;

import net.glacier.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;

/** Hunger and saturation the held food restores (main hand first, then off hand). Hidden when no food is held. */
public class FoodStats extends CachedRowsHud {
	private boolean food;
	private int lastNutrition = -1;
	private int lastSaturation = -1;

	public FoodStats() {
		super("Food Stats+", "Shows the hunger and saturation the held food restores.", "Food");
		setPos(0.62f, 0.93f);
	}

	@Override
	public void onTick() {
		var p = mc.player;
		FoodProperties fp = null;
		if (p != null) {
			fp = p.getMainHandItem().get(DataComponents.FOOD);
			if (fp == null) fp = p.getOffhandItem().get(DataComponents.FOOD);
		}
		food = fp != null;
		if (fp == null) return;
		// Saturation restored is nutrition * saturation * 2; kept in tenths for display.
		int sat10 = Math.round(fp.nutrition() * fp.saturation() * 20f);
		if (fp.nutrition() == lastNutrition && sat10 == lastSaturation) return;
		lastNutrition = fp.nutrition();
		lastSaturation = sat10;
		setRows(single(part("+" + fp.nutrition() + " food", TEXT),
				part("  +" + sat10 / 10 + "." + sat10 % 10 + " sat", Theme.HUD_GOOD)));
	}

	@Override
	public void render(GuiGraphics g, int sw, int sh) {
		if (food) super.render(g, sw, sh);
	}
}
