package net.glacier.mixin;

import net.glacier.module.impl.NoAdvancement;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Drops advancement/recipe toasts before they are queued. Target verified with javap: public ToastManager#addToast(Toast). */
@Mixin(ToastManager.class)
public class MixinToastManager {
	@Inject(method = "addToast", at = @At("HEAD"), cancellable = true)
	private void glacier$filterToast(Toast toast, CallbackInfo ci) {
		if (toast instanceof AdvancementToast && NoAdvancement.hideAdvancements()) ci.cancel();
		else if (toast instanceof RecipeToast && NoAdvancement.hideRecipes()) ci.cancel();
	}
}
