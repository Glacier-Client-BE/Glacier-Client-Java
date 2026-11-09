package net.glacier.hud.impl;

import net.glacier.hud.HudModule;
import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Depth chart of where each common ore generates (one bar per ore across the world height), with its best mining
 * level marked and your own Y level drawn as a line. The info grid under the chart lists the best level per ore and
 * is dropped by Compact Mode.
 */
public class OreVisualizer extends HudModule {
	private static final int MIN_Y = -64;
	private static final int MAX_Y = 320;
	private static final int WIDTH = 128;
	private static final int HEADER_H = 12;
	private static final int CHART_H = 64;
	private static final int COL_W = 14;
	private static final int COL_GAP = 4;
	private static final int CHART_X = 3;
	private static final int INFO_GAP = 4;
	private static final int INFO_ROW_H = 10;
	private static final int INFO_COLS = 2;
	private static final int GRID_STEP = 64;
	private static final int GRID_COLOR = 0x22FFFFFF;
	private static final int BEST_COLOR = 0xFFFFFFFF;
	private static final int PLAYER_COLOR = 0xFFFF5555;

	/** Ore display data, in the chart's left to right order: name, colour, lowest Y, highest Y, best Y. */
	private static final String[] NAMES = {"Diamond", "Iron", "Gold", "Coal", "Redstone", "Lapis", "Copper"};
	private static final int[] COLORS = {0xFF4AEDD9, 0xFFD8AF93, 0xFFFCEE4B, 0xFF5A5A5A, 0xFFFF3B3B, 0xFF345EC3, 0xFFE77C56};
	private static final int[] LOW = {-64, -64, -64, 0, -64, -64, -16};
	private static final int[] HIGH = {16, 320, 32, 320, 16, 64, 112};
	private static final int[] BEST = {-59, 16, -16, 96, -59, 0, 48};

	/** Pixel offsets from the chart top, per ore, computed once. */
	private static final int[] BAR_TOP = new int[NAMES.length];
	private static final int[] BAR_BOTTOM = new int[NAMES.length];
	private static final int[] BEST_AT = new int[NAMES.length];
	private static final String[] INFO = new String[NAMES.length];

	static {
		for (int i = 0; i < NAMES.length; i++) {
			BAR_TOP[i] = offset(HIGH[i]);
			BAR_BOTTOM[i] = offset(LOW[i]);
			BEST_AT[i] = offset(BEST[i]);
			INFO[i] = NAMES[i] + " " + BEST[i];
		}
	}

	private final ToggleSetting hideTitle = add(new ToggleSetting("Hide Title", false));
	private final ToggleSetting hideY = add(new ToggleSetting("Hide Y Level", false));
	private final ToggleSetting hideGrid = add(new ToggleSetting("Hide Chart Grid", false));
	private final ToggleSetting compact = add(new ToggleSetting("Compact Mode", false));

	private int shownY = Integer.MIN_VALUE;
	private String yText = "";
	private final WidthCache yWidth = new WidthCache();

	public OreVisualizer() {
		super("Ore Visualizer", "Visualizes optimal ore generation heights.");
		setPos(0.01f, 0.35f);
	}

	/** Pixels below the chart top for a world Y level. */
	private static int offset(int y) {
		return Math.round((MAX_Y - Math.max(MIN_Y, Math.min(MAX_Y, y))) * (CHART_H - 1) / (float) (MAX_Y - MIN_Y));
	}

	private int infoRows() {
		return (NAMES.length + INFO_COLS - 1) / INFO_COLS;
	}

	@Override
	public int contentWidth() {
		return WIDTH;
	}

	@Override
	public int contentHeight() {
		int h = HEADER_H + CHART_H;
		return compact.on() ? h : h + INFO_GAP + infoRows() * INFO_ROW_H;
	}

	@Override
	protected void drawContent(GuiGraphics g, int x, int y) {
		if (!hideTitle.on()) drawText(g, "Ore Visualizer", x, y, TEXT);
		if (!hideY.on() && mc.player != null) {
			int py = (int) Math.floor(mc.player.getY());
			if (py != shownY) {
				shownY = py;
				yText = "Y " + py;
			}
			drawText(g, yText, x + WIDTH - cachedWidth(yWidth, yText), y, Theme.HUD_WARN);
		}
		int top = y + HEADER_H;
		if (!hideGrid.on()) {
			for (int gy = MIN_Y; gy <= MAX_Y; gy += GRID_STEP) g.fill(x, top + offset(gy), x + WIDTH, top + offset(gy) + 1, GRID_COLOR);
		}
		for (int i = 0; i < NAMES.length; i++) {
			int cx = x + CHART_X + i * (COL_W + COL_GAP);
			g.fill(cx, top + BAR_TOP[i], cx + COL_W, top + BAR_BOTTOM[i] + 1, COLORS[i]);
			g.fill(cx - 1, top + BEST_AT[i], cx + COL_W + 1, top + BEST_AT[i] + 1, BEST_COLOR);
		}
		if (mc.player != null) {
			int ly = top + offset((int) Math.floor(mc.player.getY()));
			g.fill(x, ly, x + WIDTH, ly + 1, PLAYER_COLOR);
		}
		if (compact.on()) return;
		int infoTop = top + CHART_H + INFO_GAP;
		int colW = WIDTH / INFO_COLS;
		for (int i = 0; i < NAMES.length; i++) {
			drawText(g, INFO[i], x + (i % INFO_COLS) * colW, infoTop + (i / INFO_COLS) * INFO_ROW_H, COLORS[i]);
		}
	}
}
