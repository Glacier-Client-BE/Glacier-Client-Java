package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * 1.7/1.8 style animations. Every sub-option is read by a mixin in net.glacier.mixin.extrab (see MixinOldAnimations
 * and friends); the static accessors are null-safe so mixins degrade to vanilla when the module is off.
 */
public class OldAnimations extends Module {
	private static OldAnimations instance;

	private final ToggleSetting blockHitting = add(new ToggleSetting("Block Hitting", true));
	private final ToggleSetting itemSwitch = add(new ToggleSetting("Old Item Switch", true));
	private final ToggleSetting useSwinging = add(new ToggleSetting("Item Usage Swinging", true));
	private final ToggleSetting sneak = add(new ToggleSetting("Old Sneak Animation", true));
	private final ToggleSetting armorTint = add(new ToggleSetting("Armor Hurt Tint", true));
	private final ToggleSetting rightClickFix = add(new ToggleSetting("Right Click Fix", true));
	private final ToggleSetting damageTilt = add(new ToggleSetting("Old Damage Tilt", false));

	/** Render state of the player whose armor is being drawn right now (render thread only, set per armor piece). */
	public static LivingEntityRenderState currentArmorState;

	public OldAnimations() {
		super("Old Animations", "Brings back 1.7/1.8 style first-person and camera animations.", Category.RENDER);
		instance = this;
	}

	@Override
	protected void onDisable() {
		currentArmorState = null;
	}

	private static boolean on(OldAnimations o, ToggleSetting s) {
		return o != null && o.isEnabled() && s.on();
	}

	public static boolean blockHitting() {
		OldAnimations o = instance;
		return o != null && on(o, o.blockHitting);
	}

	public static boolean itemSwitch() {
		OldAnimations o = instance;
		return o != null && on(o, o.itemSwitch);
	}

	public static boolean useSwinging() {
		OldAnimations o = instance;
		return o != null && on(o, o.useSwinging);
	}

	public static boolean sneak() {
		OldAnimations o = instance;
		return o != null && on(o, o.sneak);
	}

	public static boolean armorTint() {
		OldAnimations o = instance;
		return o != null && on(o, o.armorTint);
	}

	public static boolean rightClickFix() {
		OldAnimations o = instance;
		return o != null && on(o, o.rightClickFix);
	}

	public static boolean damageTilt() {
		OldAnimations o = instance;
		return o != null && on(o, o.damageTilt);
	}
}
