package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.SliderSetting;

/**
 * Drops the chosen share of client particle spawns before they are constructed (MixinParticleLimiter on
 * ParticleEngine#createParticle). Uses an allocation-free xorshift RNG; only ever called on the render thread.
 */
public class ParticleLimiter extends Module {
	private static ParticleLimiter instance;

	private final SliderSetting drop = add(new SliderSetting("Drop %", 50, 0, 100, 5));
	private int seed = 0x9E3779B9;

	public ParticleLimiter() {
		super("Particle Limiter", "Randomly skips a share of particles to save FPS.", Category.RENDER);
		instance = this;
	}

	/** True when this spawn should be skipped. */
	public static boolean shouldDrop() {
		ParticleLimiter p = instance;
		if (p == null || !p.isEnabled()) return false;
		int pct = p.drop.i();
		if (pct <= 0) return false;
		if (pct >= 100) return true;
		int x = p.seed;
		x ^= x << 13;
		x ^= x >>> 17;
		x ^= x << 5;
		p.seed = x;
		return ((x >>> 1) % 100) < pct;
	}
}
