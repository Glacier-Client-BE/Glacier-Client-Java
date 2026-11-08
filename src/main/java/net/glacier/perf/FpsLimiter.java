package net.glacier.perf;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Reads the unfocused-window FPS cap from config/glacier/performance.json (key "unfocusedFpsCap").
 *
 * The value is cached and re-read at most every 5 seconds, so the render thread never touches the disk per frame.
 * 0 disables the cap; the default is 30 and the value is clamped to 0..260. When the file exists but lacks the key,
 * the default is written back while every other key is kept. A malformed file is never overwritten.
 */
public final class FpsLimiter {
    public static final int DEFAULT_UNFOCUSED_CAP = 30;
    static final String KEY = "unfocusedFpsCap";

    private static final Logger LOG = Logger.getLogger("Glacier/FpsLimiter");
    private static final long RELOAD_INTERVAL_NS = 5_000_000_000L;
    private static final int MAX_CAP = 260;

    private static int cap = DEFAULT_UNFOCUSED_CAP;
    private static long lastReadNs;
    private static boolean readOnce;
    private static boolean warnedMalformed;

    private FpsLimiter() {
    }

    /** Returns the unfocused cap in FPS, or 0 when disabled. Cheap enough to call every frame. */
    public static synchronized int unfocusedCap() {
        long now = System.nanoTime();
        if (!readOnce || now - lastReadNs >= RELOAD_INTERVAL_NS) {
            readOnce = true;
            lastReadNs = now;
            cap = readCap();
        }
        return cap;
    }

    private static int readCap() {
        Path file = configFile();
        try {
            JsonObject root = new JsonObject();
            if (Files.exists(file)) {
                JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
                if (!parsed.isJsonObject()) {
                    warnMalformed(file, "root is not a JSON object");
                    return DEFAULT_UNFOCUSED_CAP;
                }
                root = parsed.getAsJsonObject();
            }

            JsonElement value = root.get(KEY);
            if (value != null) {
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                    return clamp(value.getAsInt());
                }
                return DEFAULT_UNFOCUSED_CAP;
            }

            root.addProperty(KEY, DEFAULT_UNFOCUSED_CAP);
            Files.createDirectories(file.getParent());
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(root) + "\n",
                    StandardCharsets.UTF_8);
            return DEFAULT_UNFOCUSED_CAP;
        } catch (IOException | RuntimeException e) {
            warnMalformed(file, e.toString());
            return DEFAULT_UNFOCUSED_CAP;
        }
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(MAX_CAP, value));
    }

    private static void warnMalformed(Path file, String reason) {
        if (!warnedMalformed) {
            warnedMalformed = true;
            LOG.warning("Ignoring " + file + " for " + KEY + ": " + reason + "; using " + DEFAULT_UNFOCUSED_CAP);
        }
    }

    private static Path configFile() {
        return FabricLoader.getInstance().getGameDir().resolve("config").resolve("glacier").resolve("performance.json");
    }
}
