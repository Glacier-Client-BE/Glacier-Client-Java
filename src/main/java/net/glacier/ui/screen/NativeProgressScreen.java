package net.glacier.ui.screen;

import net.glacier.mixin.MixinLevelLoadingAccessor;
import net.glacier.mixin.MixinProgressScreenAccessor;
import net.glacier.ui.Draw;
import net.glacier.ui.Theme;
import net.glacier.web.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * The loading and saving screen as plain Java widgets (replaces progress.html). It wraps the vanilla screen it
 * replaces ({@code LevelLoadingScreen} or {@code ProgressScreen}) and reads that screen's progress through the
 * accessor mixins every frame, so the vanilla code keeps updating its own instance. Percent -1 means unknown and the
 * bar sweeps. Esc and Exit only close when the vanilla screen allows it, and Exit is hidden when it does not.
 *
 * <p>Immediate mode, like {@link NativeMenuScreen}: the layout is recomputed each frame in css units (the same units
 * as progress.css) and scaled to the window with the same rule WebPage used, so the card keeps its proportions.
 */
public final class NativeProgressScreen extends Screen {
	private static final Identifier LOGO = Identifier.fromNamespaceAndPath("glacier", "textures/ui/logo.png");
	private static final Identifier WORDMARK = Identifier.fromNamespaceAndPath("glacier", "textures/start/wordmark.png");

	/** Design height in css px and minimum logical width, as the page screens scale (WebPage). */
	private static final float DESIGN_H = 540f;
	private static final float MIN_W = 480f;
	private static final float STEP = 0.25f;

	/** Card geometry in css px (progress.css). The card border is 2 px and the padding 20 px across, 18 px down. */
	private static final float CARD_W = 528f;
	private static final float CARD_MAX_SHARE = 0.92f;
	private static final float CARD_R = 16f;
	private static final float BORDER = 2f;
	private static final float X0 = BORDER + 20f;
	private static final float Y0 = BORDER + 18f;
	private static final float BRAND_H = 72f;
	private static final float ROW_H = 28f;
	private static final float GAP = 14f;
	private static final float TRACK_H = 14f;
	private static final float EXIT_W = 168f;
	private static final float EXIT_H = 36f;
	private static final float CHIP_MIN_W = 56f;
	private static final float CHIP_H = 26f;
	private static final float CHIP_R = 9f;
	/** The unknown-progress segment is 30% of the track wide and travels from -30% to 100%. */
	private static final float SWEEP_SHARE = 30f;
	private static final long SWEEP_STEP_MS = 33L;
	/** The spinner glyph pulses over one 0.9 s cycle (18 steps of 50 ms in the old page). */
	private static final long SPIN_CYCLE_MS = 900L;

	/** Colours from progress.css. Shared tokens come from Theme. */
	private static final int CARD_BG = 0xF7232728;
	private static final int PANEL = 0xFF232728;
	private static final int PANEL_EDGE = 0xFF33373A;
	private static final int CARD = Theme.CARD;
	private static final int CARD_EDGE = Theme.CARD_BORDER;
	private static final int CARD_HOVER = 0xFF33373C;
	private static final int CARD_HOVER_EDGE = 0xFF484C52;
	private static final int TRACK = 0xFF1D1F22;
	private static final int ACCENT = Theme.ACCENT;
	private static final int TEXT = Theme.TEXT;

	/** Font Awesome spinner (fa-spinner) and the text scales: css px divided by about ten. */
	private static final int SPIN_ICON = 0xF1CE;
	private static final String SPIN = new String(Character.toChars(SPIN_ICON));
	private static final java.util.regex.Pattern CONTROL = java.util.regex.Pattern.compile("\\p{Cntrl}");
	private static final float SPIN_K = 1.4f;
	private static final float TITLE_K = 1.6f;
	private static final float PCT_K = 1.3f;
	private static final float EXIT_K = 1.4f;

	private final Screen source;

	/* Per-frame state, set by layout() and readProgress(). */
	private float s = 1f;
	private float cardX;
	private float cardY;
	private float cardW;
	private float cardH;
	private boolean canClose;
	private Box exitBox;
	private int pct = -1;
	private String title = "Loading";

	private NativeProgressScreen(Screen source) {
		super(Component.literal("Loading"));
		this.source = source;
	}

	/**
	 * Wraps a vanilla loading or saving screen. The swap hook (MixinScreenSwap) returns this in place of the vanilla
	 * screen, and the wrapper reads that vanilla instance's progress through the accessor mixins.
	 */
	public static NativeProgressScreen wrap(Screen source) {
		return new NativeProgressScreen(source);
	}

	/** Closes the vanilla screen the same way Esc does. Saving a world cannot be cancelled, so nothing happens there. */
	private void closeSource() {
		if (source.shouldCloseOnEsc()) source.onClose();
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		WebPage.drawPanorama(g, width, height);
		readProgress();
		layout();
		float mx = mouseX / s, my = mouseY / s;

		var pose = g.pose();
		pose.pushMatrix();
		pose.scale(s, s);

		// .pg-card: 2 px border, radius 16, padding 18 20, gap 14
		int cx = Math.round(cardX), cy = Math.round(cardY), cw0 = Math.round(cardW), ch0 = Math.round(cardH);
		Draw.round(g, cx, cy, cw0, ch0, 16, CARD_BG);
		Draw.outline(g, cx, cy, cw0, ch0, 16, PANEL_EDGE, 2);

		int ix = cx + 22, iy = cy + 20;
		int cw = cw0 - 44;

		// .pg-brand 72 px, inset 56 px at 6/6 (inside the border), logo 40, wordmark 220x40 at 76/16
		Draw.round(g, ix, iy, cw, 72, 12, CARD);
		Draw.outline(g, ix, iy, cw, 72, 12, CARD_EDGE, 2);
		Draw.round(g, ix + 8, iy + 8, 56, 56, 12, PANEL);
		Draw.outline(g, ix + 8, iy + 8, 56, 56, 12, PANEL_EDGE, 2);
		RenderUtil.image(g, LOGO, ix + 16, iy + 16, ix + 56, iy + 56, 1f);
		RenderUtil.image(g, WORDMARK, ix + 78, iy + 18, ix + 298, iy + 58, 1f);

		// .pg-status: spinner 28, title 16 bold, chip
		int rowY = iy + 72 + 14;
		int rowMid = rowY + 14;
		Draw.round(g, ix, rowY, 28, 28, 8, CARD);
		Draw.outline(g, ix, rowY, 28, 28, 8, CARD_EDGE, 2);
		float spinW = RenderUtil.textWidth(SPIN, false, 14, RenderUtil.FA_SOLID);
		RenderUtil.drawText(g, SPIN,ix + (28 - spinW) / 2f, rowMid - 7, 14, false, RenderUtil.FA_SOLID,
				Draw.alpha(ACCENT, spinOpacity()));

		boolean unknown = pct < 0;
		String pctText = unknown ? "..." : Math.max(0, Math.min(100, Math.round(pct))) + "%";
		float pctW = RenderUtil.textWidth(pctText, true, 13, null);
		int chipW = Math.round(Math.max(56f, pctW + 20f));
		int chipX = ix + cw - chipW;
		Draw.round(g, chipX, rowMid - 13, chipW, 26, 9, ACCENT);
		RenderUtil.drawText(g, pctText, chipX + (chipW - pctW) / 2f, rowMid - 6.5f, 13, true, null, TEXT);

		float titleX = ix + 28 + 10;
		float maxTitleW = chipX - 10 - titleX;
		RenderUtil.drawText(g, RenderUtil.ellipsize(title, maxTitleW, true, 16, null), titleX, rowMid - 8, 16, true, null, TEXT);

		// .pg-track: 14 px, 2 px border, radius 7; the fill lives in the 10 px padding box (radius clamps to 5)
		int ty = rowY + 28 + 14;
		Draw.round(g, ix, ty, cw, 14, 7, TRACK);
		Draw.outline(g, ix, ty, cw, 14, 7, CARD_EDGE, 2);
		float innerX = ix + 2, innerY = ty + 2, innerW = cw - 4;
		if (unknown) {
			double pos = (System.currentTimeMillis() / (double) SWEEP_STEP_MS * 2.0) % (100.0 + SWEEP_SHARE);
			float start = (float) Math.max(0, pos - SWEEP_SHARE);
			float end = (float) Math.min(100, pos);
			if (end > start) fillBar(g, innerX + start / 100f * innerW, innerX + end / 100f * innerW, innerY);
		} else {
			float p = Math.max(0, Math.min(100, Math.round(pct)));
			fillBar(g, innerX, innerX + p / 100f * innerW, innerY);
		}

		// .pg-exit: 168x36, centred, only when the vanilla screen allows closing
		if (exitBox != null) {
			boolean hover = exitBox.contains(mx, my);
			Draw.round(g, exitBox.x(), exitBox.y(), exitBox.w(), exitBox.h(), 10, hover ? CARD_HOVER : CARD);
			Draw.outline(g, exitBox.x(), exitBox.y(), exitBox.w(), exitBox.h(), 10, hover ? CARD_HOVER_EDGE : CARD_EDGE, 2);
			float w = RenderUtil.textWidth("Exit", false, 14, null);
			RenderUtil.drawText(g, "Exit", exitBox.x() + (exitBox.w() - w) / 2f, exitBox.y() + 18 - 7, 14, false, null, TEXT);
		}
		String tip = net.glacier.module.impl.LoadingTips.current();
		if (!tip.isEmpty()) RenderUtil.drawTextCentered(g, RenderUtil.ellipsize(tip, cardW, false, 13, null), cardX + cardW / 2f, cardY + cardH + 22f, 13, null, 0xFFB8C0CC);
		pose.popMatrix();
	}

	private static void fillBar(GuiGraphics g, float x0, float x1, float y) {
		int a = Math.round(x0), b = Math.round(x1);
		if (b > a) Draw.round(g, a, Math.round(y), b - a, 10, 5, ACCENT);
	}

	/** Scale and the card box in css px of the viewport (ceil(window / scale)); the card is centred in it. */
	private void layout() {
		s = scaleFor(width, height);
		float vw = (float) Math.ceil(width / s), vh = (float) Math.ceil(height / s);
		canClose = source.shouldCloseOnEsc();
		cardW = Math.min(CARD_W, CARD_MAX_SHARE * vw);
		cardH = canClose ? 232f : 182f;
		cardX = (vw - cardW) / 2f;
		cardY = (vh - cardH) / 2f;
		if (canClose) {
			int ix = Math.round(cardX) + 22;
			int cw = Math.round(cardW) - 44;
			int ty = Math.round(cardY) + 20 + 72 + 14 + 28 + 14;
			exitBox = new Box(ix + Math.round((cw - EXIT_W) / 2f), ty + 14 + 14, (int) EXIT_W, (int) EXIT_H);
		} else {
			exitBox = null;
		}
	}

	/** Same scale rule as WebPage: the design height fits the window, snapped to quarter steps. */
	static float scaleFor(int w, int h) {
		float v = Math.min(h / DESIGN_H, w / MIN_W);
		return v < 0.5f ? Math.max(0.1f, v) : (float) Math.floor(v / STEP) * STEP;
	}

	/** The spinner's opacity: the old page stepped it between full and 55% over 0.9 s, the same curve here. */
	private static float spinOpacity() {
		float phase = (System.currentTimeMillis() % SPIN_CYCLE_MS) / (float) SPIN_CYCLE_MS;
		float a = phase < 0.5f ? 1 - 0.9f * (phase * 2) : 0.1f + 0.9f * ((phase - 0.5f) * 2);
		return 0.55f + 0.45f * a;
	}

	/** Trims text with a trailing "..." so it fits in maxW screen px. */
	private static String ellipsize(String text, int maxW, float k) {
		if (Draw.textWidth(text, k) <= maxW) return text;
		String t = text;
		while (!t.isEmpty() && Draw.textWidth(t + "...", k) > maxW) t = t.substring(0, t.length() - 1);
		return t + "...";
	}

	/**
	 * Reads the vanilla progress and title. Keeps the last values when the game throws, the same as the old wrapper.
	 * Control characters in game text become spaces.
	 */
	private void readProgress() {
		try {
			int p = -1;
			String t = "Loading";
			if (source instanceof MixinLevelLoadingAccessor level) {
				LevelLoadTracker tracker = level.glacier$loadTracker();
				if (tracker != null && tracker.hasProgress()) p = Math.round(tracker.serverProgress() * 100f);
				t = "Loading world";
			} else if (source instanceof MixinProgressScreenAccessor progress) {
				p = progress.glacier$progress();
				Component stage = progress.glacier$stage();
				Component header = progress.glacier$header();
				if (stage != null) t = stage.getString();
				else if (header != null) t = header.getString();
				else t = "Saving world";
			}
			pct = p;
			title = CONTROL.matcher(t).replaceAll(" ");
		} catch (RuntimeException e) {
			// keep the previous values
		}
	}

	/** The vanilla LevelLoadingScreen closes itself in tick() once the level is ready; this wrapper is what the game sees. */
	@Override
	public void tick() {
		if (source instanceof MixinLevelLoadingAccessor level && this.minecraft != null) {
			LevelLoadTracker tracker = level.glacier$loadTracker();
			if (tracker != null && tracker.isLevelReady()) {
				this.minecraft.setScreen(null);
			}
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		layout();
		if (event.button() == 0 && exitBox != null && exitBox.contains(event.x() / s, event.y() / s)) {
			closeSource();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) closeSource();
		return true;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		return true;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return source.shouldCloseOnEsc();
	}

	@Override
	public boolean isPauseScreen() {
		return source.isPauseScreen();
	}

	/** A screen rectangle in whole pixels. */
	private record Box(int x, int y, int w, int h) {
		boolean contains(double mx, double my) {
			return mx >= x && my >= y && mx < x + w && my < y + h;
		}
	}
}
