package net.glacier.mixin;

import net.glacier.module.impl.ParticleLimiter;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Skips particles before construction. Target verified with javap: public ParticleEngine#createParticle(
 * ParticleOptions;DDDDDD)Particle, which vanilla already returns null from (unknown provider), so callers handle null.
 */
@Mixin(ParticleEngine.class)
public class MixinParticleLimiter {
	@Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
	private void glacier$limit(ParticleOptions options, double x, double y, double z, double dx, double dy, double dz,
			CallbackInfoReturnable<Particle> cir) {
		if (ParticleLimiter.shouldDrop()) cir.setReturnValue(null);
	}
}
