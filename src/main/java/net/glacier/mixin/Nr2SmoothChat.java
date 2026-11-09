package net.glacier.mixin;

import net.glacier.module.impl.SmoothChat;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
 * public render(GuiGraphicsExtractor, Font, int, int, int, boolean, boolean).
 */
@Mixin(ChatComponent.class)
public class Nr2SmoothChat {
	@Unique
	private boolean glacier$pushed;

	@Inject(method = "addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V", at = @At("HEAD"))
	private void glacier$smoothAdd(Component c, MessageSignature sig, GuiMessageTag tag, CallbackInfo ci) {
		SmoothChat.onMessage();
	}

	@Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V", at = @At("HEAD"))
	private void glacier$smoothPush(GuiGraphicsExtractor g, Font font, int a, int b, int c, net.minecraft.client.gui.components.ChatComponent.DisplayMode d, boolean e, CallbackInfo ci) {
		float off = SmoothChat.offset();
		glacier$pushed = off > 0f;
		if (glacier$pushed) {
			g.pose().pushMatrix();
			g.pose().translate(0f, off);
		}
	}

	@Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V", at = @At("RETURN"))
	private void glacier$smoothPop(GuiGraphicsExtractor g, Font font, int a, int b, int c, net.minecraft.client.gui.components.ChatComponent.DisplayMode d, boolean e, CallbackInfo ci) {
		if (glacier$pushed) {
			g.pose().popMatrix();
			glacier$pushed = false;
		}
	}
}
