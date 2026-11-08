package net.glacier.mixin;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import net.glacier.perf.FpsLimiter;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Caps the frame rate at FpsLimiter#unfocusedCap() while the game window is not focused.
 *
 * Verified with javap on the 1.21.11 Mojang-mapped merged jar:
 * - Target: com.mojang.blaze3d.platform.FramerateLimitTracker#getFramerateLimit()I. Minecraft.runTick calls it once per
 *   frame after Window.updateDisplay (offset 659 to 663 of the bytecode) and passes the result to
 *   RenderSystem.limitDisplayFPS when it is below 260. No setFramerateLimit call happens per frame, so the injection
 *   only changes the returned value.
 * - Focus: Minecraft.isWindowActive()Z reads the windowActive field, which Window.onFocus sets through
 *   WindowEventHandler#setWindowActive(Z). The injection reads it directly, so no extra state is needed.
 * - Vanilla has no unfocused throttle. Its only limits are ICONIFIED 10 (minimized), AFK 30 or 10, and menu 60.
 *   Cap = min(vanilla, unfocusedCap), so minimized, AFK and menu limits are never raised and stay as they were.
 */
@Mixin(FramerateLimitTracker.class)
public class MixinFramerateLimit {
	@Inject(method = "getFramerateLimit", at = @At("RETURN"), cancellable = true)
	private void glacier$unfocusedFpsCap(CallbackInfoReturnable<Integer> cir) {
		int menu = net.glacier.perf.MenuFps.apply(cir.getReturnValue());
		if (menu != cir.getReturnValue()) {
			cir.setReturnValue(menu);
		}
		int cap = FpsLimiter.unfocusedCap();
		if (cap <= 0) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.isWindowActive()) {
			return;
		}
		int vanilla = cir.getReturnValue();
		if (cap < vanilla) {
			cir.setReturnValue(cap);
		}
	}
}
