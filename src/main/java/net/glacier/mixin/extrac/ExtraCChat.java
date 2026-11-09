package net.glacier.mixin.extrac;

import net.glacier.module.impl.ChatPlus;
import net.glacier.module.impl.ChatTextLookup;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Chat Plus hooks on ChatComponent. Every target was read with javap on the 1.21.11 merged jar:
 * <ul>
 * <li>addMessage(Component, MessageSignature, GuiMessageTag): the Component argument is rewritten (timestamp, head,
 * repeat counter, name highlight). Nr2SmoothChat injects at HEAD of the same method but only reads, so both coexist.</li>
 * <li>addMessageToDisplayQueue and addMessageToQueue each hold one {@code bipush 100} (the history size).</li>
 * <li>getScale()D, getWidth()I are private and used by render, hit testing and line wrapping, so overriding their
 * result keeps all of them consistent. getLineHeight() and the private render read the line spacing option through
 * OptionInstance.get(): ordinal 0 inside getLineHeight, ordinals 0 (chat opacity), 1 (background opacity) and 2
 * (line spacing) inside extractRenderState(ChatGraphicsAccess, int, int, DisplayMode), verified with javap -c on 26.3.</li>
 * </ul>
 */
@Mixin(ChatComponent.class)
public abstract class ExtraCChat implements ChatTextLookup {
	/* Full descriptor: ChatComponent has two extractRenderState overloads, and only the private one reads the options. */
	private static final String RENDER = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V";
	private static final String OPTION_GET = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;";

	@Shadow
	@Final
	private List<GuiMessage> allMessages;
	@Shadow
	@Final
	private List<GuiMessage.Line> trimmedMessages;
	@Shadow
	private int chatScrollbarPos;

	@Shadow
	private void refreshTrimmedMessages() {
	}

	@Shadow
	private double getScale() {
		return 1.0;
	}

	@Shadow
	private int getWidth() {
		return 0;
	}

	@Shadow
	private int getLineHeight() {
		return 9;
	}

	@Shadow
	public int getLinesPerPage() {
		return 0;
	}

	@ModifyVariable(method = "addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
			at = @At("HEAD"), argsOnly = true, require = 0)
	private Component glacier$chatPlus(Component c) {
		ChatPlus p = ChatPlus.active();
		if (p == null || c == null) return c;
		Component out = p.process(c);
		if (p.takeRemovePrevious() && !allMessages.isEmpty()) {
			allMessages.removeFirst();
			refreshTrimmedMessages();
		}
		return out;
	}

	@ModifyConstant(method = {"addMessageToDisplayQueue", "addMessageToQueue"}, constant = @Constant(intValue = 100), require = 0)
	private int glacier$history(int vanilla) {
		return ChatPlus.historyOr(vanilla);
	}

	@Inject(method = "getScale()D", at = @At("RETURN"), cancellable = true, require = 0)
	private void glacier$scale(CallbackInfoReturnable<Double> cir) {
		double v = ChatPlus.scaleOr(cir.getReturnValue());
		if (v != cir.getReturnValue()) cir.setReturnValue(v);
	}

	@Inject(method = "getWidth()I", at = @At("RETURN"), cancellable = true, require = 0)
	private void glacier$width(CallbackInfoReturnable<Integer> cir) {
		int v = ChatPlus.widthOr(cir.getReturnValue());
		if (v != cir.getReturnValue()) cir.setReturnValue(v);
	}

	@Redirect(method = "getLineHeight()I", at = @At(value = "INVOKE", target = OPTION_GET, ordinal = 0), require = 0)
	private Object glacier$lineHeightSpacing(OptionInstance<?> option) {
		Double v = ChatPlus.spacingOrNull();
		return v != null ? (Object) v : option.get();
	}

	@Redirect(method = RENDER, at = @At(value = "INVOKE", target = OPTION_GET, ordinal = 0), require = 0)
	private Object glacier$textOpacity(OptionInstance<?> option) {
		Double v = ChatPlus.textOpacityOrNull();
		return v != null ? (Object) v : option.get();
	}

	@Redirect(method = RENDER, at = @At(value = "INVOKE", target = OPTION_GET, ordinal = 1), require = 0)
	private Object glacier$backgroundOpacity(OptionInstance<?> option) {
		Double v = ChatPlus.backgroundOrNull();
		return v != null ? (Object) v : option.get();
	}

	@Redirect(method = RENDER, at = @At(value = "INVOKE", target = OPTION_GET, ordinal = 2), require = 0)
	private Object glacier$renderSpacing(OptionInstance<?> option) {
		Double v = ChatPlus.spacingOrNull();
		return v != null ? (Object) v : option.get();
	}

	/**
	 * Mirrors the render geometry: the newest line sits above y = floor((guiHeight - 40) / scale), every line is
	 * getLineHeight() tall, the pose is scaled by the chat scale and shifted 4 px right. A line is one entry of
	 * trimmedMessages (newest first, endOfEntry marks the last line of a message), so a whole message is the run of
	 * lines around the clicked one.
	 */
	@Override
	public String glacier$textAt(double mouseX, double mouseY) {
		double scale = getScale();
		int lineH = getLineHeight();
		if (scale <= 0.0 || lineH <= 0 || trimmedMessages.isEmpty()) return null;
		int bottom = (int) Math.floor((Minecraft.getInstance().getWindow().getGuiScaledHeight() - 40) / scale);
		double d = bottom - mouseY / scale;
		double x = mouseX / scale - 4.0;
		if (d < 0.0 || x < -4.0 || x > getWidth() / scale) return null;
		int visible = (int) (d / lineH);
		if (visible >= getLinesPerPage()) return null;
		int k = visible + chatScrollbarPos;
		if (k < 0 || k >= trimmedMessages.size()) return null;
		int first = k;
		while (first + 1 < trimmedMessages.size() && !trimmedMessages.get(first + 1).endOfEntry()) first++;
		int last = k;
		while (last > 0 && !trimmedMessages.get(last).endOfEntry()) last--;
		StringBuilder sb = new StringBuilder();
		for (int i = first; i >= last; i--) {
			if (sb.length() > 0) sb.append(' ');
			trimmedMessages.get(i).content().accept((index, style, cp) -> {
				if (ChatPlus.copyable(cp)) sb.appendCodePoint(cp);
				return true;
			});
		}
		return sb.toString().trim();
	}
}
