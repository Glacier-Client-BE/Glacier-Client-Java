package net.glacier.hud.impl;

import net.glacier.hud.SlotHudModule;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Draw;
import net.glacier.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Shows the main inventory grid, the hotbar, or both, with item counts and durability bars. */
public class InventoryHud extends SlotHudModule {
	private static final int GAP = 1;
	private static final int COLS = 9;
	private static final int SELECTED = 0x88FFFFFF;

	private final ChoiceSetting mode = add(new ChoiceSetting("Mode", "Main", "Main", "Hotbar", "Both"));
	private final ToggleSetting counts = add(new ToggleSetting("Show Counts", true));
	private final ToggleSetting highlight = add(new ToggleSetting("Highlight Selected", true));
	/** Horizontal lays the slot rows out as rows; Vertical turns the grid on its side. */
	private final ChoiceSetting orientation = add(new ChoiceSetting("Orientation", "Horizontal", "Horizontal", "Vertical"));
	private final ToggleSetting slotNumbers = add(new ToggleSetting("Show Slot Numbers", false));
	/** Slot numbers 1..36 in inventory order (hotbar 1..9), built once so drawing never allocates. */
	private static final String[] NUMBERS = new String[36];

	static {
		for (int i = 0; i < NUMBERS.length; i++) NUMBERS[i] = String.valueOf(i + 1);
	}

	public InventoryHud() {
		super("Inventory HUD", "Shows your inventory or hotbar on screen.", 20);
		stackTopLeft(40);
	}

	/** Number of slot rows for the current mode. */
	private int rows() {
		return mode.is("Both") ? 4 : mode.is("Hotbar") ? 1 : 3;
	}

	private boolean vertical() {
		return orientation.is("Vertical");
	}

	/** Pixel size of a grid with the given number of slots along one side. */
	private int span(int slots) {
		return slots * (slot() + GAP) - GAP;
	}

	@Override
	public int contentWidth() {
		return span(vertical() ? rows() : COLS);
	}

	@Override
	public int contentHeight() {
		return span(vertical() ? COLS : rows());
	}

	@Override
	protected void drawContent(GuiGraphicsExtractor g, int x, int y) {
		if (mc.player == null) return;
		Inventory inv = mc.player.getInventory();
		int n = rows();
		boolean hotbarOnly = mode.is("Hotbar");
		boolean both = mode.is("Both");
		int sel = inv.getSelectedSlot();
		// Settings are read once per frame here, not once per slot.
		int s = slot();
		int step = s + GAP;
		boolean vert = vertical();
		boolean hl = highlight.on();
		boolean numbers = slotNumbers.on();
		boolean withCounts = counts.on();
		int ox = (s - 16) / 2;
		int oy = (s - 16) / 2 - (showBar.on() ? 1 : 0);
		for (int r = 0; r < n; r++) {
			// Rows 0..2 are inventory slots 9..35; the hotbar row comes last in Both and alone in Hotbar.
			boolean hotbarRow = hotbarOnly || (both && r == 3);
			int base = hotbarRow ? 0 : 9 + r * COLS;
			for (int c = 0; c < COLS; c++) {
				int slot = base + c;
				int sx = x + (vert ? r : c) * step;
				int sy = y + (vert ? c : r) * step;
				ItemStack st = inv.getItem(slot);
				drawSlot(g, st, sx, sy);
				if (hl && hotbarRow && slot == sel) Draw.outline(g, sx, sy, s, s, 0, SELECTED, 1);
				if (numbers) {
					g.text(mc.font, NUMBERS[slot], sx + 2, sy + 2, Theme.HUD_MUTED, true);
				}
				if (!st.isEmpty() && withCounts && st.getCount() > 1) {
					g.itemDecorations(mc.font, st, sx + ox, sy + oy);
				}
			}
		}
	}
}
