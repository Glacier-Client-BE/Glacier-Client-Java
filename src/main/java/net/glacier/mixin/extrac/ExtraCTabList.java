package net.glacier.mixin.extrac;

import net.glacier.module.impl.PlayerTabList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Player Tab List hooks, all verified with javap on PlayerTabOverlay (1.21.11):
 * <ul>
 * <li>private getPlayerInfos() returns stream().sorted(PLAYER_COMPARATOR).limit(80L).toList(): the limit is the
 * only long constant 80, and the returned list is reordered at RETURN.</li>
 * <li>render(GuiGraphicsExtractor, int, Scoreboard, Objective) contains one bipush 20 (rows per column), fill(IIIII)
 * calls for the panel, header, footer and cells, and one PlayerFaceExtractor.draw call per player.</li>
 * <li>protected extractPingIcon(GuiGraphicsExtractor, int, int, int, PlayerInfo) draws the ping sprite.</li>
 * </ul>
 * MixinPlayerTabOverlay (getNameForDisplay) and Nr2StreamerTab hook other members of the same class.
 */
@Mixin(PlayerTabOverlay.class)
public class ExtraCTabList {
	@Inject(method = "getPlayerInfos()Ljava/util/List;", at = @At("RETURN"), cancellable = true, require = 0)
	private void glacier$sort(CallbackInfoReturnable<List<PlayerInfo>> cir) {
		List<PlayerInfo> v = cir.getReturnValue();
		if (v == null) return;
		List<PlayerInfo> s = PlayerTabList.sorted(v);
		if (s != v) cir.setReturnValue(s);
	}

	@ModifyConstant(method = "getPlayerInfos()Ljava/util/List;", constant = @Constant(longValue = 80L), require = 0)
	private long glacier$limit(long vanilla) {
		return PlayerTabList.limitOr(vanilla);
	}

	@ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 20), require = 0)
	private int glacier$rows(int vanilla) {
		return PlayerTabList.rowsOr(vanilla);
	}

	@Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"), require = 0)
	private void glacier$fill(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
		PlayerTabList.fill(g, x0, y0, x1, y1, color);
	}

	@Redirect(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/PlayerFaceExtractor;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/Identifier;IIIZZI)V"), require = 0)
	private void glacier$face(GuiGraphicsExtractor g, Identifier skin, int x, int y, int size, boolean hat, boolean flip, int argb) {
		PlayerTabList t = PlayerTabList.active();
		if (t != null && t.hideFaces.on()) return;
		PlayerFaceExtractor.extractRenderState(g, skin, x, y, size, hat, flip, argb);
	}

	@Inject(method = "extractPingIcon(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIILnet/minecraft/client/multiplayer/PlayerInfo;)V",
			at = @At("HEAD"), cancellable = true, require = 0)
	private void glacier$ping(GuiGraphicsExtractor g, int cellWidth, int x, int y, PlayerInfo info, CallbackInfo ci) {
		if (PlayerTabList.drawPing(g, cellWidth, x, y, info)) ci.cancel();
	}
}
