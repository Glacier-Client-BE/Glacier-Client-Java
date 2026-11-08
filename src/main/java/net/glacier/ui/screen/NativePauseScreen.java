package net.glacier.ui.screen;

import net.glacier.GlacierClient;
import net.glacier.ui.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The pause menu (pause.html) as plain Java widgets drawn on GuiGraphics. It still extends {@link PauseScreen} so the
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
	private static final String INFO = "Glacier Client v7 on Minecraft 1.21.11";

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
	public void renderBackground(GuiGraphics g, int mx, int my, float partialTick) {
	}

	/** Same fit rule as the web page: height-driven, snapped to quarter steps, never below 0.1. */
	private float scale() {
		float s = Math.min(height / DESIGN_H, width / MIN_W);
		return s < 0.5f ? Math.max(0.1f, s) : (float) Math.floor(s / STEP) * STEP;
	}

	private static float fs(float css) {
		return css / 9f;
	}

	private static void mid(GuiGraphics g, String s, float x, float cy, float css, int argb) {
		float f = fs(css);
		Draw.text(g, s, x, cy - 4f * f, f, argb);
	}

	private static void center(GuiGraphics g, String s, float cx, float cy, float css, int argb) {
		float f = fs(css);
		Draw.text(g, s, cx - Draw.textWidth(s, f) / 2f, cy - 4f * f, f, argb);
	}

	// ------------------------------------------------------------------ render

	@Override
	public void render(GuiGraphics g, int rawX, int rawY, float partial) {
		hits.clear();
		g.fill(0, 0, width, height, DIM);
		float s = scale();
		int lw = (int) Math.ceil(width / s), lh = (int) Math.ceil(height / s);
		int mx = Math.round(rawX / s), my = Math.round(rawY / s);

		var pose = g.pose();
		pose.pushMatrix();
		pose.scale(s, s);

		int px = Math.round((lw - PANEL_W) / 2f), py = PANEL_Y;
		g.blit(RenderPipelines.GUI_TEXTURED, ART, px + 100, py + 35, 0f, 0f, 200, 200, ART_W, ART_H, ART_W, ART_H,
				Draw.alpha(0xFFFFFFFF, 0.85f));

		boolean live = !confirming;
		button(g, mx, my, px + 29, py + 4, 168, 40, 10, 14, "Return to Game", Kind.ACCENT, live, this::resume);
		button(g, mx, my, px + 203, py + 4, 168, 40, 10, 14, "Disconnect", Kind.CARD, live, () -> confirming = true);
		button(g, mx, my, px + 29, py + 50, 168, 40, 10, 14, "Achievements", Kind.CARD, live, this::achievements);
		button(g, mx, my, px + 203, py + 50, 168, 40, 10, 14, "Wardrobe", Kind.CARD, live,
				() -> show(() -> NativeMenuScreen.open("cosmetics")));
		button(g, mx, my, px + 29, py + 96, 342, 40, 10, 14, "Take Screenshot", Kind.CARD, live, this::screenshot);
		button(g, mx, my, px + 29, py + 142, 168, 40, 10, 14, "Options", Kind.CARD, live, this::options);
		button(g, mx, my, px + 203, py + 142, 168, 40, 10, 14, "Information", Kind.CARD, live,
				() -> showInfo = !showInfo);
		button(g, mx, my, px + 29, py + 186, 168, 40, 10, 14, "Mods", Kind.PILL, live,
				() -> show(() -> NativeMenuScreen.open(null)));
		if (canShareToLan()) {
			button(g, mx, my, px + 203, py + 186, 168, 40, 10, 14, "Open to LAN", Kind.PILL, live,
					() -> show(() -> new ShareToLanScreen(this)));
		}
		if (showInfo) center(g, INFO, lw / 2f, py + 232 + 7, 12, MUTED);

		drawStatus(g);
		if (confirming) drawConfirm(g, mx, my, lw, lh);
		pose.popMatrix();
	}

	/** .pz-status: 8,8, 176 wide, 2px border, padding 8/10, label 11px bold, row 14px icon + text with 8px gap. */
	private void drawStatus(GuiGraphics g) {
		int x = 8, y = 8, w = 176, h = 53;
		Draw.round(g, x, y, w, h, 10, CARD);
		Draw.outline(g, x, y, w, h, 10, CARD_EDGE, 2);
		mid(g, "Game Status:", x + 12, y + 16.5f, 11, MUTED);
		float f = fs(14), cy = y + 35f;
		int iw = Draw.iconWidth(ICON_PAUSE, f);
		Draw.icon(g, ICON_PAUSE, x + 12, cy - 4f * f, f, ACCENT);
		mid(g, "Paused", x + 12 + iw + 8, cy, 14, WHITE);
	}

	/** .pz-cbox: 352x124 centred over a second 50% dim, with its soft drop shadow. */
	private void drawConfirm(GuiGraphics g, int mx, int my, int lw, int lh) {
		g.fill(-4096, -4096, 8192, 8192, DIM);
		int bw = 352, bh = 124;
		int bx = Math.round((lw - bw) / 2f), by = Math.round((lh - bh) / 2f);
		for (int i = 4; i >= 1; i--) {
			int e = i * 4;
			Draw.round(g, bx - e, by + 8 - e, bw + 2 * e, bh + 2 * e, Math.min(16, 12 + e), 0x0D000000);
		}
		Draw.round(g, bx, by, bw, bh, 12, CARD);
		Draw.outline(g, bx, by, bw, bh, 12, CARD_EDGE, 2);
		mid(g, "Leave this world?", bx + 18, by + 16 + 9.5f, 16, WHITE);
		mid(g, "Your progress is saved.", bx + 18, by + 48, 12, MUTED);
		int rowY = by + bh - 2 - 14 - 34;
		button(g, mx, my, bx + 18, rowY, 156, 34, 9, 13, "Cancel", Kind.PILL, true, () -> confirming = false);
		button(g, mx, my, bx + 18 + 318 - 156, rowY, 156, 34, 9, 13, "Disconnect", Kind.ACCENT, true, this::disconnect);
	}

	/**
	 * One css button: background fill over the whole box, then the 2px border ring (pause.css). Accent keeps its
	 * border colour on hover; cards change border on hover; pills only the background.
	 */
	private void button(GuiGraphics g, int mx, int my, int x, int y, int w, int h, int radius, float font, String label,
						Kind kind, boolean active, Runnable action) {
		boolean hover = active && mx >= x && my >= y && mx < x + w && my < y + h;
		int fill = kind == Kind.ACCENT ? (hover ? ACCENT_HOVER : ACCENT) : (hover ? CARD_HOVER : CARD);
		int edge = kind == Kind.ACCENT ? ACCENT : (hover && kind == Kind.CARD ? CARD_HOVER_EDGE : CARD_EDGE);
		Draw.round(g, x, y, w, h, radius, fill);
		Draw.outline(g, x, y, w, h, radius, edge, 2);
		center(g, label, x + w / 2f, y + h / 2f, font, WHITE);
		if (active) hits.add(new Hit(x, y, w, h, action));
	}

	// ----------------------------------------------------------------- actions

	private void resume() {
		run(() -> Minecraft.getInstance().setScreen(null));
	}

	private void achievements() {
		run(() -> {
			Minecraft mc = Minecraft.getInstance();
			var conn = mc.getConnection();
			ClientAdvancements adv = conn == null ? null : conn.getAdvancements();
			if (adv != null) mc.setScreen(new AdvancementsScreen(adv, this));
		});
	}

	private void options() {
		show(() -> new OptionsScreen(this, Minecraft.getInstance().options));
	}

	private void screenshot() {
		run(() -> {
			Minecraft mc = Minecraft.getInstance();
			Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(),
					msg -> mc.execute(() -> mc.gui.getChat().addMessage(msg)));
		});
	}

	private void disconnect() {
		run(() -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.isLocalServer()) mc.disconnectWithSavingScreen();
			else mc.disconnect(new JoinMultiplayerScreen(net.glacier.ui.screen.ScreenFactory.start()), false);
		});
	}

	/** Only an integrated server that is not yet opened to LAN can be shared. */
	private static boolean canShareToLan() {
		IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
		return server != null && !server.isPublished();
	}

	/** Opens a screen built at click time; the screen returns to this one where vanilla does that. */
	private static void show(Supplier<Screen> screen) {
		run(() -> Minecraft.getInstance().setScreen(screen.get()));
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
		float s = scale();
		for (int i = hits.size() - 1; i >= 0; i--) {
			Hit h = hits.get(i);
			if (!h.in(Math.round(e.x() / s), Math.round(e.y() / s))) continue;
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
