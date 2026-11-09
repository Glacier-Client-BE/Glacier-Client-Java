package net.glacier.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.glacier.hud.HotbarSkin;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ExperienceBar;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Draws the EXP bar from the Glacier pack colour textures when a colour is picked. */
@Mixin(ExperienceBar.class)
public class MixinExperienceBar {
	@Redirect(method = "extractBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite("
			+ "Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
	private void glacier$expTrack(GuiGraphicsExtractor g, RenderPipeline p, Identifier id, int x, int y, int w, int h) {
		if (HotbarSkin.customExp()) HotbarSkin.expBar(g, false, x, y, w);
		else g.blitSprite(p, id, x, y, w, h);
	}

	@Redirect(method = "extractBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite("
			+ "Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIIIII)V"))
	private void glacier$expFill(GuiGraphicsExtractor g, RenderPipeline p, Identifier id, int tw, int th, int u, int v,
			int x, int y, int w, int h) {
		if (HotbarSkin.customExp()) HotbarSkin.expBar(g, true, x, y, w);
		else g.blitSprite(p, id, tw, th, u, v, x, y, w, h);
	}
}
