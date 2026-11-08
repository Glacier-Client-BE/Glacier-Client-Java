package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Theme;

/** Remembers where the player last died this session and shows it. */
public class DeathCoordinates extends CachedRowsHud {
	private final ToggleSetting showDimension = add(new ToggleSetting("Show Dimension", true));
	private boolean wasDead;
	private int dx, dy, dz;
	private String dim;
	private boolean lastDimShown;

	public DeathCoordinates() {
		super("Death Coordinates", "Shows where you last died.", "Last Death: none");
		stackTopLeft(10);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.level == null) return;
		boolean dead = mc.player.isDeadOrDying();
		boolean changed = showDimension.on() != lastDimShown;
		if (dead && !wasDead) {
			dx = (int) Math.floor(mc.player.getX());
			dy = (int) Math.floor(mc.player.getY());
			dz = (int) Math.floor(mc.player.getZ());
			dim = mc.level.dimension().identifier().getPath().replace('_', ' ');
			changed = true;
		}
		wasDead = dead;
		if (!changed || dim == null) return;
		lastDimShown = showDimension.on();
		String pos = dx + ", " + dy + ", " + dz;
		setRows(lastDimShown
				? single(part("Death: ", TEXT), part(pos, Theme.HUD_BAD), part(" (" + dim + ")", TEXT))
				: single(part("Death: ", TEXT), part(pos, Theme.HUD_BAD)));
	}
}
