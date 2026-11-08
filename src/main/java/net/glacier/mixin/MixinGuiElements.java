package net.glacier.mixin;

import net.glacier.module.ModuleManager;
import net.glacier.module.impl.ElementsModule;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Gui.class)
public class MixinGuiElements {
	/** The third drawString in displayScoreboardSidebar (ordinal 2) draws the score number; the first two are title and name. */
	@Redirect(method = "displayScoreboardSidebar", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
			ordinal = 2))
	private void glacier$hideScores(GuiGraphics g, Font font, Component text, int x, int y, int color, boolean shadow) {
		ElementsModule e = ModuleManager.get(ElementsModule.class);
		if (e != null && e.hideScores.on()) return;
		g.drawString(font, text, x, y, color, shadow);
	}
}
