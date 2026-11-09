package net.glacier.ui.screen.pages;

import net.glacier.music.MusicPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.sounds.SoundSource;

/**
 * Music tab (pages/music.js, .mu-* rules in pages-extra.css): now-playing card, volume card and the track list, backed by
 * {@link MusicPlayer} and the vanilla Records volume. Vanilla sounds cannot pause, so the main button stops and restarts.
 */
public final class MusicPage {
	private static final int[] DISCS = {
			0xFF43B581, 0xFFF04747, 0xFFFAA61A, 0xFF4AA3DF, 0xFF9B59B6,
			0xFFE3B341, 0xFF3FA7A0, 0xFFC1563A, 0xFFF5F5F5, 0xFF6B8FA8};
	private static final int INSET = 0xFF1F2127, EDGE = 0xFF383B40, MUTED = 0xFF99AAB5, ACCENT = 0xFF7289DA;
	private static final int ICON_PREV = 0xF048, ICON_NEXT = 0xF051, ICON_PLAY = 0xF04B, ICON_STOP = 0xF04D, ICON_VOL = 0xF028;

	public float draw(MenuKit k, float x, float y, float w) {
		y = now(k, x, y, w) + 10;
		y = volume(k, x, y, w) + 10;
		return tracks(k, x, y, w);
	}

	// ------------------------------------------------------------ now playing

	private float now(MenuKit k, float x, float y, float w) {
		float h = 96;
		k.box(x, y, w, h, 18, 0xFF2C2F33, 0xFF393C41);
		float ix = x + 15, iw = w - 30, cy = y + h / 2f;
		int cur = MusicPlayer.current();
		boolean playing = cur >= 0;
		int idx = Math.max(0, cur);
		int len = playing ? MusicPlayer.lengthSeconds(idx) : 0;
		float prog = playing ? MusicPlayer.progressSeconds() : 0f;

		// art: 64px + 1px border, a 40px record with a 12px label
		k.box(ix, cy - 33, 66, 66, 12, INSET, EDGE);
		k.round(ix + 13, cy - 20, 40, 40, 20, 0xFF141414);
		k.round(ix + 27, cy - 6, 12, 12, 6, DISCS[idx % DISCS.length]);

		float mx = ix + 66 + 14;
		float ctrlX = ix + iw - 136;
		float metaW = ctrlX - 14 - mx;
		float top = cy - 29.125f;
		k.text("NOW PLAYING", mx, top + 6.25f, 10, 0xFF8EA0E0);
		k.text(MenuKit.fit(playing ? MusicPlayer.title(cur) : "Nothing playing", metaW, 16, true), mx,
				top + 12.5f + 6 + 10, 16, 0xFFFFFFFF, true);
		float py = top + 12.5f + 6 + 20 + 6 + 6.875f;
		k.text(mmss(prog), mx, py, 11, MUTED);
		float bx = mx + 34 + 10, bw = metaW - 88;
		k.round(bx, py - 3, bw, 6, 3, INSET);
		float frac = len > 0 ? Math.max(0f, Math.min(1f, prog / len)) : 0f;
		k.round(bx, py - 3, bw * frac, 6, 3, ACCENT);
		k.textRight(mmss(len), mx + metaW, py, 11, MUTED, false);

		button(k, ctrlX, cy - 20, 40, ICON_PREV, false, MusicPage::prev);
		button(k, ctrlX + 48, cy - 20, 40, playing ? ICON_STOP : ICON_PLAY, true, MusicPage::toggle);
		button(k, ctrlX + 96, cy - 20, 40, ICON_NEXT, false, MusicPage::next);
		return y + h;
	}

	/** .mu-btn: circle with a 1px border. The glyph keeps the .fa accent colour, white on the filled primary. */
	private static void button(MenuKit k, float x, float y, float s, int cp, boolean primary, Runnable act) {
		boolean hov = k.hover(x, y, s, s);
		int fill = primary ? ACCENT : hov ? 0x14FFFFFF : 0x0AFFFFFF;
		k.box(x, y, s, s, s / 2f, fill, primary ? ACCENT : 0x1AFFFFFF);
		k.icon(cp, x + s / 2f, y + s / 2f, 14, primary ? 0xFFFFFFFF : ACCENT);
		k.hit(x, y, s, s, act);
	}

	private static void prev() {
		int n = MusicPlayer.count();
		int cur = MusicPlayer.current();
		if (n > 0) MusicPlayer.play(cur <= 0 ? n - 1 : cur - 1);
	}

	private static void next() {
		int n = MusicPlayer.count();
		int cur = MusicPlayer.current();
		if (n > 0) MusicPlayer.play(cur < 0 ? 0 : (cur + 1) % n);
	}

	private static void toggle() {
		if (MusicPlayer.current() >= 0) MusicPlayer.stop();
		else if (MusicPlayer.count() > 0) MusicPlayer.play(0);
	}

	// ----------------------------------------------------------------- volume

	private float volume(MenuKit k, float x, float y, float w) {
		float h = 43.5f;
		k.box(x, y, w, h, 18, 0xFF2C2F33, 0xFF393C41);
		float ix = x + 15, iw = w - 30, cy = y + h / 2f;
		float iconW = MenuKit.iconWidth(ICON_VOL, 14);
		k.icon(ICON_VOL, ix + iconW / 2f, cy, 14, 0xFF8EA0E0);
		float lx = ix + iconW + 12;
		k.text("Volume", lx, cy, 12, 0xFFFFFFFF);
		float rx = lx + MenuKit.width("Volume", 12, false) + 12;
		float rw = ix + iw - 36 - 12 - rx;
		float vol = recordsVolume();
		k.range(rx, cy - 7, rw, 14, vol, ACCENT);
		k.drag(rx, cy - 11, rw, 22, lx2 -> setVolume(MenuKit.fracAt(lx2, rx, rw)), MusicPage::saveOptions);
		k.textCenter(Math.round(vol * 100) + "%", ix + iw - 18, cy, 12, 0xFF8EA0E0, false);
		return y + h;
	}

	private static float recordsVolume() {
		Options o = Minecraft.getInstance().options;
		return o == null ? 0f : o.getSoundSourceVolume(SoundSource.RECORDS);
	}

	private static void setVolume(double fraction) {
		Options o = Minecraft.getInstance().options;
		if (o == null) return;
		double v = Math.max(0.0, Math.min(1.0, Math.round(fraction * 100) / 100.0));
		o.getSoundSourceOptionInstance(SoundSource.RECORDS).set(v);
	}

	/** Writes options.txt once a volume drag ends, so the Records volume survives a restart. */
	private static void saveOptions() {
		Options o = Minecraft.getInstance().options;
		if (o != null) o.save();
	}

	// ----------------------------------------------------------------- tracks

	private float tracks(MenuKit k, float x, float y, float w) {
		int n = MusicPlayer.count();
		float h = 2 + 24 + 17.5f + 6 + 2 + (n == 0 ? 65 : n * 36 + (n - 1) * 4);
		k.box(x, y, w, h, 18, 0xFF2C2F33, 0xFF393C41);
		float ix = x + 15, iw = w - 30;
		k.text("Tracks", ix, y + 13 + 8.75f, 14, 0xFFFFFFFF, true);
		float ry = y + 38.5f;
		if (n == 0) k.textCenter("No music tracks found.", ix + iw / 2f, ry + 24 + 7.5f, 12, MUTED, false);
		int cur = MusicPlayer.current();
		for (int i = 0; i < n; i++) {
			boolean on = cur == i;
			k.round(ix, ry, iw, 36, 12, on ? 0x1A7289DA : 0x08FFFFFF);
			float cy = ry + 18;
			k.round(ix + 8, cy - 11, 22, 22, 11, 0xFF141414);
			k.round(ix + 8 + 7, cy - 4, 8, 8, 4, DISCS[i % DISCS.length]);
			float bx = ix + iw - 8 - 26;
			float tx = bx - 10 - 34;
			float nx = ix + 8 + 22 + 10;
			k.text(MenuKit.fit(MusicPlayer.title(i), tx - 10 - nx, 12, false), nx, cy, 12, on ? 0xFF8EA0E0 : 0xFFFFFFFF);
			k.text(mmss(MusicPlayer.lengthSeconds(i)), tx, cy, 11, MUTED);
			boolean hov = k.hover(bx, cy - 13, 26, 26);
			k.box(bx, cy - 13, 26, 26, 13, on ? ACCENT : hov ? 0x14FFFFFF : 0x0AFFFFFF, on ? ACCENT : 0x1AFFFFFF);
			k.icon(on ? ICON_STOP : ICON_PLAY, bx + 13, cy, 14, on ? 0xFFFFFFFF : ACCENT);
			final int idx = i;
			k.hit(bx, cy - 13, 26, 26, () -> {
				if (MusicPlayer.current() == idx) MusicPlayer.stop();
				else MusicPlayer.play(idx);
			});
			ry += 40;
		}
		return y + h;
	}

	private static String mmss(float seconds) {
		int s = Math.max(0, (int) seconds);
		return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
	}
}
