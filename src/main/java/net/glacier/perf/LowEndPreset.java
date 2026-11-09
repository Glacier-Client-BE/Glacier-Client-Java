package net.glacier.perf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.glacier.GlacierClient;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.server.level.ParticleStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

/**
 * The "Low-end laptop preset": one batch of option writes, applied when the toggle goes on and restored when it goes
 * off. Originals are written to {@code config/glacier/perf-restore.json} before anything changes, so a crash with the
 * preset on is undone on the next launch. Never call this from a per-tick path.
 */
public final class LowEndPreset {
	/** One vanilla option: its key in the restore file, how to reach it, and how to store its value as text. */
	private static final class Opt<T> {
		final String key;
		final Function<Options, OptionInstance<T>> option;
		final Function<T, String> write;
		final Function<String, T> read;

		Opt(String key, Function<Options, OptionInstance<T>> option, Function<T, String> write, Function<String, T> read) {
			this.key = key;
			this.option = option;
			this.write = write;
			this.read = read;
		}
	}

	// Every option that GraphicsPreset.apply(FAST) can touch, plus the ones the preset sets directly.
	private static final Opt<GraphicsPreset> GRAPHICS_PRESET = new Opt<>("graphicsPreset", Options::graphicsPreset, GraphicsPreset::name, GraphicsPreset::valueOf);
	private static final Opt<Integer> RENDER_DISTANCE = new Opt<>("renderDistance", Options::renderDistance, String::valueOf, Integer::parseInt);
	private static final Opt<Integer> SIMULATION_DISTANCE = new Opt<>("simulationDistance", Options::simulationDistance, String::valueOf, Integer::parseInt);
	private static final Opt<Double> ENTITY_DISTANCE = new Opt<>("entityDistanceScaling", Options::entityDistanceScaling, String::valueOf, Double::parseDouble);
	private static final Opt<Boolean> ENTITY_SHADOWS = new Opt<>("entityShadows", Options::entityShadows, String::valueOf, Boolean::parseBoolean);
	private static final Opt<ParticleStatus> PARTICLES = new Opt<>("particles", Options::particles, ParticleStatus::name, ParticleStatus::valueOf);
	private static final Opt<CloudStatus> CLOUDS = new Opt<>("cloudStatus", Options::cloudStatus, CloudStatus::name, CloudStatus::valueOf);
	private static final Opt<Integer> CLOUD_RANGE = new Opt<>("cloudRange", Options::cloudRange, String::valueOf, Integer::parseInt);
	private static final Opt<Integer> BIOME_BLEND = new Opt<>("biomeBlendRadius", Options::biomeBlendRadius, String::valueOf, Integer::parseInt);
	private static final Opt<Integer> MIPMAPS = new Opt<>("mipmapLevels", Options::mipmapLevels, String::valueOf, Integer::parseInt);
	private static final Opt<Integer> FRAMERATE = new Opt<>("framerateLimit", Options::framerateLimit, String::valueOf, Integer::parseInt);
	private static final Opt<Boolean> AMBIENT_OCCLUSION = new Opt<>("ambientOcclusion", Options::ambientOcclusion, String::valueOf, Boolean::parseBoolean);
	private static final Opt<Boolean> CUTOUT_LEAVES = new Opt<>("cutoutLeaves", Options::cutoutLeaves, String::valueOf, Boolean::parseBoolean);
	private static final Opt<Boolean> IMPROVED_TRANSPARENCY = new Opt<>("improvedTransparency", Options::improvedTransparency, String::valueOf, Boolean::parseBoolean);
	private static final Opt<PrioritizeChunkUpdates> PRIORITIZE_CHUNKS = new Opt<>("prioritizeChunkUpdates", Options::prioritizeChunkUpdates, PrioritizeChunkUpdates::name, PrioritizeChunkUpdates::valueOf);
	private static final Opt<Integer> MENU_BLUR = new Opt<>("menuBackgroundBlurriness", Options::menuBackgroundBlurriness, String::valueOf, Integer::parseInt);
	private static final Opt<Integer> WEATHER_RADIUS = new Opt<>("weatherRadius", Options::weatherRadius, String::valueOf, Integer::parseInt);
	private static final Opt<Integer> MAX_ANISOTROPY = new Opt<>("maxAnisotropyBit", Options::maxAnisotropyBit, String::valueOf, Integer::parseInt);
	private static final Opt<TextureFilteringMethod> TEXTURE_FILTERING = new Opt<>("textureFiltering", Options::textureFiltering, TextureFilteringMethod::name, TextureFilteringMethod::valueOf);

	private static final List<Opt<?>> ALL = List.of(
		GRAPHICS_PRESET, RENDER_DISTANCE, SIMULATION_DISTANCE, ENTITY_DISTANCE, ENTITY_SHADOWS, PARTICLES, CLOUDS,
		CLOUD_RANGE, BIOME_BLEND, MIPMAPS, FRAMERATE, AMBIENT_OCCLUSION, CUTOUT_LEAVES, IMPROVED_TRANSPARENCY,
		PRIORITIZE_CHUNKS, MENU_BLUR, WEATHER_RADIUS, MAX_ANISOTROPY, TEXTURE_FILTERING);

	private static final int MAX_LOW_END_FPS = 60;
	private static final int MIN_FPS = 10;

	private LowEndPreset() {
	}

	/** config/glacier/perf-restore.json under the game directory. */
	public static Path restoreFile() {
		return FabricLoader.getInstance().getGameDir().resolve("config").resolve("glacier").resolve("perf-restore.json");
	}

	public static boolean hasRestoreFile() {
		return Files.exists(restoreFile());
	}

	/**
	 * Saves the originals, then applies the preset as one batch and saves options.txt. Returns false, with nothing
	 * changed, when the originals cannot be written. If a restore file already exists, its originals are kept.
	 */
	public static boolean apply(Minecraft mc) {
		Options o = mc.options;
		if (o == null) return false;
		Path file = restoreFile();
		if (!Files.exists(file)) {
			JsonObject saved = new JsonObject();
			for (Opt<?> p : ALL) save(p, o, saved);
			if (!write(file, saved)) return false;
		}

		o.applyGraphicsPreset(GraphicsPreset.FAST);
		put(o, RENDER_DISTANCE, 6);
		put(o, SIMULATION_DISTANCE, 5);
		put(o, ENTITY_DISTANCE, 0.75);
		put(o, ENTITY_SHADOWS, false);
		put(o, PARTICLES, ParticleStatus.MINIMAL);
		put(o, CLOUDS, CloudStatus.OFF);
		put(o, BIOME_BLEND, 1);
		put(o, MIPMAPS, 2);
		put(o, FRAMERATE, lowEndFps(mc));
		o.save();
		return true;
	}

	/** Writes the saved originals back, saves options.txt, and deletes the restore file. A no-op without the file. */
	public static void restore(Minecraft mc) {
		Path file = restoreFile();
		if (!Files.exists(file)) return;
		Options o = mc.options;
		if (o == null) return;

		JsonObject saved = read(file);
		if (saved != null) {
			for (Opt<?> p : ALL) {
				JsonElement e = saved.get(p.key);
				if (e == null || !e.isJsonPrimitive()) continue;
				try {
					restoreOne(p, o, e.getAsString());
				} catch (RuntimeException ex) {
					GlacierClient.LOGGER.warn("[perf] cannot restore {}", p.key, ex);
				}
			}
			o.save();
		} else {
			GlacierClient.LOGGER.warn("[perf] restore file unreadable, originals lost: {}", file);
		}
		try {
			Files.deleteIfExists(file);
		} catch (IOException e) {
			GlacierClient.LOGGER.warn("[perf] cannot delete {}", file, e);
		}
	}

	/** 60 FPS, or the monitor's refresh rate when it is lower, never below the option's minimum. */
	private static int lowEndFps(Minecraft mc) {
		int refresh = (int) mc.getWindow().findBestMonitor().currentMode().getRefreshRate();
		int target = refresh > 0 ? Math.min(MAX_LOW_END_FPS, refresh) : MAX_LOW_END_FPS;
		return Math.max(MIN_FPS, target);
	}

	private static <T> void save(Opt<T> p, Options o, JsonObject out) {
		out.addProperty(p.key, p.write.apply(p.option.apply(o).get()));
	}

	private static <T> void restoreOne(Opt<T> p, Options o, String text) {
		p.option.apply(o).set(p.read.apply(text));
	}

	private static <T> void put(Options o, Opt<T> p, T value) {
		p.option.apply(o).set(value);
	}

	private static boolean write(Path file, JsonObject saved) {
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, saved.toString());
			return true;
		} catch (IOException e) {
			GlacierClient.LOGGER.warn("[perf] cannot write {}, preset not applied", file, e);
			return false;
		}
	}

	private static JsonObject read(Path file) {
		try {
			return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
		} catch (IOException | RuntimeException e) {
			return null;
		}
	}
}
