package net.glacier.mixin.extrab;

import com.mojang.blaze3d.vertex.PoseStack;
import net.glacier.module.impl.OldAnimations;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
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

/**
 * Old Animations in the first-person renderer (26.3). Verified with javap on FirstPersonHandsAndItemsRenderer:
 * submitArmWithItem (renamed from renderArmWithItem, now fed by render states instead of the player) applies the use
 * transform (eat, block, bow...) without swingArm while an item is in use, and submits the item model last through
 * ItemStackRenderState#submit. The equip-animation hook (shouldInstantlyReplaceVisibleItem) moved to
 * FirstPersonHandsAndItems, see {@link MixinOldHands}.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class MixinOldItemInHand {
	@Shadow
	protected abstract void swingArm(float swingProgress, PoseStack pose, int sign, HumanoidArm arm);

	/** Adds the swing on top of the use pose (block hitting for BLOCK items, item usage swinging for the rest). */
	@Inject(method = "submitArmWithItem", require = 0, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
	private void glacier$swingWhileUsing(PlayerRenderState player, FirstPersonHandsAndItemsRenderState hands,
										 float partialTick, float pitch, InteractionHand hand, float swingProgress,
										 ItemStack stack, float equip, PoseStack pose, SubmitNodeCollector collector,
										 int light, CallbackInfo ci) {
		if (swingProgress <= 0f || stack.is(Items.CROSSBOW)) return;
		AvatarRenderState avatar = player.avatarRenderState;
		if (!avatar.isUsingItem || hands.useItemRemainingTicks <= 0 || avatar.useItemHand != hand) return;
		boolean block = stack.getUseAnimation() == ItemUseAnimation.BLOCK;
		if (block ? !OldAnimations.blockHitting() : !OldAnimations.useSwinging()) return;
		boolean right = (hand == InteractionHand.MAIN_HAND) == (avatar.mainArm == HumanoidArm.RIGHT);
		swingArm(swingProgress, pose, right ? 1 : -1, right ? HumanoidArm.RIGHT : HumanoidArm.LEFT);
	}
}
