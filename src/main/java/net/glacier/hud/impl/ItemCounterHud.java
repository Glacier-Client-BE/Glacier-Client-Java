package net.glacier.hud.impl;

import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Counts one chosen item family (arrows, totems, pearls, golden apples, potions, blocks and more) in the inventory. */
public class ItemCounterHud extends CachedRowsHud {
	private static final String[] FAMILIES = {"Arrows", "Totems", "Ender Pearls", "Golden Apples", "Potions",
			"Blocks", "XP Bottles", "Fireworks", "End Crystals", "Obsidian", "Wind Charges"};
	private static final int STACK = 64;

	private final ChoiceSetting family = add(new ChoiceSetting("Item", "Arrows", FAMILIES));
	private final ToggleSetting label = add(new ToggleSetting("Show Label", true));
	private final ToggleSetting stacks = add(new ToggleSetting("Show As Stacks", false));
	private final ToggleSetting colored = add(new ToggleSetting("Color Low Count", false));
	private int lastCount = -1;
	private int lastKey = -1;

	public ItemCounterHud() {
		super("Item Counter", "Counts a chosen item family in your inventory.", "Arrows: 0");
		stackTopLeft(41);
	}

	@Override
	protected boolean pill() {
		return true;
	}

	/** True when the item belongs to the chosen family index. */
	private static boolean matches(int fam, Item it) {
		return switch (fam) {
			case 0 -> it == Items.ARROW || it == Items.TIPPED_ARROW || it == Items.SPECTRAL_ARROW;
			case 1 -> it == Items.TOTEM_OF_UNDYING;
			case 2 -> it == Items.ENDER_PEARL;
			case 3 -> it == Items.GOLDEN_APPLE || it == Items.ENCHANTED_GOLDEN_APPLE;
			case 4 -> it == Items.POTION || it == Items.SPLASH_POTION || it == Items.LINGERING_POTION;
			case 5 -> it instanceof BlockItem;
			case 6 -> it == Items.EXPERIENCE_BOTTLE;
			case 7 -> it == Items.FIREWORK_ROCKET;
			case 8 -> it == Items.END_CRYSTAL;
			case 9 -> it == Items.OBSIDIAN;
			default -> it == Items.WIND_CHARGE;
		};
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		int fam = family.index();
		var inv = mc.player.getInventory();
		int total = 0;
		for (int i = 0; i < 36; i++) {
			ItemStack st = inv.getItem(i);
			if (!st.isEmpty() && matches(fam, st.getItem())) total += st.getCount();
		}
		ItemStack off = mc.player.getOffhandItem();
		if (!off.isEmpty() && matches(fam, off.getItem())) total += off.getCount();

		int key = fam * 8 + (label.on() ? 1 : 0) + (stacks.on() ? 2 : 0) + (colored.on() ? 4 : 0);
		if (total == lastCount && key == lastKey) return;
		lastCount = total;
		lastKey = key;
		String text = stacks.on() && total >= STACK ? total / STACK + "s" + total % STACK : String.valueOf(total);
		int color = colored.on() ? goodHigh(total, 8, 3) : TEXT;
		setReadout(label.on(), family.get(), text, color, "");
	}
}
