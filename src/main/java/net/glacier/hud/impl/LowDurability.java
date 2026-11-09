package net.glacier.hud.impl;

import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Warns when a held or worn item drops below the threshold; hidden in game otherwise. */
public class LowDurability extends CachedRowsHud {
	/** Font Awesome "triangle-exclamation" glyph. */
	private static final int WARN_ICON = 0xf071;
	private static final EquipmentSlot[] SLOTS = {
			EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};
	private final SliderSetting threshold = add(new SliderSetting("Threshold %", 10, 1, 50, 1));
	private final ToggleSetting hideIcon = add(new ToggleSetting("Hide Icon", false));
	private boolean warning;
	private ItemStack lastStack = ItemStack.EMPTY;
	private int lastPct = -1;
	private boolean lastHideIcon;

	public LowDurability() {
		super("Low Durability", "Warns when held or worn gear is about to break.", "Low durability!");
		setPos(0.5f, 0.3f);
	}

	@Override
	public void onTick() {
		ItemStack worst = ItemStack.EMPTY;
		int worstPct = 101;
		if (mc.player != null) {
			for (EquipmentSlot slot : SLOTS) {
				ItemStack st = mc.player.getItemBySlot(slot);
				if (st.isEmpty() || !st.isDamageableItem()) continue;
				int max = Math.max(1, st.getMaxDamage());
				int pct = (max - st.getDamageValue()) * 100 / max;
				if (pct < worstPct) {
					worstPct = pct;
					worst = st;
				}
			}
		}
		warning = worstPct < threshold.i();
		boolean hide = hideIcon.on();
		if (!warning || (worst == lastStack && worstPct == lastPct && hide == lastHideIcon)) return;
		lastStack = worst;
		lastPct = worstPct;
		lastHideIcon = hide;
		if (hide) {
			setRows(single(part(worst.getHoverName().getString() + ": ", TEXT), part(worstPct + "%", Theme.HUD_BAD)));
		} else {
			setRows(single(icon(WARN_ICON, Theme.HUD_WARN), part(" " + worst.getHoverName().getString() + ": ", TEXT),
					part(worstPct + "%", Theme.HUD_BAD)));
		}
	}

	@Override
	public void render(GuiGraphicsExtractor g, int sw, int sh) {
		if (warning) super.render(g, sw, sh);
	}
}
