package net.glacier.mixin.extraa;

import net.glacier.module.impl.ShinyPots;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shiny Potions. Verified with javap: ItemStack#hasFoil()Z (single overload) is consulted by BlockModelWrapper and
 * SpecialModelWrapper when they set the layer foil type; it returns the glint override component or Item#isFoil.
 */
@Mixin(ItemStack.class)
public abstract class MixinItemStack {
	@Inject(method = "hasFoil()Z", at = @At("RETURN"), cancellable = true, require = 0)
	private void glacier$shinyPots(CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue() && ShinyPots.shines((ItemStack) (Object) this)) cir.setReturnValue(true);
	}
}
