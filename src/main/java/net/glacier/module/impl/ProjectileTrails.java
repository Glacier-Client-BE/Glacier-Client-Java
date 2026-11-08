package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.ChoiceSetting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.block.Blocks;

/**
 * Particle trail behind arrows, fireballs, eggs, snowballs, ender pearls and fishing hooks, mirroring the Bedrock
 * Projectile Particles pack styles. Pure client tick logic: one particle per moving projectile per tick, no mixin.
 */
public class ProjectileTrails extends Module {
	private final ChoiceSetting style = add(new ChoiceSetting("Trail Style", "Fire", "Fire", "Villager Happy",
			"Blood Particles", "Villager Angry", "Sonic Explosion", "Music Notes", "Heart", "Sand", "Lava", "Redstone",
			"Smoke", "Bubble"));

	private final ParticleOptions blood = new DustParticleOptions(0x8A0303, 1.0f);
	private final ParticleOptions redstone = new DustParticleOptions(0xFF0000, 1.0f);
	private final ParticleOptions sand = new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState());

	public ProjectileTrails() {
		super("Projectile Trails", "Particle trail behind arrows, fireballs, eggs, pearls and snowballs.", Category.RENDER);
	}

	@Override
	public void onTick() {
		ClientLevel level = mc.level;
		if (level == null) return;
		ParticleOptions p = particle();
		for (Entity e : level.entitiesForRendering()) {
			if (!isTrailed(e)) continue;
			if (e.getDeltaMovement().lengthSqr() < 0.01) continue;
			level.addParticle(p, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 0, 0, 0);
		}
	}

	private static boolean isTrailed(Entity e) {
		return e instanceof AbstractArrow || e instanceof Fireball || e instanceof SmallFireball
				|| e instanceof DragonFireball || e instanceof ThrownEgg || e instanceof Snowball
				|| e instanceof ThrownEnderpearl || e instanceof FishingHook;
	}

	private ParticleOptions particle() {
		return switch (style.get()) {
			case "Villager Happy" -> ParticleTypes.HAPPY_VILLAGER;
			case "Blood Particles" -> blood;
			case "Villager Angry" -> ParticleTypes.ANGRY_VILLAGER;
			case "Sonic Explosion" -> ParticleTypes.SONIC_BOOM;
			case "Music Notes" -> ParticleTypes.NOTE;
			case "Heart" -> ParticleTypes.HEART;
			case "Sand" -> sand;
			case "Lava" -> ParticleTypes.LAVA;
			case "Redstone" -> redstone;
			case "Smoke" -> ParticleTypes.SMOKE;
			case "Bubble" -> ParticleTypes.BUBBLE_POP;
			default -> ParticleTypes.FLAME;
		};
	}
}
