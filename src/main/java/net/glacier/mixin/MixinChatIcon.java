package net.glacier.mixin;

import com.mojang.authlib.GameProfile;
import net.glacier.social.GlacierIcon;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.time.Instant;
import java.util.UUID;

@Mixin(ChatListener.class)
public abstract class MixinChatIcon {
	/** Sender of the signed player message that showMessageToPlayer is adding right now. Client thread only. */
	@Unique
	private UUID glacier$sender;

	/** Remembers who sent the signed message; the two addMessage calls below run synchronously inside this method. */
	@Inject(method = "showMessageToPlayer", at = @At("HEAD"))
	private void glacier$senderBegin(ChatType.Bound bound, PlayerChatMessage message, Component decorated,
			GameProfile profile, boolean flag, Instant time, CallbackInfoReturnable<Boolean> cir) {
		try {
			glacier$sender = message == null ? null : message.sender();
		} catch (RuntimeException ignored) {
			glacier$sender = null;
		}
	}

	/** Prefixes the Glacier icon on a player chat line. Messages without a known sender are not changed. */
	@ModifyArg(method = "showMessageToPlayer",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/gui/components/ChatComponent;addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V"),
			index = 0)
	private Component glacier$chatIcon(Component message) {
		try {
			UUID sender = glacier$sender;
			if (sender == null || message == null || !GlacierIcon.show(sender, m -> m.showChat.get())) return message;
			return Component.empty().append(GlacierIcon.prefix()).append(message);
		} catch (RuntimeException ignored) {
			return message;
		}
	}
}
