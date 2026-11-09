package net.glacier.mixin.extrab;

import com.mojang.blaze3d.vertex.PoseStack;
import net.glacier.module.impl.HideArmor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hide Shield. Verified with javap: ItemInHandLayer.submitArmWithItem(S, ItemStackRenderState, ItemStack, HumanoidArm,
 * PoseStack, SubmitNodeCollector, int) draws each hand item and is the super target of PlayerItemInHandLayer.
 */
@Mixin(ItemInHandLayer.class)
public class MixinHideShield {
	@Inject(method = "submitArmWithItem", require = 0, at = @At("HEAD"), cancellable = true)
	private void glacier$hideShield(ArmedEntityRenderState state, ItemStackRenderState itemState, ItemStack stack,
									HumanoidArm arm, PoseStack pose, SubmitNodeCollector collector, int light,
									CallbackInfo ci) {
		if (stack.is(Items.SHIELD) && HideArmor.hidesShield(state)) ci.cancel();
	}
}
