package net.glacier.hud;

import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/** HUD element made of rows, each row a sequence of coloured parts. */
public abstract class RowsHudModule extends HudModule {
	private static final int LINE_GAP = 1;

	protected RowsHudModule(String name, String desc) {
		super(name, desc);
	}

	/** The rows to draw, top to bottom. Recomputed each frame. */
	protected abstract List<Part[]> rows();

	@Override
	public int contentWidth() {
		int max = 0;
		for (Part[] row : rows()) max = Math.max(max, partsWidth(row));
		return max;
	}

	@Override
	public int contentHeight() {
		int n = rows().size();
		if (n == 0) return lineHeight();
		return n * lineHeight() + (n - 1) * LINE_GAP;
	}

	@Override
	protected void drawContent(GuiGraphics g, int x, int y) {
		int cy = y;
		for (Part[] row : rows()) {
			drawParts(g, x, cy, row);
			cy += lineHeight() + LINE_GAP;
		}
	}
}
