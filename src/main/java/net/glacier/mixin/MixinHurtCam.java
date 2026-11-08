package net.glacier.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.glacier.module.impl.NoHurtCam;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cancels the damage tilt. Target verified with javap: private GameRenderer#bobHurt(PoseStack;F)V. */
@Mixin(GameRenderer.class)
public class MixinHurtCam {
	@Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
	private void glacier$noHurtCam(PoseStack poseStack, float partialTick, CallbackInfo ci) {
		if (NoHurtCam.active()) ci.cancel();
	}
}
