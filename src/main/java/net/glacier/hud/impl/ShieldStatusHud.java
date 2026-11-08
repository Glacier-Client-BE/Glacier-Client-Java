package net.glacier.hud.impl;

import net.glacier.ui.Theme;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** State of the shield in your hands: Blocking, Cooldown with a percentage after an axe hit, Ready, or None. */
public class ShieldStatusHud extends LabelValueHud {
	private int color = TEXT;

	public ShieldStatusHud() {
		super("Shield Status", "Shows whether your shield is blocking, ready or on cooldown.", "Shield", 41);
	}

	@Override
	protected String value() {
		if (mc.player == null) return null;
		ItemStack shield = mc.player.getMainHandItem();
		if (!shield.is(Items.SHIELD)) shield = mc.player.getOffhandItem();
		if (!shield.is(Items.SHIELD)) {
			color = Theme.HUD_MUTED;
			return "None";
		}
		if (mc.player.getCooldowns().isOnCooldown(shield)) {
			color = Theme.HUD_BAD;
			int pct = Math.round(mc.player.getCooldowns().getCooldownPercent(shield, 0f) * 100f);
			return "Cooldown " + pct + "%";
		}
		if (mc.player.isBlocking()) {
			color = Theme.HUD_WARN;
			return "Blocking";
		}
		color = Theme.HUD_GOOD;
		return "Ready";
	}

	@Override
	protected int valueColor() {
		return color;
	}
}
