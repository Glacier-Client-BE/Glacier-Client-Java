package net.glacier.music;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.function.Supplier;

/**
 * Plays the vanilla music disc sounds through the sound manager. Vanilla sounds cannot be paused,
 * so playback can only be restarted or stopped. Playback ends at the track length or when the
 * client world changes (for example when the player leaves the world).
 */
public final class MusicPlayer {
	private record Track(String title, int seconds, Supplier<SoundEvent> sound) {
	}

	private static final Track[] TRACKS = {
			new Track("C418 - 13", 178, () -> SoundEvents.MUSIC_DISC_13.value()),
			new Track("C418 - Cat", 183, () -> SoundEvents.MUSIC_DISC_CAT.value()),
			new Track("C418 - Blocks", 345, () -> SoundEvents.MUSIC_DISC_BLOCKS.value()),
			new Track("C418 - Chirp", 185, () -> SoundEvents.MUSIC_DISC_CHIRP.value()),
			new Track("C418 - Far", 174, () -> SoundEvents.MUSIC_DISC_FAR.value()),
			new Track("C418 - Mall", 197, () -> SoundEvents.MUSIC_DISC_MALL.value()),
			new Track("C418 - Mellohi", 96, () -> SoundEvents.MUSIC_DISC_MELLOHI.value()),
			new Track("C418 - Stal", 150, () -> SoundEvents.MUSIC_DISC_STAL.value()),
			new Track("C418 - Strad", 188, () -> SoundEvents.MUSIC_DISC_STRAD.value()),
			new Track("C418 - 11", 71, () -> SoundEvents.MUSIC_DISC_11.value()),
			new Track("C418 - Wait", 237, () -> SoundEvents.MUSIC_DISC_WAIT.value()),
			new Track("Lena Raine - Otherside", 195, () -> SoundEvents.MUSIC_DISC_OTHERSIDE.value()),
			new Track("Samuel Åberg - 5", 178, () -> SoundEvents.MUSIC_DISC_5.value()),
			new Track("Lena Raine - Pigstep", 148, () -> SoundEvents.MUSIC_DISC_PIGSTEP.value()),
			new Track("Aaron Cherof - Relic", 219, () -> SoundEvents.MUSIC_DISC_RELIC.value()),
			new Track("Lena Raine - Creator", 176, () -> SoundEvents.MUSIC_DISC_CREATOR.value()),
	};

	private static int current = -1;
	private static SoundInstance active;
	private static ClientLevel playLevel;
	private static long startNanos;

	private MusicPlayer() {
	}

	/** Starts the track at index from the beginning, replacing any track already playing. */
	public static void play(int index) {
		if (index < 0 || index >= TRACKS.length) return;
		Minecraft mc = Minecraft.getInstance();
		SoundManager sounds = mc.getSoundManager();
		if (sounds == null) return;
		stop();
		SoundInstance instance = new SimpleSoundInstance(TRACKS[index].sound().get().location(), SoundSource.RECORDS,
				1f, 1f, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE,
				0.0, 0.0, 0.0, true);
		sounds.play(instance);
		active = instance;
		current = index;
		startNanos = System.nanoTime();
		playLevel = mc.level;
	}

	/** Stops the current track, if any. */
	public static void stop() {
		if (active != null) {
			SoundManager sounds = Minecraft.getInstance().getSoundManager();
			if (sounds != null) sounds.stop(active);
		}
		active = null;
		current = -1;
		playLevel = null;
	}

	/** Index of the playing track, or -1 when nothing is playing. */
	public static int current() {
		return current;
	}

	/** Seconds elapsed in the playing track, capped at its length; 0 when nothing is playing. */
	public static float progressSeconds() {
		if (current < 0) return 0f;
		float s = (System.nanoTime() - startNanos) / 1_000_000_000f;
		return Math.min(s, lengthSeconds(current));
	}

	public static int lengthSeconds(int index) {
		return index >= 0 && index < TRACKS.length ? TRACKS[index].seconds() : 0;
	}

	public static String title(int index) {
		return index >= 0 && index < TRACKS.length ? TRACKS[index].title() : "";
	}

	public static int count() {
		return TRACKS.length;
	}

	/** Called once per client tick. Ends playback at the track length or when the world has changed. */
	public static void tick() {
		if (current < 0) return;
		if (Minecraft.getInstance().level != playLevel || progressSeconds() >= lengthSeconds(current)) {
			stop();
		}
	}
}
