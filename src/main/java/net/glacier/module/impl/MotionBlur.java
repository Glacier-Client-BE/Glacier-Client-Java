package net.glacier.module.impl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;

/**
 * Motion blur by frame accumulation: a persistent render target keeps the previous blurred frame and the post pass
 * (assets/glacier/post_effect/motion_blur.json) blends it into the current frame. Run from MixinGameRenderer. The blend
 * weight is converted to a per-frame weight from the current FPS so the blur length stays roughly constant in time.
 */
public class MotionBlur extends Module {
	private static final Identifier CHAIN = Identifier.fromNamespaceAndPath("glacier", "motion_blur");
	private static MotionBlur instance;

	private final SliderSetting strength = add(new SliderSetting("Strength %", 50, 5, 95, 1));
	private final ToggleSetting adaptive = add(new ToggleSetting("FPS Adaptive", true));

	private PostChain applied;
	private float appliedWeight = -1f;
	private boolean broken;

	public MotionBlur() {
		super("Motion Blur", "Smears frames together for a cinematic motion blur.", Category.RENDER);
		instance = this;
	}

	public static boolean active() {
		MotionBlur m = instance;
		return m != null && m.isEnabled() && !m.broken;
	}

	static void render(Minecraft mc, RenderTarget main, GraphicsResourceAllocator pool) {
		MotionBlur m = instance;
		if (m != null) m.draw(mc, main, pool);
	}

	@Override
	protected void onEnable() {
		broken = false;
		appliedWeight = -1f;
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
				appliedWeight = -1f;
			}
			float w = strength.f() / 100f;
			if (adaptive.on()) {
				int fps = Math.max(20, Math.min(500, mc.getFps()));
				w = (float) Math.pow(w, 60.0 / fps);
			}
			if (Math.abs(w - appliedWeight) > 0.01f) {
				if (PostFx.writeVec4(chain, 0, "BlurConfig", w, 0f, 0f, 0f)) appliedWeight = w;
			}
			chain.process(main, pool);
		} catch (Throwable t) {
			broken = true;
		}
	}
}
