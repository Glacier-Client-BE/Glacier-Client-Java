package net.glacier.mixin.extrab;

import net.glacier.module.impl.OldAnimations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Right click fix (26.3). Verified with javap on Minecraft.handleKeybinds: while the player uses an item every attack
 * click is swallowed (consumeClick loop), so the arm never swings. We turn those clicks into a swing first; no attack
 * is sent. The open screen is read through Gui#screen (the Minecraft#screen field is gone in 26.x).
 */
@Mixin(Minecraft.class)
public abstract class MixinOldMinecraft {
	@Shadow
	public LocalPlayer player;

	@Shadow
	public net.minecraft.client.Options options;

	@Shadow
	public net.minecraft.client.gui.Gui gui;

	@Inject(method = "handleKeybinds", require = 0, at = @At("HEAD"))
	private void glacier$rightClickFix(CallbackInfo ci) {
		LocalPlayer p = this.player;
		if (p == null || this.gui.screen() != null || !p.isUsingItem() || !OldAnimations.rightClickFix()) return;
		while (this.options.keyAttack.consumeClick()) p.swing(InteractionHand.MAIN_HAND);
	}
}
