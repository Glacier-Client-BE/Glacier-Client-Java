package net.glacier.hud.impl;

import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.multiplayer.PlayerInfo;

/** "Ping: 32 ms", green under 80, yellow under 150, red above. */
public class PingCounter extends CachedRowsHud {
	private final ToggleSetting label = add(new ToggleSetting("Show Label", true));
	private final ToggleSetting colored = add(new ToggleSetting("Color By Value", true));
	private final ChoiceSetting labelText = add(new ChoiceSetting("Label Text", "Ping", "Ping", "Latency"));
	private int lastPing = -1;
	private int lastKey = -1;

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
		int key = (label.on() ? 1 : 0) + (colored.on() ? 2 : 0) + labelText.index() * 4;
		if (p == lastPing && key == lastKey) return;
		lastPing = p;
		lastKey = key;
		setReadout(label.on(), labelText.get(), String.valueOf(p), colored.on() ? goodLow(p, 80, 150) : TEXT, " ms");
	}
}
