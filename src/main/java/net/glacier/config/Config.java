package net.glacier.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.glacier.GlacierClient;
import net.glacier.module.Module;
import net.glacier.module.ModuleManager;
import net.glacier.setting.Setting;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Persists every module's enabled state and settings to config/glacier/config.json. */
public final class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private Config() {
	}

	/** Bumped when HUD defaults change; configs older than this get their panel look reset once. */
	private static final int STYLE_VERSION = 2;

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("glacier").resolve("config.json");
	}

	public static synchronized void save() {
		JsonObject modules = new JsonObject();
		for (Module m : ModuleManager.all()) {
			JsonObject mo = new JsonObject();
			mo.addProperty("enabled", m.isEnabled());
			mo.addProperty("key", m.keyCode());
			JsonObject settings = new JsonObject();
			for (Setting<?> s : m.settings()) {
				settings.add(s.getKey(), s.save());
			}
			mo.add("settings", settings);
			modules.add(m.id(), mo);
		}
		JsonObject root = new JsonObject();
		root.add("modules", modules);
		root.addProperty("styleVersion", STYLE_VERSION);

		try {
			Path f = file();
			Files.createDirectories(f.getParent());
			// Write a temp file first, then swap it in, so a crash mid-write never leaves a truncated config.
			Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
			try (Writer w = Files.newBufferedWriter(tmp)) {
				GSON.toJson(root, w);
			}
			try {
				Files.move(tmp, f, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception e) {
			GlacierClient.LOGGER.error("Failed to save config", e);
		}
	}

	/** Keeps an unreadable config as config.json.corrupt so the next save cannot overwrite the user's data. */
	private static void backupCorrupt(Path f) {
		try {
			Files.copy(f, f.resolveSibling(f.getFileName() + ".corrupt"), StandardCopyOption.REPLACE_EXISTING);
			GlacierClient.LOGGER.warn("Saved unreadable config as {}.corrupt", f);
		} catch (Exception e) {
			GlacierClient.LOGGER.error("Could not back up unreadable config", e);
		}
	}

	public static synchronized void load() {
		Path f = file();
		if (!Files.exists(f)) {
			save();
			return;
		}

		JsonObject root;
		try (Reader r = Files.newBufferedReader(f)) {
			root = GSON.fromJson(r, JsonObject.class);
		} catch (Exception e) {
			GlacierClient.LOGGER.error("Failed to read config", e);
			backupCorrupt(f);
			return;
		}
		if (root == null) {
			GlacierClient.LOGGER.error("Config file is empty");
			backupCorrupt(f);
			return;
		}
		if (!root.has("modules") || !root.get("modules").isJsonObject()) return;
		JsonObject modules = root.getAsJsonObject("modules");

		for (Module m : ModuleManager.all()) {
			try {
				JsonElement entry = modules.get(m.id());
				if (entry != null && entry.isJsonObject()) {
					JsonObject mo = entry.getAsJsonObject();
					JsonElement st = mo.get("settings");
					if (st != null && st.isJsonObject()) {
						JsonObject settings = st.getAsJsonObject();
						for (Setting<?> s : m.settings()) {
							JsonElement v = settings.get(s.getKey());
							if (v != null) s.load(v);
						}
					}
					JsonElement key = mo.get("key");
					if (key != null && key.isJsonPrimitive() && key.getAsJsonPrimitive().isNumber()) {
						m.setKeyCode(key.getAsInt());
					}
					JsonElement en = mo.get("enabled");
					if (en != null && en.isJsonPrimitive() && en.getAsJsonPrimitive().isBoolean()) {
						m.setEnabled(en.getAsBoolean());
					}
				}
			} catch (RuntimeException e) {
				GlacierClient.LOGGER.error("Failed to load config for module {}", m.name(), e);
			}
			try {
				m.afterLoad();
			} catch (RuntimeException e) {
				GlacierClient.LOGGER.error("afterLoad failed for module {}", m.name(), e);
			}
		}

		int old = 0;
		try {
			JsonElement sv = root.get("styleVersion");
			if (sv != null && sv.isJsonPrimitive()) old = sv.getAsInt();
		} catch (RuntimeException e) {
			GlacierClient.LOGGER.warn("Ignoring invalid styleVersion in config", e);
		}
		if (old < STYLE_VERSION) {
			for (Module m : ModuleManager.all()) {
				if (m instanceof net.glacier.hud.HudModule h) h.resetLook();
			}
			net.glacier.module.impl.ElementsModule em = ModuleManager.get(net.glacier.module.impl.ElementsModule.class);
			if (em != null) {
				em.glacierDesign.resetToDefault();
				em.useFrostedGlass.resetToDefault();
				em.useBaseBg.resetToDefault();
			}
			save();
		}
	}
}
