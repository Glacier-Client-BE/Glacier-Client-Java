package net.glacier.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

/**
 * Dropped items lie flat on the ground and stop bobbing, like the NoRisk / Item Physics mods. Hooked into
 * ItemEntityRenderer#submit by MixinItemEntityRenderer: flat (sprite) items are tipped 90 degrees about X around their own
 * centre and lifted by their half thickness; 3D items (blocks, tools with depth) keep their upright pose. The rotation
 * setting controls the yaw: vanilla spin, a slower spin or a fixed random heading per item.
 */
public class ItemPhysics extends Module {
	/** Items thinner than this (model depth in blocks) are rendered as flat sprites, same threshold as vanilla. */
	private static final double FLAT_DEPTH = 0.0625;
	private static ItemPhysics instance;

	private final ToggleSetting flat = add(new ToggleSetting("Lay Flat", true));
	private final ToggleSetting noBob = add(new ToggleSetting("No Bobbing", true));
	private final ChoiceSetting rotation = add(new ChoiceSetting("Rotation", "Slow Spin", "Spin", "Slow Spin", "Static"));
	private final SliderSetting slow = add(new SliderSetting("Slow Spin Speed", 0.25, 0.05, 1.0, 0.05));

	public ItemPhysics() {
		super("Item Physics", "Dropped items lie flat on the ground instead of bobbing and spinning.", Category.RENDER);
		instance = this;
	}

	public static boolean active() {
		ItemPhysics p = instance;
		return p != null && p.isEnabled();
	}

	/** True when the hover bob of dropped items should be removed. */
	public static boolean noBob() {
		ItemPhysics p = instance;
		return p != null && p.isEnabled() && p.noBob.on();
	}

	private static boolean isFlatSprite(AABB box) {
		return (float) box.getZsize() <= (float) FLAT_DEPTH;
	}

	/**
	 * Replacement for the vertical translate of ItemEntityRenderer#submit.
	 *
	 * @param y the y vanilla computed: {@code -minY + 0.0625 + bob}
	 */
	public static void translate(PoseStack pose, ItemEntityRenderState s, AABB box, float x, float y, float z) {
		ItemPhysics p = instance;
		if (p != null && p.isEnabled() && p.flat.on() && box != null && isFlatSprite(box)) {
			float bob = y - (float) (-box.minY + 0.0625f);
			float stack = (Math.max(1, s.count) - 1) * 0.75f * (float) box.getZsize();
			y = bob + (float) -box.minZ + stack + 0.01f;
		}
		pose.translate(x, y, z);
	}

	/**
	 * Replacement for the yaw rotation of ItemEntityRenderer#submit (applied right after {@link #translate}). 26.x
	 * rotates with {@code PoseStack#rotate(Axis, float)}, so the vanilla spin arrives as a plain angle in radians.
	 */
	public static void rotate(PoseStack pose, ItemEntityRenderState s, AABB box, float vanillaSpin) {
		ItemPhysics p = instance;
		if (p == null || !p.isEnabled()) {
			pose.rotate(Axis.YP, vanillaSpin);
			return;
		}
		switch (p.rotation.get()) {
			case "Static" -> pose.rotate(Axis.YP, s.bobOffset);
			case "Slow Spin" -> pose.rotate(Axis.YP, ItemEntity.getSpin(s.ageInTicks * p.slow.f(), s.bobOffset));
			default -> pose.rotate(Axis.YP, vanillaSpin);
		}
		if (p.flat.on() && box != null && isFlatSprite(box)) {
			pose.rotate(Axis.XP, (float) Math.toRadians(-90.0));
			pose.translate(-(float) ((box.minX + box.maxX) * 0.5), -(float) ((box.minY + box.maxY) * 0.5), 0f);
		}
	}
}
