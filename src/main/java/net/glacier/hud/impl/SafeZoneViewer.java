package net.glacier.hud.impl;

import net.glacier.hud.HudModule;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.SliderSetting;
import net.glacier.ui.Draw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Outlines the screen's safe zone: a frame inset from every screen edge. In the game it only draws the frame, in the
 * HUD editor it appears as a small draggable label. Colour and Opacity follow the pack's Safe Zone Viewer.
 */
public class SafeZoneViewer extends HudModule {
	private static final String[] NAMES = {"White", "Red", "Blue", "Green", "Yellow", "Cyan", "Orange", "Purple", "Chroma"};
	private static final int[] COLORS = {
			0xFFFFFFFF, 0xFFFF5555, 0xFF5555FF, 0xFF55FF55, 0xFFFFFF55, 0xFF55FFFF, 0xFFFFA500, 0xFFAA55FF
	};
	private static final String LABEL = "Safe Zone";
	/** Milliseconds for one full hue cycle of the Chroma colour. */
	private static final float CHROMA_MS = 4000f;

	private final ChoiceSetting color = add(new ChoiceSetting("Color", "White", NAMES));
	private final SliderSetting margin = add(new SliderSetting("Margin %", 5, 1, 20, 1));
	private final SliderSetting thickness = add(new SliderSetting("Thickness", 1, 1, 4, 1));

	public SafeZoneViewer() {
		super("Safe Zone Viewer", "Highlights the safe zone area of your screen.");
		setPos(0.5f, 0.5f);
	}

	@Override
	public int contentWidth() {
		return textWidth(LABEL);
	}

	@Override
	public int contentHeight() {
		return lineHeight();
	}

	@Override
	protected void drawContent(GuiGraphics g, int x, int y) {
		drawText(g, LABEL, x, y);
	}

	private int frameColor() {
		int i = color.index();
		int rgb = i < COLORS.length ? COLORS[i]
				: Mth.hsvToRgb((System.currentTimeMillis() % (long) CHROMA_MS) / CHROMA_MS, 0.7f, 1f);
		return Draw.alpha(0xFF000000 | rgb, opacity.f());
	}

	@Override
	public void render(GuiGraphics g, int sw, int sh) {
		int mx = Math.round(sw * margin.f() / 100f);
		int my = Math.round(sh * margin.f() / 100f);
		int c = frameColor();
		int t = thickness.i();
		int x2 = sw - mx, y2 = sh - my;
		g.fill(mx, my, x2, my + t, c);
		g.fill(mx, y2 - t, x2, y2, c);
		g.fill(mx, my + t, mx + t, y2 - t, c);
		g.fill(x2 - t, my + t, x2, y2 - t, c);
	}
}
