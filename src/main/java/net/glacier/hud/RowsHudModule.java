package net.glacier.hud;

import net.glacier.ui.Draw;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/** HUD element made of rows, each row a sequence of coloured parts. */
public abstract class RowsHudModule extends HudModule {
	private static final int LINE_GAP = 1;

	/** The rows list the measurements below were taken from. Compared by identity, so a new list is measured again. */
	private List<Part[]> measured;
	private Font measuredFont;
	private boolean measuredGlacier;
	private int measuredW;
	private int measuredH;
	/** Width of every part of every row, in the same order as {@link #measured}. */
	private int[][] partW = new int[0][];

	protected RowsHudModule(String name, String desc) {
		super(name, desc);
	}

	/** The rows to draw, top to bottom. Recomputed each frame. */
	protected abstract List<Part[]> rows();

	/** Measures the rows only when the list, the font or the Glacier design changed since the last measure. */
	private void measure() {
		List<Part[]> rows = rows();
		Font font = mc.font;
		boolean gl = glacier();
		if (rows == measured && font == measuredFont && gl == measuredGlacier) return;
		measured = rows;
		measuredFont = font;
		measuredGlacier = gl;
		int n = rows.size();
		if (partW.length != n) partW = new int[n][];
		int max = 0;
		for (int r = 0; r < n; r++) {
			Part[] row = rows.get(r);
			int[] w = partW[r];
			if (w == null || w.length != row.length) {
				w = new int[row.length];
				partW[r] = w;
			}
			int sum = 0;
			for (int j = 0; j < row.length; j++) {
				Part p = row[j];
				w[j] = p.cp >= 0 ? Draw.iconWidth(p.cp, 1f) : textWidth(p.text);
				sum += w[j];
			}
			max = Math.max(max, sum);
		}
		measuredW = max;
		measuredH = n == 0 ? lineHeight() : n * lineHeight() + (n - 1) * LINE_GAP;
	}

	@Override
	public int contentWidth() {
		measure();
		return measuredW;
	}

	@Override
	public int contentHeight() {
		measure();
		return measuredH;
	}

	@Override
	protected void drawContent(GuiGraphicsExtractor g, int x, int y) {
		measure();
		List<Part[]> rows = measured;
		int cy = y;
		for (int r = 0; r < rows.size(); r++) {
			Part[] row = rows.get(r);
			int[] w = partW[r];
			int cx = x;
			for (int j = 0; j < row.length; j++) {
				Part p = row[j];
				if (p.cp >= 0) Draw.icon(g, p.cp, cx, cy, 1f, resolve(p.argb));
				else drawText(g, p.text, cx, cy, p.argb);
				cx += w[j];
			}
			cy += lineHeight() + LINE_GAP;
		}
	}
}
