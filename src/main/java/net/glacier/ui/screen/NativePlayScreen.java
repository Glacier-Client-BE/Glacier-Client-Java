package net.glacier.ui.screen;

import net.glacier.GlacierClient;
import net.glacier.ui.Draw;
import net.glacier.web.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * The play screen (play.html / play.css) as Java widgets. Everything is laid out in css px and drawn through a pose
 * scale chosen by the same rule as WebPage (design height 540, snapped to 0.25 steps), so sizes match the page 1:1.
 * Worlds open the vanilla world browser or play the chosen world, servers join through the vanilla ConnectScreen.
 */
public final class NativePlayScreen extends Screen {
	/** Worlds still loading after this long show the failure text, as the web page did after 10 s. */
	private static final long WORLD_WAIT_NS = 10_000_000_000L;

	private static final int WINDOW = 0xF5232728, EDGE = 0xFF33373A, HEADER = 0xFF2A2D31;
	private static final int CARD = 0xFF2C2F33, CARD_EDGE = 0xFF393C41, ACCENT = 0xFF7289DA;
	private static final int CARD_HOVER = 0xFF33373C, CARD_HOVER_EDGE = 0xFF484C52, ACCENT_HOVER = 0xFF8296E0;
	private static final int MUTED = 0xFFB9BBBE, TEXT = 0xFFFFFFFF;
	private static final int ROW_H = 46, ROW_GAP = 6, WORLD_H = 66;
	private static final java.util.Map<String, Identifier> ICONS = new java.util.HashMap<>();
	private static final Identifier UNKNOWN_ICON = Identifier.withDefaultNamespace("textures/misc/unknown_server.png");
	private static final float DESIGN_H = 540f, MIN_W = 480f, STEP = 0.25f;
	private static final float SCROLL_STEP = 20f;
	private static final float NOTE_H = 16.25f;
	private static final String FA = RenderUtil.FA_SOLID;
	private static final String[] TABS = {"Worlds", "Friends", "Servers"};
	private static final int[] TAB_ICONS = {0xf0ac, 0xf0c0, 0xf233};
	private static final int TAB_WORLDS = 0, TAB_FRIENDS = 1, TAB_SERVERS = 2;
	private static final int ICON_BACK = 0xf060, ICON_SETTINGS = 0xf013, ICON_SEARCH = 0xf002, ICON_EDIT = 0xf303;

	/** A clickable rectangle in css px. */
	private record Hit(float x, float y, float w, float h, boolean inList, Runnable click) {
		boolean in(double mx, double my) {
			return mx >= x && my >= y && mx < x + w && my < y + h;
		}
	}

	private final Screen parent;
	private final List<Hit> hits = new ArrayList<>();
	private final List<ServerData> servers = new ArrayList<>();
	/** Loaded world summaries, newest first. Null until the async load finishes; written from another thread. */
	private volatile List<LevelSummary> worlds;
	private boolean worldsRequested;
	private long worldsRequestedAt;

	private int tab;
	private String query = "";
	private float worldScroll;
	private float serverScroll;
	/** Page scale and css viewport of the current frame. */
	private float scale = 1f;
	private int vw, vh;
	/** Viewport and content height of the list drawn this frame (css px). */
	private float listX, listY, listW, listH, listContentH;
	private int rowH = ROW_H;
	private int iconBudget;

	public NativePlayScreen(Screen parent) {
		super(Component.literal("Play"));
		this.parent = parent;
		reloadServers();
	}

	/** Shows the play screen on top of parent; closing it returns to parent. */
	public static void open(Screen parent) {
		Minecraft.getInstance().setScreen(new NativePlayScreen(parent));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
	}

	/** Same rule as WebPage.fit. */
	static float scaleFor(int w, int h) {
		float s = Math.min(h / DESIGN_H, w / MIN_W);
		return s < 0.5f ? Math.max(0.1f, s) : (float) Math.floor(s / STEP) * STEP;
	}

	private void fit() {
		scale = scaleFor(width, height);
		vw = (int) Math.ceil(width / scale);
		vh = (int) Math.ceil(height / scale);
	}

	// ------------------------------------------------------------------ render

	@Override
	public void render(GuiGraphics g, int mxs, int mys, float partial) {
		hits.clear();
		listX = listY = listW = listH = listContentH = 0;
		if (!worldsRequested) requestWorlds();
		WebPage.drawPanorama(g, width, height);
		fit();
		int mx = Math.round(mxs / scale), my = Math.round(mys / scale);

		var pose = g.pose();
		pose.pushMatrix();
		pose.scale(scale, scale);

		// .pl-window: left/right 12, top/bottom 8, 2 px border, radius 14, clips its children
		int wx = 12, wy = 8, ww = vw - 24, wh = vh - 16;
		Draw.round(g, wx, wy, ww, wh, 14, WINDOW);
		int x0 = wx + 2 + 12, x1 = wx + ww - 2 - 12;

		drawHeader(g, mx, my, wx + 2, wy + 2, ww - 4, x0, x1);
		drawTabs(g, mx, my, x0, x1);

		int bodyY = wy + 2 + 48 + 44 + 6;
		int bodyBottom = wy + wh - 2 - 12;
		int bw = x1 - x0, bh = bodyBottom - bodyY;
		switch (tab) {
			case TAB_WORLDS -> drawWorlds(g, mx, my, x0, bodyY, bw, bh);
			case TAB_FRIENDS -> drawFriends(g, mx, my, x0, bodyY, bw);
			default -> drawServers(g, mx, my, x0, bodyY, bw, bh);
		}
		Draw.outline(g, wx, wy, ww, wh, 14, EDGE, 2);
		pose.popMatrix();
	}

	private void drawHeader(GuiGraphics g, int mx, int my, int ix, int iy, int iw, int x0, int x1) {
		// 48 px tall incl. a 2 px bottom border; its top corners follow the window's inner radius (12)
		Draw.round(g, ix, iy, iw, 48, 12, HEADER);
		g.fill(ix, iy + 24, ix + iw, iy + 46, HEADER);
		g.fill(ix, iy + 46, ix + iw, iy + 48, EDGE);
		float mid = iy + 23;

		iconBox(g, mx, my, x0, (int) mid - 16, ICON_BACK, this::back);
		text(g, "Play", x0 + 32 + 10, mid, 20, true, TEXT);

		int settingsX = x1 - 32;
		iconBox(g, mx, my, settingsX, (int) mid - 16, ICON_SETTINGS, this::openMods);

		// .pl-search: 240x30, 2 px border, padding 0 10, gap 8
		int sx = settingsX - 10 - 240, sy = (int) mid - 15;
		Draw.round(g, sx, sy, 240, 30, 9, CARD);
		Draw.outline(g, sx, sy, 240, 30, 9, CARD_EDGE, 2);
		float ix0 = sx + 12;
		icon(g, ICON_SEARCH, ix0, mid, 12, MUTED);
		float tx = ix0 + RenderUtil.textWidth(new String(Character.toChars(ICON_SEARCH)), false, 12, FA) + 8;
		float maxW = sx + 240 - 12 - tx;
		if (query.isEmpty()) text(g, "Search Worlds...", tx, mid, 13, false, MUTED);
		else text(g, RenderUtil.ellipsize(query, maxW, false, 13, null), tx, mid, 13, false, TEXT);
	}

	private void drawTabs(GuiGraphics g, int mx, int my, int x0, int x1) {
		// .pl-tabs: padding 8 12, gap 8; flex-grow 1 on content-sized (auto basis) tabs
		float[] basis = new float[3];
		float sum = 0;
		for (int i = 0; i < 3; i++) {
			basis[i] = tabContentW(i) + 4;
			sum += basis[i];
		}
		float free = (x1 - x0) - 16 - sum;
		float x = x0;
		int th = 28, ty = 8 + 2 + 48 + 8;
		for (int i = 0; i < 3; i++) {
			final int idx = i;
			float fw = basis[i] + free / 3f;
			int ix = Math.round(x), iw = Math.round(x + fw) - ix;
			boolean on = i == tab;
			boolean hover = inside(mx, my, ix, ty, iw, th);
			int fill = on ? (hover ? ACCENT_HOVER : ACCENT) : (hover ? CARD_HOVER : CARD);
			Draw.round(g, ix, ty, iw, th, 14, fill);
			Draw.outline(g, ix, ty, iw, th, 14, on ? ACCENT : CARD_EDGE, 2);
			float lx = ix + (iw - tabContentW(i)) / 2f;
			float iconW = RenderUtil.textWidth(new String(Character.toChars(TAB_ICONS[i])), false, 13, FA);
			icon(g, TAB_ICONS[i], lx, ty + th / 2f, 13, TEXT);
			text(g, TABS[i], lx + iconW + 6, ty + th / 2f, 13, false, TEXT);
			hits.add(new Hit(ix, ty, iw, th, false, () -> selectTab(idx)));
			x += fw + 8;
		}
	}

	private static float tabContentW(int i) {
		return RenderUtil.textWidth(new String(Character.toChars(TAB_ICONS[i])), false, 13, FA) + 6
				+ RenderUtil.textWidth(TABS[i], false, 13, null);
	}

	// ----------------------------------------------------------------- worlds

	private void drawWorlds(GuiGraphics g, int mx, int my, int bx, int by, int bw, int bh) {
		iconBudget = 1;
		int third = (bw - 20) / 3;
		button(g, mx, my, bx, by, third, 34, 10, 14, "Create New World", true, () -> run(() -> {
			Minecraft mc = Minecraft.getInstance();
			CreateWorldScreen.openFresh(mc, () -> mc.setScreen(this));
		}));
		button(g, mx, my, bx + third + 10, by, third, 34, 10, 14, "Quick Play", false, this::quickPlay);
		button(g, mx, my, bx + 2 * (third + 10), by, bw - 2 * (third + 10), 34, 10, 14, "Browse Worlds", false,
				() -> show(() -> new SelectWorldScreen(this)));

		List<LevelSummary> all = worlds == null ? List.of() : worlds;
		String q = query.toLowerCase();
		List<Integer> shown = new ArrayList<>();
		for (int i = 0; i < all.size(); i++) {
			if (q.isEmpty() || all.get(i).getLevelName().toLowerCase().contains(q)) shown.add(i);
		}
		boolean empty = shown.isEmpty();
		int listTop = by + 34 + 10;
		int listHeight = bh - 44 - (empty ? (int) Math.ceil(NOTE_H) + 10 : 0);
		rowH = WORLD_H;
		worldScroll = clampScroll(worldScroll, shown.size(), listHeight);
		beginList(bx, listTop, bw, listHeight);
		g.enableScissor(bx, listTop, bx + bw, listTop + listHeight);
		for (int n = 0; n < shown.size(); n++) {
			int y = listTop - (int) worldScroll + n * (WORLD_H + ROW_GAP);
			if (y + WORLD_H < listTop || y > listTop + listHeight) continue;
			int index = shown.get(n);
			LevelSummary s = all.get(index);
			boolean playable = !s.isDisabled() && !s.isLocked() && !s.requiresManualConversion();
			boolean hover = playable && inside(mx, my, bx, y, bw, WORLD_H) && inView(mx, my);
			worldRow(g, mx, my, bx, y, bw, s, playable, hover);
			if (playable) hits.add(new Hit(bx, y, bw, WORLD_H, true, () -> playWorld(index)));
			hits.add(new Hit(bx + bw - 44, y + (WORLD_H - 32) / 2f, 32, 32, true, () -> editWorld(s.getLevelId())));
		}
		g.disableScissor();

		if (empty) {
			String note = worlds != null ? (q.isEmpty() ? "No worlds yet. Create one to get started." : "No worlds match your search.")
					: System.nanoTime() - worldsRequestedAt > WORLD_WAIT_NS ? "Could not load worlds" : "Loading worlds...";
			text(g, note, bx, listTop + listHeight + 10 + NOTE_H / 2f, 13, false, MUTED);
		}
	}

	/** One world card: thumbnail, bold name, mode / flags line, version and last played, and an edit button. */
	private void worldRow(GuiGraphics g, int mx, int my, int x, int y, int w, LevelSummary s, boolean playable, boolean hover) {
		Draw.round(g, x, y, w, WORLD_H, 12, hover ? CARD_HOVER : CARD);
		Draw.outline(g, x, y, w, WORLD_H, 12, hover ? ACCENT : CARD_EDGE, 2);
		int ix = x + 8, iy = y + 8, is = WORLD_H - 16;
		Draw.round(g, ix - 1, iy - 1, is + 2, is + 2, 8, hover ? ACCENT : CARD_EDGE);
		RenderUtil.image(g, iconFor(s), ix, iy, ix + is, iy + is, playable ? 1f : 0.45f);

		float tx = ix + is + 12;
		float tw = w - (tx - x) - 56;
		text(g, RenderUtil.ellipsize(s.getLevelName(), tw, true, 15, null), tx, y + 18, 15, true, playable ? TEXT : MUTED);
		StringBuilder d = new StringBuilder(modeName(s));
		if (s.isHardcore()) d.append(" · Hardcore");
		if (s.hasCommands()) d.append(" · Cheats");
		if (s.isExperimental()) d.append(" · Experimental");
		text(g, RenderUtil.ellipsize(d.toString(), tw, false, 12, null), tx, y + 35, 12, false, MUTED);
		String foot;
		if (s.isLocked()) foot = "Locked (open in another instance)";
		else if (s.isDisabled()) foot = "Cannot be played: " + s.getWorldVersionName().getString();
		else foot = s.getWorldVersionName().getString() + " · " + ago(s.getLastPlayed());
		text(g, RenderUtil.ellipsize(foot, tw, false, 11, null), tx, y + 50, 11, false, MUTED);

		int ex = x + w - 44, ey = y + (WORLD_H - 32) / 2;
		boolean eh = inside(mx, my, ex, ey, 32, 32) && inView(mx, my);
		Draw.round(g, ex, ey, 32, 32, 9, eh ? CARD_HOVER_EDGE : HEADER);
		Draw.outline(g, ex, ey, 32, 32, 9, CARD_EDGE, 2);
		float iw = RenderUtil.textWidth(new String(Character.toChars(ICON_EDIT)), false, 13, FA);
		icon(g, ICON_EDIT, ex + (32 - iw) / 2f, ey + 16, 13, TEXT);
	}

	private static String modeName(LevelSummary s) {
		String n = s.getGameMode().getName();
		return n.isEmpty() ? n : Character.toUpperCase(n.charAt(0)) + n.substring(1);
	}

	/** Plays the most recently played playable world. */
	private void quickPlay() {
		List<LevelSummary> all = worlds;
		if (all == null) return;
		for (int i = 0; i < all.size(); i++) {
			LevelSummary s = all.get(i);
			if (!s.isDisabled() && !s.isLocked() && !s.requiresManualConversion()) {
				playWorld(i);
				return;
			}
		}
	}

	/** Opens the vanilla edit screen for one world, then reloads the list. */
	private void editWorld(String levelId) {
		run(() -> {
			Minecraft mc = Minecraft.getInstance();
			try {
				var access = mc.getLevelSource().createAccess(levelId);
				mc.setScreen(EditWorldScreen.create(mc, access, ok -> {
					try {
						access.close();
					} catch (java.io.IOException ignored) {
					}
					worldsRequested = false;
					worlds = null;
					mc.setScreen(this);
				}));
			} catch (java.io.IOException e) {
				GlacierClient.LOGGER.warn("[ui] could not open the world editor", e);
			}
		});
	}

	/** World thumbnail, loaded lazily (one per frame) from the world's icon.png and cached for the session. */
	private Identifier iconFor(LevelSummary s) {
		Identifier id = ICONS.get(s.getLevelId());
		if (id != null) return id;
		if (iconBudget <= 0) return UNKNOWN_ICON;
		iconBudget--;
		Identifier made = UNKNOWN_ICON;
		try {
			java.nio.file.Path p = s.getIcon();
			if (p != null && java.nio.file.Files.isRegularFile(p)) {
				try (java.io.InputStream in = java.nio.file.Files.newInputStream(p)) {
					NativeImage img = NativeImage.read(in);
					Identifier nid = Identifier.fromNamespaceAndPath("glacier", "world_icon/" + Integer.toHexString(s.getLevelId().hashCode()));
					Minecraft.getInstance().getTextureManager().register(nid, new DynamicTexture(() -> "glacier world icon", img));
					RenderUtil.putImageSize(nid, img.getWidth(), img.getHeight());
					made = nid;
				}
			}
		} catch (java.io.IOException | RuntimeException e) {
			GlacierClient.LOGGER.debug("[ui] no icon for world {}", s.getLevelId());
		}
		ICONS.put(s.getLevelId(), made);
		return made;
	}

	private void playWorld(int index) {
		run(() -> {
			Minecraft mc = Minecraft.getInstance();
			List<LevelSummary> all = worlds;
			if (all == null || index < 0 || index >= all.size()) return;
			mc.createWorldOpenFlows().openWorld(all.get(index).getLevelId(), () -> mc.setScreen(this));
		});
	}

	/** Starts the async world-summary load on first use, as ScreenApi.worlds() did. */
	private void requestWorlds() {
		worldsRequested = true;
		worldsRequestedAt = System.nanoTime();
		try {
			var source = Minecraft.getInstance().getLevelSource();
			source.loadLevelSummaries(source.findLevelCandidates()).thenAccept(list -> {
				List<LevelSummary> sorted = new ArrayList<>(list);
				Collections.sort(sorted);
				worlds = sorted;
			}).exceptionally(e -> {
				GlacierClient.LOGGER.warn("[ui] could not load worlds", e);
				worlds = new ArrayList<>();
				return null;
			});
		} catch (Exception e) {
			GlacierClient.LOGGER.warn("[ui] could not list worlds", e);
			worlds = new ArrayList<>();
		}
	}

	// ---------------------------------------------------------------- friends

	private void drawFriends(GuiGraphics g, int mx, int my, int bx, int by, int bw) {
		int half = (bw - 10) / 2;
		button(g, mx, my, bx, by, half, 34, 10, 14, "Add Friend", true, () -> show(() -> new JoinMultiplayerScreen(this)));
		button(g, mx, my, bx + half + 10, by, bw - half - 10, 34, 10, 14, "Join by Code", false,
				() -> show(() -> new JoinMultiplayerScreen(this)));
		text(g, "No friends to show yet.", bx, by + 44 + NOTE_H / 2f, 13, false, MUTED);
	}

	// ---------------------------------------------------------------- servers

	private void drawServers(GuiGraphics g, int mx, int my, int bx, int by, int bw, int bh) {
		text(g, "Servers", bx, by + 17, 15, true, TEXT);
		button(g, mx, my, bx + bw - 150, by + 3, 150, 28, 10, 13, "Add Server", false,
				() -> show(() -> new JoinMultiplayerScreen(this)));

		String q = query.toLowerCase();
		List<Integer> shown = new ArrayList<>();
		for (int i = 0; i < servers.size(); i++) {
			ServerData d = servers.get(i);
			if (q.isEmpty() || nameOf(d).toLowerCase().contains(q) || ipOf(d).toLowerCase().contains(q)) shown.add(i);
		}
		boolean empty = shown.isEmpty();
		int listTop = by + 34 + 10;
		int listHeight = bh - 44 - (empty ? (int) Math.ceil(NOTE_H) + 10 : 0);
		rowH = ROW_H;
		serverScroll = clampScroll(serverScroll, shown.size(), listHeight);
		beginList(bx, listTop, bw, listHeight);
		g.enableScissor(bx, listTop, bx + bw, listTop + listHeight);
		for (int n = 0; n < shown.size(); n++) {
			int y = listTop - (int) serverScroll + n * (ROW_H + ROW_GAP);
			if (y + ROW_H < listTop || y > listTop + listHeight) continue;
			int index = shown.get(n);
			ServerData d = servers.get(index);
			boolean hover = inside(mx, my, bx, y, bw, ROW_H) && inView(mx, my);
			row(g, bx, y, bw, nameOf(d), ipOf(d), hover);
			hits.add(new Hit(bx, y, bw, ROW_H, true, () -> joinServer(index)));
		}
		g.disableScissor();

		if (empty) text(g, "No saved servers.", bx, listTop + listHeight + 10 + NOTE_H / 2f, 13, false, MUTED);
	}

	private void joinServer(int index) {
		run(() -> {
			if (index < 0 || index >= servers.size()) return;
			Minecraft mc = Minecraft.getInstance();
			ServerData data = servers.get(index);
			ConnectScreen.startConnecting(this, mc, ServerAddress.parseString(data.ip), data, false, null);
		});
	}

	/** Reads the saved server list, as ScreenApi.servers() did. */
	private void reloadServers() {
		try {
			ServerList list = new ServerList(Minecraft.getInstance());
			list.load();
			for (int i = 0; i < list.size(); i++) servers.add(list.get(i));
		} catch (RuntimeException e) {
			GlacierClient.LOGGER.warn("[ui] could not read the server list", e);
		}
	}

	// ----------------------------------------------------------------- helpers

	/** Text at css font size px, vertically centred on cy (the engine centres a 1.25 line box on the glyph box). */
	private static void text(GuiGraphics g, String s, float x, float cy, float px, boolean bold, int argb) {
		RenderUtil.drawText(g, s, x, cy - px / 2f, px, bold, null, argb);
	}

	private static void icon(GuiGraphics g, int cp, float x, float cy, float px, int argb) {
		RenderUtil.drawText(g, new String(Character.toChars(cp)), x, cy - px / 2f, px, false, FA, argb);
	}

	private void beginList(int x, int y, int w, int h) {
		listX = x;
		listY = y;
		listW = w;
		listH = h;
	}

	private float clampScroll(float scroll, int count, int viewH) {
		listContentH = count == 0 ? 0 : count * (rowH + ROW_GAP) - ROW_GAP;
		return Math.max(0, Math.min(scroll, Math.max(0, listContentH - viewH)));
	}

	private boolean inView(double mx, double my) {
		return mx >= listX && mx < listX + listW && my >= listY && my < listY + listH;
	}

	/** .pl-item: 46 px card, 2 px border, radius 10, padding 0 12, bold name over a muted 12 px line. */
	private static void row(GuiGraphics g, int x, int y, int w, String name, String detail, boolean hover) {
		Draw.round(g, x, y, w, ROW_H, 10, hover ? CARD_HOVER : CARD);
		Draw.outline(g, x, y, w, ROW_H, 10, hover ? CARD_HOVER_EDGE : CARD_EDGE, 2);
		float tw = w - 28;
		float tx = x + 14;
		text(g, RenderUtil.ellipsize(name, tw, true, 14, null), tx, y + 15.5f, 14, true, TEXT);
		text(g, RenderUtil.ellipsize(detail, tw, false, 12, null), tx, y + 31.75f, 12, false, MUTED);
	}

	/** .pl-btn: accent fill, or card with a hover fill (the border stays). */
	private void button(GuiGraphics g, int mx, int my, int x, int y, int w, int h, int radius, float px, String label,
						boolean accent, Runnable action) {
		boolean hover = inside(mx, my, x, y, w, h);
		int fill = accent ? (hover ? ACCENT_HOVER : ACCENT) : (hover ? CARD_HOVER : CARD);
		Draw.round(g, x, y, w, h, radius, fill);
		Draw.outline(g, x, y, w, h, radius, accent ? ACCENT : CARD_EDGE, 2);
		float tw = RenderUtil.textWidth(label, false, px, null);
		text(g, label, x + (w - tw) / 2f, y + h / 2f, px, false, TEXT);
		hits.add(new Hit(x, y, w, h, false, action));
	}

	/** .pl-sq: 32 px square icon button, radius 9, 14 px glyph. */
	private void iconBox(GuiGraphics g, int mx, int my, int x, int y, int codepoint, Runnable action) {
		boolean hover = inside(mx, my, x, y, 32, 32);
		Draw.round(g, x, y, 32, 32, 9, hover ? CARD_HOVER : CARD);
		Draw.outline(g, x, y, 32, 32, 9, hover ? CARD_HOVER_EDGE : CARD_EDGE, 2);
		float iw = RenderUtil.textWidth(new String(Character.toChars(codepoint)), false, 14, FA);
		icon(g, codepoint, x + (32 - iw) / 2f, y + 16, 14, TEXT);
		hits.add(new Hit(x, y, 32, 32, false, action));
	}

	private static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}

	private static String nameOf(ServerData d) {
		return d.name == null ? "" : d.name;
	}

	private static String ipOf(ServerData d) {
		return d.ip == null ? "" : d.ip;
	}

	private static String ago(long ms) {
		if (ms == 0) return "";
		long d = (System.currentTimeMillis() - ms) / 60_000L;
		if (d < 1) return "just now";
		if (d < 60) return d + " min ago";
		if (d < 1440) return d / 60 + " h ago";
		return d / 1440 + " d ago";
	}

	/** Runs an action and logs a failure instead of letting it break the frame. */
	private static void run(Runnable r) {
		try {
			r.run();
		} catch (RuntimeException e) {
			GlacierClient.LOGGER.error("[ui] screen action failed", e);
		}
	}

	/** Opens a screen built at click time. */
	private static void show(Supplier<Screen> screen) {
		run(() -> Minecraft.getInstance().setScreen(screen.get()));
	}

	private void back() {
		run(() -> Minecraft.getInstance().setScreen(parent));
	}

	private void openMods() {
		show(() -> NativeMenuScreen.open(null));
	}

	// ------------------------------------------------------------------- input

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
		double cx = e.x() / scale, cy = e.y() / scale;
		for (int i = hits.size() - 1; i >= 0; i--) {
			Hit h = hits.get(i);
			if (!h.in(cx, cy)) continue;
			if (h.inList() && !inView(cx, cy)) continue;
			h.click().run();
			return true;
		}
		return super.mouseClicked(e, dbl);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy) {
		float max = Math.max(0, listContentH - listH);
		if (tab == TAB_WORLDS) worldScroll = (float) Math.max(0, Math.min(worldScroll - sy * SCROLL_STEP, max));
		else if (tab == TAB_SERVERS) serverScroll = (float) Math.max(0, Math.min(serverScroll - sy * SCROLL_STEP, max));
		return true;
	}

	/** Esc goes back to the parent screen; Backspace deletes the last search character. */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) {
			setQuery(query.substring(0, query.length() - 1));
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
			back();
			return true;
		}
		return super.keyPressed(event);
	}

	/** Typed characters fill the search box. */
	@Override
	public boolean charTyped(CharacterEvent event) {
		if (event.isAllowedChatCharacter()) setQuery(query + event.codepointAsString());
		return true;
	}

	private void setQuery(String v) {
		query = v;
		worldScroll = 0;
		serverScroll = 0;
	}

	private void selectTab(int index) {
		tab = index;
	}
}
