package net.glacier.mixin.extrab;

import net.glacier.module.impl.OldAnimations;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Old damage tilt: bobHurt rotates by getHurtDir around Y before and after the roll; with 0 the camera simply rolls. */
@Mixin(GameRenderer.class)
public class MixinOldGameRenderer {
	@Redirect(method = "bobHurt", require = 0, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;getHurtDir()F"))
	private float glacier$oldTilt(LivingEntity entity) {
		return OldAnimations.damageTilt() ? 0f : entity.getHurtDir();
	}
}
