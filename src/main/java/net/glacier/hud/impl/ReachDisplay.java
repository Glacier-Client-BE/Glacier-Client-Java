package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;
import net.minecraft.world.phys.HitResult;

import java.util.Locale;

/** "Reach: 3.42 m" to the block or entity under the crosshair, measured from your eyes. Rebuilt once per tick. */
public class ReachDisplay extends CachedRowsHud {
	private final ToggleSetting label = add(new ToggleSetting("Show Label", true));
	private final ToggleSetting blocks = add(new ToggleSetting("Count Blocks", true));

	public ReachDisplay() {
		super("Reach Display", "Shows how far away the block or entity under your crosshair is.", "Reach: --");
		stackTopLeft(17);
	}

	@Override
	protected boolean pill() {
		return true;
	}

	@Override
	public void onTick() {
		String value = "--";
		HitResult hit = mc.hitResult;
		if (mc.player != null && hit != null) {
			HitResult.Type type = hit.getType();
			boolean counts = type == HitResult.Type.ENTITY || (type == HitResult.Type.BLOCK && blocks.on());
			if (counts) {
				double d = mc.player.getEyePosition().distanceTo(hit.getLocation());
				value = String.format(Locale.ROOT, "%.2f m", d);
			}
		}
		if (label.on()) setRows(single(part("Reach: ", TEXT), part(value, TEXT)));
		else setRows(single(part(value, TEXT)));
	}
}
