package net.glacier.mixin;

import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Writes FogRenderer's private static fogEnabled flag (the same one F3+F toggles). Field verified with javap. */
@Mixin(FogRenderer.class)
public interface FogAccess {
	@Accessor("fogEnabled")
	static void glacier$setFogEnabled(boolean v) {
		throw new AssertionError();
	}
}
