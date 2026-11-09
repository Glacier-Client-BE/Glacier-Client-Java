package net.glacier.mixin.extrab;

import com.mojang.blaze3d.vertex.PoseStack;
import net.glacier.module.impl.OldAnimations;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Old Animations in the first-person renderer. Verified with javap on ItemInHandRenderer: renderArmWithItem applies
 * the use transform (eat, block, bow...) without swingArm while an item is in use, and calls renderItem last;
 * shouldInstantlyReplaceVisibleItem decides whether a changed stack re-plays the equip animation.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class MixinOldItemInHand {
	@Shadow
	protected abstract void swingArm(float swingProgress, PoseStack pose, int sign, HumanoidArm arm);

	/** Adds the swing on top of the use pose (block hitting for BLOCK items, item usage swinging for the rest). */
	@Inject(method = "renderArmWithItem", require = 0, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
	private void glacier$swingWhileUsing(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand,
										 float swingProgress, ItemStack stack, float equip, PoseStack pose,
										 SubmitNodeCollector collector, int light, CallbackInfo ci) {
		if (swingProgress <= 0f || stack.is(Items.CROSSBOW)) return;
		if (!player.isUsingItem() || player.getUseItemRemainingTicks() <= 0 || player.getUsedItemHand() != hand) return;
		boolean block = stack.getUseAnimation() == ItemUseAnimation.BLOCK;
		if (block ? !OldAnimations.blockHitting() : !OldAnimations.useSwinging()) return;
		HumanoidArm main = player.getMainArm();
		boolean right = (hand == InteractionHand.MAIN_HAND) == (main == HumanoidArm.RIGHT);
		swingArm(swingProgress, pose, right ? 1 : -1, right ? HumanoidArm.RIGHT : HumanoidArm.LEFT);
	}

	/** Old item switch: swapping to another stack of the same item never replays the equip animation. */
	@Inject(method = "shouldInstantlyReplaceVisibleItem", require = 0, at = @At("RETURN"), cancellable = true)
	private void glacier$oldItemSwitch(ItemStack from, ItemStack to, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && OldAnimations.itemSwitch() && ItemStack.isSameItem(from, to)) cir.setReturnValue(true);
	}
}
