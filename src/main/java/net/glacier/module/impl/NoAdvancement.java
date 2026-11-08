package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.ToggleSetting;

/** Suppresses advancement (and optionally recipe) toast pop-ups (see MixinToastManager). */
public class NoAdvancement extends Module {
	private static NoAdvancement instance;

	private final ToggleSetting advancements = add(new ToggleSetting("Hide Advancement Toasts", true));
	private final ToggleSetting recipes = add(new ToggleSetting("Hide Recipe Toasts", false));

	public NoAdvancement() {
		super("No Advancement", "Hides advancement and recipe pop-up toasts.", Category.MISC);
		instance = this;
	}

	public static boolean hideAdvancements() {
		return instance != null && instance.isEnabled() && instance.advancements.on();
	}

	public static boolean hideRecipes() {
		return instance != null && instance.isEnabled() && instance.recipes.on();
	}
}
