package net.glacier.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.glacier.module.impl.SpinningProjectiles;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tumbles thrown-item sprites. Target verified with javap: ThrownItemRenderer#submit(ThrownItemRenderState, PoseStack,
 * SubmitNodeCollector, CameraRenderState)V has a single return and its own balanced pushPose/popPose.
 */
@Mixin(ThrownItemRenderer.class)
public class PkSpinProjectile {
	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ThrownItemRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
			at = @At("HEAD"))
	private void glacier$spinStart(ThrownItemRenderState state, PoseStack pose, SubmitNodeCollector collector,
								   CameraRenderState camera, CallbackInfo ci) {
		if (!SpinningProjectiles.active()) return;
		float t = state.ageInTicks * SpinningProjectiles.speed();
		pose.pushPose();
		pose.mulPose(Axis.XP.rotationDegrees(t * 10f));
		pose.mulPose(Axis.YP.rotationDegrees(t * 2f));
	}

	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ThrownItemRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
			at = @At("RETURN"))
	private void glacier$spinEnd(ThrownItemRenderState state, PoseStack pose, SubmitNodeCollector collector,
								 CameraRenderState camera, CallbackInfo ci) {
		if (SpinningProjectiles.active()) pose.popPose();
	}
}
