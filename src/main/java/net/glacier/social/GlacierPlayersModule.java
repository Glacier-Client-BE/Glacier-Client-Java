package net.glacier.social;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.module.ModuleManager;
import net.glacier.setting.ToggleSetting;

/**
 * Marks players who run Glacier Client. Opt-in and off by default: while disabled, no network request is made and
 * no data is sent. See GlacierRegistry for exactly what is sent when enabled.
 */
public final class GlacierPlayersModule extends Module {
	public final ToggleSetting showTab = add(new ToggleSetting("Show In Tab List", true));
	public final ToggleSetting showNametag = add(new ToggleSetting("Show On Nametag", true));
	public final ToggleSetting showChat = add(new ToggleSetting("Show In Chat", true));

	public GlacierPlayersModule() {
		super("Glacier Players", "Marks other players who run Glacier Client. Sends your username and the UUIDs of players in your tab list to the Glacier registry.", Category.MISC);
	}

	/** True when the module is enabled. */
	public static boolean on() {
		GlacierPlayersModule m = ModuleManager.get(GlacierPlayersModule.class);
		return m != null && m.isEnabled();
	}

	@Override
	protected void onDisable() {
		GlacierRegistry.reset();
	}
}
