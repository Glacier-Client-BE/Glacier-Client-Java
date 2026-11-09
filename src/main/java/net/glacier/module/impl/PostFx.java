package net.glacier.module.impl;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * Shared plumbing for full-screen post effects (Motion Blur, Color Saturation). The chains are plain vanilla
 * post_effect JSON files in assets/glacier; vanilla compiles and caches them in ShaderManager. Their uniform buffers are
 * created once with static values, so {@link #writeVec4} swaps in a freshly built buffer when a setting changes
 * (through the accessors in MixinPostChain / MixinPostPass). Everything degrades to a no-op if the chain failed to
 * compile or an accessor mixin did not apply.
 */
public final class PostFx {
	/** Implemented by MixinPostChain. */
	public interface ChainAccess {
		List<PostPass> glacier$passes();
	}

	/** Implemented by MixinPostPass. */
	public interface PassAccess {
		Map<String, GpuBuffer> glacier$uniforms();
	}

	private PostFx() {
	}

	/** Called from MixinGameRenderer right after the world was rendered and before the GUI is drawn. */
	public static void run(GraphicsResourceAllocator pool) {
		boolean blur = MotionBlur.active();
		boolean sat = ColorSaturation.active();
		if (!blur && !sat) return;
		Minecraft mc = Minecraft.getInstance();
		RenderTarget main = mc.gameRenderer.mainRenderTarget();
		if (main == null) return;
		if (sat) ColorSaturation.render(mc, main, pool);
		if (blur) MotionBlur.render(mc, main, pool);
	}

	/** Returns the compiled chain, or null when it is unavailable (compile error is logged once by vanilla). */
	static PostChain chain(Minecraft mc, Identifier id) {
		return mc.getShaderManager().getPostChain(id, LevelTargetBundle.MAIN_TARGETS);
	}

	/**
	 * Replaces the uniform block {@code block} of pass {@code pass} with a vec4 buffer.
	 *
	 * @return false when the chain layout is not what we expect or an accessor is missing
	 */
	static boolean writeVec4(PostChain chain, int pass, String block, float x, float y, float z, float w) {
		try {
			List<PostPass> passes = ((ChainAccess) (Object) chain).glacier$passes();
			if (passes == null || pass >= passes.size()) return false;
			Map<String, GpuBuffer> map = ((PassAccess) (Object) passes.get(pass)).glacier$uniforms();
			GpuBuffer old = map.get(block);
			if (old == null) return false;
			GpuBuffer fresh;
			try (MemoryStack stack = MemoryStack.stackPush()) {
				ByteBuffer data = Std140Builder.onStack(stack, 16).putVec4(x, y, z, w).get();
				fresh = RenderSystem.getDevice().createBuffer(() -> "glacier post uniform", GpuBuffer.USAGE_UNIFORM, data);
			}
			map.put(block, fresh);
			old.close();
			return true;
		} catch (Throwable t) {
			return false;
		}
	}
}
