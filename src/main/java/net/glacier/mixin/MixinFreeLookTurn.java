package net.glacier.mixin;

import net.glacier.module.impl.FreeLook;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Diverts mouse turning of the local player into the free look camera. Target verified: Entity#turn(double, double). */
@Mixin(Entity.class)
public class MixinFreeLookTurn {
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void glacier$freeLook(double yRot, double xRot, CallbackInfo ci) {
		if (FreeLook.active() && (Object) this == Minecraft.getInstance().player) {
			FreeLook.turn(yRot, xRot);
			ci.cancel();
		}
	}
}
