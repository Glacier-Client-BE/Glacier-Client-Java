package net.glacier.mixin.extrac;

import net.glacier.module.impl.ActionBarPlus;
import net.glacier.module.impl.ScoreboardPlus;
import net.glacier.module.impl.TitlesPlus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scoreboard, title and action bar hooks on Gui. Everything was read with javap on the 1.21.11 merged jar:
 * <ul>
 * <li>private displayScoreboardSidebar(GuiGraphics, Objective): two Options.getBackgroundColor(F) calls (rows 0.3f,
 * title 0.4f) and three drawString(Font, Component, IIIZ) calls with ordinals 0 title, 1 entry name, 2 score. Ordinal 2
 * belongs to MixinGuiElements (hide scores), so only 0 and 1 are redirected here.</li>
 * <li>private renderTitle(GuiGraphics, DeltaTracker) draws the title (ordinal 0) and then the subtitle (ordinal 1)
 * with drawStringWithBackdrop(Font, Component, IIII).</li>
 * <li>private renderOverlayMessage(GuiGraphics, DeltaTracker) draws at (guiWidth / 2, guiHeight - 68).</li>
 * <li>public setOverlayMessage(Component, boolean) stores {@code bipush 60} as the display time; public
 * setTimes(int, int, int) takes fade in, stay, fade out.</li>
 * </ul>
 * Transforms are pushed at HEAD and popped at every RETURN of the same method; a flag keeps the pair balanced.
 */
@Mixin(Gui.class)
public class ExtraCGui {
	private static final String SIDEBAR = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V";
	private static final String TITLE = "renderTitle(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V";
	private static final String OVERLAY = "renderOverlayMessage(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V";
	private static final String DRAW_STRING = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V";
	private static final String BACKDROP = "Lnet/minecraft/client/gui/GuiGraphics;drawStringWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V";

	@Unique
	private boolean glacier$sidebarPushed;
	@Unique
	private boolean glacier$titlePushed;
	@Unique
	private boolean glacier$overlayPushed;

	/* ------------------------------------------------------------ scoreboard */

	@Inject(method = SIDEBAR, at = @At("HEAD"), require = 0)
	private void glacier$sidebarPush(GuiGraphics g, Objective objective, CallbackInfo ci) {
		glacier$sidebarPushed = ScoreboardPlus.push(g);
	}

	@Inject(method = SIDEBAR, at = @At("RETURN"), require = 0)
	private void glacier$sidebarPop(GuiGraphics g, Objective objective, CallbackInfo ci) {
		if (glacier$sidebarPushed) {
			g.pose().popMatrix();
			glacier$sidebarPushed = false;
		}
	}

	@Redirect(method = SIDEBAR, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getBackgroundColor(F)I"), require = 0)
	private int glacier$sidebarBackground(Options options, float fallbackAlpha) {
		return ScoreboardPlus.backgroundOr(options.getBackgroundColor(fallbackAlpha));
	}

	@Redirect(method = SIDEBAR, at = @At(value = "INVOKE", target = DRAW_STRING, ordinal = 0), require = 0)
	private void glacier$sidebarTitle(GuiGraphics g, Font font, Component text, int x, int y, int color, boolean shadow) {
		g.drawString(font, text, x, y, color, ScoreboardPlus.shadowOr(shadow));
	}

	@Redirect(method = SIDEBAR, at = @At(value = "INVOKE", target = DRAW_STRING, ordinal = 1), require = 0)
	private void glacier$sidebarName(GuiGraphics g, Font font, Component text, int x, int y, int color, boolean shadow) {
		g.drawString(font, text, x, y, color, ScoreboardPlus.shadowOr(shadow));
	}

	/* ------------------------------------------------------------ titles */

	@Inject(method = TITLE, at = @At("HEAD"), cancellable = true, require = 0)
	private void glacier$titlePush(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
		if (TitlesPlus.hideAll()) {
			ci.cancel();
			return;
		}
		glacier$titlePushed = TitlesPlus.push(g);
	}

	@Inject(method = TITLE, at = @At("RETURN"), require = 0)
	private void glacier$titlePop(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
		if (glacier$titlePushed) {
			g.pose().popMatrix();
			glacier$titlePushed = false;
		}
	}

	@Redirect(method = TITLE, at = @At(value = "INVOKE", target = BACKDROP, ordinal = 0), require = 0)
	private void glacier$title(GuiGraphics g, Font font, Component text, int x, int y, int width, int color) {
		if (!TitlesPlus.hideTitleNow()) g.drawStringWithBackdrop(font, text, x, y, width, color);
	}

	@Redirect(method = TITLE, at = @At(value = "INVOKE", target = BACKDROP, ordinal = 1), require = 0)
	private void glacier$subtitle(GuiGraphics g, Font font, Component text, int x, int y, int width, int color) {
		if (!TitlesPlus.hideSubtitleNow()) g.drawStringWithBackdrop(font, text, x, y, width, color);
	}

	/** setTimes(fadeIn, stay, fadeOut): the second int argument is the stay time. */
	@ModifyVariable(method = "setTimes(III)V", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
	private int glacier$stay(int stay) {
		return TitlesPlus.stayOr(stay);
	}

	@ModifyVariable(method = "setTimes(III)V", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
	private int glacier$fadeIn(int fadeIn) {
		return TitlesPlus.fadeOr(fadeIn);
	}

	@ModifyVariable(method = "setTimes(III)V", at = @At("HEAD"), argsOnly = true, ordinal = 2, require = 0)
	private int glacier$fadeOut(int fadeOut) {
		return TitlesPlus.fadeOr(fadeOut);
	}

	/* ------------------------------------------------------------ action bar */

	@Inject(method = OVERLAY, at = @At("HEAD"), cancellable = true, require = 0)
	private void glacier$overlayPush(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
		if (ActionBarPlus.hidden()) {
			ci.cancel();
			return;
		}
		glacier$overlayPushed = ActionBarPlus.push(g);
	}

	@Inject(method = OVERLAY, at = @At("RETURN"), require = 0)
	private void glacier$overlayPop(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
		if (glacier$overlayPushed) {
			g.pose().popMatrix();
			glacier$overlayPushed = false;
		}
	}

	@ModifyConstant(method = "setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V", constant = @Constant(intValue = 60), require = 0)
	private int glacier$overlayDuration(int vanilla) {
		return ActionBarPlus.durationOr(vanilla);
	}
}
