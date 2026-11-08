package net.glacier.hud.impl;

import net.minecraft.world.entity.player.Player;

/** Name and distance of the closest other player. */
public class NearestPlayerHud extends LabelValueHud {
	public NearestPlayerHud() {
		super("Nearest Player", "Shows the closest player and their distance.", "Nearest", 30);
	}

	@Override
	protected String value() {
		if (mc.level == null || mc.player == null) return null;
		Player best = null;
		double bd = Double.MAX_VALUE;
		for (Player p : mc.level.players()) {
			if (p == mc.player) continue;
			double d = p.distanceToSqr(mc.player);
			if (d < bd) {
				bd = d;
				best = p;
			}
		}
		return best == null ? "-" : best.getName().getString() + " " + Math.round(Math.sqrt(bd)) + "m";
	}
}
