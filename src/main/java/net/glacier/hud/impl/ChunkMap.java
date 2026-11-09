package net.glacier.hud.impl;

import net.glacier.hud.HudModule;
import net.glacier.setting.ColorSetting;
import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.Level;

import java.util.Random;

/**
 * A top-down map of the chunks around you with chunk borders, a marker for your exact position and slime chunks
 * in green. Slime chunks depend on the world seed, which the client only knows in singleplayer, so on servers
 * and outside the Overworld the map shows just the grid.
 */
public class ChunkMap extends HudModule {
	private static final int CELL = 8;
	private static final int RADIUS = 3;
	private static final int RADIUS_GREATER = 5;
	/** Space reserved for a direction letter on each side of the grid. */
	private static final int LETTER_W = 8;
	private static final int LETTER_H = 9;
	private static final int SLIME_COLOR = 0xAA55FF55;
	private static final int MARKER_SIZE = 3;
	private static final int MAX_CELLS = (RADIUS_GREATER * 2 + 1) * (RADIUS_GREATER * 2 + 1);

	private final ToggleSetting showPosition = add(new ToggleSetting("Show Chunk Position", true));
	private final ToggleSetting hideSlime = add(new ToggleSetting("Hide Slime Chunks", false));
	private final ToggleSetting greaterRange = add(new ToggleSetting("Greater Grid Range", false));
	private final ToggleSetting hideGrid = add(new ToggleSetting("Hide Chunk Grid", false));
	private final ToggleSetting hideMarker = add(new ToggleSetting("Hide Player Marker", false));
	private final ToggleSetting hideDirections = add(new ToggleSetting("Hide Directions", false));
	private final ColorSetting gridColor = add(new ColorSetting("Grid Color", 0x40FFFFFF));
	private final ColorSetting iconColor = add(new ColorSetting("Icon Color", 0xFFFF5555));

	/** Slime flags for the cells around the player, row-major, rebuilt when the chunk, range or setting changes. */
	private final boolean[] slime = new boolean[MAX_CELLS];
	private int chunkX = Integer.MIN_VALUE;
	private int chunkZ;
	private int builtRadius = -1;
	private boolean builtHide;
	private long builtSeed;
	private boolean seedKnown;
	private String posText = "0, 0";
	private final WidthCache posWidth = new WidthCache();

	public ChunkMap() {
		super("Chunk Map", "Locate slime chunks and view grid boundaries.");
		setPos(0.01f, 0.5f);
	}

	private int gridRadius() {
		return greaterRange.on() ? RADIUS_GREATER : RADIUS;
	}

	private int cells() {
		return gridRadius() * 2 + 1;
	}

	private int gridSize() {
		return cells() * CELL;
	}

	/** Java Edition's slime chunk test for a chunk of the given seed. */
	private static boolean isSlime(long seed, int x, int z) {
		Random r = new Random(seed + (long) (x * x * 4987142) + (long) (x * 5947611)
				+ (long) (z * z) * 4392871L + (long) (z * 389711) ^ 987234911L);
		return r.nextInt(10) == 0;
	}

	@Override
	public int contentWidth() {
		return gridSize() + (hideDirections.on() ? 0 : LETTER_W * 2);
	}

	@Override
	public int contentHeight() {
		int h = gridSize() + (hideDirections.on() ? 0 : LETTER_H * 2);
		return showPosition.on() ? h + lineHeight() + 1 : h;
	}

	@Override
	public void onTick() {
		var p = mc.player;
		if (p == null) return;
		int cx = p.getBlockX() >> 4, cz = p.getBlockZ() >> 4;
		boolean known = mc.level != null && mc.level.dimension() == Level.OVERWORLD && mc.getSingleplayerServer() != null;
		long seed = known ? mc.getSingleplayerServer().overworld().getSeed() : 0L;
		boolean hide = hideSlime.on();
		int r = gridRadius();
		if (cx == chunkX && cz == chunkZ && r == builtRadius && hide == builtHide && known == seedKnown && seed == builtSeed) return;
		if (cx != chunkX || cz != chunkZ) posText = cx + ", " + cz;
		chunkX = cx;
		chunkZ = cz;
		builtRadius = r;
		builtHide = hide;
		seedKnown = known;
		builtSeed = seed;
		int n = r * 2 + 1;
		for (int row = 0; row < n; row++) {
			for (int col = 0; col < n; col++) {
				slime[row * n + col] = known && !hide && isSlime(seed, cx + col - r, cz + row - r);
			}
		}
	}

	@Override
	protected void drawContent(GuiGraphics g, int x, int y) {
		boolean letters = !hideDirections.on();
		int size = gridSize();
		int n = cells();
		int gx = x + (letters ? LETTER_W : 0);
		int gy = y + (letters ? LETTER_H : 0);
		for (int row = 0; row < n; row++) {
			for (int col = 0; col < n; col++) {
				if (slime[row * n + col]) g.fill(gx + col * CELL, gy + row * CELL, gx + (col + 1) * CELL, gy + (row + 1) * CELL, SLIME_COLOR);
			}
		}
		if (!hideGrid.on()) {
			int gc = gridColor.argb();
			for (int i = 0; i <= n; i++) {
				g.fill(gx + i * CELL, gy, gx + i * CELL + 1, gy + size + 1, gc);
				g.fill(gx, gy + i * CELL, gx + size + 1, gy + i * CELL + 1, gc);
			}
		}
		if (!hideMarker.on() && mc.player != null && chunkX != Integer.MIN_VALUE) {
			int mid = gridRadius() * CELL;
			int mx = gx + mid + Math.round((float) (mc.player.getX() - (chunkX << 4)) * CELL / 16f);
			int my = gy + mid + Math.round((float) (mc.player.getZ() - (chunkZ << 4)) * CELL / 16f);
			g.fill(mx - 1, my - 1, mx - 1 + MARKER_SIZE, my - 1 + MARKER_SIZE, iconColor.argb());
		}
		if (letters) {
			int cxm = gx + size / 2;
			int cym = gy + size / 2 - lineHeight() / 2;
			drawText(g, "N", cxm - textWidth("N") / 2, y, TEXT);
			drawText(g, "S", cxm - textWidth("S") / 2, gy + size, TEXT);
			drawText(g, "W", x + (LETTER_W - textWidth("W")) / 2, cym, TEXT);
			drawText(g, "E", gx + size + (LETTER_W - textWidth("E")) / 2, cym, TEXT);
		}
		if (showPosition.on()) {
			int ty = gy + size + (letters ? LETTER_H : 0) + 1;
			drawText(g, posText, gx + size / 2 - cachedWidth(posWidth, posText) / 2, ty, Theme.HUD_MUTED);
		}
	}
}
