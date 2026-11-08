package net.glacier.module.impl;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.ToggleSetting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Hides noisy chat lines: join/leave notices, death messages, advancement announcements and
 * back-to-back duplicate messages. Uses Fabric's receive-message events (no mixin); matching is
 * done on the translation key first and falls back to the plain text for server-formatted lines.
 */
public class ChatFilter extends Module {
	private final ToggleSetting joinLeave = add(new ToggleSetting("Hide Join / Leave", true));
	private final ToggleSetting deaths = add(new ToggleSetting("Hide Death Messages", false));
	private final ToggleSetting advancements = add(new ToggleSetting("Hide Advancements", false));
	private final ToggleSetting duplicates = add(new ToggleSetting("Hide Duplicates", true));

	private String lastText = "";
	private long lastAt;

	public ChatFilter() {
		super("Chat Filter", "Filters join/leave, death, advancement and duplicate chat lines.", Category.MISC);
		ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> overlay || !isEnabled() || allow(message));
		ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, params, time) -> !isEnabled() || !isDuplicate(message));
	}

	private boolean allow(Component message) {
		String key = message.getContents() instanceof TranslatableContents tc ? tc.getKey() : "";
		if (joinLeave.on()) {
			if (key.equals("multiplayer.player.joined") || key.equals("multiplayer.player.left")
					|| key.equals("multiplayer.player.joined.renamed")) return false;
		}
		if (deaths.on() && key.startsWith("death.")) return false;
		if (advancements.on() && key.startsWith("chat.type.advancement.")) return false;
		if (joinLeave.on() && key.isEmpty()) {
			String s = message.getString();
			if (s.endsWith(" joined the game") || s.endsWith(" left the game")) return false;
		}
		return !isDuplicate(message);
	}

	private boolean isDuplicate(Component message) {
		if (!duplicates.on()) return false;
		String text = message.getString();
		long now = System.currentTimeMillis();
		boolean dup = text.equals(lastText) && now - lastAt < 10_000L;
		lastText = text;
		lastAt = now;
		return dup;
	}

	@Override
	protected void onDisable() {
		lastText = "";
	}
}
