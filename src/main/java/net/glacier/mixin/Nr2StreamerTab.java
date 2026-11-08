package net.glacier.mixin;

import net.glacier.module.impl.StreamerMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Streamer Mode: aliases the local player's tab list entry. Same verified target as MixinPlayerTabOverlay. */
@Mixin(PlayerTabOverlay.class)
public class Nr2StreamerTab {
	@Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
	private void glacier$streamerTab(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
		try {
			Component name = cir.getReturnValue();
			Minecraft mc = Minecraft.getInstance();
			if (name == null || info == null || mc.player == null) return;
			boolean self = info.getProfile().id().equals(mc.player.getUUID());
			cir.setReturnValue(StreamerMode.forTab(name, self));
		} catch (RuntimeException ignored) {
			// Keep the original tab name.
		}
	}
}
