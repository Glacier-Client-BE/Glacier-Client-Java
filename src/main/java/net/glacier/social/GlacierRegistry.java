/*
 * Glacier registry client.
 *
 * What is sent, and only while the "Glacier Players" module is enabled (opt-in, off by default):
 *   - Announce: the account's own UUID and username, and a one-time server id from the registry. The Mojang session
 *     join for that server id is made directly with Mojang using the account's access token. The access token is
 *     never sent to the registry and is never logged.
 *   - Query: the UUIDs of players currently in your tab list (at most 100 per cycle, in requests of 50).
 *
 * Nothing else is sent. The registry base URL defaults to https://api.glacierclient.xyz and can be overridden in
 * <config>/glacier/registry.json with {"url":"https://..."}; only https (or http for localhost/127.0.0.1) is accepted.
 * All network work runs on one daemon thread. tick() only reads game state and schedules work.
 */
package net.glacier.social;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.exceptions.AuthenticationException;
import net.fabricmc.loader.api.FabricLoader;
import net.glacier.GlacierClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;

import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class GlacierRegistry {
	private static final String DEFAULT_URL = "https://api.glacierclient.xyz";
	private static final long ANNOUNCE_INTERVAL_MS = 4L * 60_000L;
	private static final long QUERY_INTERVAL_MS = 20_000L;
	private static final long ENTRY_TTL_MS = 90_000L;
	private static final long ERROR_LOG_INTERVAL_MS = 60_000L;
	private static final int MAX_UUIDS = 100;
	private static final int CHUNK = 50;
	private static final Duration TIMEOUT = Duration.ofSeconds(5);

	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
	private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "glacier-registry");
		t.setDaemon(true);
		return t;
	});

	/** Guards publishing results and resets so a stale request cannot repopulate a cleared cache. */
	private static final Object LOCK = new Object();
	private static int generation;
	private static volatile Map<UUID, Long> glacierUntil = Map.of();

	private static final AtomicBoolean announcing = new AtomicBoolean();
	private static final AtomicBoolean querying = new AtomicBoolean();
	private static final AtomicLong lastErrorLog = new AtomicLong();

	// Only touched from tick(), which runs on the client thread.
	private static boolean inWorld;
	private static long nextAnnounceAt;
	private static long nextQueryAt;

	private GlacierRegistry() {
	}

	/** True when the id is a known Glacier player, or the local player while the module is on. */
	public static boolean isGlacier(UUID id) {
		if (id == null || !GlacierPlayersModule.on()) return false;
		LocalPlayer self = Minecraft.getInstance().player;
		if (self != null && self.getUUID().equals(id)) return true;
		Long until = glacierUntil.get(id);
		return until != null && until > System.currentTimeMillis();
	}

	/** Called every client tick. Does its own throttling; all HTTP work happens on the registry thread. */
	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		ClientPacketListener conn = mc.getConnection();
		boolean inWorldNow = GlacierPlayersModule.on() && mc.player != null && mc.level != null && conn != null;
		if (!inWorldNow) {
			if (inWorld) reset();
			inWorld = false;
			return;
		}
		if (!inWorld) {
			inWorld = true;
			nextAnnounceAt = 0L;
			nextQueryAt = 0L;
		}

		long now = System.currentTimeMillis();
		if (now >= nextAnnounceAt && !announcing.get()) {
			SessionAuth.Credentials creds = SessionAuth.capture();
			if (creds != null) {
				nextAnnounceAt = now + ANNOUNCE_INTERVAL_MS;
				announcing.set(true);
				int gen = currentGeneration();
				WORKER.execute(() -> runAnnounce(gen, creds));
			} else {
				nextAnnounceAt = now + ANNOUNCE_INTERVAL_MS;
			}
		}

		if (now >= nextQueryAt && !querying.get()) {
			List<UUID> ids = tabListIds(conn);
			nextQueryAt = now + QUERY_INTERVAL_MS;
			if (!ids.isEmpty()) {
				querying.set(true);
				int gen = currentGeneration();
				WORKER.execute(() -> runQuery(gen, ids));
			}
		}
	}

	/** Clears the cache and cancels results still in flight. Called when leaving a world or disabling the module. */
	static void reset() {
		synchronized (LOCK) {
			generation++;
			glacierUntil = Map.of();
		}
		inWorld = false;
	}

	private static int currentGeneration() {
		synchronized (LOCK) {
			return generation;
		}
	}

	private static void publish(int gen, Map<UUID, Long> next) {
		synchronized (LOCK) {
			if (gen == generation) glacierUntil = Map.copyOf(next);
		}
	}

	private static List<UUID> tabListIds(ClientPacketListener conn) {
		Set<UUID> ids = new LinkedHashSet<>();
		for (PlayerInfo info : conn.getOnlinePlayers()) {
			if (ids.size() >= MAX_UUIDS) break;
			UUID id = info.getProfile().id();
			if (id != null) ids.add(id);
		}
		return new ArrayList<>(ids);
	}

	private static void runAnnounce(int gen, SessionAuth.Credentials creds) {
		try {
			if (!GlacierPlayersModule.on()) return;
			String base = baseUrl();
			String serverId = fetchChallenge(base);
			SessionAuth.join(creds, serverId);
			if (!GlacierPlayersModule.on() || gen != currentGeneration()) return;
			JsonObject body = new JsonObject();
			body.addProperty("uuid", creds.profileId().toString());
			body.addProperty("username", creds.name());
			body.addProperty("serverId", serverId);
			post(base + "/v1/announce", body);
		} catch (Exception e) {
			logError("announce", e);
		} finally {
			announcing.set(false);
		}
	}

	private static String fetchChallenge(String base) throws Exception {
		HttpRequest req = HttpRequest.newBuilder(URI.create(base + "/v1/challenge"))
				.timeout(TIMEOUT)
				.header("Accept", "application/json")
				.GET()
				.build();
		HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
		requireOk(res);
		JsonObject o = JsonParser.parseString(res.body()).getAsJsonObject();
		String serverId = o.get("serverId").getAsString();
		if (!serverId.matches("[A-Za-z0-9_-]{1,128}")) throw new IllegalStateException("bad serverId");
		return serverId;
	}

	private static void runQuery(int gen, List<UUID> ids) {
		try {
			if (!GlacierPlayersModule.on()) return;
			String base = baseUrl();
			Set<UUID> requested = new HashSet<>(ids);
			long now = System.currentTimeMillis();
			Map<UUID, Long> next = new HashMap<>();
			for (int i = 0; i < ids.size(); i += CHUNK) {
				List<UUID> chunk = ids.subList(i, Math.min(ids.size(), i + CHUNK));
				JsonArray arr = new JsonArray();
				for (UUID id : chunk) arr.add(id.toString());
				JsonObject body = new JsonObject();
				body.add("uuids", arr);
				String resp = post(base + "/v1/query", body);
				JsonArray glacier = JsonParser.parseString(resp).getAsJsonObject().getAsJsonArray("glacier");
				for (JsonElement e : glacier) {
					try {
						UUID u = UUID.fromString(e.getAsString());
						if (requested.contains(u)) next.put(u, now + ENTRY_TTL_MS);
					} catch (RuntimeException ignored) {
						// Skip malformed entries.
					}
				}
			}
			publish(gen, next);
		} catch (Exception e) {
			logError("query", e);
		} finally {
			querying.set(false);
		}
	}

	private static String post(String url, JsonObject body) throws Exception {
		HttpRequest req = HttpRequest.newBuilder(URI.create(url))
				.timeout(TIMEOUT)
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body.toString()))
				.build();
		HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
		requireOk(res);
		return res.body();
	}

	private static void requireOk(HttpResponse<String> res) throws Exception {
		int code = res.statusCode();
		if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);
	}

	/** Returns the base URL without a trailing slash: registry.json override when valid, otherwise the default. */
	private static String baseUrl() {
		Path f = FabricLoader.getInstance().getConfigDir().resolve("glacier").resolve("registry.json");
		if (Files.isRegularFile(f)) {
			try (Reader r = Files.newBufferedReader(f)) {
				JsonElement root = JsonParser.parseReader(r);
				if (root.isJsonObject() && root.getAsJsonObject().has("url")) {
					String candidate = validBase(root.getAsJsonObject().get("url").getAsString());
					if (candidate != null) return candidate;
					logError("registry.json", new IllegalArgumentException("url must be https (or localhost); using default"));
				}
			} catch (Exception e) {
				logError("registry.json", e);
			}
		}
		return DEFAULT_URL;
	}

	private static String validBase(String raw) {
		try {
			URI u = URI.create(raw.trim());
			String host = u.getHost();
			if (host == null) return null;
			String scheme = u.getScheme() == null ? "" : u.getScheme().toLowerCase();
			boolean local = host.equals("localhost") || host.equals("127.0.0.1");
			boolean ok = scheme.equals("https") || (scheme.equals("http") && local);
			if (!ok) return null;
			String s = raw.trim();
			while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
			return s;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/** At most one line per minute. Session join failures log only the exception type, never the message. */
	private static void logError(String what, Throwable e) {
		long now = System.currentTimeMillis();
		long last = lastErrorLog.get();
		if (now - last < ERROR_LOG_INTERVAL_MS || !lastErrorLog.compareAndSet(last, now)) return;
		boolean noDetail = e instanceof AuthenticationException || "announce".equals(what);
		String detail = noDetail ? "" : ": " + e.getMessage();
		GlacierClient.LOGGER.warn("Glacier registry {} failed ({}){}", what, e.getClass().getSimpleName(), detail);
	}
}
