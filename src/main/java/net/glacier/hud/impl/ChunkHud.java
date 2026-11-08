package net.glacier.hud.impl;

/** Chunk coordinates and your offset inside the current chunk. */
public class ChunkHud extends LabelValueHud {
	public ChunkHud() {
		super("Chunk", "Shows the chunk you are standing in.", "Chunk", 28);
	}

	@Override
	protected String value() {
		if (mc.player == null) return null;
		int x = mc.player.getBlockX(), z = mc.player.getBlockZ();
		return (x >> 4) + ", " + (z >> 4) + " (" + (x & 15) + ", " + (z & 15) + ")";
	}
}
