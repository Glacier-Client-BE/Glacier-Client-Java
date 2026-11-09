package net.glacier.module.impl;

import net.glacier.config.Config;
import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.perf.LowEndPreset;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.server.level.ParticleStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

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

	public final ToggleSetting noAo = add(new ToggleSetting("No Ambient Occlusion", false));
	public final ToggleSetting lowBlend = add(new ToggleSetting("Minimal Biome Blend", false));
	public final ToggleSetting entityDist = add(new ToggleSetting("Reduce Entity Distance", false));
	public final ToggleSetting capRender = add(new ToggleSetting("Cap Render Distance", false));
	public final SliderSetting renderCap = add(new SliderSetting("Render Distance Cap", 8, 2, 32, 1));
	public final ToggleSetting capSim = add(new ToggleSetting("Cap Simulation Distance", false));
	public final SliderSetting simCap = add(new SliderSetting("Simulation Distance Cap", 6, 5, 32, 1));

	/** Cap style boosts: force a value while on, restore the player's own afterwards. */
	private final List<Boost<?>> boosts = new ArrayList<>();

	private final class Boost<T> {
		final ToggleSetting toggle;
		final Supplier<OptionInstance<T>> option;
		final Supplier<T> target;
		final BinaryOperator<T> pick;
		T saved;
		boolean has;

		Boost(ToggleSetting toggle, Supplier<OptionInstance<T>> option, Supplier<T> target, BinaryOperator<T> pick) {
			this.toggle = toggle;
			this.option = option;
			this.target = target;
			this.pick = pick;
		}

		void apply(boolean active) {
			OptionInstance<T> o = option.get();
			if (active && toggle.on()) {
				if (!has) {
					saved = o.get();
					has = true;
				}
				force(o, pick.apply(has ? saved : o.get(), target.get()));
			} else if (has) {
				force(o, saved);
				has = false;
				saved = null;
			}
		}
	}

	private Boolean savedShadows;
	private ParticleStatus savedParticles;
	private CloudStatus savedClouds;
	private Boolean savedBob;
	/** True while the low-end preset's values are in effect. */
	private boolean presetApplied;

	public PerformanceMods() {
		super("Performance Mods", "Reduces visual work to raise frame rate.", Category.SYSTEM);
		boosts.add(new Boost<>(noAo, () -> mc.options.ambientOcclusion(), () -> false, (a, b) -> b));
		boosts.add(new Boost<>(lowBlend, () -> mc.options.biomeBlendRadius(), () -> 1, (a, b) -> b));
		boosts.add(new Boost<>(entityDist, () -> mc.options.entityDistanceScaling(), () -> 0.5, Math::min));
		boosts.add(new Boost<>(capRender, () -> mc.options.renderDistance(), () -> renderCap.i(), Math::min));
		boosts.add(new Boost<>(capSim, () -> mc.options.simulationDistance(), () -> simCap.i(), Math::min));
	}

	/**
	 * Enables the module when any boost is on, then applies or restores every boost. Pages call this after a toggle.
	 * The low-end preset only changes state when its toggle has flipped, so this is cheap to call often.
	 */
	public void syncEnabled() {
		syncPreset();
		setEnabled(lowEndPreset.on() || noShadows.on() || minParticles.on() || noClouds.on() || noViewBob.on() || anyBoost());
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

	private boolean anyBoost() {
		for (Boost<?> b : boosts) if (b.toggle.on()) return true;
		return false;
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
		for (Boost<?> b : boosts) b.apply(active);

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
