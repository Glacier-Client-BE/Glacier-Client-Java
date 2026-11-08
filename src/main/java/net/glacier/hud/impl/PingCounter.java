package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;
import net.minecraft.client.multiplayer.PlayerInfo;

/** "Ping: 32 ms", green under 80, yellow under 150, red above. */
public class PingCounter extends CachedRowsHud {
	private final ToggleSetting label = add(new ToggleSetting("Show Label", true));
	private int lastPing = -1;
	private boolean lastLabel;

	public PingCounter() {
		super("Ping Counter", "Displays your latency to the server.", "Ping: 0 ms");
		stackTopLeft(1);
	}

	@Override
	protected boolean pill() {
		return true;
	}

	private int ping() {
		if (mc.player == null || mc.getConnection() == null) return 0;
		PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
		return info == null ? 0 : Math.max(0, info.getLatency());
	}

	@Override
	public void onTick() {
		int p = ping();
		boolean showLabel = label.on();
		if (p == lastPing && showLabel == lastLabel) return;
		lastPing = p;
		lastLabel = showLabel;
		int color = goodLow(p, 80, 150);
		setRows(!showLabel
				? single(part(p + " ms", color))
				: single(part("Ping: ", TEXT), part(String.valueOf(p), color), part(" ms", TEXT)));
	}
}
