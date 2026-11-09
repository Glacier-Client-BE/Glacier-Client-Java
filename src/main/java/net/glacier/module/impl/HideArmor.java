package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Stops armor, trims, the held shield and the elytra from being drawn on player models (see MixinHideArmor,
 * MixinOldEquipmentLayer and MixinHideShield). Mirrors the Bedrock "Hide Armor" extension: it only touches your own
 * model by default and can show everything again while you sneak. Gameplay is unaffected.
 * <p>
 * Limit: Java armor is drawn per slot (the chestplate model contains body and both arms, the leggings contain both
 * legs), so arms and legs cannot be hidden individually; Hide Chestplate / Hide Leggings cover them.
 */
public class HideArmor extends Module {
	private static HideArmor instance;

	private final ToggleSetting helmet = add(new ToggleSetting("Hide Helmet", true));
	private final ToggleSetting chest = add(new ToggleSetting("Hide Chestplate", true));
	private final ToggleSetting legs = add(new ToggleSetting("Hide Leggings", true));
	private final ToggleSetting boots = add(new ToggleSetting("Hide Boots", true));

	private final ToggleSetting trims = add(new ToggleSetting("Hide Trims", false));
	private final ToggleSetting shield = add(new ToggleSetting("Hide Shield", false));
	private final ToggleSetting elytra = add(new ToggleSetting("Hide Elytra", false));
	private final ToggleSetting onlySelf = add(new ToggleSetting("Only Hide Mine", true));
	private final ToggleSetting showSneaking = add(new ToggleSetting("Show While Sneaking", false));

	public HideArmor() {
		super("Hide Armor", "Hides worn armor pieces on player models.", Category.RENDER);
		instance = this;
	}

	@Override
	protected void onDisable() {
		OldAnimations.currentArmorState = null;
	}

	/** True when the render state belongs to the local player. */
	private static boolean isSelf(Object state) {
		Minecraft mc = Minecraft.getInstance();
		return state instanceof AvatarRenderState a && mc.player != null && a.id == mc.player.getId();
	}

	/** Whether the module's filters (own model only, sneak exception) let hiding apply to this render state. */
	private boolean applies(Object state) {
		if (onlySelf.on() && !isSelf(state)) return false;
		return !(showSneaking.on() && state instanceof HumanoidRenderState h && h.isCrouching);
	}

	/** True when the elytra wings layer should be skipped (PkHideElytra). */
	public static boolean hidesElytra() {
		HideArmor h = instance;
		return h != null && h.isEnabled() && h.elytra.on();
	}

	/** Slot-only check kept for callers without a render state; ignores the own-model and sneak filters. */
	public static boolean hides(EquipmentSlot slot) {
		return hides(slot, null);
	}

	/** True when this armor slot must not be drawn on the model described by {@code state} (null = unknown model). */
	public static boolean hides(EquipmentSlot slot, Object state) {
		HideArmor h = instance;
		if (h == null || !h.isEnabled()) return false;
		if (state != null && !h.applies(state)) return false;
		return switch (slot) {
			case HEAD -> h.helmet.on();
			case CHEST -> h.chest.on();
			case LEGS -> h.legs.on();
			case FEET -> h.boots.on();
			default -> false;
		};
	}

	/** True when armor trims are not drawn for the model with this render state. */
	public static boolean hidesTrims(Object state) {
		HideArmor h = instance;
		return h != null && h.isEnabled() && h.trims.on() && state != null && h.applies(state);
	}

	/** True when a shield held (and not raised) on this model is hidden. */
	public static boolean hidesShield(Object state) {
		HideArmor h = instance;
		if (h == null || !h.isEnabled() || !h.shield.on() || !h.applies(state)) return false;
		return !(state instanceof HumanoidRenderState s && s.isUsingItem);
	}
}
