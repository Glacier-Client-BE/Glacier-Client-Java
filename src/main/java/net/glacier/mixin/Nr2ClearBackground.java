package net.glacier.mixin;

import net.glacier.module.impl.ClearBackground;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clear Background: skips the menu blur. Target verified with javap: protected Screen#renderBlurredBackground(GuiGraphics), single overload. */
@Mixin(Screen.class)
public class Nr2ClearBackground {
	@Inject(method = "renderBlurredBackground", at = @At("HEAD"), cancellable = true)
	private void glacier$noBlur(GuiGraphics graphics, CallbackInfo ci) {
		if (ClearBackground.active()) ci.cancel();
	}
}
