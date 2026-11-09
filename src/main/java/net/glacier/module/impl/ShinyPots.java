package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Shiny Potions: potion bottles (drinkable, splash and lingering) get the enchantment glint, like the pack module that
 * "adds a glint effect to potion items". MixinItemStack forces ItemStack#hasFoil() for them, which is what both item
 * model wrappers consult when they pick the foil type. The glint takes its colour from the Glint Colorizer when that is on;
 * a per-potion colour is not possible because the glint render types are shared between all items.
 */
public class ShinyPots extends Module {
	private static ShinyPots instance;

	public ShinyPots() {
		super("Shiny Potions", "Adds a glint effect to potion items.", Category.RENDER);
		instance = this;
	}

	/** True when this stack should be drawn with a glint although vanilla would not. */
	public static boolean shines(ItemStack stack) {
		ShinyPots s = instance;
		if (s == null || !s.isEnabled()) return false;
		Item i = stack.getItem();
		return i == Items.POTION || i == Items.SPLASH_POTION || i == Items.LINGERING_POTION;
	}
}
