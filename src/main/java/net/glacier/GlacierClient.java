package net.glacier;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.glacier.config.Config;
import net.glacier.hud.HudExtras;
import net.glacier.hud.HudModule;
import net.glacier.module.ModuleManager;
import net.glacier.social.GlacierRegistry;
import net.glacier.util.PerfStats;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.glacier.compat.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlacierClient implements ClientModInitializer {
	public static final String MOD_ID = "glacier";
	public static final Logger LOGGER = LoggerFactory.getLogger("Glacier");

	@Override
	public void onInitializeClient() {
		LOGGER.info("Glacier Client initializing...");

		ModuleManager.init();
		Config.load();
		net.glacier.cosmetics.AccessoryLayer.register();

		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		KeyMapping openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.glacier.menu",
				InputConstants.Type.KEYBOARD,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				category));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openMenuKey.consumeClick()) {
				if (client.gui.screen() == null) {
					client.gui.setScreen(net.glacier.ui.screen.ScreenFactory.quickBar());
				}
			}
			ModuleManager.tick();
			GlacierRegistry.tick();
			PerfStats.tick();
			net.glacier.music.MusicPlayer.tick();
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Config.save());

		LOGGER.info("Glacier Client ready.");
	}

	/** Called from MixinGui at the end of the in-game HUD render. */
	public static void renderHud(GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.gui.hud.isHidden() || mc.gui.screen() != null) return;

		int w = g.guiWidth();
		int h = g.guiHeight();
		try {
			HudExtras.draw(g);
		} catch (RuntimeException e) {
			LOGGER.error("HUD extras failed", e);
		}
		for (HudModule hud : ModuleManager.hud()) {
			if (!hud.isEnabled()) continue;
			try {
				hud.render(g, w, h);
			} catch (RuntimeException e) {
				LOGGER.error("Error rendering HUD module {}", hud.name(), e);
			}
		}
	}
}
