package net.glacier.web;

import net.glacier.GlacierClient;
import net.glacier.ui.screen.StartScreen;
import net.glacier.ui.screen.WebScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.server.IntegratedServer;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code glacier.screen.*}: actions for the vanilla-replacement pages (pause, progress, play). Every action maps to
 * the real vanilla screen or call. {@code self} is the page's own screen, so vanilla sub-screens return to it;
 * {@code close} is what closing the page does (resume the game, return to the parent, or leave a loading screen).
 * Installed after {@link WebApi} so the {@code glacier} global already exists; merges into it.
 */
public final class ScreenApi {
	private final Screen self;
	private final Runnable close;
	/** The saved-server list read by servers(); joinServer() uses it by index. */
	private ServerList serverList;
	/** Set by escape() while a key event is dispatched to JS; read once by takeEscape(). */
	private boolean escapeConsumed;

	public ScreenApi(Screen self, Runnable close) {
		this.self = self;
		this.close = close;
	}

	public void install(JsBridge js) {
		Scriptable scope = js.scope();
		Object g = scope.get("glacier", scope);
		Scriptable glacier = g instanceof Scriptable s ? s : js.newObject();
		Scriptable o = js.newObject();
		Minecraft mc = Minecraft.getInstance();

		put(js, o, "close", a -> run(close));
		put(js, o, "escape", a -> {
			escapeConsumed = true;
			return Undefined.instance;
		});
		put(js, o, "achievements", a -> run(() -> {
			var conn = mc.getConnection();
			ClientAdvancements adv = conn == null ? null : conn.getAdvancements();
			if (adv != null) mc.setScreen(new AdvancementsScreen(adv, self));
		}));
		put(js, o, "options", a -> run(() -> mc.setScreen(net.glacier.ui.screen.ScreenFactory.options(self))));
		put(js, o, "canShareToLan", a -> canShareToLan());
		put(js, o, "shareToLan", a -> run(() -> {
			if (canShareToLan()) mc.setScreen(new ShareToLanScreen(self));
		}));
		put(js, o, "screenshot", a -> run(() -> Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(),
				msg -> mc.execute(() -> mc.gui.getChat().addMessage(msg)))));
		put(js, o, "mods", a -> run(() -> mc.setScreen(net.glacier.ui.screen.ScreenFactory.menu(null))));
		put(js, o, "cosmetics", a -> run(() -> mc.setScreen(net.glacier.ui.screen.ScreenFactory.menu("cosmetics"))));
		put(js, o, "disconnect", a -> run(() -> {
			if (mc.isLocalServer()) mc.disconnectWithSavingScreen();
			else mc.disconnect(new JoinMultiplayerScreen(new StartScreen()), false);
		}));
		put(js, o, "singleplayer", a -> run(() -> mc.setScreen(new SelectWorldScreen(self))));
		put(js, o, "multiplayer", a -> run(() -> mc.setScreen(new JoinMultiplayerScreen(self))));
		put(js, o, "addServer", a -> run(() -> mc.setScreen(new JoinMultiplayerScreen(self))));
		put(js, o, "createWorld", a -> run(() -> CreateWorldScreen.openFresh(mc, () -> mc.setScreen(self))));
		put(js, o, "worlds", a -> worlds(js, mc));
		put(js, o, "playWorld", a -> run(() -> {
			int i = (int) JsBridge.argNumber(a, 0);
			if (worldList != null && i >= 0 && i < worldList.size()) {
				String id = worldList.get(i).getLevelId();
				mc.createWorldOpenFlows().openWorld(id, () -> mc.setScreen(self));
			}
		}));
		put(js, o, "servers", a -> servers(js, mc));
		put(js, o, "joinServer", a -> joinServer((int) JsBridge.argNumber(a, 0), mc));

		glacier.put("screen", glacier, o);
		if (!(g instanceof Scriptable)) scope.put("glacier", scope, glacier);
	}

	/** True when the page consumed Esc (glacier.screen.escape()). Clears the flag. */
	public boolean takeEscape() {
		boolean v = escapeConsumed;
		escapeConsumed = false;
		return v;
	}

	/** Only an integrated server that is not yet opened to LAN can be shared. */
	private static boolean canShareToLan() {
		IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
		return server != null && !server.isPublished();
	}

	/** Loaded world summaries, newest first. Null until the async load finishes. */
	private volatile List<net.minecraft.world.level.storage.LevelSummary> worldList;
	private boolean worldsRequested;

	/**
	 * Returns null while the world summaries are still loading (the page retries), else [{name, id, mode, last, hardcore,
	 * playable}] newest first. The load starts on the first call.
	 */
	private Object worlds(JsBridge js, Minecraft mc) {
		if (!worldsRequested) {
			worldsRequested = true;
			try {
				var source = mc.getLevelSource();
				source.loadLevelSummaries(source.findLevelCandidates()).thenAccept(list -> {
					List<net.minecraft.world.level.storage.LevelSummary> sorted = new ArrayList<>(list);
					java.util.Collections.sort(sorted);
					worldList = sorted;
				}).exceptionally(e -> {
					GlacierClient.LOGGER.warn("[ui] could not load worlds", e);
					worldList = new ArrayList<>();
					return null;
				});
			} catch (Exception e) {
				GlacierClient.LOGGER.warn("[ui] could not list worlds", e);
				worldList = new ArrayList<>();
			}
		}
		List<net.minecraft.world.level.storage.LevelSummary> src = worldList;
		if (src == null) return null;
		List<Object> out = new ArrayList<>();
		for (var s : src) {
			Scriptable item = js.newObject();
			item.put("name", item, s.getLevelName());
			item.put("id", item, s.getLevelId());
			item.put("mode", item, s.getGameMode().getName());
			item.put("last", item, (double) s.getLastPlayed());
			item.put("hardcore", item, s.isHardcore());
			item.put("playable", item, !s.isDisabled() && !s.isLocked() && !s.requiresManualConversion());
			out.add(item);
		}
		return js.newArray(out);
	}

	/** Reads the saved server list and returns [{name, ip}] for the page. */
	private Object servers(JsBridge js, Minecraft mc) {
		List<Object> out = new ArrayList<>();
		try {
			serverList = new ServerList(mc);
			serverList.load();
			for (int i = 0; i < serverList.size(); i++) {
				ServerData d = serverList.get(i);
				Scriptable item = js.newObject();
				item.put("name", item, d.name == null ? "" : d.name);
				item.put("ip", item, d.ip == null ? "" : d.ip);
				out.add(item);
			}
		} catch (RuntimeException e) {
			GlacierClient.LOGGER.warn("[ui] could not read the server list", e);
		}
		return js.newArray(out);
	}

	private Object joinServer(int index, Minecraft mc) {
		if (serverList == null || index < 0 || index >= serverList.size()) return Undefined.instance;
		ServerData data = serverList.get(index);
		ConnectScreen.startConnecting(self, mc, ServerAddress.parseString(data.ip), data, false, null);
		return Undefined.instance;
	}

	/** Runs a page action and returns undefined. Failures are logged; the page keeps running. */
	private static Object run(Runnable r) {
		try {
			r.run();
		} catch (RuntimeException e) {
			GlacierClient.LOGGER.error("[ui] screen action failed", e);
		}
		return Undefined.instance;
	}

	private static void put(JsBridge js, Scriptable o, String name, JsBridge.Fn f) {
		o.put(name, o, js.fn(name, f));
	}
}
