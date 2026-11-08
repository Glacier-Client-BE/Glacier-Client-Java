package net.glacier.hud.impl;

import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Theme;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Shows item cooldowns (ender pearl, shield, chorus fruit, wind charge) while they are running. Hidden when idle. */
public class CooldownHud extends CachedRowsHud {
	private static final int N = 4;
	private static final String[] NAMES = {"Pearl", "Shield", "Chorus", "Wind Charge"};
	/** Default cooldown length in ticks per item, used only for the approximate seconds readout. */
	private static final int[] TICKS = {20, 100, 20, 10};

	private final ToggleSetting[] show = new ToggleSetting[N];
	private final ChoiceSetting format = add(new ChoiceSetting("Format", "Seconds", "Seconds", "Percent"));
	private final ItemStack[] stacks = new ItemStack[N];
	private final int[] lastPct = new int[N];
	private int lastFormat = -1;
	private boolean any;

	public CooldownHud() {
		super("Cooldowns", "Shows running item cooldowns.", "Pearl: 1.0s");
		for (int i = 0; i < N; i++) show[i] = add(new ToggleSetting("Show " + NAMES[i], true));
		Arrays.fill(lastPct, -1);
		stackTopLeft(43);
	}

	@Override
	protected boolean hidden() {
		return !any;
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		if (stacks[0] == null) {
			Item[] items = {Items.ENDER_PEARL, Items.SHIELD, Items.CHORUS_FRUIT, Items.WIND_CHARGE};
			for (int i = 0; i < N; i++) stacks[i] = new ItemStack(items[i]);
		}
		var cd = mc.player.getCooldowns();
		boolean changed = lastFormat != format.index();
		lastFormat = format.index();
		any = false;
		for (int i = 0; i < N; i++) {
			int pct = show[i].on() ? Math.round(cd.getCooldownPercent(stacks[i], 0f) * 100f) : 0;
			if (pct > 0) any = true;
			if (pct != lastPct[i]) {
				lastPct[i] = pct;
				changed = true;
			}
		}
		if (!changed) return;
		List<Part[]> rows = new ArrayList<>(N);
		for (int i = 0; i < N; i++) {
			if (lastPct[i] <= 0) continue;
			String v = format.is("Percent") ? lastPct[i] + "%"
					: String.format("%.1fs", lastPct[i] / 100f * TICKS[i] / 20f);
			rows.add(new Part[]{part(NAMES[i] + ": ", Theme.HUD_MUTED), part(v, goodLow(lastPct[i], 30, 70))});
		}
		setRows(rows);
	}
}
