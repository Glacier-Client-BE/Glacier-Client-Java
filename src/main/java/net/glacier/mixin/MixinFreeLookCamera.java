package net.glacier.mixin;

import net.glacier.module.impl.FreeLook;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the free look angles after the camera has been set up. Target verified: Camera#setup(Level, Entity, boolean, boolean, float). */
@Mixin(Camera.class)
public abstract class MixinFreeLookCamera {
	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Inject(method = "setup", at = @At("TAIL"))
	private void glacier$freeLook(Level level, Entity entity, boolean detached, boolean mirrored, float partial, CallbackInfo ci) {
		if (FreeLook.active()) setRotation(FreeLook.yaw(), FreeLook.pitch());
	}
}
