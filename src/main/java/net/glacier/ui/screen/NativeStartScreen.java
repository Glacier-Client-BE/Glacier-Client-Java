package net.glacier.ui.screen;

import net.glacier.GlacierClient;
import net.glacier.ui.Draw;
import net.glacier.ui.Theme;
import net.glacier.web.RenderUtil;
import net.glacier.web.StartApi;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * The Glacier start screen (start.html / start.css / start.js) as plain Java widgets drawn on GuiGraphicsExtractor.
 * Layout is in the same css-px design units as the web page (design height 540, scaled to the window), so the
 * positions match it one to one. Immediate mode like {@link NativeMenuScreen}: each frame records the click regions.
 */
public class NativeStartScreen extends Screen {
	/** Design height in css px; the page scales with the window height, same rule as the web start screen. */
	private static final float DESIGN_H = 540f;
	private static final float MIN_W = 480f;
	private static final float STEP = 0.25f;
	private static final int KEY_ESCAPE = 256;
	/** The logo intro runs for this many seconds, then the UI is live. */
	private static final float INTRO_END = 2.6f;
	private static final long PAN_MS = 120_000L;
	private static final float NEWS_FRAME_S = 5f;

	private static final Identifier PANORAMA = id("textures/start/panorama.png");
	private static final Identifier LOGO = id("textures/start/logo.png");
	private static final Identifier WORDMARK = id("textures/start/wordmark.png");
	private static final Identifier[] NEWS = {
			id("textures/start/news_0.png"), id("textures/start/news_1.png"), id("textures/start/news_2.png"),
			id("textures/start/news_3.png"), id("textures/start/news_4.png"), id("textures/start/news_5.png")};
	private static final Identifier FA_BRANDS = Identifier.fromNamespaceAndPath("glacier", "fa_brands");

	/** Start-page colours from start.css. Where they differ from Theme they are kept here. */
	private static final int BG = 0xFF101214;
	private static final int BG_DIM = 0x59000000;
	private static final int VIGNETTE_STEPS = 8;
	private static final int VIGNETTE_MAX = 0x99;
	private static final float VIGNETTE_BAND = 0.2f;
	private static final int PANEL = 0xFF232728;
	private static final int PANEL_EDGE = 0xFF33373A;
	private static final int CARD = Theme.CARD;
	private static final int CARD_EDGE = Theme.CARD_BORDER;
	private static final int CARD_HOVER = 0xFF33373C;
	private static final int CARD_HOVER_EDGE = 0xFF484C52;
	private static final int CARD_BOX = 0xF0232728;
	private static final int WINDOW = 0xF5232728;
	private static final int DD_BG = 0xEB2C2F33;
	private static final int BADGE = 0xEB273833;
	private static final int BADGE_EDGE = 0xFF2E493F;
	private static final int ACCENT = Theme.ACCENT;
	private static final int ACCENT_HOVER = 0xFF8296E0;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int DIM = 0xFFB9BBBE;
	private static final int DISCORD = 0xFF7289DA;
	private static final int ROW_HOVER = 0x13FFFFFF;
	private static final int SCRIM = 0x80000000;

	private static final int[] TAB_ICONS = {0xF02D, 0xF19D, 0xF2B9};
	private static final String[] TAB_LABELS = {"Changelog", "Tutorials", "Credits"};
	private static final String[][][] TAB_TEXT = {CHANGELOG(), TUTORIALS(), CREDITS()};

	private final long startNs = System.nanoTime();
	private final String playerName = Minecraft.getInstance().getUser().getName();
	private final String version = SharedConstants.getCurrentVersion().name();

	private final List<Hit> hits = new ArrayList<>();
	private float scale = 1f;
	private int vw, vh;
	private float mxd, myd;
	private boolean introActive = true;
	private float uiOff, cardDy, cf = 1f, logoA, logoY, logoSize;
	private boolean ddOpen, infoOpen;
	/** Start layout from the pack: false is the card, true is the legacy centred button stack. Saved between launches. */
	private static boolean legacy = loadLegacy();
	private int infoTab;
	private float infoScroll, infoMax;

	private record Hit(int x, int y, int w, int h, boolean overlay, Runnable click) {
		boolean in(double mx, double my) {
			return mx >= x && my >= y && mx < x + w && my < y + h;
		}
	}

	public NativeStartScreen() {
		super(Component.literal("Glacier"));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	// ------------------------------------------------------------------ render

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partialTick) {
		hits.clear();
		scale = fitScale();
		vw = (int) Math.ceil(width / scale);
		vh = (int) Math.ceil(height / scale);
		mxd = mx / scale;
		myd = my / scale;
		intro((System.nanoTime() - startNs) / 1e9f);
		drawBackground(g);

		var pose = g.pose();
		pose.pushMatrix();
		pose.scale(scale, scale);
		pose.pushMatrix();
		pose.translate(0f, uiOff);
		if (legacy) drawLegacy(g);
		else drawCard(g);
		drawPlayer(g);
		drawTop(g);
		drawNews(g);
		drawFoot(g);
		pose.popMatrix();
		if (ddOpen) drawDropdown(g);
		if (introActive) drawLogo(g);
		if (infoOpen) drawInfo(g);
		pose.popMatrix();
	}

	/** Same fit rule as StartScreen: height-driven, stepped to quarters, never below 0.1. */
	private float fitScale() {
		float s = Math.min(height / DESIGN_H, width / MIN_W);
		return s < 0.5f ? Math.max(0.1f, s) : (float) Math.floor(s / STEP) * STEP;
	}

	private void drawBackground(GuiGraphicsExtractor g) {
		g.fill(0, 0, width, height, BG);
		int[] nat = RenderUtil.imageSize(PANORAMA);
		if (nat != null && width > 0 && height > 0) {
			float aspect = width / (float) height;
			boolean wide = nat[0] / (float) nat[1] >= aspect;
			float uw = wide ? nat[1] * aspect : nat[0];
			float uh = wide ? nat[1] : nat[0] / aspect;
			float phase = ((System.nanoTime() - startNs) / 1_000_000L % PAN_MS) / (float) PAN_MS;
			float u = (nat[0] - uw) * phase;
			float v = (nat[1] - uh) / 2f;
			RenderUtil.imageUv(g, PANORAMA, 0, 0, width, height, u, v, uw, uh, 1f);
		}
		g.fill(0, 0, width, height, BG_DIM);
		vignette(g);
	}

	/** Darkens the top and bottom edges in steps. The bands are disjoint, so each row is filled once. */
	private void vignette(GuiGraphicsExtractor g) {
		int band = Math.max(1, (int) (height * VIGNETTE_BAND) / VIGNETTE_STEPS);
		for (int i = 0; i < VIGNETTE_STEPS; i++) {
			int c = (VIGNETTE_MAX * (VIGNETTE_STEPS - i) / VIGNETTE_STEPS) << 24;
			int y = i * band;
			g.fill(0, y, width, y + band, c);
			g.fill(0, height - y - band, width, height - y, c);
		}
	}

	/** Centre card: 392x400 css px, stack of brand and five buttons, rising in after the logo intro. */
	private void drawCard(GuiGraphicsExtractor g) {
		int dy = Math.round(cardDy);
		int ox = (vw - 396) / 2;
		int oy = 24 + (vh - 24 - 404) / 2 + dy;
		shadow(g, ox, oy, 396, 404, 18);
		Draw.round(g, ox, oy, 396, 404, 18, fa(CARD_BOX));
		Draw.outline(g, ox, oy, 396, 404, 18, fa(PANEL_EDGE), 2);

		int sx = ox + 22, sy = oy + 22;
		surface(g, sx, sy, 352, 80, 14, false);
		Draw.round(g, sx + 6, sy + 6, 68, 68, 16, fa(PANEL));
		Draw.outline(g, sx + 6, sy + 6, 68, 68, 16, fa(PANEL_EDGE), 2);
		RenderUtil.image(g, LOGO, sx + 12, sy + 12, sx + 68, sy + 68, cf);
		RenderUtil.image(g, WORDMARK, sx + 102, sy + 9, sx + 326, sy + 71, cf);

		boolean hp = hov(sx, sy + 100, 352, 52);
		Draw.round(g, sx, sy + 100, 352, 52, 12, fa(hp ? ACCENT_HOVER : ACCENT));
		txtCenter(g, "Play Game", sx + 176, sy + 126, 18, fa(TEXT));
		hit(sx, sy + 100, 352, 52, false, () -> NativePlayScreen.open(this));

		button(g, "Options", sx, sy + 160, () -> open(ScreenFactory.options(this)));
		button(g, "Marketplace", sx, sy + 212, () -> open(NativeMenuScreen.open(null)));
		button(g, "Dressing Room", sx, sy + 264, () -> open(NativeMenuScreen.open("cosmetics")));

		boolean hi = infoOpen || hov(sx, sy + 316, 352, 44);
		surface(g, sx, sy + 316, 352, 44, 14, hi);
		txtCenter(g, "Information", sx + 176, sy + 338, 18, fa(TEXT));
		hit(sx, sy + 316, 352, 44, false, () -> infoOpen = true);
	}

	/**
	 * Legacy layout from the pack (glacier_start_panel_legacy): the logo above a centred stack of Play, Options and
	 * Dressing Room buttons, with Marketplace, Achievements and Information as square icon buttons along the bottom.
	 * Pack units are doubled to css px.
	 */
	private void drawLegacy(GuiGraphicsExtractor g) {
		int dy = Math.round(cardDy);
		int cx = vw / 2;
		int cy = vh / 2 + 14 + dy;

		float ls = 120f;
		RenderUtil.image(g, LOGO, cx - ls / 2f, cy - 130 - ls / 2f, cx + ls / 2f, cy - 130 + ls / 2f, cf);
		RenderUtil.image(g, WORDMARK, cx - 112, cy - 62 - 16, cx + 112, cy - 62 + 15, cf);

		legacyButton(g, "Play Game", cx - 200, cy - 20, () -> NativePlayScreen.open(this));
		legacyButton(g, "Options", cx - 200, cy + 22, () -> open(new OptionsScreen(this, Minecraft.getInstance().options)));
		legacyButton(g, "Dressing Room", cx - 200, cy + 64, () -> open(NativeMenuScreen.open("cosmetics")));

		int iy = vh - 48 - 44;
		legacyIcon(g, 0xF54F, cx - 24 - 50, iy, () -> open(NativeMenuScreen.open(null)));
		legacyIcon(g, 0xF091, cx - 24, iy, () -> open(NativeMenuScreen.open(null)));
		legacyIcon(g, 0xF0E0, cx - 24 + 50, iy, () -> infoOpen = true);
	}

	private void legacyButton(GuiGraphicsExtractor g, String label, int x, int y, Runnable action) {
		boolean h = hov(x, y, 400, 40);
		surface(g, x, y, 400, 40, 12, h);
		txtCenter(g, label, x + 200, y + 20, 18, fa(TEXT));
		hit(x, y, 400, 40, false, action);
	}

	private void legacyIcon(GuiGraphicsExtractor g, int cp, int x, int y, Runnable action) {
		boolean h = hov(x, y, 48, 48);
		surface(g, x, y, 48, 48, 14, h);
		iconC(g, cp, x + 24, y + 24, 20, fa(TEXT));
		hit(x, y, 48, 48, false, action);
	}

	private static java.nio.file.Path modeFile() {
		return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("glacier").resolve("start_mode.txt");
	}

	private static boolean loadLegacy() {
		try {
			return "legacy".equals(java.nio.file.Files.readString(modeFile()).trim());
		} catch (java.io.IOException | RuntimeException e) {
			return false;
		}
	}

	private static void setLegacy(boolean on) {
		legacy = on;
		try {
			java.nio.file.Files.createDirectories(modeFile().getParent());
			java.nio.file.Files.writeString(modeFile(), on ? "legacy" : "default");
		} catch (java.io.IOException | RuntimeException e) {
			GlacierClient.LOGGER.warn("[ui] could not save the start layout", e);
		}
	}


	/** Top-left player panel: 300x48 at (6,6). */
	private void drawPlayer(GuiGraphicsExtractor g) {
		surface(g, 6, 6, 56, 48, 14, false);
		iconC(g, 0xF007, 34, 30, 18, ACCENT);

		surface(g, 68, 6, 184, 48, 14, false);
		txt(g, "Logged in as:", 86, 12.5f, 11, TEXT);
		txt(g, fit(playerName, fs(18), 164), 86, 25.5f, 18, ACCENT);

		boolean hs = hov(258, 6, 48, 48);
		surface(g, 258, 6, 48, 48, 14, hs);
		iconC(g, 0xF007, 282, 30, 18, TEXT);
		hit(258, 6, 48, 48, false, () -> open(NativeMenuScreen.open("cosmetics")));
	}

	/** Top-right controls: achievements, inbox, version dropdown and quit, 304 css px wide, right edge at 4. */
	private void drawTop(GuiGraphicsExtractor g) {
		int s = vw - 308;

		boolean ha = hov(s, 4, 48, 48);
		surface(g, s, 4, 48, 48, 14, ha);
		iconC(g, 0xF559, s + 24, 28, 18, TEXT);
		hit(s, 4, 48, 48, false, () -> open(NativeMenuScreen.open(null)));

		int ib = s + 52;
		boolean hb = infoOpen || hov(ib, 4, 48, 48);
		surface(g, ib, 4, 48, 48, 14, hb);
		iconC(g, 0xF0E0, ib + 24, 28, 18, TEXT);
		hit(ib, 4, 48, 48, false, () -> infoOpen = true);

		int dx = s + 104;
		boolean hd = hov(dx, 4, 152, 48);
		Draw.round(g, dx, 4, 152, 48, 14, hd ? CARD_HOVER : DD_BG);
		Draw.outline(g, dx, 4, 152, 48, 14, CARD_EDGE, 2);
		txtCenter(g, legacy ? "Legacy" : "Default", dx + 76, 28, 16, TEXT);
		iconC(g, 0xF078, dx + 135, 23, 14, DIM);
		hit(dx, 4, 152, 48, false, () -> ddOpen = true);

		int ex = s + 260;
		boolean he = hov(ex, 6, 44, 44);
		surface(g, ex, 6, 44, 44, 14, he);
		iconC(g, 0xF00D, ex + 22, 28, 18, TEXT);
		hit(ex, 6, 44, 44, false, () -> Minecraft.getInstance().stop());
	}

	/** Version dropdown under the header; rows only close it, as in start.js. */
	private void drawDropdown(GuiGraphicsExtractor g) {
		int x = vw - 208, y = 56;
		shadow(g, x, y, 152, 92, 16);
		Draw.round(g, x, y, 152, 92, 16, PANEL);
		Draw.outline(g, x, y, 152, 92, 16, PANEL_EDGE, 2);
		for (int i = 0; i < 2; i++) {
			int ry = y + 6 + 42 * i;
			boolean on = (i == 1) == legacy;
			if (on) Draw.round(g, x + 6, ry, 140, 38, 12, ACCENT);
			else if (inRect(mxd, myd, x + 6, ry, 140, 38)) Draw.round(g, x + 6, ry, 140, 38, 12, ROW_HOVER);
			txtCenter(g, i == 0 ? "Default" : "Legacy", x + 76, ry + 19, 16, on ? TEXT : DIM);
		}
	}

	/** Bottom-right news flipbook: 202x128, one frame every 5 s. */
	private void drawNews(GuiGraphicsExtractor g) {
		long secs = (System.nanoTime() - startNs) / 1_000_000_000L;
		int frame = (int) ((secs / (long) NEWS_FRAME_S) % NEWS.length);
		int nx = vw - 212, ny = vh - 164;
		RenderUtil.image(g, NEWS[frame], nx, ny, nx + 202, ny + 128, 1f);
	}

	/** Footer: version badge and Discord button at the bottom left, version badge at the bottom right. */
	private void drawFoot(GuiGraphicsExtractor g) {
		String left = "Glacier v7";
		int lw = badgeWidth(left);
		int by = vh - 33;
		badge(g, left, 4, by, lw);

		int dx = 4 + lw + 6, dy = vh - 38;
		boolean hd = hov(dx, dy, 34, 34);
		surface(g, dx, dy, 34, 34, 14, hd);
		brandIconC(g, 0xF392, dx + 17, dy + 17, 16, DISCORD);
		hit(dx, dy, 34, 34, false, this::copyDiscord);

		int rw = badgeWidth(version);
		badge(g, version, vw - 4 - rw, by, rw);
	}

	/** Logo intro: pops in, grows, holds, then flies up and fades out. */
	private void drawLogo(GuiGraphicsExtractor g) {
		if (logoA <= 0f) return;
		float x = vw / 2f - logoSize / 2f;
		float y = vh / 2f - logoSize / 2f + logoY;
		RenderUtil.image(g, LOGO, x, y, x + logoSize, y + logoSize, logoA);
	}

	/** Full-screen scrim with the Information window: changelog, tutorials and credits. */
	private void drawInfo(GuiGraphicsExtractor g) {
		g.fill(0, 0, vw, vh, SCRIM);
		int ww = Math.round(vw * 0.88f), wh = Math.round(vh * 0.86f);
		int wx = (vw - ww) / 2, wy = (vh - wh) / 2;
		shadow(g, wx, wy, ww, wh, 18);
		Draw.round(g, wx, wy, ww, wh, 18, WINDOW);
		Draw.outline(g, wx, wy, ww, wh, 18, PANEL_EDGE, 2);

		int x0 = wx + 18, y0 = wy + 18, cw = ww - 36, ch = wh - 36;

		int bx = x0, by = y0 + 6;
		surface(g, bx, by, 256, 60, 14, false);
		Draw.round(g, bx + 6, by + 6, 48, 48, 12, PANEL);
		Draw.outline(g, bx + 6, by + 6, 48, 48, 12, PANEL_EDGE, 2);
		RenderUtil.image(g, LOGO, bx + 10, by + 10, bx + 50, by + 50, 1f);
		RenderUtil.image(g, WORDMARK, bx + 76, by + 8, bx + 244, by + 54, 1f);

		int[] widths = new int[TAB_LABELS.length];
		int total = 0;
		for (int i = 0; i < widths.length; i++) {
			int iw = Draw.iconWidth(TAB_ICONS[i], fs(20));
			int w = i == infoTab
					? 14 + iw + 8 + Draw.textWidth(TAB_LABELS[i], fs(16)) + 14
					: 14 + iw + 14;
			widths[i] = Math.max(56, w);
			total += widths[i] + (i > 0 ? 8 : 0);
		}
		int tx = x0 + cw - 80 - total;
		int ty = y0 + 8;
		for (int i = 0; i < widths.length; i++) {
			final int idx = i;
			int w = widths[i];
			if (i == infoTab) {
				boolean hv = inRect(mxd, myd, tx, ty, w, 56);
				Draw.round(g, tx, ty, w, 56, 14, hv ? ACCENT_HOVER : ACCENT);
				Draw.outline(g, tx, ty, w, 56, 14, ACCENT, 2);
				int iw = Draw.iconWidth(TAB_ICONS[i], fs(20));
				Draw.icon(g, TAB_ICONS[i], tx + 14, ty + 28 - 4 * fs(20), fs(20), TEXT);
				txtMid(g, TAB_LABELS[i], tx + 14 + iw + 8, ty + 28, 16, TEXT);
			} else {
				surface(g, tx, ty, w, 56, 14, inRect(mxd, myd, tx, ty, w, 56));
				iconC(g, TAB_ICONS[i], tx + w / 2f, ty + 28, 20, TEXT);
			}
			hit(tx, ty, w, 56, true, () -> {
				infoTab = idx;
				infoScroll = 0;
			});
			tx += w + 8;
		}

		int cx = x0 + cw - 68, cy = y0 + 6;
		surface(g, cx, cy, 60, 60, 14, inRect(mxd, myd, cx, cy, 60, 60));
		iconC(g, 0xF00D, cx + 30, cy + 30, 20, TEXT);
		hit(cx, cy, 60, 60, true, () -> infoOpen = false);

		int px = x0, py = y0 + 84, pw = cw, ph = ch - 84;
		infoScroll = Math.min(infoScroll, infoMax);
		g.enableScissor(px, py, px + pw, py + ph);
		float h = entries(g, TAB_TEXT[infoTab], px + 8, py + 4 - infoScroll, pw - 16, true);
		g.disableScissor();
		infoMax = Math.max(0, h + 8 - ph);
	}

	/** Lays out a flex column of changelog entries (gap 6, word wrap). Returns the height, drawing only when asked. */
	private float entries(GuiGraphicsExtractor g, String[][] es, float x, float y, float w, boolean draw) {
		float yy = y;
		for (int i = 0; i < es.length; i++) {
			if (i > 0) yy += 6;
			String kind = es[i][0];
			float f, lineH, mt = 0f, mb = 0f;
			int indent = 0, color;
			switch (kind) {
				case "ver" -> {
					f = fs(27);
					lineH = 32;
					mb = 4;
					color = ACCENT;
				}
				case "sec" -> {
					f = fs(18);
					lineH = 22;
					mt = 6;
					color = TEXT;
				}
				default -> {
					f = fs(16);
					lineH = 19;
					indent = 12;
					color = DIM;
				}
			}
			yy += mt;
			for (String line : wrap(es[i][1], f, Math.round(w) - indent)) {
				if (draw) Draw.text(g, line, x + indent, yy + (lineH - 8 * f) / 2f, f, color);
				yy += lineH;
			}
			yy += mb;
		}
		return yy - y;
	}

	private static List<String> wrap(String s, float f, int maxW) {
		List<String> out = new ArrayList<>();
		String cur = "";
		for (String word : s.split(" ")) {
			String cand = cur.isEmpty() ? word : cur + " " + word;
			if (!cur.isEmpty() && Draw.textWidth(cand, f) > maxW) {
				out.add(cur);
				cur = word;
			} else {
				cur = cand;
			}
		}
		if (!cur.isEmpty()) out.add(cur);
		return out;
	}

	// ----------------------------------------------------------------- widgets

	private void button(GuiGraphicsExtractor g, String label, int x, int y, Runnable action) {
		surface(g, x, y, 352, 44, 14, hov(x, y, 352, 44));
		txtCenter(g, label, x + 176, y + 22, 18, fa(TEXT));
		hit(x, y, 352, 44, false, action);
	}

	/** Card surface: card fill with a 2 px card-edge border, lighter when hovered. */
	private void surface(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, boolean hover) {
		Draw.round(g, x, y, w, h, r, fa(hover ? CARD_HOVER : CARD));
		Draw.outline(g, x, y, w, h, r, fa(hover ? CARD_HOVER_EDGE : CARD_EDGE), 2);
	}

	/** Soft drop shadow (0 8px 30px), approximated with stacked translucent rounded rects. */
	private void shadow(GuiGraphicsExtractor g, int x, int y, int w, int h, int r) {
		for (int k = 6; k >= 1; k--) {
			Draw.round(g, x - 3 * k, y + 8 - 3 * k, w + 6 * k, h + 6 * k, r + 3 * k, 0x14000000);
		}
	}

	private void badge(GuiGraphicsExtractor g, String s, int x, int y, int w) {
		Draw.round(g, x, y, w, 29, 8, BADGE);
		Draw.outline(g, x, y, w, 29, 8, BADGE_EDGE, 2);
		txt(g, s, x + 8, y + 4, 18, TEXT);
	}

	private static int badgeWidth(String s) {
		return Draw.textWidth(s, fs(18)) + 16;
	}

	private void hit(int x, int y, int w, int h, boolean overlay, Runnable click) {
		hits.add(new Hit(x, y, w, h, overlay, click));
	}

	/** Hover for the page underneath: none while a window or the dropdown covers it. */
	private boolean hov(int x, int y, int w, int h) {
		return !infoOpen && !ddOpen && inRect(mxd, myd, x, y, w, h);
	}

	private static boolean inRect(double px, double py, int x, int y, int w, int h) {
		return px >= x && py >= y && px < x + w && py < y + h;
	}

	/** Applies the card fade-in to a colour. */
	private int fa(int argb) {
		return cf >= 1f ? argb : Draw.alpha(argb, cf);
	}

	// ------------------------------------------------------------------- text

	/** css px to Draw font scale: the Glacier UI font is 9 px tall per 1 scale unit. */
	private static float fs(float css) {
		return css / 9f;
	}

	private static void txt(GuiGraphicsExtractor g, String s, float x, float y, float css, int argb) {
		Draw.text(g, s, x, y, fs(css), argb);
	}

	/** Left-aligned text centred vertically on cy. */
	private static void txtMid(GuiGraphicsExtractor g, String s, float x, float cy, float css, int argb) {
		float f = fs(css);
		Draw.text(g, s, x, cy - 4f * f, f, argb);
	}

	private static void txtCenter(GuiGraphicsExtractor g, String s, float cx, float cy, float css, int argb) {
		float f = fs(css);
		Draw.text(g, s, cx - Draw.textWidth(s, f) / 2f, cy - 4f * f, f, argb);
	}

	private static void iconC(GuiGraphicsExtractor g, int cp, float cx, float cy, float css, int argb) {
		float f = fs(css);
		Draw.icon(g, cp, cx - Draw.iconWidth(cp, f) / 2f, cy - 4f * f, f, argb);
	}

	/** Font Awesome brands glyph (Discord), which the Draw helpers do not cover. */
	private static void brandIconC(GuiGraphicsExtractor g, int cp, float cx, float cy, float css, int argb) {
		float f = fs(css);
		Font font = Minecraft.getInstance().font;
		Component c = Component.literal(new String(Character.toChars(cp)))
				.setStyle(Style.EMPTY.withFont(new FontDescription.Resource(FA_BRANDS)));
		var p = g.pose();
		p.pushMatrix();
		p.translate(cx - font.width(c) * f / 2f, cy - 4f * f);
		p.scale(f, f);
		g.text(font, c, 0, 0, argb, false);
		p.popMatrix();
	}

	/** Shortens s with "..." so it fits maxW, as the web page's text-overflow ellipsis does. */
	private static String fit(String s, float f, int maxW) {
		if (Draw.textWidth(s, f) <= maxW) return s;
		String t = s;
		while (t.length() > 1 && Draw.textWidth(t + "...", f) > maxW) t = t.substring(0, t.length() - 1);
		return t + "...";
	}

	// ------------------------------------------------------------------ intro

	/** Logo and card timing from start.js: logo for 2.6 s, UI slides up after 1.75 s, card rises from 2.2 s. */
	private void intro(float t) {
		if (t >= INTRO_END) {
			introActive = false;
			uiOff = 0f;
			cardDy = 0f;
			cf = 1f;
			return;
		}
		introActive = true;
		float a = outExpo(clamp(t / 0.35f));
		float y = 100f * (1f - outExpo(clamp(t / 0.6f)));
		float size = 180f;
		if (t > 0.6f && t <= 0.8f) size = 180f + 16f * outBack(clamp((t - 0.6f) / 0.2f));
		else if (t > 0.8f && t <= 1.0f) size = 196f - 16f * sine(clamp((t - 0.8f) / 0.2f));
		if (t > 1.4f) y = -1400f * inExpo(clamp((t - 1.4f) / 0.45f));
		if (t > 1.85f) a = 1f - inExpo(clamp((t - 1.85f) / 0.15f));
		logoA = a;
		logoY = y;
		logoSize = size;

		float u = t < 1.75f ? 1f : 1f - outExpo(clamp((t - 1.75f) / 0.45f));
		uiOff = u * vh;

		float c = clamp((t - 2.2f) / 0.36f);
		float rise = 1f - (1f - c) * (1f - c) * (1f - c);
		cardDy = (1f - rise) * 12f;
		cf = clamp((t - 2.2f) / 0.28f);
	}

	private static float outExpo(float x) {
		return x >= 1f ? 1f : 1f - (float) Math.pow(2, -10 * x);
	}

	private static float inExpo(float x) {
		return x <= 0f ? 0f : (float) Math.pow(2, 10 * x - 10);
	}

	private static float outBack(float x) {
		float c = 1.70158f;
		return 1f + (c + 1f) * (float) Math.pow(x - 1, 3) + c * (float) Math.pow(x - 1, 2);
	}

	private static float sine(float x) {
		return (float) (-(Math.cos(Math.PI * x) - 1) / 2);
	}

	private static float clamp(float x) {
		return x < 0f ? 0f : x > 1f ? 1f : x;
	}

	// ------------------------------------------------------------------ actions

	private void copyDiscord() {
		try {
			Minecraft.getInstance().keyboardHandler.setClipboard("https://" + StartApi.DISCORD);
		} catch (RuntimeException e) {
			GlacierClient.LOGGER.warn("[ui] clipboard copy failed", e);
		}
	}

	private static void open(Screen s) {
		Minecraft.getInstance().gui.setScreen(s);
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("glacier", path);
	}

	// ------------------------------------------------------------------- input

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
		if (introActive) return true;
		// The dropdown's catch layer closes it on any click and passes nothing through.
		if (ddOpen) {
			ddOpen = false;
			double dx = e.x() / scale, dy = e.y() / scale;
			int px = vw - 208 + 6;
			for (int i = 0; i < 2; i++) {
				if (inRect(dx, dy, px, 56 + 6 + 42 * i, 140, 38)) setLegacy(i == 1);
			}
			return true;
		}
		double x = e.x() / scale, y = e.y() / scale;
		for (int i = hits.size() - 1; i >= 0; i--) {
			Hit h = hits.get(i);
			if (infoOpen && !h.overlay()) continue;
			if (!h.in(x, y)) continue;
			h.click().run();
			return true;
		}
		// The open window's scrim swallows clicks that miss its controls.
		return infoOpen;
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy) {
		if (!infoOpen) return false;
		infoScroll = (float) Math.max(0, Math.min(infoScroll - sy * 24, infoMax));
		return true;
	}

	/** Esc closes the dropdown, then the Information window; the screen itself never closes on Esc. */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == KEY_ESCAPE) {
			if (ddOpen) ddOpen = false;
			else infoOpen = false;
		}
		return true;
	}

	private static String[][] CHANGELOG() {
		return new String[][]{
				{"ver", "Glacier Client - 7"},
				{"sec", "Start Screen:"},
				{"item", "New centred start card with Play Game, Options, Marketplace, Dressing Room and Information."},
				{"item", "Animated panorama background that slowly pans across the scene."},
				{"item", "Start screen logo animation refined with smoother bounce and slide transitions."},
				{"item", "Start screen UI now stays hidden during the logo intro animation."},
				{"sec", "Mod Menu:"},
				{"item", "HTML/CSS Glacier menu with module settings, HUD editor and cosmetics."},
				{"sec", "Optimization:"},
				{"item", "Performance mods tuned for low-end laptops."}};
	}

	private static String[][] TUTORIALS() {
		return new String[][]{
				{"sec", "Opening the Glacier menu:"},
				{"item", "Press Right Shift in game to open the Glacier menu and toggle modules."},
				{"sec", "Moving HUD modules:"},
				{"item", "Open the HUD editor from the Glacier menu, drag modules to move them and scroll to resize."}};
	}

	private static String[][] CREDITS() {
		return new String[][]{
				{"sec", "Glacier Client"},
				{"item", "Made by Pepe."},
				{"item", "Minecraft is a trademark of Mojang AB."}};
	}
}
