package net.glacier.web;

import com.mojang.realmsclient.RealmsMainScreen;
import net.glacier.GlacierClient;
import net.glacier.ui.screen.WebScreen;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

import java.util.function.Supplier;

/** {@code glacier.start.*}: actions for the start screen. Installed after {@link WebApi}. */
public final class StartApi {
	public static final String DISCORD = "discord.glacierclient.xyz";

	private final Screen screen;

	public StartApi(Screen screen) {
		this.screen = screen;
	}

	public void install(JsBridge bridge) {
		Scriptable scope = bridge.scope();
		Object g = scope.get("glacier", scope);
		Scriptable glacier = g instanceof Scriptable s ? s : bridge.newObject();
		Scriptable o = bridge.newObject();
		Minecraft mc = Minecraft.getInstance();
		String user = mc.getUser().getName();
		String version = SharedConstants.getCurrentVersion().name();
		put(bridge, o, "playerName", () -> user);
		put(bridge, o, "mcVersion", () -> version);
		put(bridge, o, "play", () -> open(new net.glacier.ui.screen.PlayWebScreen(screen)));
		put(bridge, o, "singleplayer", () -> open(new SelectWorldScreen(screen)));
		put(bridge, o, "multiplayer", () -> open(new JoinMultiplayerScreen(screen)));
		put(bridge, o, "realms", () -> open(new RealmsMainScreen(screen)));
		put(bridge, o, "options", () -> open(new OptionsScreen(screen, mc.options)));
		put(bridge, o, "modMenu", () -> open(net.glacier.ui.screen.NativeMenuScreen.open(null)));
		put(bridge, o, "cosmetics", () -> open(net.glacier.ui.screen.NativeMenuScreen.open("cosmetics")));
		put(bridge, o, "quit", () -> {
			mc.stop();
			return Undefined.instance;
		});
		put(bridge, o, "copyDiscord", () -> {
			try {
				mc.keyboardHandler.setClipboard("https://" + DISCORD);
				return true;
			} catch (RuntimeException e) {
				GlacierClient.LOGGER.warn("[ui] clipboard copy failed", e);
				return false;
			}
		});
		glacier.put("start", glacier, o);
		if (!(g instanceof Scriptable)) scope.put("glacier", scope, glacier);
	}

	private static void put(JsBridge bridge, Scriptable o, String name, Supplier<Object> f) {
		o.put(name, o, bridge.fn(name, a -> f.get()));
	}

	private static Object open(Screen s) {
		Minecraft.getInstance().setScreen(s);
		return Undefined.instance;
	}
}
