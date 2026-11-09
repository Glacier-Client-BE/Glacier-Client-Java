package net.glacier.mixin.extraa;

import net.glacier.module.impl.PostFx;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** Exposes the passes of a PostChain (field "passes", verified with javap). */
@Mixin(PostChain.class)
public interface MixinPostChain extends PostFx.ChainAccess {
	@Override
	@Accessor("passes")
	List<PostPass> glacier$passes();
}
