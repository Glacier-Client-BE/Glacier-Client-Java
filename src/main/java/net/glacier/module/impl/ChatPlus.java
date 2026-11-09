package net.glacier.module.impl;

import com.mojang.authlib.GameProfile;
import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.ColorSetting;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.component.ResolvableProfile;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Chat upgrades from the owner's Bedrock chat module and the NRC chat modules, in one module: timestamps,
 * player heads in front of messages, compacting repeated lines, highlighting your own name, message sounds,
 * copy on click, a longer history and the chat layout (scale, width, line spacing, text and background opacity).
 *
 * <p>All hooks live in {@code mixin.extrac.ExtraCChat} (ChatComponent) and {@code ExtraCChatScreen}. The text
 * changes happen once when a message arrives (the component is rewritten before the vanilla code stores it), the
 * layout values are read by the mixins while rendering. Disabled, every static method returns the vanilla value.
 */
public class ChatPlus extends Module {
	private static ChatPlus instance;

	public final ToggleSetting timestamps = add(new ToggleSetting("Timestamps", true));
	public final ChoiceSetting timeFormat = add(new ChoiceSetting("Time Format", "24 Hour", "24 Hour", "12 Hour"));
	public final ToggleSetting seconds = add(new ToggleSetting("Show Seconds", false));
	public final ColorSetting timeColor = add(new ColorSetting("Timestamp Color", 0xFF8B93A0));
	public final ToggleSetting heads = add(new ToggleSetting("Player Heads", true));
	public final ToggleSetting compact = add(new ToggleSetting("Compact Repeats", true));
	public final ToggleSetting highlight = add(new ToggleSetting("Highlight My Name", true));
	public final ColorSetting highlightColor = add(new ColorSetting("Highlight Color", 0xFFFFD166));
	public final ChoiceSetting sound = add(new ChoiceSetting("Message Sound", "Mentions", "Off", "Mentions", "All Messages"));
	public final ChoiceSetting copy = add(new ChoiceSetting("Copy Message", "Ctrl+Click", "Off", "Ctrl+Click", "Right Click"));
	/** Number of stored lines. 100 is the vanilla history. */
	public final SliderSetting history = add(new SliderSetting("History Size", 100, 100, 2000, 50));
	/** Percent of the vanilla GUI scale rules; 0 keeps the vanilla chat scale option. */
	public final SliderSetting scale = add(new SliderSetting("Scale % (0 = vanilla)", 0, 0, 200, 5));
	/** Pixel width of the chat; 0 keeps the vanilla chat width option. */
	public final SliderSetting width = add(new SliderSetting("Width px (0 = vanilla)", 0, 0, 600, 10));
	public final ToggleSetting customSpacing = add(new ToggleSetting("Custom Line Spacing", false));
	public final SliderSetting spacing = add(new SliderSetting("Line Spacing %", 0, 0, 100, 5));
	/** Percent; 0 keeps the vanilla chat opacity option. */
	public final SliderSetting textOpacity = add(new SliderSetting("Text Opacity % (0 = vanilla)", 0, 0, 100, 5));
	public final ChoiceSetting background = add(new ChoiceSetting("Background", "Vanilla", "Vanilla", "None", "Custom"));
	public final SliderSetting backgroundOpacity = add(new SliderSetting("Background Opacity %", 50, 0, 100, 5));

	private static final DateTimeFormatter F24 = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
	private static final DateTimeFormatter F24S = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH);
	private static final DateTimeFormatter F12 = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
	private static final DateTimeFormatter F12S = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH);
	private static final Pattern STAMP = Pattern.compile("^\\[\\d{1,2}:\\d{2}(:\\d{2})?( [AP]M)?] ");
	/** Object replacement character that vanilla uses as the glyph of a player head component. */
	private static final int OBJECT_CHAR = 0xFFFC;
	/** Repeats further apart than this many milliseconds start a new line. */
	private static final long REPEAT_WINDOW_MS = 60_000L;

	private final Map<UUID, Component> headCache = new HashMap<>();
	private String lastRaw = "";
	private Component lastBase;
	private int repeats;
	private long lastAt;
	private boolean removePrevious;

	public ChatPlus() {
		super("Chat Plus", "Timestamps, player heads, compact repeats, name highlight, copy on click and chat layout.", Category.MISC);
		instance = this;
	}

	/** The enabled module, or null so callers can fall back to vanilla in one check. */
	public static ChatPlus active() {
		ChatPlus c = instance;
		return c != null && c.isEnabled() ? c : null;
	}

	@Override
	protected void onDisable() {
		headCache.clear();
		lastRaw = "";
		lastBase = null;
		repeats = 0;
		removePrevious = false;
	}

	/* ---------------------------------------------------------------- incoming messages */

	/** True once after {@link #process} decided the newest stored message must be replaced by this one. */
	public boolean takeRemovePrevious() {
		boolean r = removePrevious;
		removePrevious = false;
		return r;
	}

	/** Rewrites an incoming chat component. Never throws; on any problem the original is returned. */
	public Component process(Component in) {
		try {
			return rewrite(in);
		} catch (RuntimeException e) {
			return in;
		}
	}

	private Component rewrite(Component in) {
		String raw = in.getString();
		if (raw.isEmpty()) return in;
		long now = System.currentTimeMillis();
		Component body = in;
		removePrevious = false;

		if (compact.on()) {
			if (lastBase != null && raw.equals(lastRaw) && now - lastAt < REPEAT_WINDOW_MS) {
				repeats++;
				removePrevious = true;
			} else {
				repeats = 1;
				lastRaw = raw;
				lastBase = in;
			}
			lastAt = now;
			if (repeats > 1) {
				body = Component.empty().append(lastBase)
						.append(Component.literal(" (x" + repeats + ")").withStyle(s -> s.withColor(0x8B93A0)));
			}
		}

		String me = mc.player == null ? "" : mc.player.getName().getString();
		if (highlight.on() && !me.isEmpty()) body = highlightName(body, raw, me);

		MutableComponent out = null;
		if (timestamps.on()) {
			out = Component.empty().append(Component.literal("[" + time() + "] ")
					.withStyle(s -> s.withColor(timeColor.argb() & 0xFFFFFF)));
		}
		if (heads.on()) {
			Component head = headFor(raw);
			if (head != null) {
				if (out == null) out = Component.empty();
				out.append(head);
			}
		}
		if (out != null) body = out.append(body);

		if (!sound.is("Off") && !removePrevious) playSound(raw, me);
		return body;
	}

	private String time() {
		LocalTime t = LocalTime.now();
		boolean s = seconds.on();
		return (timeFormat.is("12 Hour") ? (s ? F12S : F12) : (s ? F24S : F24)).format(t);
	}

	/** Colours every occurrence of the player's name; keeps the styles (click, hover, font) of each text run. */
	private Component highlightName(Component c, String raw, String me) {
		if (!raw.toLowerCase(Locale.ROOT).contains(me.toLowerCase(Locale.ROOT))) return c;
		MutableComponent out = Component.empty();
		int color = highlightColor.argb() & 0xFFFFFF;
		String lowerMe = me.toLowerCase(Locale.ROOT);
		c.<Object>visit((style, text) -> {
			String lower = text.toLowerCase(Locale.ROOT);
			int from = 0;
			int at;
			while ((at = lower.indexOf(lowerMe, from)) >= 0) {
				if (at > from) out.append(Component.literal(text.substring(from, at)).setStyle(style));
				out.append(Component.literal(text.substring(at, at + me.length())).setStyle(style.withColor(color).withBold(true)));
				from = at + me.length();
			}
			if (from < text.length()) out.append(Component.literal(from == 0 ? text : text.substring(from)).setStyle(style));
			return Optional.empty();
		}, Style.EMPTY);
		return out;
	}

	/** Head component of the player named earliest in the first 40 characters of the line, or null. */
	private Component headFor(String raw) {
		if (mc.getConnection() == null) return null;
		int limit = Math.min(raw.length(), 40);
		PlayerInfo best = null;
		int bestAt = Integer.MAX_VALUE;
		for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
			String name = info.getProfile().name();
			if (name == null || name.isEmpty()) continue;
			int at = indexOfWord(raw, name, limit);
			if (at < 0 || at > bestAt) continue;
			if (at == bestAt && best != null && best.getProfile().name().length() >= name.length()) continue;
			best = info;
			bestAt = at;
		}
		if (best == null) return null;
		UUID id = best.getProfile().id();
		Component head = headCache.get(id);
		if (head == null) {
			GameProfile gp = best.getProfile();
			head = Component.empty().append(Component.object(new PlayerSprite(ResolvableProfile.createResolved(gp), true)))
					.append(Component.literal(" "));
			if (headCache.size() > 256) headCache.clear();
			headCache.put(id, head);
		}
		return head;
	}

	/** Index of name as a whole word that starts before limit, or -1. */
	private static int indexOfWord(String text, String name, int limit) {
		int from = 0;
		while (true) {
			int at = text.indexOf(name, from);
			if (at < 0 || at >= limit) return -1;
			boolean before = at == 0 || !isNameChar(text.charAt(at - 1));
			int end = at + name.length();
			boolean after = end >= text.length() || !isNameChar(text.charAt(end));
			if (before && after) return at;
			from = at + 1;
		}
	}

	private static boolean isNameChar(char c) {
		return Character.isLetterOrDigit(c) || c == '_';
	}

	private void playSound(String raw, String me) {
		if (mc.getSoundManager() == null) return;
		boolean play;
		if (sound.is("All Messages")) {
			play = true;
		} else {
			int at = me.isEmpty() ? -1 : raw.indexOf(me);
			play = at > 12 || (at >= 0 && raw.indexOf(me, at + me.length()) >= 0);
		}
		if (play) mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME, 1.4f));
	}

	/* ---------------------------------------------------------------- copy on click */

	/** Handles a click in the chat screen. True when the click copied a message and must not go on. */
	public static boolean tryCopy(MouseButtonEvent e) {
		ChatPlus p = active();
		if (p == null || mc.gui == null) return false;
		boolean hit = (p.copy.is("Ctrl+Click") && e.button() == 0 && e.hasControlDown())
				|| (p.copy.is("Right Click") && e.button() == 1);
		if (!hit || !(mc.gui.getChat() instanceof ChatTextLookup lookup)) return false;
		String text = lookup.glacier$textAt(e.x(), e.y());
		if (text == null || text.isBlank()) return false;
		if (p.timestamps.on()) text = STAMP.matcher(text).replaceFirst("");
		mc.keyboardHandler.setClipboard(text);
		mc.gui.setOverlayMessage(Component.literal("Copied chat message"), false);
		mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2f));
		return true;
	}

	/** Strips the object replacement character that player heads leave in the visible text. */
	public static boolean copyable(int codePoint) {
		return codePoint != OBJECT_CHAR;
	}

	/* ---------------------------------------------------------------- layout values read by the mixins */

	public static int historyOr(int vanilla) {
		ChatPlus p = active();
		return p == null ? vanilla : p.history.i();
	}

	public static double scaleOr(double vanilla) {
		ChatPlus p = active();
		return p == null || p.scale.i() <= 0 ? vanilla : p.scale.get() / 100.0;
	}

	public static int widthOr(int vanilla) {
		ChatPlus p = active();
		return p == null || p.width.i() <= 0 ? vanilla : p.width.i();
	}

	/** Chat line spacing option value (0..1) or null to keep the vanilla option. */
	public static Double spacingOrNull() {
		ChatPlus p = active();
		return p == null || !p.customSpacing.on() ? null : p.spacing.get() / 100.0;
	}

	/** Chat opacity option value (0..1) or null to keep the vanilla option. */
	public static Double textOpacityOrNull() {
		ChatPlus p = active();
		return p == null || p.textOpacity.i() <= 0 ? null : p.textOpacity.get() / 100.0;
	}

	/** Text background opacity option value (0..1) or null to keep the vanilla option. */
	public static Double backgroundOrNull() {
		ChatPlus p = active();
		if (p == null || p.background.is("Vanilla")) return null;
		return p.background.is("None") ? 0.0 : p.backgroundOpacity.get() / 100.0;
	}
}
