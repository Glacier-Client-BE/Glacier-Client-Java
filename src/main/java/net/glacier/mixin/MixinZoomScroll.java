package net.glacier.mixin;

import net.glacier.module.impl.ZoomModule;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While zooming, the wheel adjusts zoom instead of the hotbar. Target verified: private MouseHandler#onScroll(JDD)V. */
@Mixin(MouseHandler.class)
public class MixinZoomScroll {
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void glacier$zoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
		if (ZoomModule.onScroll(vertical)) ci.cancel();
	}
}
