package net.glacier.mixin.extrac;

import net.glacier.module.impl.ChatPlus;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Chat Plus copy on click. ChatScreen.mouseClicked(MouseButtonEvent, boolean) is public and not overloaded. */
@Mixin(ChatScreen.class)
public class ExtraCChatScreen {
	@Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z", at = @At("HEAD"), cancellable = true, require = 0)
	private void glacier$copyClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		if (ChatPlus.tryCopy(event)) cir.setReturnValue(true);
	}
}
