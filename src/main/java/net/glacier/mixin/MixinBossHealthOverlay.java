package net.glacier.mixin;

import net.glacier.module.ModuleManager;
import net.glacier.module.impl.ElementsModule;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.glacier.hud.BetterBossBar;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;
import java.util.UUID;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BossHealthOverlay.class)
public class MixinBossHealthOverlay {
	@Shadow
	@Final
	Map<UUID, LerpingBossEvent> events;

	/** Draws the Glacier boss bars instead of vanilla's; Hide Boss Bar just cancels. */
	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void glacier$better(GuiGraphicsExtractor g, CallbackInfo ci) {
		ElementsModule e = ModuleManager.get(ElementsModule.class);
		if (e == null) return;
		if (e.hideBossBar.on()) {
			ci.cancel();
		} else if (e.betterBossBar.on()) {
			BetterBossBar.render(g, events, e);
			ci.cancel();
		}
	}

	/** Skips the boss name line in BossHealthOverlay.render; the bar itself is still drawn. */
	@Redirect(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
	private void glacier$hideBossName(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int color) {
		ElementsModule e = ModuleManager.get(ElementsModule.class);
		if (e != null && e.hideBossName.on()) return;
		g.text(font, text, x, y, color);
	}

	/** Appends the health percentage to the name; the same name is used for width and drawing. */
	@Redirect(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/LerpingBossEvent;getName()Lnet/minecraft/network/chat/Component;"))
	private Component glacier$bossName(LerpingBossEvent event) {
		Component name = event.getName();
		ElementsModule e = ModuleManager.get(ElementsModule.class);
		if (e == null || !e.showBossPercent.on()) return name;
		int percent = Math.round(event.getProgress() * 100f);
		return name.copy().append(Component.literal(" " + percent + "%"));
	}

	/**
	 * The vertical step between stacked bars is 10 (bar) + 9 (name line) in vanilla, the second 9 being the
	 * step constant (ordinal 1; ordinal 0 is the name's y offset). Vanilla spacing is padding 4, i.e. 5 + 4 = 9.
	 */
	@ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 9, ordinal = 1))
	private int glacier$bossStep(int original) {
		ElementsModule e = ModuleManager.get(ElementsModule.class);
		if (e == null) return original;
		return 5 + e.bossPadding.i();
	}
}
