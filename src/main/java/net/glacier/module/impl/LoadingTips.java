package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;

import java.util.Random;

/**
 * Loading tips (NRC loadingtips): a one line tip under the card of the loading screen. The screen asks
 * {@link #current()} every frame; it returns an empty string while the module is off. A new tip is picked whenever
 * the loading screen was not drawn for a couple of seconds (a new loading), and it can cycle while it stays open.
 */
public class LoadingTips extends Module {
	private static LoadingTips instance;

	private static final String[] MINECRAFT = {
			"Hold Shift while clicking an item to move the whole stack at once.",
			"Sleeping in a bed sets your respawn point.",
			"Sneaking at the edge of a block keeps you from falling off.",
			"Falling into water cancels fall damage.",
			"Torches stop hostile mobs from spawning on the blocks they light.",
			"F3 + B shows the hitboxes of every entity around you.",
			"A smoker cooks food twice as fast as a furnace.",
			"Netherite items survive fire and lava.",
			"A shield blocks arrows and melee hits while you hold it up.",
			"Villagers restock their trades after they have worked at their job site.",
			"Phantom membrane repairs an elytra in an anvil.",
			"Silk Touch lets you mine glass, ice and ores as blocks.",
			"Mending repairs your tools with the experience you collect.",
			"Bone meal makes crops and saplings grow faster.",
			"Bring food, torches and a spare pickaxe before you go caving.",
			"Wolves are tamed with bones, cats and ocelots with raw fish.",
			"A name tag keeps a mob from despawning.",
			"F5 cycles between first person and the two third person views.",
			"A map fills in as you explore and can be copied on a cartography table.",
			"Ladders and scaffolding let you climb without using blocks.",
	};
	private static final String[] GLACIER = {
			"Glacier: every module can be switched on and off from the Glacier menu.",
			"Glacier: HUD elements can be moved and scaled in the HUD editor.",
			"Glacier: most modules have their own settings, open a module card to see them.",
	};

	public final ToggleSetting glacierTips = add(new ToggleSetting("Include Glacier Tips", true));
	public final ToggleSetting cycle = add(new ToggleSetting("Cycle Tips", true));
	public final SliderSetting interval = add(new SliderSetting("Cycle Seconds", 6, 3, 20, 1));

	private final Random random = new Random();
	private long lastCall;
	private long startedAt;
	private int start;

	public LoadingTips() {
		super("Loading Tips", "Shows a gameplay tip on the loading screen.", Category.MISC);
		instance = this;
	}

	/** The tip to show right now, or an empty string when the module is off. Safe to call every frame. */
	public static String current() {
		LoadingTips t = instance;
		return t != null && t.isEnabled() ? t.pick() : "";
	}

	private String pick() {
		int total = MINECRAFT.length + (glacierTips.on() ? GLACIER.length : 0);
		long now = System.currentTimeMillis();
		if (now - lastCall > 2500L) {
			start = random.nextInt(total);
			startedAt = now;
		}
		lastCall = now;
		int step = cycle.on() ? (int) ((now - startedAt) / (interval.i() * 1000L)) : 0;
		int index = (start + step) % total;
		return index < MINECRAFT.length ? MINECRAFT[index] : GLACIER[index - MINECRAFT.length];
	}
}
