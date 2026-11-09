package net.glacier.mixin.extraa;

import net.glacier.module.impl.GlintColorizer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Swaps the enchantment glint texture for the tinted copy of the Glint Colorizer. Verified with javap:
 * RenderSetup#getTextures() resolves every bound texture through exactly one TextureManager#getTexture(Identifier)
 * call (per draw, from RenderType#draw), and the glint render types bind ItemRenderer.ENCHANTED_GLINT_ITEM/ARMOR.
 */
@Mixin(RenderSetup.class)
public class MixinRenderSetup {
	@Redirect(method = "getTextures()Ljava/util/Map;",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/texture/TextureManager;getTexture(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/texture/AbstractTexture;"),
			require = 0)
	private AbstractTexture glacier$glintTexture(TextureManager manager, Identifier id) {
		AbstractTexture tinted = GlintColorizer.textureFor(id);
		return tinted != null ? tinted : manager.getTexture(id);
	}
}
