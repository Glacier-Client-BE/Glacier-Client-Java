package net.glacier.module.impl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;

/**
 * Screen-wide colour grading: hue, saturation, brightness and contrast, applied as a post effect
 * (assets/glacier/post_effect/color_adjust.json) between the world and the GUI. Run from MixinGameRenderer.
 */
public class ColorSaturation extends Module {
	private static final Identifier CHAIN = Identifier.fromNamespaceAndPath("glacier", "color_adjust");
	private static ColorSaturation instance;

	private final SliderSetting saturation = add(new SliderSetting("Saturation %", 130, 0, 300, 5));
	private final SliderSetting hue = add(new SliderSetting("Hue Shift", 0, -180, 180, 5));
	private final SliderSetting brightness = add(new SliderSetting("Brightness %", 100, 50, 200, 5));
	private final SliderSetting contrast = add(new SliderSetting("Contrast %", 100, 50, 200, 5));

	private PostChain applied;
	private float lx = Float.NaN, ly, lz, lw;
	private boolean broken;

	public ColorSaturation() {
		super("Color Saturation", "Adjusts saturation, hue, brightness and contrast of the whole screen.", Category.RENDER);
		instance = this;
	}

	public static boolean active() {
		ColorSaturation m = instance;
		return m != null && m.isEnabled() && !m.broken;
	}

	static void render(Minecraft mc, RenderTarget main, GraphicsResourceAllocator pool) {
		ColorSaturation m = instance;
		if (m != null) m.draw(mc, main, pool);
	}

	@Override
	protected void onEnable() {
		broken = false;
		lx = Float.NaN;
	}

	@Override
	protected void onDisable() {
		applied = null;
	}

	private void draw(Minecraft mc, RenderTarget main, GraphicsResourceAllocator pool) {
		try {
			PostChain chain = PostFx.chain(mc, CHAIN);
			if (chain == null) {
				broken = true;
				return;
			}
			if (chain != applied) {
				applied = chain;
				lx = Float.NaN;
			}
			float x = hue.f();
			float y = saturation.f() / 100f;
			float z = brightness.f() / 100f;
			float w = contrast.f() / 100f;
			if (x != lx || y != ly || z != lz || w != lw) {
				if (PostFx.writeVec4(chain, 0, "AdjustConfig", x, y, z, w)) {
					lx = x;
					ly = y;
					lz = z;
					lw = w;
				}
			}
			chain.process(main, pool);
		} catch (Throwable t) {
			broken = true;
		}
	}
}
