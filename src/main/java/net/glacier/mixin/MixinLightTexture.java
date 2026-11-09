package net.glacier.mixin;

import net.glacier.module.impl.Fullbright;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.3 lightmap: LightmapRenderStateExtractor#extract fills a LightmapRenderState that both the OpenGL and the Vulkan
 * lightmap pass read. Fullbright replaces the gamma read (the only Double#floatValue before the darkness scale read,
 * verified with javap) and, at the end of extract, raises the night vision intensity (and gives it a white tint when the
 * vanilla code left none) so the world is lit like under the night vision potion. Disabled, vanilla values are kept.
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

	@Inject(method = "extract", at = @At("RETURN"))
	private void glacier$fullbrightNightVision(LightmapRenderState state, float partialTick, CallbackInfo ci) {
		if (!Fullbright.active()) return;
		float nv = Fullbright.nightVisionValue();
		if (nv > state.nightVisionEffectIntensity) {
			state.nightVisionEffectIntensity = nv;
			if (state.nightVisionColor == null) state.nightVisionColor = LightmapRenderStateExtractor.WHITE;
		}
	}
}
