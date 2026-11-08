package net.glacier.mixin;

import net.glacier.social.GlacierIcon;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public class MixinPlayerTabOverlay {
	/** Puts the Glacier icon in front of the tab list name of a Glacier player. */
	@Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
	private void glacier$tabIcon(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
		try {
			Component name = cir.getReturnValue();
			if (name == null || info == null) return;
			if (!GlacierIcon.show(info.getProfile().id(), m -> m.showTab.get())) return;
			cir.setReturnValue(Component.empty().append(GlacierIcon.prefix()).append(name));
		} catch (RuntimeException ignored) {
			// Never break the tab list; the original name stays.
		}
	}
}
