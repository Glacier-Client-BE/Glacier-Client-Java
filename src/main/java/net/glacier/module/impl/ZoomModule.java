package net.glacier.module.impl;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.glacier.GlacierClient;
import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Hold the zoom key (default C) to divide the FOV. The divisor eases toward its target once per tick and is
 * interpolated with the partial tick in MixinZoom, so the render path does only a lerp and a divide.
 */
public class ZoomModule extends Module {
	private static ZoomModule instance;

	private final SliderSetting factor = add(new SliderSetting("Zoom", 4, 1.5, 50, 0.5));
	private final ToggleSetting smooth = add(new ToggleSetting("Smooth", true));
	private final ToggleSetting scroll = add(new ToggleSetting("Scroll Adjust", true));
	private final SliderSetting scrollStrength = add(new SliderSetting("Scroll Strength", 1.25, 1.05, 2.0, 0.05));
	private final SliderSetting inTicks = add(new SliderSetting("Zoom In Duration (ticks)", 3, 1, 20, 1));
	private final SliderSetting outTicks = add(new SliderSetting("Zoom Out Duration (ticks)", 3, 1, 20, 1));
	private final ChoiceSetting easing = add(new ChoiceSetting("Easing", "Ease Out", "Linear", "Ease Out", "Ease In Out"));
	private final ToggleSetting adjustSensitivity = add(new ToggleSetting("Adjust Mouse Sensitivity", true));

	/** Record equality: same id as the category GlacierClient registers, so the key lands in the Glacier group. */
	private final KeyMapping key = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.glacier.zoom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C,
			new KeyMapping.Category(Identifier.fromNamespaceAndPath(GlacierClient.MOD_ID, "main"))));

	private float prev = 1f, cur = 1f, progress;
	private double savedSensitivity = -1;
	private double scrollMul = 1.0;
	private boolean zooming;

	public ZoomModule() {
		super("Zoom", "Hold C to zoom in; scroll while zooming to adjust.", Category.RENDER);
		instance = this;
	}

	@Override
	public void onTick() {
		zooming = mc.screen == null && key.isDown();
		if (!zooming) scrollMul = 1.0;
		float target = (float) Math.max(1.0, factor.get() * scrollMul);
		prev = cur;
		if (smooth.on()) {
			float step = 1f / (zooming ? inTicks.f() : outTicks.f());
			progress = Math.max(0f, Math.min(1f, progress + (zooming ? step : -step)));
		} else {
			progress = zooming ? 1f : 0f;
		}
		cur = 1f + (target - 1f) * ease(progress);
		applySensitivity(cur);
	}

	private float ease(float t) {
		return switch (easing.get()) {
			case "Linear" -> t;
			case "Ease In Out" -> t * t * (3f - 2f * t);
			default -> 1f - (1f - t) * (1f - t);
		};
	}

	/** Divides mouse sensitivity by the zoom so aiming stays controllable; restores the original when zoom ends. */
	private void applySensitivity(float divisor) {
		if (mc.options == null) return;
		var opt = mc.options.sensitivity();
		if (adjustSensitivity.on() && divisor > 1.001f) {
			if (savedSensitivity < 0) savedSensitivity = opt.get();
			opt.set(Math.max(0.0, savedSensitivity / divisor));
		} else if (savedSensitivity >= 0) {
			opt.set(savedSensitivity);
			savedSensitivity = -1;
		}
	}

	@Override
	protected void onDisable() {
		prev = cur = 1f;
		progress = 0f;
		applySensitivity(1f);
		zooming = false;
		scrollMul = 1.0;
	}

	/** Returns the FOV divided by the current zoom divisor, or fov unchanged when idle. */
	public static float apply(float fov, float partial) {
		ZoomModule z = instance;
		if (z == null || !z.isEnabled()) return fov;
		float d = z.prev + (z.cur - z.prev) * partial;
		return d <= 1.0001f ? fov : fov / d;
	}

	/** Handles a scroll while zooming; returns true when it was consumed. */
	public static boolean onScroll(double vertical) {
		ZoomModule z = instance;
		if (z == null || !z.isEnabled() || !z.zooming || !z.scroll.on() || vertical == 0) return false;
		z.scrollMul = Math.max(0.25, Math.min(4.0, z.scrollMul * (vertical > 0 ? z.scrollStrength.get() : 1.0 / z.scrollStrength.get())));
		return true;
	}
}
