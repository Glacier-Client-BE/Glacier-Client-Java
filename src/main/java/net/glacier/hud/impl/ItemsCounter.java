package net.glacier.hud.impl;

import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Totals for potions, totems and arrows in the inventory, plus one custom item by raw registry id. */
public class ItemsCounter extends CachedRowsHud {
	private static final int STACK = 64;
	private final ToggleSetting showPotion = add(new ToggleSetting("Show Potion Counter", true));
	private final ToggleSetting showTotem = add(new ToggleSetting("Show Totem Counter", true));
	private final ToggleSetting showArrow = add(new ToggleSetting("Show Arrow Counter", true));
	private final ToggleSetting showCustom = add(new ToggleSetting("Show Custom Counter", false));
	private final SliderSetting customId = add(new SliderSetting("Custom Item ID", 0, 0, 999, 1));
	private final ToggleSetting precise = add(new ToggleSetting("Precise Stack Counts", true));
	/** Counts and settings the rows were last built from; the rows are rebuilt only when one of them changes. */
	private int lastPotions = -1;
	private int lastTotems = -1;
	private int lastArrows = -1;
	private int lastCustom = -1;
	private int lastId = -1;
	private int lastFlags = -1;

	public ItemsCounter() {
		super("Items Counter", "Counts potions, totems, arrows and a custom item in your inventory.", "Items: 0");
		stackTopLeft(32);
	}

	/** Exact count, or stacks and the remainder when Precise Stack Counts is off (for example "2s11"). */
	private String fmt(int count) {
		if (precise.on() || count < STACK) return String.valueOf(count);
		return count / STACK + "s" + count % STACK;
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		Inventory inv = mc.player.getInventory();
		int potions = 0;
		int totems = 0;
		int arrows = 0;
		int custom = 0;
		int id = customId.i();
		boolean counting = showCustom.on();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack st = inv.getItem(i);
			if (st.isEmpty()) continue;
			Item item = st.getItem();
			if (item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION) {
				potions += st.getCount();
			} else if (item == Items.TOTEM_OF_UNDYING) {
				totems += st.getCount();
			} else if (item == Items.ARROW) {
				arrows += st.getCount();
			}
			if (counting && Item.getId(item) == id) custom += st.getCount();
		}
		int flags = (showPotion.on() ? 1 : 0) | (showTotem.on() ? 2 : 0) | (showArrow.on() ? 4 : 0)
				| (counting ? 8 : 0) | (precise.on() ? 16 : 0);
		if (potions == lastPotions && totems == lastTotems && arrows == lastArrows && custom == lastCustom
				&& id == lastId && flags == lastFlags) return;
		lastPotions = potions;
		lastTotems = totems;
		lastArrows = arrows;
		lastCustom = custom;
		lastId = id;
		lastFlags = flags;

		List<Part[]> rows = new ArrayList<>();
		if (showPotion.on()) rows.add(new Part[]{part("Potions: " + fmt(potions), TEXT)});
		if (showTotem.on()) rows.add(new Part[]{part("Totems: " + fmt(totems), TEXT)});
		if (showArrow.on()) rows.add(new Part[]{part("Arrows: " + fmt(arrows), TEXT)});
		if (counting) rows.add(new Part[]{part("Custom " + id + ": " + fmt(custom), TEXT)});
		setRows(rows);
	}
}
