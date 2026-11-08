package net.glacier.ui.screen;

import net.glacier.GlacierClient;
import net.glacier.web.JsBridge;
import net.glacier.web.Node;
import net.glacier.web.RenderUtil;
import net.glacier.web.StartApi;
import net.glacier.web.WebApi;
import net.glacier.web.WebView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Map;

/** The Glacier start screen (start.html), shown instead of the vanilla title screen. Fills the whole window. */
public class StartScreen extends Screen {
	/** Design height in css px; the page scales with the window height. */
	private static final float DESIGN_H = 540f;
	private static final float MIN_W = 480f;
	private static final float STEP = 0.25f;

	private final WebView view;
	private final JsBridge bridge;
	private int fitW = -1, fitH = -1;
	private float scale = 1f;
	private int viewW, viewH;

	/** Panorama: 1920x360 texture, cover-fitted to the window and panned across it once every 120 s. */
	private static final Identifier PANORAMA = Identifier.fromNamespaceAndPath("glacier", "textures/start/panorama.png");
	private static final long PAN_MS = 120_000L;
	private static final int BG = 0xFF101214;
	/** Flat dim over the panorama, then a stepped vignette on the top and bottom edges. */
	private static final int DIM = 0x59000000;
	private static final int VIGNETTE_STEPS = 8;
	private static final int VIGNETTE_MAX = 0x99;
	private static final float VIGNETTE_BAND = 0.2f;
	private final long startNs = System.nanoTime();

	public StartScreen() {
		super(Component.literal("Glacier"));
		view = new WebView(asset("start.html"), asset("start.css"));
		bridge = new JsBridge(view);
		new WebApi(this).install(bridge);
		new StartApi(this).install(bridge);
		bridge.loadScripts("start.js");
	}

	private static String asset(String name) {
		String text = JsBridge.readAsset(name);
		if (text == null) {
			GlacierClient.LOGGER.error("[ui] missing web asset {}", name);
			return "";
		}
		return text;
	}

	/** Recomputes the scale only when the window size changed. */
	private void fit() {
		if (fitW == width && fitH == height) return;
		fitW = width;
		fitH = height;
		float s = Math.min(height / DESIGN_H, width / MIN_W);
		scale = s < 0.5f ? Math.max(0.1f, s) : (float) Math.floor(s / STEP) * STEP;
		viewW = (int) Math.ceil(width / scale);
		viewH = (int) Math.ceil(height / scale);
		view.setSize(viewW, viewH);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		fit();
		drawBackground(g);
		bridge.tick();
		var pose = g.pose();
		pose.pushMatrix();
		pose.scale(scale, scale);
		view.render(g, Math.round(mouseX / scale), Math.round(mouseY / scale));
		pose.popMatrix();
	}

	/**
	 * The background is drawn natively in window pixels, behind the page (whose own background is transparent).
	 * The panorama is cropped to the window's aspect ratio, so it always fills the window, and its horizontal
	 * position follows the wall clock, so the pan speed does not depend on the frame rate.
	 */
	private void drawBackground(GuiGraphics g) {
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
		g.fill(0, 0, width, height, DIM);
		vignette(g);
	}

	/** Darkens the top and bottom edges in steps. The bands are disjoint, so each row is filled once. */
	private void vignette(GuiGraphics g) {
		int band = Math.max(1, (int) (height * VIGNETTE_BAND) / VIGNETTE_STEPS);
		for (int i = 0; i < VIGNETTE_STEPS; i++) {
			int c = (VIGNETTE_MAX * (VIGNETTE_STEPS - i) / VIGNETTE_STEPS) << 24;
			int y = i * band;
			g.fill(0, y, width, y + band, c);
			g.fill(0, height - y - band, width, height - y, c);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
		fit();
		return view.mouseClicked(e.x() / scale, e.y() / scale, e.button());
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent e) {
		fit();
		return view.mouseReleased(e.x() / scale, e.y() / scale, e.button());
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
		fit();
		return view.mouseDragged(e.x() / scale, e.y() / scale, e.button());
	}

	@Override
	public boolean mouseScrolled(double x, double y, double sx, double sy) {
		fit();
		return view.mouseScrolled(x / scale, y / scale, sy);
	}

	/** Keys go to the page (Esc closes an open overlay); the screen itself never closes on Esc. */
	@Override
	public boolean keyPressed(KeyEvent event) {
		Node top = view.root().firstElement();
		if (top != null) bridge.fire(top, "keydown", Map.<String, Object>of("key", event.key()));
		return true;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
