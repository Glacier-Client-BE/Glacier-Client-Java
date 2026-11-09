package net.glacier.mixin.extrab;

import com.mojang.blaze3d.vertex.PoseStack;
import net.glacier.module.impl.HideArmor;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.glacier.module.impl.OldAnimations;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Armor hurt tint and Hide Armor trims. Verified with javap on EquipmentLayerRenderer: the 11-argument renderLayers
 * submits every layer with OverlayTexture.NO_OVERLAY (so armor never turns red when hit) and reads the trim with
 * ItemStack.get(TRIM) once. The render state is argument 4 (S); it is stashed at HEAD for the two hooks below.
 */
@Mixin(net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer.class)
public class MixinOldEquipmentLayer {
	private static final String RENDER_LAYERS = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V";

	@Inject(method = RENDER_LAYERS, require = 0, at = @At("HEAD"))
	private void glacier$stashState(EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> key,
									Model<?> model, Object state, ItemStack stack, PoseStack pose,
									SubmitNodeCollector collector, int light, Identifier texture, int tint, int outline,
									CallbackInfo ci) {
		OldAnimations.currentArmorState = state instanceof LivingEntityRenderState s ? s : null;
	}

	@Redirect(method = RENDER_LAYERS, require = 0, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
	private Object glacier$hideTrim(ItemStack stack, DataComponentType<?> type) {
		if (HideArmor.hidesTrims(OldAnimations.currentArmorState)) return null;
		return stack.get(type);
	}

	@ModifyArg(method = RENDER_LAYERS, require = 0, index = 5, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V"))
	private int glacier$hurtTint(int overlay) {
		LivingEntityRenderState s = OldAnimations.currentArmorState;
		if (s != null && overlay == OverlayTexture.NO_OVERLAY && OldAnimations.armorTint()) {
			return LivingEntityRenderer.getOverlayCoords(s, 0f);
		}
		return overlay;
	}
}
