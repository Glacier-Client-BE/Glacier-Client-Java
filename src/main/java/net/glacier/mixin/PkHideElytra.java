package net.glacier.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.glacier.module.impl.HideArmor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Skips the elytra layer. Target verified with javap: WingsLayer#submit(PoseStack, SubmitNodeCollector, int, S extends HumanoidRenderState, float, float)V. */
@Mixin(WingsLayer.class)
public class PkHideElytra {
	@Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
			at = @At("HEAD"), cancellable = true)
	private void glacier$hideElytra(PoseStack pose, SubmitNodeCollector collector, int light, HumanoidRenderState state,
									float yRot, float xRot, CallbackInfo ci) {
		if (HideArmor.hidesElytra()) ci.cancel();
	}
}
