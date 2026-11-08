package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.SliderSetting;

/** Makes thrown-item projectiles (eggs, snowballs, ender pearls) tumble in flight (see PkSpinProjectile). */
public class SpinningProjectiles extends Module {
	private static SpinningProjectiles instance;

	private final SliderSetting speed = add(new SliderSetting("Spin Speed", 1.0, 0.25, 4.0, 0.25));

	public SpinningProjectiles() {
		super("Spinning Projectiles", "Eggs, snowballs and ender pearls spin while in flight.", Category.RENDER);
		instance = this;
	}

	public static boolean active() {
		SpinningProjectiles s = instance;
		return s != null && s.isEnabled();
	}

	public static float speed() {
		SpinningProjectiles s = instance;
		return s == null ? 1f : s.speed.f();
	}
}
