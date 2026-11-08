package net.glacier.module.impl;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.glacier.GlacierClient;
import net.glacier.module.Category;
import net.glacier.module.Module;
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

	/** Record equality: same id as the category GlacierClient registers, so the key lands in the Glacier group. */
	private final KeyMapping key = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.glacier.zoom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C,
			new KeyMapping.Category(Identifier.fromNamespaceAndPath(GlacierClient.MOD_ID, "main"))));

	private float prev = 1f, cur = 1f;
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
		float target = zooming ? (float) Math.max(1.0, factor.get() * scrollMul) : 1f;
		prev = cur;
		cur = smooth.on() ? cur + (target - cur) * 0.5f : target;
		if (Math.abs(cur - target) < 0.01f) cur = target;
	}

	@Override
	protected void onDisable() {
		prev = cur = 1f;
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
		z.scrollMul = Math.max(0.25, Math.min(4.0, z.scrollMul * (vertical > 0 ? 1.25 : 0.8)));
		return true;
	}
}
