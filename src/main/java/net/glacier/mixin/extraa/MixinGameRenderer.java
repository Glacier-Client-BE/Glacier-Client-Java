package net.glacier.mixin.extraa;

import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.glacier.module.impl.PostFx;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs the Glacier post effects right where vanilla runs its own "postEffectId" chain: GameRenderer#render() (no
 * arguments in 26.x) calls LevelRenderer#blitEntityOutline() once (verified with javap, formerly doEntityOutline) inside
 * the block that only runs while a level is rendered, followed by applyPostEffects (the vanilla chains) and before the
 * depth clear + GUI rendering.
 */
@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
	@Shadow
	@Final
	private CrossFrameResourcePool resourcePool;

	@Inject(method = "render()V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;blitEntityOutline()V",
					shift = At.Shift.AFTER), require = 0)
	private void glacier$postEffects(CallbackInfo ci) {
		PostFx.run(this.resourcePool);
	}
}
