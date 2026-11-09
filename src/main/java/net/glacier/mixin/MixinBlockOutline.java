package net.glacier.mixin;

import net.glacier.module.impl.BlockOutline;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Overrides colour and width of the block outline. Target verified with javap: private
 * LevelRenderer#renderHitOutline(PoseStack, VertexConsumer, double, double, double, BlockOutlineRenderState, int color, float width),
 * which has exactly one int and one float argument.
 */
@Mixin(LevelRenderer.class)
public class MixinBlockOutline {
	@ModifyVariable(method = "submitHitOutline", at = @At("HEAD"), argsOnly = true)
	private int glacier$outlineColor(int color) {
		return BlockOutline.active() ? BlockOutline.color() : color;
	}

	@ModifyVariable(method = "submitHitOutline", at = @At("HEAD"), argsOnly = true)
	private float glacier$outlineWidth(float width) {
		return BlockOutline.active() ? BlockOutline.width(width) : width;
	}
}
