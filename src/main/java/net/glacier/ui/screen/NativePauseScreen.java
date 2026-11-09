package net.glacier.ui.screen;

import net.glacier.GlacierClient;
import net.glacier.ui.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.resources.Identifier;
import net.glacier.compat.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The pause menu (pause.html) as plain Java widgets drawn on GuiGraphicsExtractor. It still extends {@link PauseScreen} so the
 * existing {@code instanceof PauseScreen} checks (MenuFps) match. Immediate mode, like {@link NativeMenuScreen}: every
 * frame the buttons are drawn and their click regions recorded. The actions are the ones ScreenApi performed for the
 * web page. Swapped in by {@link net.glacier.mixin.MixinScreenSwap}.
 */
public final class NativePauseScreen extends PauseScreen {
	/** Design height and minimum width in css px, the snapped-scale rule of WebPage / WebScreen. */
	private static final float DESIGN_H = 540f, MIN_W = 320f, STEP = 0.25f;
	private static final int PANEL_W = 400, PANEL_Y = 118;
	private static final Identifier ART = Identifier.fromNamespaceAndPath("glacier", "textures/screens/pause_screen.png");
	/** Natural size of pause_screen.png: the blit needs it to sample the whole image into the 200x200 box. */
	private static final int ART_W = 1040, ART_H = 783;
	private static final int ICON_PAUSE = 0xf04c;
	private static final int DIM = 0x80000000;
	private static final int MUTED = 0xFFB9BBBE, ACCENT = 0xFF7289DA, ACCENT_HOVER = 0xFF8296E0;
	private static final int CARD = 0xFF2C2F33, CARD_EDGE = 0xFF393C41;
	private static final int CARD_HOVER = 0xFF33373C, CARD_HOVER_EDGE = 0xFF484C52;
	private static final int WHITE = 0xFFFFFFFF;
	private static final String INFO = "Glacier Client v7 on Minecraft " + net.minecraft.SharedConstants.getCurrentVersion().name();

	private enum Kind { ACCENT, CARD, PILL }

	private record Hit(int x, int y, int w, int h, Runnable click) {
		boolean in(double mx, double my) {
			return mx >= x && my >= y && mx < x + w && my < y + h;
		}
	}

	private final List<Hit> hits = new ArrayList<>();
	/** The Disconnect confirm box is open; it covers the menu and takes all clicks. */
	private boolean confirming;
	private boolean showInfo;

	public NativePauseScreen(boolean showPauseMenu) {
		super(showPauseMenu);
	}

	/** No vanilla widgets: the menu is drawn and clicked directly. */
	@Override
	protected void init() {
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partialTick) {
	}

	/** Same fit rule as the web page: height-driven, snapped to quarter steps, never below 0.1. */
	private float scale() {
		float s = Math.min(height / DESIGN_H, width / MIN_W);
		return s < 0.5f ? Math.max(0.1f, s) : (float) Math.floor(s / STEP) * STEP;
	}

	private static float fs(float css) {
		return css / 9f;
	}

	// ------------------------------------------------------------------ render

	/** Current css-to-GUI scale; every rectangle is rounded to whole GUI pixels (no pose scale) so edges stay crisp. */
	private float sc = 1f;

	private int p(float css) {
		return Math.round(css * sc);
	}

	/** Text centred on a css y, positioned on whole GUI pixels, sized by the css font size times the scale. */
	private void mid(GuiGraphicsExtractor g, String s, float x, float cy, float css, int argb) {
		float f = fs(css) * sc;
		Draw.text(g, s, p(x), Math.round(p(cy) - 4f * f), f, argb);
	}

	private void center(GuiGraphicsExtractor g, String s, float cx, float cy, float css, int argb) {
		float f = fs(css) * sc;
		Draw.text(g, s, Math.round(p(cx) - Draw.textWidth(s, f) / 2f), Math.round(p(cy) - 4f * f), f, argb);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int rawX, int rawY, float partial) {
		hits.clear();
		g.fill(0, 0, width, height, DIM);
		sc = scale();
		float lw = width / sc, lh = height / sc;
		int mx = rawX, my = rawY;

		float px = (lw - PANEL_W) / 2f, py = PANEL_Y;
		g.blit(RenderPipelines.GUI_TEXTURED, ART, p(px + 100), p(py + 35), 0f, 0f, p(200), p(200), ART_W, ART_H, ART_W,
				ART_H, Draw.alpha(0xFFFFFFFF, 0.85f));

		boolean live = !confirming;
		button(g, mx, my, px + 29, py + 4, 168, 40, 14, "Return to Game", Kind.ACCENT, live, this::resume);
		button(g, mx, my, px + 203, py + 4, 168, 40, 14, "Disconnect", Kind.CARD, live, () -> confirming = true);
		button(g, mx, my, px + 29, py + 50, 168, 40, 14, "Achievements", Kind.CARD, live, this::achievements);
		button(g, mx, my, px + 203, py + 50, 168, 40, 14, "Wardrobe", Kind.CARD, live,
				() -> show(() -> NativeMenuScreen.open("cosmetics")));
		button(g, mx, my, px + 29, py + 96, 342, 40, 14, "Take Screenshot", Kind.CARD, live, this::screenshot);
		button(g, mx, my, px + 29, py + 142, 168, 40, 14, "Options", Kind.CARD, live, this::options);
		button(g, mx, my, px + 203, py + 142, 168, 40, 14, "Information", Kind.CARD, live,
				() -> showInfo = !showInfo);
		// Open to LAN no longer exists in 26.x, so Mods takes the whole row instead of leaving a hole beside it.
		button(g, mx, my, px + 29, py + 186, 342, 40, 14, "Mods", Kind.PILL, live,
				() -> show(() -> NativeMenuScreen.open(null)));
		if (showInfo) center(g, INFO, lw / 2f, py + 232 + 7, 12, MUTED);

		drawStatus(g);
		if (confirming) drawConfirm(g, mx, my, lw, lh);
	}

	/** One nine-slice rect from css coordinates, both edges rounded so it lands on whole GUI pixels. */
	private void box(GuiGraphicsExtractor g, String tex, float x, float y, float w, float h, int argb) {
		int x0 = p(x), y0 = p(y);
		Draw.sprite(g, tex, x0, y0, Math.max(1, p(x + w) - x0), Math.max(1, p(y + h) - y0), argb);
	}

	/** .pz-status: 8,8, 176 wide, label 11px bold, row 14px icon + text with 8px gap. */
	private void drawStatus(GuiGraphicsExtractor g) {
		int x = 8, y = 8, w = 176, h = 53;
		box(g, "mm_card", x, y, w, h, WHITE);
		mid(g, "Game Status:", x + 12, y + 16.5f, 11, MUTED);
		float f = fs(14) * sc, cy = y + 35f;
		int iw = Draw.iconWidth(ICON_PAUSE, f);
		Draw.icon(g, ICON_PAUSE, p(x + 12), Math.round(p(cy) - 4f * f), f, ACCENT);
		Draw.text(g, "Paused", p(x + 12) + iw + p(8), Math.round(p(cy) - 4f * f), f, WHITE);
	}

	/** .pz-cbox: 352x124 centred over a second 50% dim, with its soft drop shadow. */
	private void drawConfirm(GuiGraphicsExtractor g, int mx, int my, float lw, float lh) {
		g.fill(0, 0, width, height, DIM);
		int bw = 352, bh = 124;
		float bx = (lw - bw) / 2f, by = (lh - bh) / 2f;
		for (int i = 4; i >= 1; i--) {
			int e = i * 4;
			Draw.round(g, p(bx - e), p(by + 8 - e), p(bw + 2 * e), p(bh + 2 * e), Math.min(16, p(12 + e)), 0x0D000000);
		}
		box(g, "mm_card", bx, by, bw, bh, WHITE);
		mid(g, "Leave this world?", bx + 18, by + 16 + 9.5f, 16, WHITE);
		mid(g, "Your progress is saved.", bx + 18, by + 48, 12, MUTED);
		float rowY = by + bh - 2 - 14 - 34;
		button(g, mx, my, bx + 18, rowY, 156, 34, 13, "Cancel", Kind.PILL, true, () -> confirming = false);
		button(g, mx, my, bx + 18 + 318 - 156, rowY, 156, 34, 13, "Disconnect", Kind.ACCENT, true, this::disconnect);
	}

	/**
	 * One css button drawn as a single nine-slice texture (the pack's mm_card / mm_pill / mm_pill_active) on whole GUI
	 * pixels. Accent keeps its look on hover (slightly brighter tint); cards and pills swap to their hover texture.
	 */
	private void button(GuiGraphicsExtractor g, int mx, int my, float x, float y, float w, float h, float font, String label,
						Kind kind, boolean active, Runnable action) {
		int x0 = p(x), y0 = p(y), w0 = Math.max(1, p(x + w) - x0), h0 = Math.max(1, p(y + h) - y0);
		boolean hover = active && mx >= x0 && my >= y0 && mx < x0 + w0 && my < y0 + h0;
		String tex;
		int tint = WHITE;
		switch (kind) {
			case ACCENT -> {
				tex = "mm_pill_active";
				if (hover) tint = 0xFFE6EAFF;
			}
			case CARD -> tex = hover ? "mm_card_hover" : "mm_card";
			default -> tex = hover ? "mm_pill_hover" : "mm_pill";
		}
		Draw.sprite(g, tex, x0, y0, w0, h0, tint);
		center(g, label, x + w / 2f, y + h / 2f, font, WHITE);
		if (active) hits.add(new Hit(x0, y0, w0, h0, action));
	}

	// ----------------------------------------------------------------- actions

	private void resume() {
		run(() -> Minecraft.getInstance().gui.setScreen(null));
	}

	private void achievements() {
		run(() -> {
			Minecraft mc = Minecraft.getInstance();
			var conn = mc.getConnection();
			ClientAdvancements adv = conn == null ? null : conn.getAdvancements();
			if (adv != null) mc.gui.setScreen(new AdvancementsScreen(adv, this));
		});
	}

	private void options() {
		show(() -> ScreenFactory.options(this));
	}

	private void screenshot() {
		run(() -> {
			Minecraft mc = Minecraft.getInstance();
			Screenshot.grab(mc.gameDirectory, mc.gameRenderer.mainRenderTarget(),
					msg -> mc.execute(() -> mc.gui.hud.getChat().addClientSystemMessage(msg)));
		});
	}

	private void disconnect() {
		run(() -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.isLocalServer()) mc.disconnectWithSavingScreen();
			else mc.disconnect(new JoinMultiplayerScreen(net.glacier.ui.screen.ScreenFactory.start()), false);
		});
	}

	/** Opens a screen built at click time; the screen returns to this one where vanilla does that. */
	private static void show(Supplier<Screen> screen) {
		run(() -> Minecraft.getInstance().gui.setScreen(screen.get()));
	}

	/** Runs an action and logs a failure instead of letting it break the frame. */
	private static void run(Runnable r) {
		try {
			r.run();
		} catch (RuntimeException e) {
			GlacierClient.LOGGER.error("[ui] screen action failed", e);
		}
	}

	// ------------------------------------------------------------------- input

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
		if (net.glacier.compat.Mouse.button(e) != 0) return super.mouseClicked(e, dbl);
		for (int i = hits.size() - 1; i >= 0; i--) {
			Hit h = hits.get(i);
			if (!h.in(e.x(), e.y())) continue;
			h.click().run();
			return true;
		}
		return super.mouseClicked(e, dbl);
	}

	/** Esc closes the confirm box first; otherwise it resumes the game. */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
			if (confirming) {
				confirming = false;
			} else {
				onClose();
			}
			return true;
		}
		return super.keyPressed(event);
	}
}
