package net.glacier.hud.impl;

import net.minecraft.util.Mth;

/** Yaw and pitch in degrees, yaw wrapped to -180..180. */
public class RotationHud extends LabelValueHud {
	public RotationHud() {
		super("Rotation", "Shows your yaw and pitch.", "Yaw/Pitch", 27);
	}

	@Override
	protected String value() {
		if (mc.player == null) return null;
		return String.format("%.1f / %.1f", Mth.wrapDegrees(mc.player.getYRot()), mc.player.getXRot());
	}
}
