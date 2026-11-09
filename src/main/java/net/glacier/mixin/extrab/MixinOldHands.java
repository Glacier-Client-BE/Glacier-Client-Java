package net.glacier.mixin.extrab;

import net.glacier.module.impl.OldAnimations;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Old item switch (26.3): swapping to another stack of the same item never replays the equip animation.
 * Verified with javap: FirstPersonHandsAndItems#shouldInstantlyReplaceVisibleItem(ItemStack, ItemStack, LocalPlayer)
 * is called from tick for both hands.
 */
@Mixin(FirstPersonHandsAndItems.class)
public abstract class MixinOldHands {
	@Inject(method = "shouldInstantlyReplaceVisibleItem", require = 0, at = @At("RETURN"), cancellable = true)
	private void glacier$oldItemSwitch(ItemStack from, ItemStack to, LocalPlayer player, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && OldAnimations.itemSwitch() && ItemStack.isSameItem(from, to)) cir.setReturnValue(true);
	}
}
