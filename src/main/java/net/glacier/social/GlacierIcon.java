package net.glacier.social;

import net.glacier.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.UUID;
import java.util.function.Predicate;

/** The small glacier badge shown in front of names of players who run Glacier Client. */
public final class GlacierIcon {
	private static final Identifier FONT = Identifier.fromNamespaceAndPath("glacier", "icons");
	private static final String GLYPH = "";

	/** Built once; callers only read it and append it into new components, never mutate it. */
	private static final MutableComponent PREFIX = Component.literal(GLYPH)
			.setStyle(Style.EMPTY.withFont(new FontDescription.Resource(FONT)))
			.append(Component.literal(" ").setStyle(Style.EMPTY.withFont(FontDescription.DEFAULT)));

	private GlacierIcon() {
	}

	/** The icon glyph followed by one space, ready to prepend to a name. Do not mutate the returned component. */
	public static Component prefix() {
		return PREFIX;
	}

	/**
	 * True when the module is on, the where-test passes, and the player is the local player or a known Glacier
	 * player. Called per frame from render hooks, so it only does map lookups.
	 */
	public static boolean show(UUID id, Predicate<GlacierPlayersModule> where) {
		if (id == null) return false;
		GlacierPlayersModule m = ModuleManager.get(GlacierPlayersModule.class);
		if (m == null || !m.isEnabled() || !where.test(m)) return false;
		LocalPlayer self = Minecraft.getInstance().player;
		if (self != null && self.getUUID().equals(id)) return true;
		return GlacierRegistry.isGlacier(id);
	}
}
