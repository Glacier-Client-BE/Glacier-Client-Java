package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.ColorSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.IdentityHashMap;

/**
 * Gives chosen dropped items a coloured outline, using the same outline pass as glowing entities. MixinItemEntityRenderer
 * sets EntityRenderState#outlineColor while the item entity's render state is extracted (a non-zero value is what makes
 * vanilla draw the entity into the outline target). Item selection is by category toggles since the settings have no
 * text field: enchanted, rare, valuables, gear, potions or everything.
 */
public class ItemHighlighter extends Module {
	private static final int VALUABLE = 1;
	private static final int GEAR = 2;
	private static final int POTION = 4;

	private static final String[] VALUABLE_KEYS = {"diamond", "netherite", "emerald", "totem", "nether_star", "elytra",
			"golden_apple", "beacon", "dragon", "shulker_box", "trident", "heart_of_the_sea", "ancient_debris", "wither_skeleton"};
	private static final String[] GEAR_KEYS = {"_sword", "_axe", "_pickaxe", "_shovel", "_hoe", "_helmet", "_chestplate",
			"_leggings", "_boots", "bow", "mace", "shield", "trident", "fishing_rod", "elytra"};

	private static ItemHighlighter instance;

	private final ToggleSetting all = add(new ToggleSetting("All Items", false));
	private final ToggleSetting enchanted = add(new ToggleSetting("Enchanted", true));
	private final ToggleSetting rare = add(new ToggleSetting("Rare Items", true));
	private final ToggleSetting valuables = add(new ToggleSetting("Valuables", true));
	private final ToggleSetting gear = add(new ToggleSetting("Gear", false));
	private final ToggleSetting potions = add(new ToggleSetting("Potions", false));
	private final ToggleSetting rainbow = add(new ToggleSetting("Rainbow", false));
	private final ColorSetting color = add(new ColorSetting("Color", 0xFFFFD83A));

	private final IdentityHashMap<Item, Integer> flagCache = new IdentityHashMap<>();

	public ItemHighlighter() {
		super("Item Highlighter", "Outlines important dropped items so you spot them from afar.", Category.RENDER);
		instance = this;
	}

	public static boolean active() {
		ItemHighlighter h = instance;
		return h != null && h.isEnabled();
	}

	/** Outline colour (opaque ARGB) for this dropped stack, or 0 when it should not be highlighted. */
	public static int outlineFor(ItemStack stack) {
		ItemHighlighter h = instance;
		if (h == null || !h.isEnabled() || stack == null || stack.isEmpty()) return 0;
		if (!h.matches(stack)) return 0;
		if (h.rainbow.on()) {
			return java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 3000L) / 3000f, 0.9f, 1f) | 0xFF000000;
		}
		return h.color.argb() | 0xFF000000;
	}

	private boolean matches(ItemStack stack) {
		if (all.on()) return true;
		if (enchanted.on() && stack.isEnchanted()) return true;
		if (rare.on() && stack.getRarity() != Rarity.COMMON) return true;
		int f = flags(stack.getItem());
		return (valuables.on() && (f & VALUABLE) != 0)
				|| (gear.on() && (f & GEAR) != 0)
				|| (potions.on() && (f & POTION) != 0);
	}

	private int flags(Item item) {
		Integer cached = flagCache.get(item);
		if (cached != null) return cached;
		String path = BuiltInRegistries.ITEM.getKey(item).getPath();
		int f = 0;
		for (String k : VALUABLE_KEYS) if (path.contains(k)) f |= VALUABLE;
		for (String k : GEAR_KEYS) if (path.contains(k)) f |= GEAR;
		if (path.contains("potion")) f |= POTION;
		flagCache.put(item, f);
		return f;
	}
}
