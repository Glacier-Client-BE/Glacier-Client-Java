package net.glacier.hud.impl;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;

/** Seconds until the nearest lit TNT within 64 blocks explodes. */
public class TntTimerHud extends LabelValueHud {
	private static final double RANGE_SQ = 64 * 64;
	private int fuse = -1;

	public TntTimerHud() {
		super("TNT Timer", "Shows the fuse of the nearest lit TNT.", "TNT", 29);
	}

	@Override
	protected String value() {
		if (mc.level == null || mc.player == null) return null;
		double best = RANGE_SQ;
		fuse = -1;
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!(e instanceof PrimedTnt t)) continue;
			double d = t.distanceToSqr(mc.player);
			if (d < best) {
				best = d;
				fuse = t.getFuse();
			}
		}
		return fuse < 0 ? "-" : String.format("%.1fs", fuse / 20.0);
	}

	@Override
	protected int valueColor() {
		return fuse < 0 ? TEXT : goodHigh(fuse, 60, 30);
	}
}
