package net.glacier.mixin.extrab;

import net.glacier.module.impl.OldAnimations;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Old sneak animation. Camera.tick eases eyeHeight toward the entity eye height by 0.5 per tick; old versions snapped. */
@Mixin(Camera.class)
public class MixinOldCamera {
	@ModifyConstant(method = "tick", require = 0, constant = @Constant(floatValue = 0.5f))
	private float glacier$sneakSnap(float factor) {
		return OldAnimations.sneak() ? 1.0f : factor;
	}
}
