package net.glacier.mixin;

import net.glacier.module.impl.Fullbright;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Replaces the gamma read in LightTexture#updateLightTexture (the second Double#floatValue in that method, which
 * feeds the lightmap shader's brightness factor) while Fullbright is enabled. Disabled, it returns the vanilla value.
 */
@Mixin(LightmapRenderStateExtractor.class)
public class MixinLightTexture {
	@Redirect(
			method = "extract",
			at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 0)
	)
	private float glacier$fullbrightGamma(Double gamma) {
		return Fullbright.active() ? Fullbright.gammaValue() : gamma.floatValue();
	}
}
