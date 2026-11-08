package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.SuspiciousStewEffects;

/**
 * Hunger and saturation the held food restores (main hand first, then off hand). With Show Potion/Stew Info on it
 * also lists the effects of a held potion or suspicious stew. Hidden when nothing relevant is held.
 */
public class FoodStats extends CachedRowsHud {
	private final ToggleSetting showEffects = add(new ToggleSetting("Show Potion/Stew Info", false));
	private boolean visible;
	private int lastNutrition = -1;
	private int lastSaturation = -1;
	/** The stack the effect line was built for, so the line is rebuilt only when the held item changes. */
	private ItemStack effectStack = ItemStack.EMPTY;

	public FoodStats() {
		super("Food Stats+", "Shows the hunger and saturation the held food restores.", "Food");
		setPos(0.62f, 0.93f);
	}

	@Override
	public void onTick() {
		var p = mc.player;
		visible = false;
		if (p == null) return;
		ItemStack main = p.getMainHandItem();
		ItemStack off = p.getOffhandItem();
		FoodProperties fp = main.get(DataComponents.FOOD);
		if (fp == null) fp = off.get(DataComponents.FOOD);
		if (fp != null) {
			visible = true;
			// Saturation restored is nutrition * saturation * 2; kept in tenths for display.
			int sat10 = Math.round(fp.nutrition() * fp.saturation() * 20f);
			if (fp.nutrition() == lastNutrition && sat10 == lastSaturation) return;
			lastNutrition = fp.nutrition();
			lastSaturation = sat10;
			effectStack = ItemStack.EMPTY;
			setRows(single(part("+" + fp.nutrition() + " food", TEXT),
					part("  +" + sat10 / 10 + "." + sat10 % 10 + " sat", Theme.HUD_GOOD)));
			return;
		}
		if (!showEffects.on()) return;
		ItemStack held = hasEffects(main) ? main : off;
		if (!hasEffects(held)) return;
		visible = true;
		if (held == effectStack) return;
		effectStack = held;
		lastNutrition = -1;
		setRows(single(part(effectLine(held), Theme.HUD_BIOME)));
	}

	private static boolean hasEffects(ItemStack st) {
		return st.has(DataComponents.POTION_CONTENTS) || st.has(DataComponents.SUSPICIOUS_STEW_EFFECTS);
	}

	/** "Speed 30s, Haste 10s" style list for a potion or suspicious stew. Allocates; call only on change. */
	private static String effectLine(ItemStack st) {
		StringBuilder sb = new StringBuilder();
		PotionContents pc = st.get(DataComponents.POTION_CONTENTS);
		if (pc != null) {
			for (MobEffectInstance e : pc.getAllEffects()) append(sb, e);
		}
		SuspiciousStewEffects se = st.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
		if (se != null) {
			for (SuspiciousStewEffects.Entry e : se.effects()) append(sb, e.createEffectInstance());
		}
		return sb.length() == 0 ? "No effects" : sb.toString();
	}

	private static void append(StringBuilder sb, MobEffectInstance e) {
		if (sb.length() > 0) sb.append(", ");
		sb.append(e.getEffect().value().getDisplayName().getString());
		if (e.getAmplifier() > 0) sb.append(' ').append(e.getAmplifier() + 1);
		sb.append(' ').append(e.getDuration() / 20).append('s');
	}

	@Override
	public void render(GuiGraphics g, int sw, int sh) {
		if (visible) super.render(g, sw, sh);
	}
}
