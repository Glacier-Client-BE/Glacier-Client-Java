package net.glacier.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/** Convenience base for HUD elements that are just a list of text lines. */
public abstract class TextHudModule extends HudModule {
	private static final int LINE_GAP = 1;

	protected TextHudModule(String name, String desc) {
		super(name, desc);
	}

	/** The lines to draw, top to bottom. Recomputed each frame. */
	protected abstract List<String> lines();

	@Override
	public int contentWidth() {
		int max = 8;
		for (String l : lines()) max = Math.max(max, textWidth(l));
		return max;
	}

	@Override
	public int contentHeight() {
		int n = lines().size();
		if (n == 0) return lineHeight();
		return n * lineHeight() + (n - 1) * LINE_GAP;
	}

	@Override
	protected void drawContent(GuiGraphicsExtractor g, int x, int y) {
		int cy = y;
		for (String l : lines()) {
			drawText(g, l, x, cy);
			cy += lineHeight() + LINE_GAP;
		}
	}
}
