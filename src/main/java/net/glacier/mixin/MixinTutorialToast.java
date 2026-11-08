package net.glacier.mixin;

import net.glacier.module.ModuleManager;
import net.glacier.module.impl.ElementsModule;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.gui.components.toasts.Toast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tutorial tips are toasts; asking for HIDE removes them from the toast stack without touching tutorial state. */
@Mixin(TutorialToast.class)
public class MixinTutorialToast {
	@Inject(method = "getWantedVisibility", at = @At("HEAD"), cancellable = true)
	private void glacier$hideTip(CallbackInfoReturnable<Toast.Visibility> cir) {
		ElementsModule e = ModuleManager.get(ElementsModule.class);
		if (e != null && e.hideTip.on()) cir.setReturnValue(Toast.Visibility.HIDE);
	}
}
