package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Draw;
import net.glacier.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Player Tab List upgrade (Bedrock pack module "playerlist" and NRC tab editor): sorting, rows per column (which
 * decides the column count), the player limit, ping as coloured bars or a number, hiding the skin faces and a Glacier
 * or custom opacity panel. The hooks are in {@code mixin.extrac.ExtraCTabList}; this class holds settings and the
 * small drawing helpers they call. Vanilla values are returned whenever the module is off.
 */
public class PlayerTabList extends Module {
	private static PlayerTabList instance;

	public final ChoiceSetting style = add(new ChoiceSetting("Style", "Glacier", "Vanilla", "Glacier"));
	public final ChoiceSetting background = add(new ChoiceSetting("Background", "Vanilla", "Vanilla", "None", "Custom"));
	public final SliderSetting opacity = add(new SliderSetting("Background Opacity %", 50, 0, 100, 5));
	public final ChoiceSetting sort = add(new ChoiceSetting("Sort", "Vanilla", "Vanilla", "Name", "Ping", "Game Mode"));
	public final ChoiceSetting pingStyle = add(new ChoiceSetting("Ping Style", "Colored Bars", "Vanilla", "Colored Bars", "Number", "Hidden"));
	/** A column holds this many rows before another column starts. Vanilla is 20. */
	public final SliderSetting rows = add(new SliderSetting("Rows Per Column", 20, 8, 40, 1));
	/** Vanilla shows at most 80 players. */
	public final SliderSetting maxPlayers = add(new SliderSetting("Max Players", 80, 20, 200, 10));
	public final ToggleSetting hideFaces = add(new ToggleSetting("Hide Player Icons", false));

	private static final int PANEL = 0x80000000;
	private static final int UNLIT = 0x40FFFFFF;
	private static final Comparator<PlayerInfo> BY_NAME = Comparator.comparing(i -> i.getProfile().name().toLowerCase(Locale.ROOT));
	private static final Comparator<PlayerInfo> BY_PING = Comparator.<PlayerInfo>comparingInt(i -> i.getLatency()).thenComparing(BY_NAME);
	private static final Comparator<PlayerInfo> BY_MODE = Comparator.<PlayerInfo>comparingInt(i -> i.getGameMode() == GameType.SPECTATOR ? 1 : 0).thenComparing(BY_NAME);
	private static final String[] NUMBERS = new String[1000];

	private final List<PlayerInfo> cache = new ArrayList<>(96);
	private int cacheTick = -1;
	private int cacheSize = -1;

	public PlayerTabList() {
		super("Player Tab List", "Sorting, columns, ping style and a Glacier look for the player list.", Category.RENDER);
		instance = this;
	}

	/** The enabled module, or null to use vanilla. */
	public static PlayerTabList active() {
		PlayerTabList t = instance;
		return t != null && t.isEnabled() ? t : null;
	}

	public static int rowsOr(int vanilla) {
		PlayerTabList t = active();
		return t == null ? vanilla : t.rows.i();
	}

	public static long limitOr(long vanilla) {
		PlayerTabList t = active();
		return t == null ? vanilla : t.maxPlayers.i();
	}

	/** Reorders the vanilla list. Rebuilt at most once per game tick; vanilla builds a fresh list every frame anyway. */
	public static List<PlayerInfo> sorted(List<PlayerInfo> vanilla) {
		PlayerTabList t = active();
		if (t == null || t.sort.is("Vanilla") || vanilla.size() < 2) return vanilla;
		int tick = mc.gui.hud == null ? 0 : mc.gui.hud.getGuiTicks();
		if (tick != t.cacheTick || vanilla.size() != t.cacheSize) {
			t.cache.clear();
			t.cache.addAll(vanilla);
			t.cache.sort(t.sort.is("Name") ? BY_NAME : t.sort.is("Ping") ? BY_PING : BY_MODE);
			t.cacheTick = tick;
			t.cacheSize = vanilla.size();
		}
		return t.cache;
	}

	@Override
	protected void onDisable() {
		cache.clear();
		cacheTick = -1;
		cacheSize = -1;
	}

	/**
	 * Replacement for the fill calls of PlayerTabOverlay.render. The panel, header and footer use 0x80000000; every
	 * other fill is a player cell. Draws everything the same way as vanilla when no option changes it.
	 */
	public static void fill(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
		PlayerTabList t = active();
		if (t == null) {
			g.fill(x0, y0, x1, y1, color);
			return;
		}
		if (t.background.is("None")) return;
		boolean panel = color == PANEL;
		boolean custom = t.background.is("Custom");
		float a = t.opacity.f() / 100f;
		boolean glacier = t.style.is("Glacier");
		if (panel) {
			int rgb = glacier ? Theme.GLACIER_PANEL : 0xFF000000;
			int argb = custom ? Draw.alpha(rgb, a) : glacier ? Draw.alpha(rgb, 0.78f) : color;
			if (glacier) Draw.round(g, x0, y0, x1 - x0, y1 - y0, 6, argb);
			else g.fill(x0, y0, x1, y1, argb);
		} else {
			int argb = custom ? Draw.alpha(color, Math.min(1f, a * 2f)) : color;
			g.fill(x0, y0, x1, y1, argb);
		}
	}

	/** Draws the ping of a player in the slot vanilla uses for its icon. Returns false to let vanilla draw it. */
	public static boolean drawPing(GuiGraphicsExtractor g, int cellWidth, int x, int y, PlayerInfo info) {
		PlayerTabList t = active();
		if (t == null || t.pingStyle.is("Vanilla")) return false;
		if (t.pingStyle.is("Hidden")) return true;
		int ping = info.getLatency();
		int left = x + cellWidth - 11;
		if (t.pingStyle.is("Number")) {
			String s = ping < 0 ? "?" : ping >= 999 ? "999+" : number(ping);
			int w = mc.font.width(s);
			g.text(mc.font, s, x + cellWidth - 1 - w, y, color(ping), true);
			return true;
		}
		int lit = ping < 0 ? 0 : ping < 80 ? 5 : ping < 150 ? 4 : ping < 300 ? 3 : ping < 600 ? 2 : 1;
		int c = color(ping);
		for (int i = 0; i < 5; i++) {
			int h = 2 + i;
			g.fill(left + i * 2, y + 8 - h, left + i * 2 + 1, y + 8, i < lit ? c : UNLIT);
		}
		return true;
	}

	private static String number(int ping) {
		String s = NUMBERS[ping];
		if (s == null) NUMBERS[ping] = s = Integer.toString(ping);
		return s;
	}

	private static int color(int ping) {
		if (ping < 0) return 0xFFAAAAAA;
		if (ping < 80) return Theme.HUD_GOOD;
		if (ping < 150) return Theme.HUD_WARN;
		return Theme.HUD_BAD;
	}
}
