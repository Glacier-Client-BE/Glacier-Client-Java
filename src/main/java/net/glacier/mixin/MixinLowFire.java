package net.glacier.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.glacier.module.impl.LowFire;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lowers or hides the first-person fire overlay. Target verified: private static renderFire(PoseStack, MultiBufferSource, TextureAtlasSprite). */
@Mixin(ScreenEffectRenderer.class)
public class MixinLowFire {
	@Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
	private static void glacier$lowFire(PoseStack pose, MultiBufferSource buffers, TextureAtlasSprite sprite, CallbackInfo ci) {
		if (!LowFire.active()) return;
		if (LowFire.hide()) ci.cancel();
		else pose.translate(0f, -LowFire.drop(), 0f);
	}
}
