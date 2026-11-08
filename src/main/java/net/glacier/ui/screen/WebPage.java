package net.glacier.ui.screen;

import net.glacier.GlacierClient;
import net.glacier.web.JsBridge;
import net.glacier.web.Node;
import net.glacier.web.ScreenApi;
import net.glacier.web.WebApi;
import net.glacier.web.WebView;
import net.glacier.web.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

import java.util.Map;

/**
 * The HTML side of a page screen that is composed into a vanilla screen (pause, progress, play). It holds the
 * {@link WebView}, its {@link JsBridge} and a {@link ScreenApi}, and does the same scale-to-fit as
 * {@link StartScreen}: the design height is 540 css px, the scale is snapped to 0.25 steps, and the page is drawn
 * at a whole logical viewport. Backgrounds are drawn by the owning screen, not by the page.
 */
public final class WebPage {
	/** Design height in css px; the page scales with the window height. */
	private static final float DESIGN_H = 540f;
	private static final float MIN_W = 480f;
	private static final float STEP = 0.25f;

	/** Panorama behind the page screens: 1920x360, cover-fitted and panned once every 120 s. */
	private static final Identifier PANORAMA = Identifier.fromNamespaceAndPath("glacier", "textures/start/panorama.png");
	private static final long PAN_MS = 120_000L;
	private static final long START_NS = System.nanoTime();
	private static final int BG = 0xFF101214;
	private static final int DIM = 0x59000000;

	private final WebView view;
	private final JsBridge bridge;
	private final ScreenApi api;
	private int fitW = -1, fitH = -1;
	private float scale = 1f;

	/** Loads the page and runs the scripts in order. Missing assets are logged and the page still opens. */
	public WebPage(Screen owner, ScreenApi api, String html, String css, String... scripts) {
		this.api = api;
		this.view = new WebView(asset(html), asset(css));
		this.bridge = new JsBridge(view);
		new WebApi(owner).install(bridge);
		api.install(bridge);
		bridge.loadScripts(scripts);
	}

	private static String asset(String name) {
		String text = JsBridge.readAsset(name);
		if (text == null) {
			GlacierClient.LOGGER.error("[ui] missing web asset {}", name);
			return "";
		}
		return text;
	}

	/** Recomputes the scale and the logical viewport only when the window size changed. */
	private void fit(int w, int h) {
		if (fitW == w && fitH == h) return;
		fitW = w;
		fitH = h;
		float s = Math.min(h / DESIGN_H, w / MIN_W);
		scale = s < 0.5f ? Math.max(0.1f, s) : (float) Math.floor(s / STEP) * STEP;
		view.setSize((int) Math.ceil(w / scale), (int) Math.ceil(h / scale));
	}

	/** Draws the page over whatever the owner already drew. */
	public void render(GuiGraphics g, int mouseX, int mouseY, int w, int h) {
		fit(w, h);
		bridge.tick();
		var pose = g.pose();
		pose.pushMatrix();
		pose.scale(scale, scale);
		view.render(g, Math.round(mouseX / scale), Math.round(mouseY / scale));
		pose.popMatrix();
	}

	/** Runs a timer step for the page's setInterval callbacks. */
	public void tick() {
		bridge.tick();
	}

	/** Evaluates a script in the page's scope, such as a call into a page function. */
	public void run(String js) {
		bridge.eval(js, "page");
	}

	public boolean click(MouseButtonEvent e) {
		return view.mouseClicked(e.x() / scale, e.y() / scale, e.button());
	}

	public boolean release(MouseButtonEvent e) {
		return view.mouseReleased(e.x() / scale, e.y() / scale, e.button());
	}

	public boolean drag(MouseButtonEvent e) {
		return view.mouseDragged(e.x() / scale, e.y() / scale, e.button());
	}

	public boolean scroll(double x, double y, double dy) {
		return view.mouseScrolled(x / scale, y / scale, dy);
	}

	/**
	 * Sends keydown to the page's top-level element. Returns true when the page consumed Esc through
	 * glacier.screen.escape(); the owner then does not close.
	 */
	public boolean key(int key) {
		Node top = view.root().firstElement();
		if (top != null) bridge.fire(top, "keydown", Map.<String, Object>of("key", key));
		return api.takeEscape();
	}

	/** Sends a typed character to the page as keypress with detail char. */
	public void typed(String text) {
		Node top = view.root().firstElement();
		if (top != null) bridge.fire(top, "keypress", Map.<String, Object>of("char", text));
	}

	/**
	 * Draws the cover-fitted panorama across the window, panned by the wall clock, and a flat dim over it. The
	 * page sits on top of this, so the page itself has no background.
	 */
	public static void drawPanorama(GuiGraphics g, int w, int h) {
		g.fill(0, 0, w, h, BG);
		int[] nat = RenderUtil.imageSize(PANORAMA);
		if (nat != null && w > 0 && h > 0) {
			float aspect = w / (float) h;
			boolean wide = nat[0] / (float) nat[1] >= aspect;
			float uw = wide ? nat[1] * aspect : nat[0];
			float uh = wide ? nat[1] : nat[0] / aspect;
			float phase = ((System.nanoTime() - START_NS) / 1_000_000L % PAN_MS) / (float) PAN_MS;
			float u = (nat[0] - uw) * phase;
			float v = (nat[1] - uh) / 2f;
			RenderUtil.imageUv(g, PANORAMA, 0, 0, w, h, u, v, uw, uh, 1f);
		}
		g.fill(0, 0, w, h, DIM);
	}
}
