package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.ToggleSetting;
import net.minecraft.world.entity.EquipmentSlot;

/** Stops armor pieces from being drawn on player models, per slot (see MixinHideArmor). Gameplay is unaffected. */
public class HideArmor extends Module {
	private static HideArmor instance;

	private final ToggleSetting helmet = add(new ToggleSetting("Hide Helmet", true));
	private final ToggleSetting chest = add(new ToggleSetting("Hide Chestplate", true));
	private final ToggleSetting legs = add(new ToggleSetting("Hide Leggings", true));
	private final ToggleSetting boots = add(new ToggleSetting("Hide Boots", true));

	private final ToggleSetting elytra = add(new ToggleSetting("Hide Elytra", false));

	public HideArmor() {
		super("Hide Armor", "Hides worn armor pieces on player models.", Category.RENDER);
		instance = this;
	}

	/** True when the elytra wings layer should be skipped (PkHideElytra). */
	public static boolean hidesElytra() {
		HideArmor h = instance;
		return h != null && h.isEnabled() && h.elytra.on();
	}

	public static boolean hides(EquipmentSlot slot) {
		HideArmor h = instance;
		if (h == null || !h.isEnabled()) return false;
		return switch (slot) {
			case HEAD -> h.helmet.on();
			case CHEST -> h.chest.on();
			case LEGS -> h.legs.on();
			case FEET -> h.boots.on();
			default -> false;
		};
	}
}
