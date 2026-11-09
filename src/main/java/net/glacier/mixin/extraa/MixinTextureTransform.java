package net.glacier.mixin.extraa;

import net.glacier.module.impl.GlintColorizer;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Glint speed/scale. Verified with javap: TextureTransform#getMatrix() (single overload) returns a new Matrix4f from the
 * supplier on every call; the three glint transforms are named "glint_texturing", "entity_glint_texturing" and
 * "armor_entity_glint_texturing" (private final String name).
 */
@Mixin(TextureTransform.class)
public abstract class MixinTextureTransform {
	
	@Inject(method = "createMatrix", at = @At("RETURN"), require = 0)
	private void glacier$glintMatrix(CallbackInfoReturnable<Matrix4f> cir) {
		if (!GlintColorizer.active()) return;
		Object self = this;
		if (self != TextureTransform.GLINT_TEXTURING && self != TextureTransform.ENTITY_GLINT_TEXTURING && self != TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING) return;
		Matrix4f m = cir.getReturnValue();
		if (m != null) GlintColorizer.adjust(m);
	}
}
