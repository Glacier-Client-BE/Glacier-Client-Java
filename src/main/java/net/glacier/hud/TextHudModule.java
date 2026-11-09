package net.glacier.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/** Convenience base for HUD elements that are just a list of text lines. */
public abstract class TextHudModule extends HudModule {
	private static final int LINE_GAP = 1;

	/** The lines the measurements below were taken from. Compared by identity, so a new list is measured again. */
	private List<String> measured;
	private Font measuredFont;
	private boolean measuredGlacier;
	private int measuredW;
	private int measuredH;

	protected TextHudModule(String name, String desc) {
		super(name, desc);
	}

	/** The lines to draw, top to bottom. Recomputed each frame. */
	protected abstract List<String> lines();

	/** Measures the lines only when the list, the font or the Glacier design changed since the last measure. */
	private void measure(List<String> lines) {
		Font font = mc.font;
		boolean gl = glacier();
		if (lines == measured && font == measuredFont && gl == measuredGlacier) return;
		measured = lines;
		measuredFont = font;
		measuredGlacier = gl;
		int max = 8;
		for (String l : lines) max = Math.max(max, textWidth(l));
		measuredW = max;
		int n = lines.size();
		measuredH = n == 0 ? lineHeight() : n * lineHeight() + (n - 1) * LINE_GAP;
	}

	@Override
	public int contentWidth() {
		measure(lines());
		return measuredW;
	}

	@Override
	public int contentHeight() {
		measure(lines());
		return measuredH;
	}

	@Override
	protected void drawContent(GuiGraphics g, int x, int y) {
		int cy = y;
		for (String l : lines()) {
			drawText(g, l, x, cy);
			cy += lineHeight() + LINE_GAP;
		}
	}
}
