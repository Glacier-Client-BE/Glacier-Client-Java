package net.glacier.hud.impl;

import net.glacier.hud.HudModule;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.ColorSetting;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.gui.GuiGraphics;

/**
 * An extra HUD-drawn crosshair at the screen centre. It is drawn in addition to the vanilla crosshair, because
 * no verified safe hook exists to hide the vanilla one from the HUD layer. Use a distinct colour and size.
 */
public class CrosshairHud extends HudModule {
	private final ChoiceSetting style = add(new ChoiceSetting("Style", "Cross", "Cross", "Plus", "Dot", "Square", "T"));
	private final ColorSetting color = add(new ColorSetting("Color", 0xFFFFFFFF));
	private final SliderSetting size = add(new SliderSetting("Size", 5, 1, 20, 1));
	private final SliderSetting gap = add(new SliderSetting("Gap", 2, 0, 10, 1));
	private final SliderSetting thickness = add(new SliderSetting("Thickness", 1, 1, 4, 1));
	private final ToggleSetting centerDot = add(new ToggleSetting("Center Dot", false));
	private final ToggleSetting outline = add(new ToggleSetting("Outline", true));
	private final ToggleSetting dynamic = add(new ToggleSetting("Dynamic", false));
	private final ToggleSetting firstPersonOnly = add(new ToggleSetting("First Person Only", true));

	public CrosshairHud() {
		super("Custom Crosshair", "Draws a customizable crosshair (in addition to the vanilla one).");
		background.set(false);
		padding.set(0.0);
	}

	@Override
	public int contentWidth() {
		return (size.i() + gap.i() + 6) * 2 + thickness.i();
	}

	@Override
	public int contentHeight() {
		return contentWidth();
	}

	/** Always drawn at the exact screen centre, ignoring the stored position and scale. */
	@Override
	public void render(GuiGraphics g, int sw, int sh) {
		if (mc.player == null || mc.options.hideGui) return;
		if (firstPersonOnly.on() && !mc.options.getCameraType().isFirstPerson()) return;
		drawContent(g, sw / 2, sh / 2);
	}

	/** Editor preview: centred inside the element box. */
	@Override
	public void renderAt(GuiGraphics g, int px, int py) {
		drawContent(g, px + pixelW() / 2, py + pixelH() / 2);
	}

	/** Fills one bar, with a one pixel dark outline behind it when enabled. */
	private void bar(GuiGraphics g, int x0, int y0, int x1, int y1, int c) {
		if (outline.on()) g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xFF000000);
		g.fill(x0, y0, x1, y1, c);
	}

	/** Draws the crosshair around the centre point (cx, cy). */
	@Override
	protected void drawContent(GuiGraphics g, int cx, int cy) {
		int c = color.argb();
		int t = thickness.i();
		int lo = -(t / 2);
		int hi = lo + t;
		int len = size.i();
		int gp = gap.i();
		if (dynamic.on() && mc.player != null) {
			var dm = mc.player.getDeltaMovement();
			double speed = Math.sqrt(dm.x * dm.x + dm.z * dm.z);
			gp += (int) Math.min(6, speed * 20);
		}
		String s = style.get();
		if (s.equals("Dot")) {
			bar(g, cx + lo, cy + lo, cx + hi, cy + hi, c);
			return;
		}
		if (s.equals("Square")) {
			int r = gp + len;
			bar(g, cx - r, cy - r, cx + r + t, cy - r + t, c);
			bar(g, cx - r, cy + r, cx + r + t, cy + r + t, c);
			bar(g, cx - r, cy - r + t, cx - r + t, cy + r, c);
			bar(g, cx + r, cy - r + t, cx + r + t, cy + r, c);
		} else if (s.equals("Plus")) {
			bar(g, cx - len - gp, cy + lo, cx + len + gp + t, cy + hi, c);
			bar(g, cx + lo, cy - len - gp, cx + hi, cy + len + gp + t, c);
		} else {
			bar(g, cx - gp - len, cy + lo, cx - gp, cy + hi, c);
			bar(g, cx + gp + t, cy + lo, cx + gp + t + len, cy + hi, c);
			if (!s.equals("T")) bar(g, cx + lo, cy - gp - len, cx + hi, cy - gp, c);
			bar(g, cx + lo, cy + gp + t, cx + hi, cy + gp + t + len, c);
		}
		if (centerDot.on()) bar(g, cx + lo, cy + lo, cx + hi, cy + hi, c);
	}
}
