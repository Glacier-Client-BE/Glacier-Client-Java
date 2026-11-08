package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;
import net.minecraft.client.multiplayer.ServerData;

import java.util.List;

/** The server address, or "Singleplayer", with an optional server icon glyph before or after it. */
public class ServerDisplay extends CachedRowsHud {
	/** Font Awesome "server" glyph. */
	private static final int SERVER_ICON = 0xf233;
	private final ToggleSetting showIcon = add(new ToggleSetting("Show Icon", true));
	private final ToggleSetting showName = add(new ToggleSetting("Show Name", true));
	private final ToggleSetting iconRight = add(new ToggleSetting("Icon Right", true));
	private String last = "";
	private boolean lastIcon;
	private boolean lastName;
	private boolean lastRight;

	public ServerDisplay() {
		super("Server Display", "Shows the server you are playing on.", "Singleplayer");
		stackTopLeft(12);
	}

	@Override
	protected boolean pill() {
		return true;
	}

	@Override
	public void onTick() {
		ServerData sd = mc.getCurrentServer();
		String s = mc.isLocalServer() || sd == null ? "Singleplayer" : sd.ip;
		boolean wantIcon = showIcon.on();
		boolean wantName = showName.on();
		boolean right = iconRight.on();
		if (s.equals(last) && wantIcon == lastIcon && wantName == lastName && right == lastRight) return;
		last = s;
		lastIcon = wantIcon;
		lastName = wantName;
		lastRight = right;

		Part nm = part(s, TEXT);
		Part ic = icon(SERVER_ICON, TEXT);
		Part sp = part(" ", TEXT);
		if (wantIcon && wantName) setRows(single(right ? new Part[]{nm, sp, ic} : new Part[]{ic, sp, nm}));
		else if (wantName) setRows(single(nm));
		else if (wantIcon) setRows(single(ic));
		else setRows(List.of());
	}
}
