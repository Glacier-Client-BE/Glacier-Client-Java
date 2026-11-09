package net.glacier.mixin.extrab;

import net.glacier.module.impl.OldAnimations;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Old damage tilt (26.3). Verified with javap -c on GameRenderer#bobHurt(CameraRenderState, PoseStack): the method is
 * private and no longer calls LivingEntity#getHurtDir(). It reads the render state instead, once, as
 * CameraEntityRenderState.hurtDir (GETFIELD, opcode 180). That value drives the Y rotation before and after the roll,
 * so returning 0 keeps the roll and drops the twist, which is what the old animation did.
 */
@Mixin(GameRenderer.class)
public class MixinOldGameRenderer {
	private static final int GETFIELD = 180;

	@Redirect(method = "bobHurt", require = 0, at = @At(value = "FIELD", opcode = GETFIELD,
			target = "Lnet/minecraft/client/renderer/state/level/CameraEntityRenderState;hurtDir:F"))
	private float glacier$oldTilt(CameraEntityRenderState state) {
		return OldAnimations.damageTilt() ? 0f : state.hurtDir;
	}
}
