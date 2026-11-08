package net.glacier.module.impl;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

/**
 * Rejoins the last multiplayer server after a disconnect. Pure Fabric screen events: when a
 * DisconnectedScreen opens, a countdown button is added to it; clicking that button cancels. The
 * retry counter resets as soon as you are connected again. No mixin.
 */
public class AutoReconnect extends Module {
	private final SliderSetting delay = add(new SliderSetting("Delay (seconds)", 5, 1, 30, 1));
	private final SliderSetting maxAttempts = add(new SliderSetting("Max Attempts", 5, 1, 20, 1));

	private ServerData lastServer;
	private int attempts;
	/** Countdown state of the screen currently showing; ticks left, or -1 when cancelled/idle. */
	private int ticksLeft = -1;
	private Button button;

	public AutoReconnect() {
		super("Auto Reconnect", "Automatically rejoins the server after being disconnected.", Category.MISC);
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (screen instanceof DisconnectedScreen) onDisconnected(client, screen, w, h);
		});
	}

	@Override
	public void onTick() {
		if (mc.level != null && mc.player != null) {
			ServerData cur = mc.getCurrentServer();
			if (cur != null && !mc.isLocalServer()) {
				lastServer = cur;
				attempts = 0;
			}
		}
	}

	@Override
	protected void onDisable() {
		ticksLeft = -1;
		attempts = 0;
		lastServer = null;
		button = null;
	}

	private void onDisconnected(Minecraft client, Screen screen, int w, int h) {
		if (!isEnabled() || lastServer == null || attempts >= maxAttempts.i()) return;
		ticksLeft = delay.i() * 20;
		button = Button.builder(label(), b -> {
			ticksLeft = -1;
			b.setMessage(Component.literal("Auto Reconnect cancelled"));
			b.active = false;
		}).bounds(w / 2 - 100, h - 28, 200, 20).build();
		Screens.getButtons(screen).add(button);
		final Button mine = button;
		ScreenEvents.afterTick(screen).register(s -> {
			if (ticksLeft < 0 || button != mine || !isEnabled()) return;
			if (--ticksLeft <= 0) {
				ticksLeft = -1;
				reconnect(client);
			} else if (ticksLeft % 20 == 0) {
				mine.setMessage(label());
			}
		});
	}

	private Component label() {
		return Component.literal("Reconnecting in " + Math.max(1, (ticksLeft + 19) / 20) + "s (click to cancel)");
	}

	private void reconnect(Minecraft client) {
		ServerData server = lastServer;
		if (server == null) return;
		attempts++;
		ServerAddress address = ServerAddress.parseString(server.ip);
		ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), client, address, server, false, null);
	}
}
