package net.glacier.mixin;

import net.glacier.social.GlacierIcon;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class MixinPlayerNametag {
	/** Puts the Glacier icon in front of the name tag over a Glacier player's head. Only players are touched. */
	@Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
	private void glacier$nametagIcon(Entity entity, CallbackInfoReturnable<Component> cir) {
		try {
			Component name = cir.getReturnValue();
			if (name == null || !(entity instanceof Player player)) return;
			if (!GlacierIcon.show(player.getUUID(), m -> m.showNametag.get())) return;
			cir.setReturnValue(Component.empty().append(GlacierIcon.prefix()).append(name));
		} catch (RuntimeException ignored) {
			// Never break entity rendering; the original name stays.
		}
	}
}
