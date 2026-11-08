package net.glacier.mixin;

import net.glacier.module.impl.StreamerMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Streamer Mode: aliases the local player's name tag. Same verified target as MixinPlayerNametag (EntityRenderer#getNameTag). */
@Mixin(EntityRenderer.class)
public abstract class Nr2StreamerNametag {
	@Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
	private void glacier$streamerNametag(Entity entity, CallbackInfoReturnable<Component> cir) {
		try {
			Component name = cir.getReturnValue();
			if (name == null) return;
			cir.setReturnValue(StreamerMode.forNametag(name, entity == Minecraft.getInstance().player));
		} catch (RuntimeException ignored) {
			// Keep the original name tag.
		}
	}
}
