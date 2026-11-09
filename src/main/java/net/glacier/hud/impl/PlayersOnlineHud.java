package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;
import net.minecraft.client.multiplayer.ClientPacketListener;

/** "Online: 12", the number of players the server currently lists in the tab list. */
public class PlayersOnlineHud extends CachedRowsHud {
	private final ToggleSetting label = add(new ToggleSetting("Show Label", true));

	private int lastCount = Integer.MIN_VALUE;
	private boolean lastLabel;

	public PlayersOnlineHud() {
		super("Players Online", "Shows how many players the server lists as online.", "Online: -");
		stackTopLeft(21);
	}

	@Override
	protected boolean pill() {
		return true;
	}

	@Override
	public void onTick() {
		ClientPacketListener connection = mc.getConnection();
		int count = connection == null ? -1 : connection.getOnlinePlayers().size();
		if (count == lastCount && label.on() == lastLabel) return;
		lastCount = count;
		lastLabel = label.on();
		if (count < 0) {
			setRows(single(part("Online: -", TEXT)));
			return;
		}
		String n = String.valueOf(count);
		if (label.on()) setRows(single(part("Online: ", TEXT), part(n, TEXT)));
		else setRows(single(part(n, TEXT)));
	}
}
