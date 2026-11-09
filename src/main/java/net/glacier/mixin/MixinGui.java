package net.glacier.mixin;

import net.glacier.GlacierClient;
import net.glacier.module.ModuleManager;
import net.glacier.module.impl.EditorsModule;
import net.glacier.module.impl.ElementsModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(Hud.class)
public class MixinGui {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void glacier$renderHud(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		GlacierClient.renderHud(guiGraphics);
	}

	@Inject(method = "extractBossOverlay", at = @At("HEAD"), cancellable = true)
	private void glacier$hideBossBar(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
		if (elements(e -> e.hideBossBar.on())) ci.cancel();
	}

	@Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
	private void glacier$hideScoreboard(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
		if (elements(e -> e.hideScoreboard.on())) ci.cancel();
	}

	@Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
	private void glacier$hideVignette(GuiGraphicsExtractor g, Entity entity, CallbackInfo ci) {
		if (elements(e -> e.hideVignette.on())) ci.cancel();
	}

	@Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
	private void glacier$hideEffects(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
		if (elements(e -> e.hideEffects.on())) ci.cancel();
	}

	@Inject(method = "extractSelectedItemName", at = @At("HEAD"), cancellable = true)
	private void glacier$hideItemName(GuiGraphicsExtractor g, CallbackInfo ci) {
		if (elements(e -> e.hideItemName.on())) ci.cancel();
	}

	/** The animated overlay message is the jukebox now-playing text; a plain overlay message is the action bar. */
	@Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
	private void glacier$hideOverlayMessage(Component message, boolean animate, CallbackInfo ci) {
		Predicate<ElementsModule> hidden = animate ? e -> e.hideJukebox.on() : e -> e.hideActionbar.on();
		if (elements(hidden)) ci.cancel();
	}

	@Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
	private void glacier$customCrosshair(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
		if (!EditorsModule.customCrosshair()) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.options == null || !mc.options.getCameraType().isFirstPerson()) return;
		EditorsModule editors = ModuleManager.get(EditorsModule.class);
		if (editors == null) return;
		ci.cancel();
		editors.drawCrosshair(g, g.guiWidth() / 2, g.guiHeight() / 2, true);
	}

	private static boolean elements(Predicate<ElementsModule> test) {
		ElementsModule e = ModuleManager.get(ElementsModule.class);
		return e != null && test.test(e);
	}
}
