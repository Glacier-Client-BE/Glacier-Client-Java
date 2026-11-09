package net.glacier.mixin.extrab;

import net.glacier.module.impl.BetterArmorBar;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Better Armor Bar. Verified with javap: Gui.renderArmor(GuiGraphicsExtractor, Player, int baseY, int rows, int rowHeight, int x)
 * is private static; its icons sit at y = baseY + (rows - 1) * rowHeight - 10, x = x, and it returns early when the
 * armor value is 0. We reproduce that origin and draw the bar instead.
 */
@Mixin(Hud.class)
public class MixinBetterArmorBar {
	@Inject(method = "extractArmor", require = 0, at = @At("HEAD"), cancellable = true)
	private static void glacier$armorBar(GuiGraphicsExtractor g, Player player, int baseY, int rows, int rowHeight, int x,
										 CallbackInfo ci) {
		if (!BetterArmorBar.replacesVanilla()) return;
		BetterArmorBar.render(g, player, x, baseY + (rows - 1) * rowHeight - 10);
		ci.cancel();
	}
}
