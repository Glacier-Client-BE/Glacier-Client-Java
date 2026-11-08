package net.glacier.hud.impl;

import net.glacier.hud.HudModule;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Draw;
import net.glacier.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Compass strip in the look of the Glacier v7 pack: an accent line across the top, short ticks every 11.25 degrees
 * (taller at 22.5 degrees past each 45), and only the four cardinal letters N, E, S, W. The strip scrolls with the
 * heading; an optional pill beside it shows the exact bearing in degrees.
 */
public class DirectionHud extends HudModule {
	/** Degrees between two ticks (the pack's texture repeats its tick pattern every 45 degrees in 4 steps). */
	private static final float TICK_STEP = 11.25f;
	private static final int LINE_H = 1;
	private static final int TICK_SHORT = 3;
	private static final int TICK_LONG = 4;
	private static final int LETTER_Y = 5;
	private static final float LETTER_SCALE = 0.75f;
	private static final int HEIGHT = 12;
	private static final int PILL_W = 25;
	private static final int PILL_GAP = 4;
	private static final String[] CARDINALS = {"N", "E", "S", "W"};
	private static final int LINE_COLOR = Theme.ACCENT;
	private static final int TICK_COLOR = Draw.alpha(Theme.ACCENT, 0.7f);

	/** Shows the exact bearing in degrees in a pill beside the strip (the pack's Show Rotation). */
	private final ToggleSetting showRotation = add(new ToggleSetting("Show Rotation", false));
	private final SliderSetting stripWidth = add(new SliderSetting("Strip Width", 132, 80, 300, 4));
	/** Degrees visible across the strip; the pack shows about 240. */
	private final SliderSetting span = add(new SliderSetting("Visible Degrees", 240, 60, 360, 15));
	private final ToggleSetting showMarker = add(new ToggleSetting("Show Marker", false));

	private int lastBearing = Integer.MIN_VALUE;
	private String bearingText = "0";

	public DirectionHud() {
		super("Direction HUD", "A compass strip for easy navigation.");
		setPos(0.5f, 0.02f);
	}

	@Override
	public int contentWidth() {
		return stripWidth.i() + (showRotation.on() ? PILL_GAP + PILL_W : 0);
	}

	@Override
	public int contentHeight() {
		return HEIGHT;
	}

	/** Compass bearing in degrees: 0 north, 90 east, 180 south, 270 west (vanilla yaw 0 faces south). */
	private float bearing() {
		if (mc.player == null) return 0f;
		float b = (mc.player.getYRot() + 180f) % 360f;
		return b < 0f ? b + 360f : b;
	}

	@Override
	protected void drawContent(GuiGraphics g, int x, int y) {
		float b = bearing();
		int width = stripWidth.i();
		float spanDeg = span.f();
		float ppd = width / spanDeg;
		float cx = x + width / 2f;

		g.enableScissor(x, y, x + width, y + HEIGHT);
		g.fill(x, y, x + width, y + LINE_H, LINE_COLOR);
		int first = (int) Math.floor((b - spanDeg / 2f) / TICK_STEP) - 1;
		int last = (int) Math.ceil((b + spanDeg / 2f) / TICK_STEP) + 1;
		float letterH = Draw.textHeight(LETTER_SCALE);
		for (int k = first; k <= last; k++) {
			int ix = Math.round(cx + (k * TICK_STEP - b) * ppd);
			int step = Math.floorMod(k, 4);
			if (step == 0) {
				if (Math.floorMod(k, 8) == 0) {
					String l = CARDINALS[Math.floorMod(k / 8, 4)];
					Draw.text(g, l, ix - Draw.textWidth(l, LETTER_SCALE) / 2f, y + LETTER_Y + (HEIGHT - LETTER_Y - letterH) / 2f,
							LETTER_SCALE, LINE_COLOR);
				}
				continue;
			}
			int h = step == 2 ? TICK_LONG : TICK_SHORT;
			g.fill(ix, y + LINE_H, ix + 1, y + LINE_H + h, TICK_COLOR);
		}
		g.disableScissor();

		if (showMarker.on()) g.fill(Math.round(cx) - 1, y + LINE_H, Math.round(cx) + 1, y + LINE_H + 5, 0xFFFFFFFF);

		if (showRotation.on()) {
			int deg = Math.round(b) % 360;
			if (deg != lastBearing) {
				lastBearing = deg;
				bearingText = String.valueOf(deg);
			}
			int px = x + width + PILL_GAP;
			Draw.round(g, px, y - 2, PILL_W, HEIGHT + 4, 4, Draw.alpha(0xFF000000, 0.35f));
			drawText(g, bearingText, px + (PILL_W - textWidth(bearingText)) / 2, y + (HEIGHT - lineHeight()) / 2 + 1, LINE_COLOR);
		}
	}
}
