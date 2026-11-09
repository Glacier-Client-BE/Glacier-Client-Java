package net.glacier.mixin;

import net.glacier.module.impl.ClearBackground;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clear Background: skips the menu blur. Target verified with javap: protected Screen#renderBlurredBackground(GuiGraphicsExtractor), single overload. */
@Mixin(Screen.class)
public class Nr2ClearBackground {
	@Inject(method = "extractBlurredBackground", at = @At("HEAD"), cancellable = true)
	private void glacier$noBlur(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		if (ClearBackground.active()) ci.cancel();
	}
}
