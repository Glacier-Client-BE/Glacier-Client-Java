package net.glacier.module.impl;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.glacier.module.Category;
import net.glacier.module.Module;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Requires a second click on the pause menu's Disconnect / Save and Quit button, so an accidental click
 * cannot throw you out of a game. The first click only arms the button (relabelled, expires after 3 s).
 * Implemented with Fabric screen events; keyboard activation is not intercepted. No mixin.
 */
public class ConfirmDisconnect extends Module {
	private static final long ARM_MS = 3000L;

	public ConfirmDisconnect() {
		super("Confirm Disconnect", "Asks for a second click before leaving the world or server.", Category.MISC);
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (!(screen instanceof PauseScreen)) return;
			AbstractWidget leave = null;
			for (AbstractWidget wd : Screens.getWidgets(screen)) {
				if (wd.getMessage().getContents() instanceof TranslatableContents tc
						&& (tc.getKey().equals("menu.disconnect") || tc.getKey().equals("menu.returnToMenu"))) {
					leave = wd;
					break;
				}
			}
			if (leave == null) return;
			final AbstractWidget target = leave;
			final Component original = target.getMessage();
			final long[] armedAt = {0L};
			ScreenMouseEvents.allowMouseClick(screen).register((s, click) -> {
				if (!isEnabled() || click.button() != 0 || !target.isMouseOver(click.x(), click.y())) return true;
				long now = System.currentTimeMillis();
				if (armedAt[0] != 0L && now - armedAt[0] < ARM_MS) return true;
				armedAt[0] = now;
				target.setMessage(Component.literal("Click again to confirm"));
				return false;
			});
			ScreenEvents.afterTick(screen).register(s -> {
				if (armedAt[0] != 0L && System.currentTimeMillis() - armedAt[0] >= ARM_MS) {
					armedAt[0] = 0L;
					target.setMessage(original);
				}
			});
		});
	}
}
