package net.glacier.hud.impl;

import net.minecraft.world.level.Level;

/**
 * "Portal: 12, 64, -30": where the matching portal sits in the other dimension (Overworld x/z divided by 8
 * for the Nether, Nether x/z times 8 for the Overworld). Shows "-" in the End.
 */
public class PortalCoordsHud extends LabelValueHud {
	private int lastX = Integer.MIN_VALUE;
	private int lastY;
	private int lastZ;
	private boolean lastOverworld;
	private String text = "-";

	public PortalCoordsHud() {
		super("Portal Coordinates", "Shows the coordinates of the linked portal in the other dimension.", "Portal", 43);
	}

	@Override
	protected String value() {
		if (mc.player == null || mc.level == null) return null;
		boolean overworld = mc.level.dimension() == Level.OVERWORLD;
		boolean nether = mc.level.dimension() == Level.NETHER;
		if (!overworld && !nether) return "-";
		int x = (int) Math.floor(mc.player.getX());
		int y = (int) Math.floor(mc.player.getY());
		int z = (int) Math.floor(mc.player.getZ());
		if (x != lastX || y != lastY || z != lastZ || overworld != lastOverworld) {
			lastX = x;
			lastY = y;
			lastZ = z;
			lastOverworld = overworld;
			text = overworld
					? Math.floorDiv(x, 8) + ", " + y + ", " + Math.floorDiv(z, 8)
					: (x * 8) + ", " + y + ", " + (z * 8);
		}
		return text;
	}
}
