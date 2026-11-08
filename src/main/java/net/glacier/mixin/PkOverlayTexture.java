package net.glacier.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import net.glacier.module.impl.HitColor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets HitColor repaint the hurt rows (v 0..7) of the overlay texture. Verified with javap: single public no-arg
 * constructor, private final DynamicTexture texture, NativeImage#setPixel(III) takes ARGB (vanilla hurt = 0xB2FF0000).
 */
@Mixin(OverlayTexture.class)
public class PkOverlayTexture implements HitColor.Recolorable {
	@Shadow
	@Final
	private DynamicTexture texture;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void glacier$attach(CallbackInfo ci) {
		HitColor.attach(this);
	}

	@Override
	public void glacier$paintHurt(int argb) {
		NativeImage img = texture.getPixels();
		if (img == null) return;
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 16; x++) img.setPixel(x, y, argb);
		}
		texture.upload();
	}
}
