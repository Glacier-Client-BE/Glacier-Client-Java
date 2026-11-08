package net.glacier.hud.impl;

import net.glacier.ui.Theme;

import java.util.ArrayList;
import java.util.List;

/** Movement state, one line per active state: Sprinting, Sneaking, Flying, Gliding. "Standing" when none. */
public class StatusHud extends CachedRowsHud {
	private static final int SPRINT = 1;
	private static final int SNEAK = 2;
	private static final int FLY = 4;
	private static final int GLIDE = 8;
	private int lastKey = -1;

	public StatusHud() {
		super("Status HUD", "Shows whether you are sprinting, sneaking, flying or gliding.", "Standing");
		stackTopLeft(4);
	}

	@Override
	public void onTick() {
		var p = mc.player;
		int key = 0;
		if (p != null) {
			if (p.isSprinting()) key |= SPRINT;
			if (p.isCrouching()) key |= SNEAK;
			if (p.getAbilities().flying) key |= FLY;
			if (p.isFallFlying()) key |= GLIDE;
		}
		if (key == lastKey) return;
		lastKey = key;
		List<Part[]> out = new ArrayList<>();
		if ((key & SPRINT) != 0) out.add(new Part[]{part("Sprinting", Theme.HUD_GOOD)});
		if ((key & SNEAK) != 0) out.add(new Part[]{part("Sneaking", Theme.HUD_WARN)});
		if ((key & FLY) != 0) out.add(new Part[]{part("Flying", Theme.HUD_BIOME)});
		if ((key & GLIDE) != 0) out.add(new Part[]{part("Gliding", Theme.HUD_BIOME)});
		if (out.isEmpty()) out.add(new Part[]{part("Standing", TEXT)});
		setRows(out);
	}
}
