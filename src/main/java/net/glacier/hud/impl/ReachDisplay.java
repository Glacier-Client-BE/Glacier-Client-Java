package net.glacier.hud.impl;

import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.world.phys.HitResult;

import java.util.Locale;

/** "Reach: 3.42 m" to the block or entity under the crosshair, measured from your eyes. Rebuilt once per tick. */
public class ReachDisplay extends CachedRowsHud {
	private final ToggleSetting label = add(new ToggleSetting("Show Label", true));
	private final ToggleSetting blocks = add(new ToggleSetting("Count Blocks", true));
	private final SliderSetting decimals = add(new SliderSetting("Decimals", 2, 0, 3, 1));
	/** Green within 3 blocks, yellow within 4.5, red beyond (the usual survival reach). */
	private final ToggleSetting colored = add(new ToggleSetting("Color By Distance", false));
	private String lastValue = "";
	private int lastColor;
	private boolean lastLabel;

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
		int color = TEXT;
		HitResult hit = mc.hitResult;
		if (mc.player != null && hit != null) {
			HitResult.Type type = hit.getType();
			boolean counts = type == HitResult.Type.ENTITY || (type == HitResult.Type.BLOCK && blocks.on());
			if (counts) {
				double d = mc.player.getEyePosition().distanceTo(hit.getLocation());
				value = String.format(Locale.ROOT, "%." + decimals.i() + "f m", d);
				if (colored.on()) color = goodLow(d, 3.01, 4.51);
			}
		}
		if (value.equals(lastValue) && color == lastColor && label.on() == lastLabel) return;
		lastValue = value;
		lastColor = color;
		lastLabel = label.on();
		setReadout(lastLabel, "Reach", value, color, "");
	}
}
