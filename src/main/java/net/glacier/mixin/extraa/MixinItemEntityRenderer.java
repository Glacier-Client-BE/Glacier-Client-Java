package net.glacier.mixin.extraa;

import com.mojang.blaze3d.vertex.PoseStack;
import net.glacier.module.impl.ItemHighlighter;
import net.glacier.module.impl.ItemPhysics;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Item Physics and Item Highlighter. Verified with javap against ItemEntityRenderer (1.21.11):
 * <ul>
 * <li>submit(ItemEntityRenderState, PoseStack, SubmitNodeCollector, CameraRenderState)V contains exactly one
 * PoseStack#translate(FFF)V, one Mth#sin(D)F (the bob), one PoseStack#mulPose(Quaternionfc)V (the spin) and the
 * balanced pushPose/popPose around them, so the redirects below replace single calls.</li>
 * <li>extractRenderState(ItemEntity, ItemEntityRenderState, float)V ends with a plain return; EntityRenderState#outlineColor
 * != 0 is what makes LevelRenderer draw the entity into the outline target.</li>
 * </ul>
 */
@Mixin(ItemEntityRenderer.class)
public abstract class MixinItemEntityRenderer {
	private static final String SUBMIT = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V";

	private ItemEntityRenderState glacier$state;
	private AABB glacier$box;

	@Inject(method = SUBMIT, at = @At("HEAD"), require = 0)
	private void glacier$begin(ItemEntityRenderState state, PoseStack pose, SubmitNodeCollector collector,
							   CameraRenderState camera, CallbackInfo ci) {
		this.glacier$state = state;
		this.glacier$box = ItemPhysics.active() ? state.item.getModelBoundingBox() : null;
	}

	@Redirect(method = SUBMIT, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;sin(D)F"), require = 0)
	private float glacier$bob(double value) {
		return ItemPhysics.noBob() ? -1f : Mth.sin(value);
	}

	@Redirect(method = SUBMIT, at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"), require = 0)
	private void glacier$lift(PoseStack pose, float x, float y, float z) {
		if (ItemPhysics.active() && this.glacier$state != null) {
			ItemPhysics.translate(pose, this.glacier$state, this.glacier$box, x, y, z);
		} else {
			pose.translate(x, y, z);
		}
	}

	@Redirect(method = SUBMIT, at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V"), require = 0)
	private void glacier$spin(PoseStack pose, Quaternionfc vanilla) {
		if (ItemPhysics.active() && this.glacier$state != null) {
			ItemPhysics.rotate(pose, this.glacier$state, this.glacier$box, vanilla);
		} else {
			pose.mulPose(vanilla);
		}
	}

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V",
			at = @At("RETURN"), require = 0)
	private void glacier$highlight(ItemEntity entity, ItemEntityRenderState state, float partial, CallbackInfo ci) {
		if (!ItemHighlighter.active()) return;
		int c = ItemHighlighter.outlineFor(entity.getItem());
		if (c != 0) state.outlineColor = c;
	}
}
