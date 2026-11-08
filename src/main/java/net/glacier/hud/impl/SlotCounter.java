package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Used or free slots of the main inventory (36 slots), recounted once per tick. */
public class SlotCounter extends CachedRowsHud {
	private static final int MAIN_SLOTS = 36;
	/** Font Awesome "box-archive" glyph. */
	private static final int ICON = 0xf187;
	private final ToggleSetting showFree = add(new ToggleSetting("Show Free Slots", false));
	private final ToggleSetting slotColour = add(new ToggleSetting("Enable Slot Colour", false));
	private final ToggleSetting hideMax = add(new ToggleSetting("Hide Max", false));
	private final ToggleSetting hideIcon = add(new ToggleSetting("Hide Slot Icon", false));
	private int lastFree = -1;
	private int lastFlags = -1;

	public SlotCounter() {
		super("Slot Counter", "Tracks the empty slots in your inventory.", "Used: 0/36");
		stackTopLeft(9);
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		Inventory inv = mc.player.getInventory();
		int free = 0;
		for (int i = 0; i < MAIN_SLOTS; i++) {
			if (inv.getItem(i).isEmpty()) free++;
		}
		boolean freeMode = showFree.on();
		int flags = (freeMode ? 1 : 0) | (slotColour.on() ? 2 : 0) | (hideMax.on() ? 4 : 0) | (hideIcon.on() ? 8 : 0);
		if (free == lastFree && flags == lastFlags) return;
		lastFree = free;
		lastFlags = flags;

		int value = freeMode ? free : MAIN_SLOTS - free;
		String text = (freeMode ? "Free " : "Used ") + value + (hideMax.on() ? "" : "/" + MAIN_SLOTS);
		int col = slotColour.on() ? goodHigh(free, 9, 3) : TEXT;
		if (hideIcon.on()) {
			setRows(single(part(text, col)));
		} else {
			setRows(single(icon(ICON, TEXT), part(" " + text, col)));
		}
	}
}
