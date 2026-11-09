package net.glacier.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.glacier.hud.HotbarSkin;
import net.glacier.module.ModuleManager;
import net.glacier.module.impl.EditorsModule;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Swaps the hotbar background and selection sprite for the Glacier pack skins. */
@Mixin(Hud.class)
public class MixinHotbarSkin {
	private static final String BLIT = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite("
			+ "Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V";

	@Redirect(method = "extractItemHotbar", at = @At(value = "INVOKE", target = BLIT, ordinal = 0))
	private void glacier$hotbarBackground(GuiGraphicsExtractor g, RenderPipeline p, Identifier id, int x, int y, int w, int h) {
		if (HotbarSkin.customHotbar()) HotbarSkin.hotbarBackground(g, x, y);
		else g.blitSprite(p, id, x, y, w, h);
	}

	@Redirect(method = "extractItemHotbar", at = @At(value = "INVOKE", target = BLIT, ordinal = 1))
	private void glacier$hotbarSelection(GuiGraphicsExtractor g, RenderPipeline p, Identifier id, int x, int y, int w, int h) {
		if (HotbarSkin.customHotbar()) HotbarSkin.hotbarSelection(g, x, y);
		else g.blitSprite(p, id, x, y, w, h);
	}

	@Redirect(method = "extractSlot", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;itemDecorations("
					+ "Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V"))
	private void glacier$itemCount(GuiGraphicsExtractor g, Font font, ItemStack stack, int x, int y) {
		EditorsModule e = ModuleManager.get(EditorsModule.class);
		if (e != null && e.hideItemCount.on()) g.itemDecorations(font, stack, x, y, "");
		else g.itemDecorations(font, stack, x, y);
	}
}
