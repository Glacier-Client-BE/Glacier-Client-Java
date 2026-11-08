package net.glacier.hud.impl;

import net.glacier.hud.HudModule;
import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;

/** Horizontal compass strip: ticks every 15 degrees, letters every 45, heading above a fixed marker. */
public class DirectionHud extends HudModule {
	private static final int WIDTH = 150;
	/** Degrees visible across the strip. */
	private static final float SPAN = 90f;
	private static final int HEADING_Y = 0;
	private static final int MARK_Y = 10;
	private static final int TICK_Y = 15;
	private static final int TICK_H = 3;
	private static final int LABEL_Y = 19;
	private static final int HEIGHT = 27;
	/** Yaw convention, as in the Lunar reference: 0 south, 90 west, 135 north-west, 180 north. */
	private static final String[] NAMES = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
	/** Ticks are fully opaque: the Theme tick colour with its alpha forced to 0xFF. */
	private static final int TICK_COLOR = 0xFF000000 | (Theme.HUD_TICK & 0x00FFFFFF);
	/** Centre marker: 2 px wide, bright white. */
	private static final int MARKER_COLOR = 0xFFFFFFFF;

	/** Shows the heading in degrees above the strip. */
	private final ToggleSetting showRotation = add(new ToggleSetting("Show Rotation", false));

	public DirectionHud() {
		super("Direction HUD", "Shows a compass strip with your current heading.");
		setPos(0.5f, 0.02f);
	}

	@Override
	public int contentWidth() {
		return WIDTH;
	}

	@Override
	public int contentHeight() {
		return HEIGHT;
	}

	/** Compass heading in degrees from the player's yaw: 0 south, 90 west, 180 north, 270 east. */
	private float heading() {
		if (mc.player == null) return 0f;
		float h = mc.player.getYRot() % 360f;
		return h < 0f ? h + 360f : h;
	}

	@Override
	protected void drawContent(GuiGraphics g, int x, int y) {
		float h = heading();
		int cx = x + WIDTH / 2;
		float ppd = WIDTH / SPAN;

		if (showRotation.on()) {
			String num = String.valueOf(Math.round(h) % 360);
			drawText(g, num, cx - textWidth(num) / 2, y + HEADING_Y, TEXT);
		}

		g.enableScissor(x, y, x + WIDTH, y + HEIGHT);
		int base = (int) Math.floor(h / 15f) * 15;
		for (int i = -4; i <= 4; i++) {
			int a = base + i * 15;
			float d = a - h;
			if (Math.abs(d) > SPAN / 2f + 2f) continue;
			int ix = Math.round(cx + d * ppd);
			g.fill(ix, y + TICK_Y, ix + 1, y + TICK_Y + TICK_H, TICK_COLOR);
			if (a % 45 == 0) {
				int idx = Math.floorMod(a / 45, 8);
				String name = NAMES[idx];
				int col = idx % 2 == 0 ? TEXT : Theme.HUD_MUTED;
				drawText(g, name, ix - textWidth(name) / 2, y + LABEL_Y, col);
			} else {
				String deg = String.valueOf(Math.floorMod(a, 360));
				drawText(g, deg, ix - textWidth(deg) / 2, y + LABEL_Y, Theme.HUD_MUTED);
			}
		}
		g.disableScissor();

		g.fill(cx - 1, y + MARK_Y, cx + 1, y + MARK_Y + 4, MARKER_COLOR);
	}
}
