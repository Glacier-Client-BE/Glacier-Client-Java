package net.glacier.hud;

import net.glacier.module.impl.ElementsModule;
import net.glacier.ui.Draw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Glacier boss bars: rounded track and fill, name and percent above, optional notches or segments. */
public final class BetterBossBar {
	private static final int ACCENT = 0xFF7289DA;
	private static final int TRACK = 0xB0141A24;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int MARGIN = 8;
	private static final int TOP = 10;
	private static final int NAME_H = 11;

	/** Per-event cached strings and animated progress. */
	private static final class State {
		Component nameSrc;
		String name = "";
		int percent = -1;
		String percentText = "";
		float shown;
		int stamp;
	}

	private static final Map<UUID, State> STATES = new HashMap<>();
	private static int frame;
	private static long lastNanos;

	private BetterBossBar() {
	}

	public static void render(GuiGraphicsExtractor g, Map<UUID, LerpingBossEvent> events, ElementsModule e) {
		if (events.isEmpty()) {
			if (!STATES.isEmpty()) STATES.clear();
			return;
		}
		frame++;
		long now = System.nanoTime();
		float dt = lastNanos == 0 ? 0f : Math.min(0.25f, (now - lastNanos) / 1_000_000_000f);
		lastNanos = now;
		float follow = 1f - (float) Math.exp(-dt * 10f);

		float scale = Math.max(0.5f, Math.min(2f, e.bossBarScale.i() / 100f));
		float sw = g.guiWidth() / scale;
		float limit = g.guiHeight() / 3f / scale;
		int bw = e.bossBarWidth.i();
		String style = e.bossBarStyle.get();
		boolean slim = "Slim".equals(style);
		boolean segmented = "Segmented".equals(style);
		int bh = slim ? 3 : 7;
		boolean showName = !e.hideBossName.on();
		boolean showPct = e.showBossPercent.on();
		boolean text = showName || showPct;
		int pitch = (text ? NAME_H : 0) + bh + 5 + e.bossPadding.i() - 4;
		if (pitch < bh + 1) pitch = bh + 1;
		boolean smooth = e.bossSmoothFill.on();
		boolean byColor = e.bossColorByColor.on();
		boolean notches = e.bossNotches.on();

		int x;
		if (e.bossBarPosition.is("Top Left")) x = MARGIN;
		else if (e.bossBarPosition.is("Top Right")) x = Math.round(sw) - bw - MARGIN;
		else x = Math.round((sw - bw) / 2f);

		var pose = g.pose();
		pose.pushMatrix();
		pose.scale(scale, scale);
		int y = TOP;
		for (LerpingBossEvent ev : events.values()) {
			if (y >= limit) break;
			State st = STATES.get(ev.getId());
			float target = Math.max(0f, Math.min(1f, ev.getProgress()));
			if (st == null) {
				st = new State();
				st.shown = target;
				STATES.put(ev.getId(), st);
			}
			st.stamp = frame;
			st.shown = smooth ? st.shown + (target - st.shown) * follow : target;
			float p = st.shown;

			Component n = ev.getName();
			if (n != st.nameSrc) {
				st.nameSrc = n;
				st.name = n.getString();
			}
			int pct = Math.round(target * 100f);
			if (pct != st.percent) {
				st.percent = pct;
				st.percentText = pct + "%";
			}

			int barY = y + (text ? NAME_H : 0);
			if (text) {
				if (showName) Draw.text(g, st.name, x, y, TEXT);
				if (showPct) Draw.text(g, st.percentText, x + bw - Draw.textWidth(st.percentText, 1f), y, 0xFFB8C2E8);
			}

			int fill = byColor ? colorOf(ev.getColor()) : ACCENT;
			BossEvent.BossBarOverlay ov = ev.getOverlay();
			if (segmented) {
				drawSegments(g, x, barY, bw, bh, p, fill, divisions(ov));
			} else {
				int r = slim ? 1 : 3;
				Draw.round(g, x, barY, bw, bh, r, TRACK);
				int fw = Math.round(bw * p);
				if (fw > 0) {
					Draw.round(g, x, barY, fw, bh, r, fill);
					if (bh >= 6 && fw >= 6) Draw.round(g, x + 1, barY + 1, fw - 2, 2, 1, 0x33FFFFFF);
				}
				int d = divisions(ov);
				if (notches && d > 0) {
					for (int i = 1; i < d; i++) {
						int nx = x + Math.round(bw * i / (float) d);
						g.fill(nx, barY, nx + 1, barY + bh, 0x99000000);
					}
				}
			}
			y += pitch;
		}
		pose.popMatrix();

		for (Iterator<State> it = STATES.values().iterator(); it.hasNext(); ) {
			if (it.next().stamp != frame) it.remove();
		}
	}

	/** Number of notches an overlay implies, 0 for a plain progress bar. */
	private static int divisions(BossEvent.BossBarOverlay ov) {
		switch (ov) {
			case NOTCHED_6:
				return 6;
			case NOTCHED_10:
				return 10;
			case NOTCHED_12:
				return 12;
			case NOTCHED_20:
				return 20;
			default:
				return 0;
		}
	}

	private static void drawSegments(GuiGraphicsExtractor g, int x, int y, int w, int h, float p, int fill, int div) {
		int n = div > 0 ? div : 10;
		int gap = 2;
		float segW = (w - gap * (n - 1)) / (float) n;
		for (int i = 0; i < n; i++) {
			int sx = x + Math.round(i * (segW + gap));
			int ex = x + Math.round(i * (segW + gap) + segW);
			int sw = ex - sx;
			Draw.round(g, sx, y, sw, h, 2, TRACK);
			float f = Math.max(0f, Math.min(1f, p * n - i));
			int fw = Math.round(sw * f);
			if (fw > 0) Draw.round(g, sx, y, fw, h, 2, fill);
		}
	}

	public static int colorOf(BossEvent.BossBarColor c) {
		switch (c) {
			case PINK:
				return 0xFFF472B6;
			case BLUE:
				return 0xFF60A5FA;
			case RED:
				return 0xFFEF4444;
			case GREEN:
				return 0xFF4ADE80;
			case YELLOW:
				return 0xFFFACC15;
			case PURPLE:
				return 0xFFA78BFA;
			case WHITE:
				return 0xFFF1F5F9;
			default:
				return ACCENT;
		}
	}
}
