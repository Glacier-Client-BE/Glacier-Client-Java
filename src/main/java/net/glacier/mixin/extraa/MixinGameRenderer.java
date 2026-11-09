package net.glacier.mixin.extraa;

import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.glacier.module.impl.PostFx;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs the Glacier post effects right where vanilla runs its own "postEffectId" chain: GameRenderer#render calls
 * LevelRenderer#doEntityOutline() once (verified with javap) inside the block that only runs while a level is rendered,
 * followed by the vanilla post chain and before the depth clear + GUI extraction.
 */
@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
	@Shadow
	@Final
	private CrossFrameResourcePool resourcePool;

	@Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V",
					shift = At.Shift.AFTER), require = 0)
	private void glacier$postEffects(DeltaTracker delta, boolean tick, CallbackInfo ci) {
		PostFx.run(this.resourcePool);
	}
}
