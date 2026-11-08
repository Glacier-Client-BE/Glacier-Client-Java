package net.glacier.mixin;

import net.glacier.module.impl.ZoomModule;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Divides the world FOV while zooming. Target verified with javap: private GameRenderer#getFov(Camera;F;Z)F. */
@Mixin(GameRenderer.class)
public class MixinZoom {
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void glacier$zoom(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
		float fov = cir.getReturnValueF();
		float z = ZoomModule.apply(fov, partialTick);
		if (z != fov) cir.setReturnValue(z);
	}
}
