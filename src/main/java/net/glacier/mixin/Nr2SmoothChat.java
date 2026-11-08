package net.glacier.mixin;

import net.glacier.module.impl.SmoothChat;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Smooth Chat. Targets verified with javap on the 1.21.11 merged jar, both selected by full descriptor because
 * each name has overloads: public addMessage(Component, MessageSignature, GuiMessageTag) and
 * public render(GuiGraphics, Font, int, int, int, boolean, boolean).
 */
@Mixin(ChatComponent.class)
public class Nr2SmoothChat {
	@Unique
	private boolean glacier$pushed;

	@Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", at = @At("HEAD"))
	private void glacier$smoothAdd(Component c, MessageSignature sig, GuiMessageTag tag, CallbackInfo ci) {
		SmoothChat.onMessage();
	}

	@Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;IIIZZ)V", at = @At("HEAD"))
	private void glacier$smoothPush(GuiGraphics g, Font font, int a, int b, int c, boolean d, boolean e, CallbackInfo ci) {
		float off = SmoothChat.offset();
		glacier$pushed = off > 0f;
		if (glacier$pushed) {
			g.pose().pushMatrix();
			g.pose().translate(0f, off);
		}
	}

	@Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;IIIZZ)V", at = @At("RETURN"))
	private void glacier$smoothPop(GuiGraphics g, Font font, int a, int b, int c, boolean d, boolean e, CallbackInfo ci) {
		if (glacier$pushed) {
			g.pose().popMatrix();
			glacier$pushed = false;
		}
	}
}
