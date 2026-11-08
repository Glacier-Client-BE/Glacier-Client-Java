package net.glacier.ui.screen.pages;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.module.ModuleManager;
import net.glacier.module.impl.PerformanceMods;
import net.glacier.perf.JvmArgs;
import net.glacier.setting.Setting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Performance tab (pages/performance.js): stat tiles, 12 second frame-rate chart, boosts, bundled mods, JVM args. */
public final class PerformancePage {
	private record Boost(String desc, String impact) {
	}

	private static final int SAMPLE_MS = 250, BAR_COUNT = 48, HISTORY_MAX = 240;
	private static final float CHART_H = 180f, FPS_MAX = 160f;
	private static final String[][] BOOSTS = {
			{"Low-end laptop preset", "Fast graphics, render distance 6, 60 FPS cap. Undone when switched off", "HIGH"},
			{"No Entity Shadows", "Skip the shadow pass under mobs and items", "HIGH"},
			{"Minimal Particles", "Cut particle count by a large share", "HIGH"},
			{"No Clouds", "Don’t build cloud geometry", "MED"},
			{"No View Bobbing", "Keep the camera still while walking", "LOW"}};
	private static final String[][] BUNDLED = {
			{"Sodium", "Modern rendering engine with far higher FPS."},
			{"Lithium", "Game logic and server-side work optimizations."},
			{"FerriteCore", "Lower memory use for block states and models."},
			{"ImmediatelyFast", "Faster immediate-mode drawing for HUD and UI."}};
	private static final int CARD_SOFT = 0xFF2A2B2F, CARD_SOFT_BORDER = 0x0FFFFFFF;
	private static final int MUTED = 0xFF99AAB5;

	private final List<Integer> hist = new ArrayList<>();
	private long nextSample;
	private int ticks;
	private long copiedUntil;

	public void enter() {
		hist.clear();
		ticks = 0;
		nextSample = 0;
	}

	private static int fpsColor(int f) {
		return f >= 60 ? 0xFF43B581 : f >= 30 ? 0xFFFAA61A : 0xFFF04747;
	}

	private void sample() {
		long now = System.currentTimeMillis();
		if (now < nextSample) return;
		nextSample = (nextSample == 0 ? now : nextSample) + SAMPLE_MS;
		if (nextSample < now) nextSample = now + SAMPLE_MS;
		ticks++;
		hist.add(Minecraft.getInstance().getFps());
		if (hist.size() > HISTORY_MAX) hist.remove(0);
	}

	public float draw(MenuKit k, float x, float y, float w) {
		sample();
		int fps = hist.isEmpty() ? 0 : hist.get(hist.size() - 1);
		int[] sorted = hist.stream().mapToInt(Integer::intValue).sorted().toArray();
		int low = sorted.length == 0 ? 0 : sorted[(int) Math.floor(sorted.length * 0.01)];
		int minFps = sorted.length == 0 ? 0 : sorted[0];
		float ft = fps > 0 ? 1000f / fps : 0;
		float worst = minFps > 0 ? 1000f / minFps : 0;
		int modsTotal = 0, modsOn = 0;
		for (Module m : ModuleManager.all()) {
			if (m.category() == Category.SYSTEM) continue;
			modsTotal++;
			if (m.isEnabled()) modsOn++;
		}
		int secs = (int) (ticks * SAMPLE_MS / 1000L);
		String clock = String.format(Locale.ROOT, "%02d:%02d", secs / 60, secs % 60);

		// stat tiles: grid4 gap 10, padding 12 14, column gap 4
		float sw = (w - 30) / 4f;
		String[] lab = {"FRAMES PER SECOND", "FRAME TIME (MS)", "MODULES ON", "SESSION"};
		String[] val = {String.valueOf(fps), String.format(Locale.ROOT, "%.1f", ft), String.valueOf(modsOn), clock};
		String[] sub = {"1% low " + low, String.format(Locale.ROOT, "worst %.1f", worst), "of " + modsTotal, "this menu session"};
		for (int i = 0; i < 4; i++) {
			float sx = x + i * (sw + 10);
			k.box(sx, y, sw, 92.75f, 18, CARD_SOFT, CARD_SOFT_BORDER);
			float ix = sx + 15;
			k.text(lab[i], ix, y + 13 + 6.25f, 10, MUTED);
			k.text(val[i], ix, y + 13 + 12.5f + 4 + 16.25f, 26, i == 0 ? fpsColor(fps) : 0xFFFFFFFF, true);
			k.text(sub[i], ix, y + 13 + 12.5f + 4 + 32.5f + 4 + 6.875f, 11, MUTED);
		}
		y += 92.75f + 10;

		// split: chart card (grows) and boosts card (320)
		Module pm = ModuleManager.get(PerformanceMods.class);
		List<Setting<?>> toggles = new ArrayList<>();
		for (Setting<?> s : ModulesPage.visible(pm)) if (s instanceof ToggleSetting) toggles.add(s);
		float sideH = 2 + 28 + 12.5f + 10 + toggles.size() * 47.75f;
		float leftH = 281.5f;
		float splitH = Math.max(sideH, leftH);
		float leftW = w - 12 - 320;
		chart(k, x, y, leftW, splitH);
		boosts(k, pm, toggles, x + leftW + 12, y, 320, splitH);
		y += splitH + 10;

		// bundled mods
		float iw = (w - 2 - 28 - 30) / 4f;
		int maxLines = 1;
		for (String[] b : BUNDLED) maxLines = Math.max(maxLines, MenuKit.wrap(b[1], iw - 26, 11, false).size());
		float itemH = 2 + 20 + 16.25f + 4 + 13.75f * maxLines;
		float bundledH = 2 + 28 + 12.5f + 10 + itemH;
		k.box(x, y, w, bundledH, 18, CARD_SOFT, CARD_SOFT_BORDER);
		k.text("BUNDLED MODS", x + 15, y + 15 + 6.25f, 10, MUTED);
		for (int i = 0; i < BUNDLED.length; i++) {
			float bx = x + 15 + i * (iw + 10);
			float by = y + 15 + 12.5f + 10;
			k.box(bx, by, iw, itemH, 14, CARD_SOFT, CARD_SOFT_BORDER);
			k.text(MenuKit.fit(BUNDLED[i][0], iw - 26, 13, false), bx + 13, by + 11 + 8.125f, 13, 0xFFFFFFFF);
			List<String> lines = MenuKit.wrap(BUNDLED[i][1], iw - 26, 11, false);
			for (int l = 0; l < lines.size(); l++) {
				k.text(lines.get(l), bx + 13, by + 11 + 16.25f + 4 + l * 13.75f + 6.875f, 11, MUTED);
			}
		}
		y += bundledH + 10;

		// JVM arguments
		String note = "Launcher: Installations, then Edit, then More options, then the JVM Arguments field. "
				+ "Paste this line and pick a Java 21 runtime.";
		List<String> noteLines = MenuKit.wrap(note, w - 30, 11, false);
		float jvmH = 2 + 28 + 28 + 10 + noteLines.size() * 13.75f + 10 + 59.25f;
		k.box(x, y, w, jvmH, 18, CARD_SOFT, CARD_SOFT_BORDER);
		float ix = x + 15, jw = w - 30;
		k.text("JVM ARGUMENTS", ix, y + 15 + 12, 10, MUTED);
		boolean copied = System.currentTimeMillis() < copiedUntil;
		String bl = copied ? "Copied" : "Copy JVM args";
		float bw = MenuKit.width("Copy JVM args", 10, false) + 20;
		float bx = ix + jw - bw;
		boolean hov = k.hover(bx, y + 15, bw, 24);
		k.box(bx, y + 15, bw, 24, 9, hov ? 0x14FFFFFF : 0x0AFFFFFF, 0x1AFFFFFF);
		k.textCenter(bl, bx + bw / 2f, y + 15 + 12, 10, 0xFFFFFFFF, false);
		k.hit(bx, y + 15, bw, 24, () -> {
			try {
				Minecraft.getInstance().keyboardHandler.setClipboard(JvmArgs.TEXT);
				copiedUntil = System.currentTimeMillis() + 1500;
			} catch (RuntimeException ignored) {
			}
		});
		float ny = y + 15 + 28 + 10;
		for (int i = 0; i < noteLines.size(); i++) k.text(noteLines.get(i), ix, ny + i * 13.75f + 6.875f, 11, MUTED);
		float ty = ny + noteLines.size() * 13.75f + 10;
		k.box(ix, ty, jw, 59.25f, 9, 0x40000000, 0x1AFFFFFF);
		List<String> jl = MenuKit.wrap(JvmArgs.TEXT, jw - 18, 11, false);
		for (int i = 0; i < Math.min(3, jl.size()); i++) k.text(jl.get(i), ix + 9, ty + 9 + i * 13.75f + 6.875f, 11, 0xFFFFFFFF);
		return y + jvmH;
	}

	private void chart(MenuKit k, float x, float y, float w, float h) {
		k.box(x, y, w, h, 18, CARD_SOFT, CARD_SOFT_BORDER);
		float ix = x + 15, iw = w - 30;
		k.text("FRAME RATE — LAST 12 SECONDS", ix, y + 15 + 6.875f, 10, MUTED);
		k.textRight("60 fps line", ix + iw, y + 15 + 6.875f, 11, MUTED, false);
		float by = y + 15 + 13.75f + 4 + 10;
		k.round(ix, by, iw, 200, 12, 0xFF1F2227);
		float inner = iw - 20;
		float bw = (inner - 47 * 3) / 48f;
		int pad = BAR_COUNT - Math.min(BAR_COUNT, hist.size());
		int start = Math.max(0, hist.size() - BAR_COUNT);
		float bottom = by + 200 - 10;
		for (int i = 0; i < BAR_COUNT - pad; i++) {
			int f = hist.get(start + i);
			float bh = Math.max(0, Math.min(CHART_H, Math.round(f / FPS_MAX * CHART_H)));
			if (bh <= 0) continue;
			float bx = ix + 10 + (pad + i) * (bw + 3);
			int c = fpsColor(f);
			k.round(bx, bottom - bh, bw, bh, 2, c);
			k.fill(bx, bottom - Math.min(bh, 3), bw, Math.min(bh, 3), c);
		}
		k.fill(ix + 10, by + 200 - Math.round(60 / FPS_MAX * CHART_H + 10) - 1, inner, 1, 0x5999AAB5);
		float ly = by + 200 + 10 + 6.875f;
		float lx = ix;
		String[] names = {"60+ fps", "30–60", "under 30"};
		int[] cols = {0xFF43B581, 0xFFFAA61A, 0xFFF04747};
		for (int i = 0; i < 3; i++) {
			k.round(lx, ly - 4, 8, 8, 2, cols[i]);
			k.text(names[i], lx + 14, ly, 11, MUTED);
			lx += 14 + MenuKit.width(names[i], 11, false) + 14;
		}
	}

	private void boosts(MenuKit k, Module pm, List<Setting<?>> toggles, float x, float y, float w, float h) {
		k.box(x, y, w, h, 18, CARD_SOFT, CARD_SOFT_BORDER);
		float ix = x + 15, iw = w - 30;
		k.text("BOOSTS", ix, y + 15 + 6.25f, 10, MUTED);
		float ry = y + 15 + 12.5f + 10;
		for (Setting<?> s : toggles) {
			ToggleSetting t = (ToggleSetting) s;
			String desc = "", impact = "LOW";
			for (String[] b : BOOSTS) {
				if (b[0].equals(s.getName())) {
					desc = b[1];
					impact = b[2];
				}
			}
			k.fill(ix, ry, iw, 1, 0x0DFFFFFF);
			float cy = ry + 1 + 8 + 15.375f;
			float swx = ix + iw - 38;
			k.toggle(swx, cy - 11, 38, t.on(), 0xFF7289DA);
			k.hit(swx, cy - 11, 38, 22, () -> {
				t.flip();
				ModulesPage.changed(pm, s.getKey());
			});
			float pw = MenuKit.width(impact, 10, false) + 16;
			float px = swx - 12 - pw;
			int fg = impact.equals("HIGH") ? 0xFF43B581 : impact.equals("MED") ? 0xFF8EA0E0 : MUTED;
			int bg = impact.equals("HIGH") ? 0x2443B581 : impact.equals("MED") ? 0x247289DA : 0x0FFFFFFF;
			k.round(px, cy - 9.25f, pw, 18.5f, 9, bg);
			k.textCenter(impact, px + pw / 2f, cy, 10, fg, false);
			float tw = px - 12 - ix;
			k.text(MenuKit.fit(s.getName(), tw, 12, false), ix, ry + 1 + 8 + 7.5f, 12, 0xFFFFFFFF);
			k.text(MenuKit.fit(desc, tw, 11, false), ix, ry + 1 + 8 + 15 + 2 + 6.875f, 11, MUTED);
			ry += 47.75f;
		}
	}
}
