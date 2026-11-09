package net.glacier.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.glacier.module.impl.HideArmor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Skips drawing hidden armor slots. Target verified with javap: private renderArmorPiece(PoseStack, SubmitNodeCollector, ItemStack, EquipmentSlot, int, S extends HumanoidRenderState). */
@Mixin(HumanoidArmorLayer.class)
public class MixinHideArmor {
	@Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
	private void glacier$hideArmor(PoseStack pose, SubmitNodeCollector collector, ItemStack stack, EquipmentSlot slot,
								   int light, HumanoidRenderState state, CallbackInfo ci) {
		if (HideArmor.hides(slot, state)) ci.cancel();
	}
}
