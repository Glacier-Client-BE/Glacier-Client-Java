package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Drops client particle spawns before they are constructed (MixinParticleLimiter on ParticleEngine#createParticle):
 * optional per-type hiding (like the Bedrock Particle Disabler pack) plus a random share of everything else.
 * Allocation-free; only ever called on the render thread.
 */
public class ParticleLimiter extends Module {
	private static ParticleLimiter instance;

	private final SliderSetting drop = add(new SliderSetting("Drop %", 50, 0, 100, 5));
	private final ToggleSetting hideAll = add(new ToggleSetting("Disable All Particles", false));
	private final ToggleSetting smoke = add(new ToggleSetting("Hide Smoke", false));
	private final ToggleSetting flames = add(new ToggleSetting("Hide Flames", false));
	private final ToggleSetting crit = add(new ToggleSetting("Hide Hit Particles", false));
	private final ToggleSetting explosion = add(new ToggleSetting("Hide Explosions", false));
	private final ToggleSetting portal = add(new ToggleSetting("Hide Portal", false));
	private final ToggleSetting water = add(new ToggleSetting("Hide Rain and Splash", false));
	private final ToggleSetting bubbles = add(new ToggleSetting("Hide Bubbles", false));
	private final ToggleSetting blocks = add(new ToggleSetting("Hide Block Breaking", false));
	private final ToggleSetting potion = add(new ToggleSetting("Hide Potion Effects", false));
	private final ToggleSetting drips = add(new ToggleSetting("Hide Drips", false));
	private int seed = 0x9E3779B9;

	public ParticleLimiter() {
		super("Particle Limiter", "Hides chosen particle types and skips a share of the rest to save FPS.", Category.RENDER);
		instance = this;
	}

	/** True when this spawn should be skipped. */
	public static boolean shouldDrop(ParticleOptions options) {
		ParticleLimiter p = instance;
		if (p == null || !p.isEnabled()) return false;
		if (p.hideAll.on()) return true;
		if (options != null && p.typeHidden(options.getType())) return true;
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

	private boolean typeHidden(ParticleType<?> t) {
		if (t == ParticleTypes.SMOKE || t == ParticleTypes.LARGE_SMOKE || t == ParticleTypes.CAMPFIRE_COSY_SMOKE
				|| t == ParticleTypes.CAMPFIRE_SIGNAL_SMOKE || t == ParticleTypes.WHITE_SMOKE) return smoke.on();
		if (t == ParticleTypes.FLAME || t == ParticleTypes.SMALL_FLAME || t == ParticleTypes.SOUL_FIRE_FLAME
				|| t == ParticleTypes.COPPER_FIRE_FLAME || t == ParticleTypes.LAVA) return flames.on();
		if (t == ParticleTypes.CRIT || t == ParticleTypes.ENCHANTED_HIT || t == ParticleTypes.DAMAGE_INDICATOR
				|| t == ParticleTypes.SWEEP_ATTACK) return crit.on();
		if (t == ParticleTypes.EXPLOSION || t == ParticleTypes.EXPLOSION_EMITTER || t == ParticleTypes.POOF)
			return explosion.on();
		if (t == ParticleTypes.PORTAL || t == ParticleTypes.REVERSE_PORTAL) return portal.on();
		if (t == ParticleTypes.RAIN || t == ParticleTypes.SPLASH || t == ParticleTypes.FISHING) return water.on();
		if (t == ParticleTypes.BUBBLE || t == ParticleTypes.BUBBLE_POP || t == ParticleTypes.CURRENT_DOWN
				|| t == ParticleTypes.BUBBLE_COLUMN_UP || t == ParticleTypes.UNDERWATER) return bubbles.on();
		if (t == ParticleTypes.BLOCK || t == ParticleTypes.BLOCK_CRUMBLE || t == ParticleTypes.FALLING_DUST
				|| t == ParticleTypes.DUST_PLUME) return blocks.on();
		if (t == ParticleTypes.ENTITY_EFFECT || t == ParticleTypes.EFFECT || t == ParticleTypes.INSTANT_EFFECT
				|| t == ParticleTypes.WITCH) return potion.on();
		if (t == ParticleTypes.DRIPPING_WATER || t == ParticleTypes.FALLING_WATER || t == ParticleTypes.DRIPPING_LAVA
				|| t == ParticleTypes.FALLING_LAVA || t == ParticleTypes.LANDING_LAVA) return drips.on();
		return false;
	}
}
