package net.glacier.hud.impl;

/** JVM heap "Mem: used/max MB (pct%)", refreshed once per second. */
public class MemoryUsage extends CachedRowsHud {
	private int ticks;
	private long lastUsed = -1;
	private long lastMax = -1;

	public MemoryUsage() {
		super("Memory Usage", "Shows Java memory usage.", "Mem: -");
		stackTopLeft(14);
	}

	@Override
	public void onTick() {
		if (ticks-- > 0) return;
		ticks = 19;
		Runtime rt = Runtime.getRuntime();
		long max = rt.maxMemory() >> 20;
		long used = (rt.totalMemory() - rt.freeMemory()) >> 20;
		if (used == lastUsed && max == lastMax) return;
		lastUsed = used;
		lastMax = max;
		int pct = (int) (used * 100 / Math.max(1, max));
		setRows(single(part("Mem: ", TEXT), part(used + "/" + max + " MB", goodLow(pct, 70, 90)),
				part(" (" + pct + "%)", TEXT)));
	}
}
