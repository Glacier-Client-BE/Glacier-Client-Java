package net.glacier.hud.impl;

/** Look angle (pitch) while gliding, "--" otherwise. Negative is looking up. */
public class ElytraPitch extends CachedRowsHud {
	private static final int NONE = Integer.MIN_VALUE;
	private int lastKey = NONE;

	public ElytraPitch() {
		super("Elytra Pitch", "Shows your look angle (pitch) while gliding.", "Pitch: --");
		stackTopLeft(8);
	}

	@Override
	public void onTick() {
		var p = mc.player;
		boolean gliding = p != null && p.isFallFlying();
		int key = gliding ? Math.round(p.getXRot()) : NONE;
		if (key == lastKey) return;
		lastKey = key;
		setRows(single(part(gliding ? "Pitch: " + key : "Pitch: --", TEXT)));
	}
}
