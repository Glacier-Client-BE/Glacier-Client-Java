package net.glacier.ui.screen;

import net.glacier.config.Config;
import net.glacier.ui.Draw;
import net.glacier.web.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Right Shift screen as plain Java widgets (replaces quickbar.html and the web layer of HudQuickScreen). The live HUD
 * stays visible and every HUD module can be dragged, with the quick bar (logo, Back / MODS / Grid) centred on top.
 *
 * <p>Everything is laid out in css px (the units of quickbar.css) and drawn through a pose scale equal to WebScreen's
 * Fit: scale = height / 420 capped and snapped to 0.25 steps, logical viewport floor(size / scale), origin rounded so
 * the page is centred.
 */
public final class NativeQuickBar extends Screen {
	private static final float BAR_H = 420f;
	private static final float MAX_SCALE = 4f;
	private static final float STEP = 0.25f;

	private static final Identifier LOGO = Identifier.fromNamespaceAndPath("glacier", "textures/ui/logo.png");
	private static final String WORD = "Glacier";

	/** quickbar.css: logo 64, gap 8, word line 26 (22px font), gap 8 + row margin 8, buttons 52/104 x 42, gap 8. */
	private static final int LOGO_SIZE = 64, GAP = 8, WORD_H = 26, ROW_MARGIN = 8;
	private static final int BTN_H = 42, BTN_SMALL = 52, BTN_WIDE = 104, BTN_GAP = 8, BTN_R = 14;
	private static final int LIFT = 2;

	private static final int NAVY = 0xFF161A2B;
	private static final int NAVY_HOVER = 0xFF1E2440;
	private static final int NAVY_BORDER = 0x14FFFFFF;
	private static final int NAVY_BORDER_HOVER = 0x8C7289DA;
	private static final int ICON_DIM = 0xFF99AAB5;
	private static final int ICON_ACTIVE = 0xFF8EA0E0;
	private static final int SHADOW = 0xA6000000;
	private static final int ACCENT = 0xFF7289DA;
	private static final int WHITE = 0xFFFFFFFF;

	private static final int ICON_BACK = 0xF104;
	private static final int ICON_GRID = 0xF00A;
	private static final int ICON_GEAR = 0xF013;

	/** Grid overlay state, kept across opens. */
	private static boolean gridOn;

	private final HudEditorScreen.Dragger dragger = new HudEditorScreen.Dragger();

	/* Per-frame placement of the page: scale, origin (screen px) and logical size (css px). */
	private float s = 1f;
	private int originX, originY, lw, lh;
	private Box back, mods, settings, grid;

	public NativeQuickBar() {
		super(Component.literal("Glacier"));
	}

	/** WebScreen.pickScale for the quick bar. */
	private void fit() {
		float v = Math.min(Math.min(height / BAR_H, MAX_SCALE), Math.min(width / 320f, height / 240f));
		s = v < 0.5f ? Math.max(0.1f, v) : (float) Math.floor(v / STEP) * STEP;
		lw = (int) Math.floor(width / s);
		lh = (int) Math.floor(height / s);
		originX = Math.round((width - lw * s) / 2f);
		originY = Math.round((height - lh * s) / 2f);

		int total = LOGO_SIZE + GAP + WORD_H + GAP + ROW_MARGIN + BTN_H;
		int top = Math.round((lh - total) / 2f);
		int rowTop = top + LOGO_SIZE + GAP + WORD_H + GAP + ROW_MARGIN;
		// Back, MODS, Settings, Grid: one centred row; the logo and word sit on the same centre line.
		int rowW = BTN_SMALL + BTN_GAP + BTN_WIDE + BTN_GAP + BTN_SMALL + BTN_GAP + BTN_SMALL;
		int rowX = Math.round((lw - rowW) / 2f);
		back = new Box(rowX, rowTop, BTN_SMALL, BTN_H);
		mods = new Box(rowX + BTN_SMALL + BTN_GAP, rowTop, BTN_WIDE, BTN_H);
		settings = new Box(rowX + BTN_SMALL + BTN_GAP + BTN_WIDE + BTN_GAP, rowTop, BTN_SMALL, BTN_H);
		grid = new Box(rowX + BTN_SMALL + BTN_GAP + BTN_WIDE + BTN_GAP + BTN_SMALL + BTN_GAP, rowTop, BTN_SMALL, BTN_H);
		logoX = Math.round((lw - LOGO_SIZE) / 2f);
		logoY = top;
		wordCy = top + LOGO_SIZE + GAP + WORD_H / 2f;
	}

	private int logoX, logoY;
	private float wordCy;

	private double lx(double sx) {
		return (sx - originX) / s;
	}

	private double ly(double sy) {
		return (sy - originY) / s;
	}

	/** Hover area of a button in css px: the box plus the lift it gets on hover. */
	private static boolean over(Box b, double x, double y) {
		return x >= b.x() && x < b.x() + b.w() && y >= b.y() - LIFT && y < b.y() + b.h();
	}

	private boolean overButton(double sx, double sy) {
		double x = lx(sx), y = ly(sy);
		return over(back, x, y) || over(mods, x, y) || over(settings, x, y) || over(grid, x, y);
	}

	private static float fs(float css) {
		return css / 9f;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		fit();
		boolean gridShown = gridOn;
		if (gridShown) HudEditorScreen.Dragger.drawGrid(g, width, height);
		boolean onBar = dragger.dragging() == null && overButton(mouseX, mouseY);
		dragger.renderModules(g, width, height, onBar ? -1 : mouseX, onBar ? -1 : mouseY, gridShown);

		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(originX, originY);
		pose.scale(s, s);
		drawBar(g, (int) Math.round(lx(mouseX)), (int) Math.round(ly(mouseY)));
		pose.popMatrix();
	}

	private void drawBar(GuiGraphics g, int mx, int my) {
		RenderUtil.image(g, LOGO, logoX, logoY, logoX + LOGO_SIZE, logoY + LOGO_SIZE, 1f);

		float f = fs(22);
		float wx = (lw - Draw.textWidth(WORD, f)) / 2f, wy = wordCy - 4f * f;
		Draw.text(g, WORD, wx + 2, wy + 2, f, SHADOW);
		Draw.text(g, WORD, wx, wy, f, WHITE);

		boolean h = over(back, mx, my);
		Box b = button(g, back, h, false);
		icon(g, ICON_BACK, b, h ? WHITE : ICON_DIM);

		h = over(mods, mx, my);
		b = button(g, mods, h, false);
		float lf = fs(13);
		Draw.text(g, "MODS", b.x() + (b.w() - Draw.textWidth("MODS", lf)) / 2f, b.y() + b.h() / 2f - 4f * lf, lf, WHITE);

		h = over(settings, mx, my);
		b = button(g, settings, h, false);
		icon(g, ICON_GEAR, b, h ? WHITE : ICON_DIM);

		h = over(grid, mx, my);
		b = button(g, grid, h, gridOn);
		icon(g, ICON_GRID, b, gridOn ? ICON_ACTIVE : h ? WHITE : ICON_DIM);
	}

	/** Button background plus 1px border ring, lifted 2 css px on hover; returns the drawn box. */
	private Box button(GuiGraphics g, Box rest, boolean hover, boolean on) {
		Box b = hover ? new Box(rest.x(), rest.y() - LIFT, rest.w(), rest.h()) : rest;
		Draw.round(g, b.x(), b.y(), b.w(), b.h(), BTN_R, hover ? NAVY_HOVER : NAVY);
		Draw.outline(g, b.x(), b.y(), b.w(), b.h(), BTN_R, on ? ACCENT : hover ? NAVY_BORDER_HOVER : NAVY_BORDER, 1);
		return b;
	}

	private static void icon(GuiGraphics g, int cp, Box b, int argb) {
		float f = fs(15);
		Draw.icon(g, cp, b.x() + (b.w() - Draw.iconWidth(cp, f)) / 2f, b.y() + b.h() / 2f - 4f * f, f, argb);
	}

	/** No blur or dim: the game and the HUD stay fully visible. */
	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		fit();
		double mx = event.x();
		double my = event.y();
		if (overButton(mx, my)) {
			if (event.button() == 0) press(lx(mx), ly(my));
			return true;
		}
		return event.button() == 0 && dragger.begin(mx, my, width, height);
	}

	private void press(double x, double y) {
		if (over(back, x, y)) {
			onClose();
		} else if (over(mods, x, y)) {
			Minecraft.getInstance().setScreen(NativeMenuScreen.open(null));
		} else if (over(settings, x, y)) {
			Minecraft.getInstance().setScreen(NativeMenuScreen.open("Performance"));
		} else if (over(grid, x, y)) {
			gridOn = !gridOn;
		}
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		return dragger.drag(event.x(), event.y(), width, height, gridOn);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		return dragger.release() || super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		return dragger.scale(mouseX, mouseY, width, height, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		dragger.release();
		Config.save();
		super.onClose();
	}

	/** A screen rectangle in whole pixels. */
	private record Box(int x, int y, int w, int h) {
		boolean contains(double mx, double my) {
			return mx >= x && my >= y && mx < x + w && my < y + h;
		}
	}
}
