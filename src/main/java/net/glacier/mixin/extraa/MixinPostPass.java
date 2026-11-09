package net.glacier.mixin.extraa;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import net.glacier.module.impl.PostFx;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** Exposes the custom uniform buffers of a PostPass (field "customUniforms", verified with javap). */
@Mixin(PostPass.class)
public interface MixinPostPass extends PostFx.PassAccess {
	@Override
	@Accessor("customUniforms")
	Map<String, GpuBuffer> glacier$uniforms();
}
