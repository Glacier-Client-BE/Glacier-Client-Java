package net.glacier.module.impl;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.ToggleSetting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Optional;

/**
 * Hides your own account name while streaming: replaced by "Streamer" in the name tag (third person), the tab
 * list and system/game chat lines. Name tag and tab use mixins (Nr2StreamerNametag, Nr2StreamerTab); chat uses
 * the Fabric modify-game-message event. Signed player chat is not rewritten (it cannot be modified client side).
 */
public class StreamerMode extends Module {
	private static StreamerMode instance;
	private static final String ALIAS = "Streamer";

	private final ToggleSetting nametag = add(new ToggleSetting("Hide Name Tag", true));
	private final ToggleSetting tab = add(new ToggleSetting("Hide In Tab List", true));
	private final ToggleSetting chat = add(new ToggleSetting("Hide In Chat", true));

	public StreamerMode() {
		super("Streamer Mode", "Replaces your username with an alias in name tag, tab list and chat.", Category.MISC);
		instance = this;
		ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) ->
				isEnabled() && chat.on() ? scrub(message) : message);
	}

	/** Returns the name with the alias applied when it belongs to the local player. */
	public static Component forNametag(Component name, boolean isSelf) {
		StreamerMode s = instance;
		if (s == null || !s.isEnabled() || !s.nametag.on() || !isSelf) return name;
		return scrub(name);
	}

	public static Component forTab(Component name, boolean isSelf) {
		StreamerMode s = instance;
		if (s == null || !s.isEnabled() || !s.tab.on() || !isSelf) return name;
		return scrub(name);
	}

	private static Component scrub(Component in) {
		if (in == null || mc.getUser() == null) return in;
		String own = mc.getUser().getName();
		if (own == null || own.isEmpty() || !in.getString().contains(own)) return in;
		MutableComponent out = Component.empty();
		in.visit((style, text) -> {
			out.append(Component.literal(text.replace(own, ALIAS)).withStyle(style));
			return Optional.empty();
		}, Style.EMPTY);
		return out;
	}
}
