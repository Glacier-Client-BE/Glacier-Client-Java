package net.glacier.mixin;

import net.glacier.cosmetics.Capes;
import net.glacier.module.ModuleManager;
import net.glacier.module.impl.CosmeticsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Swaps the cape texture of the local player only. The skin is read by AvatarRenderer when it extracts the render
 * state, and CapeLayer already skips the cape while wings or elytra are equipped, so no extra condition is needed here.
 */
@Mixin(AbstractClientPlayer.class)
public class MixinAbstractClientPlayer {
	/** Last swapped skin for the local player, reused while the source skin and cape are unchanged. */
	private static PlayerSkin glacier$lastSource;
	private static Identifier glacier$lastCape;
	private static PlayerSkin glacier$lastResult;

	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
	private void glacier$localCape(CallbackInfoReturnable<PlayerSkin> cir) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || (Object) this != mc.player) return;

		CosmeticsModule cosmetics = ModuleManager.get(CosmeticsModule.class);
		if (cosmetics == null) return;
		Capes.Cape cape = cosmetics.activeCape();
		PlayerSkin skin = cir.getReturnValue();
		if (cape == null || skin == null || cape.pack()) return;

		Identifier texture = cape.texture();
		if (skin.equals(glacier$lastSource) && texture.equals(glacier$lastCape)) {
			cir.setReturnValue(glacier$lastResult);
			return;
		}
		PlayerSkin result = new PlayerSkin(skin.body(), new ClientAsset.ResourceTexture(texture, texture),
				skin.elytra(), skin.model(), skin.secure());
		glacier$lastSource = skin;
		glacier$lastCape = texture;
		glacier$lastResult = result;
		cir.setReturnValue(result);
	}
}
