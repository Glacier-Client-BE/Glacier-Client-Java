package net.glacier.mixin;

import net.glacier.GlacierClient;
import net.glacier.module.ModuleManager;
import net.glacier.module.impl.EditorsModule;
import net.glacier.module.impl.ElementsModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(Gui.class)
public class MixinGui {
	@Inject(method = "render", at = @At("TAIL"))
	private void glacier$renderHud(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		GlacierClient.renderHud(guiGraphics);
	}

	@Inject(method = "renderBossOverlay", at = @At("HEAD"), cancellable = true)
	private void glacier$hideBossBar(GuiGraphics g, DeltaTracker dt, CallbackInfo ci) {
		if (elements(e -> e.hideBossBar.on())) ci.cancel();
	}

	@Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
	private void glacier$hideScoreboard(GuiGraphics g, DeltaTracker dt, CallbackInfo ci) {
		if (elements(e -> e.hideScoreboard.on())) ci.cancel();
	}

	@Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
	private void glacier$hideVignette(GuiGraphics g, Entity entity, CallbackInfo ci) {
		if (elements(e -> e.hideVignette.on())) ci.cancel();
	}

	@Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
	private void glacier$hideEffects(GuiGraphics g, DeltaTracker dt, CallbackInfo ci) {
		if (elements(e -> e.hideEffects.on())) ci.cancel();
	}

	@Inject(method = "renderSelectedItemName", at = @At("HEAD"), cancellable = true)
	private void glacier$hideItemName(GuiGraphics g, CallbackInfo ci) {
		if (elements(e -> e.hideItemName.on())) ci.cancel();
	}

	/** The animated overlay message is the jukebox now-playing text; a plain overlay message is the action bar. */
	@Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
	private void glacier$hideOverlayMessage(Component message, boolean animate, CallbackInfo ci) {
		Predicate<ElementsModule> hidden = animate ? e -> e.hideJukebox.on() : e -> e.hideActionbar.on();
		if (elements(hidden)) ci.cancel();
	}

	@Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
	private void glacier$customCrosshair(GuiGraphics g, DeltaTracker dt, CallbackInfo ci) {
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
