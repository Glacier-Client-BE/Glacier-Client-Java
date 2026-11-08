package net.glacier.module.impl;

import net.glacier.config.Config;
import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.perf.LowEndPreset;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.server.level.ParticleStatus;

import java.util.Objects;

/**
 * Performance boosts shown on the Performance page. Each boost overrides one vanilla option while its toggle is on
 * and restores the player's value when it is switched off. The module is enabled while any boost is on.
 */
public class PerformanceMods extends Module {
	/** Batch preset: applied once when switched on, restored once when switched off (never per tick). */
	public final ToggleSetting lowEndPreset = add(new ToggleSetting("Low-end laptop preset", false));
	public final ToggleSetting noShadows = add(new ToggleSetting("No Entity Shadows", false));
	public final ToggleSetting minParticles = add(new ToggleSetting("Minimal Particles", false));
	public final ToggleSetting noClouds = add(new ToggleSetting("No Clouds", false));
	public final ToggleSetting noViewBob = add(new ToggleSetting("No View Bobbing", false));

	private Boolean savedShadows;
	private ParticleStatus savedParticles;
	private CloudStatus savedClouds;
	private Boolean savedBob;
	/** True while the low-end preset's values are in effect. */
	private boolean presetApplied;

	public PerformanceMods() {
		super("Performance Mods", "Reduces visual work to raise frame rate.", Category.SYSTEM);
	}

	/**
	 * Enables the module when any boost is on, then applies or restores every boost. Pages call this after a toggle.
	 * The low-end preset only changes state when its toggle has flipped, so this is cheap to call often.
	 */
	public void syncEnabled() {
		syncPreset();
		setEnabled(lowEndPreset.on() || noShadows.on() || minParticles.on() || noClouds.on() || noViewBob.on());
		apply();
	}

	@Override
	public void afterLoad() {
		if (LowEndPreset.hasRestoreFile()) {
			// The last session ended with the preset applied (crash or force quit). Undo it once the game is running.
			lowEndPreset.set(false);
			mc.execute(() -> {
				LowEndPreset.restore(mc);
				Config.save();
			});
		}
		syncEnabled();
	}

	/** Applies the preset on the off-to-on edge and restores it on the on-to-off edge. */
	private void syncPreset() {
		boolean want = lowEndPreset.on();
		if (want == presetApplied) return;
		if (want) {
			presetApplied = LowEndPreset.apply(mc);
			if (!presetApplied) lowEndPreset.set(false);
		} else {
			LowEndPreset.restore(mc);
			presetApplied = false;
		}
	}

	@Override
	public void onTick() {
		apply();
	}

	@Override
	protected void onDisable() {
		apply();
	}

	private void apply() {
		Options o = mc.options;
		if (o == null) return;
		boolean active = isEnabled();

		if (active && noShadows.on()) {
			if (savedShadows == null) savedShadows = o.entityShadows().get();
			force(o.entityShadows(), false);
		} else if (savedShadows != null) {
			force(o.entityShadows(), savedShadows);
			savedShadows = null;
		}

		if (active && minParticles.on()) {
			if (savedParticles == null) savedParticles = o.particles().get();
			force(o.particles(), ParticleStatus.MINIMAL);
		} else if (savedParticles != null) {
			force(o.particles(), savedParticles);
			savedParticles = null;
		}

		if (active && noClouds.on()) {
			if (savedClouds == null) savedClouds = o.cloudStatus().get();
			force(o.cloudStatus(), CloudStatus.OFF);
		} else if (savedClouds != null) {
			force(o.cloudStatus(), savedClouds);
			savedClouds = null;
		}

		if (active && noViewBob.on()) {
			if (savedBob == null) savedBob = o.bobView().get();
			force(o.bobView(), false);
		} else if (savedBob != null) {
			force(o.bobView(), savedBob);
			savedBob = null;
		}
	}

	/** Writes only when the value differs, so option callbacks do not fire every tick. */
	private static <T> void force(OptionInstance<T> option, T value) {
		if (!Objects.equals(option.get(), value)) option.set(value);
	}
}
