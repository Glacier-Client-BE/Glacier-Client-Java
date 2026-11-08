package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;
import net.minecraft.client.multiplayer.ClientPacketListener;

/** "Online: 12", the number of players the server currently lists in the tab list. */
public class PlayersOnlineHud extends CachedRowsHud {
	private final ToggleSetting label = add(new ToggleSetting("Show Label", true));

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
		if (connection == null) {
			setRows(single(part("Online: -", TEXT)));
			return;
		}
		String n = String.valueOf(connection.getOnlinePlayers().size());
		if (label.on()) setRows(single(part("Online: ", TEXT), part(n, TEXT)));
		else setRows(single(part(n, TEXT)));
	}
}
