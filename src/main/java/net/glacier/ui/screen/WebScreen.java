package net.glacier.ui.screen;

import net.glacier.GlacierClient;
import net.glacier.config.Config;
import net.glacier.web.JsBridge;
import net.glacier.web.Node;
import net.glacier.web.WebApi;
import net.glacier.web.WebView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * A screen whose content is an HTML document (assets/glacier/web) laid out by {@link WebView} and driven by
 * JavaScript through {@link JsBridge} and the {@code glacier} global. The page is drawn at a snapped scale with an
 * integer logical viewport, centred on the screen. A top-level element with {@code data-dim} dims the game behind
 * it. Esc closes the screen and saves the config. The game is never paused.
 */
public class WebScreen extends Screen {
	private static final int DIM = 0x99000000;

	/** Menu panel size in css px, and the share of the screen it should take. */
	private static final float MENU_W = 960f;
	private static final float MENU_H = 600f;
	private static final float MENU_FILL = 0.85f;
	/** Quick bar height in css px: its scale follows the screen height. */
	private static final float QUICK_BAR_H = 420f;
	/** The logical viewport is never smaller than this, so the page scales down instead of clipping. */
	private static final float MIN_W = 320f;
	private static final float MIN_H = 240f;
	private static final float MAX_SCALE = 4f;
	private static final float STEP = 0.25f;

	private final WebView view;
	private final JsBridge bridge;
	/** Logical viewport of the last frame, for glacier.viewport(); 0 before the first frame. */
	private int viewW;
	private int viewH;
	/** Set by glacier.keyHandled() while a key event is being dispatched to JS. */
	private boolean keyHandled;
	/** The placement for the screen size fitW x fitH, reused by frames and mouse input until the size changes. */
	private Fit fit;
	private int fitW = -1;
	private int fitH = -1;

	/**
	 * The one mapping between screen and page space: the page is drawn at {@code scale} with its origin at
	 * ({@code x}, {@code y}) and a logical viewport of {@code w} x {@code h}. Drawing and mouse input both use it.
	 */
	private record Fit(float scale, int x, int y, int w, int h, boolean dim) {
	}

	/**
	 * Loads the page and runs the scripts in order. Missing resources are logged; the screen still opens with
	 * whatever loaded.
	 */
	public WebScreen(String title, String htmlResource, String[] cssResources, String... scripts) {
		super(Component.literal(title));
		String html = asset(htmlResource);
		StringBuilder css = new StringBuilder();
		for (String name : cssResources) css.append(asset(name)).append('\n');
		this.view = new WebView(html, css.toString());
		this.bridge = new JsBridge(view);
		new WebApi(this).install(bridge);
		bridge.loadScripts(scripts);
	}

	/** The main Glacier menu (menu.html, menu.css, menu.js), on its remembered tab. */
	public static WebScreen menu() {
		return menu(null);
	}

	/** The main Glacier menu opened on the menu.js tab {@code tab} (for example "cosmetics"), or on its default when null. */
	public static WebScreen menu(String tab) {
		WebScreen s = new WebScreen("Glacier", "menu.html",
				new String[]{"menu.css", "pages.css", "pages-extra.css"},
				"pages/modules.js", "pages/elements.js", "pages/performance.js", "pages/editors.js",
				"pages/music.js", "pages/cosmetics.js", "menu.js");
		if (tab != null) s.bridge.eval("window.menu.show('" + tab + "')", "menu-tab");
		return s;
	}

	/** The quick bar opened with Right Shift (quickbar.html, quickbar.css, quickbar.js). */
	public static WebScreen quickBar() {
		return new WebScreen("Glacier", "quickbar.html", new String[]{"quickbar.css"}, "quickbar.js");
	}

	private static String asset(String name) {
		if (name == null) return "";
		String text = JsBridge.readAsset(name);
		if (text == null) {
			GlacierClient.LOGGER.error("[ui] missing web asset {}", name);
			return "";
		}
		return text;
	}

	public int viewportWidth() {
		return viewW;
	}

	public int viewportHeight() {
		return viewH;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		Fit f = fit();
		viewW = f.w();
		viewH = f.h();
		view.setSize(f.w(), f.h());
		bridge.tick();
		if (f.dim()) g.fill(0, 0, this.width, this.height, DIM);
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(f.x(), f.y());
		pose.scale(f.scale(), f.scale());
		view.render(g, Math.round((float) logicalX(f, mouseX)), Math.round((float) logicalY(f, mouseY)));
		pose.popMatrix();
		WebView.drawProfile(g);
	}

	/**
	 * Computes the page placement. The menu fills about 85% of the screen, the quick bar scales with the screen
	 * height. The scale is snapped to 0.25 steps so text stays sharp, the logical viewport is whole css px, and the
	 * origin is rounded to whole screen px so the page is centred.
	 */
	private Fit fit() {
		if (fit != null && fitW == this.width && fitH == this.height) return fit;
		boolean dim = dimmed();
		float desired = dim
				? Math.min(this.width * MENU_FILL / MENU_W, this.height * MENU_FILL / MENU_H)
				: this.height / QUICK_BAR_H;
		float s = pickScale(desired);
		int lw = (int) Math.floor(this.width / s);
		int lh = (int) Math.floor(this.height / s);
		int x = Math.round((this.width - lw * s) / 2f);
		int y = Math.round((this.height - lh * s) / 2f);
		fit = new Fit(s, x, y, lw, lh, dim);
		fitW = this.width;
		fitH = this.height;
		return fit;
	}

	/**
	 * The desired scale, capped so the logical view stays at least 320x240, then floored to a 0.25 step when it is
	 * 0.5 or more. Below 0.5 the cap wins and the scale is left fractional, so the page shrinks instead of clipping.
	 */
	private float pickScale(float desired) {
		float s = Math.min(Math.min(desired, MAX_SCALE), Math.min(this.width / MIN_W, this.height / MIN_H));
		if (s < 0.5f) return Math.max(0.1f, s);
		return (float) Math.floor(s / STEP) * STEP;
	}

	private static double logicalX(Fit f, double screenX) {
		return (screenX - f.x()) / f.scale();
	}

	private static double logicalY(Fit f, double screenY) {
		return (screenY - f.y()) / f.scale();
	}

	/** True when the root or one of its top-level elements carries data-dim. */
	private boolean dimmed() {
		Node root = view.root();
		if (root.hasAttr("data-dim")) return true;
		for (Node c : root.children) {
			if (!c.isText() && c.hasAttr("data-dim")) return true;
		}
		return false;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		Fit f = fit();
		return view.mouseClicked(logicalX(f, event.x()), logicalY(f, event.y()), event.button());
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		Fit f = fit();
		return view.mouseReleased(logicalX(f, event.x()), logicalY(f, event.y()), event.button());
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		Fit f = fit();
		return view.mouseDragged(logicalX(f, event.x()), logicalY(f, event.y()), event.button());
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		Fit f = fit();
		return view.mouseScrolled(logicalX(f, mouseX), logicalY(f, mouseY), scrollY);
	}

	/** Called through glacier.keyHandled() by a JS keydown handler that consumed the key. */
	public void markKeyHandled() {
		keyHandled = true;
	}

	/**
	 * Key events go to JS first: to the focused element (bubbling up), or to the top-level element when nothing has
	 * focus. Esc closes the screen only when no handler called glacier.keyHandled().
	 */
	@Override
	public boolean keyPressed(KeyEvent event) {
		keyHandled = false;
		if (!view.keyPressed(event.key())) fireTop("keydown", "key", event.key());
		if (keyHandled) return true;
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	/** Typed characters reach JS as a "keypress" event on the top-level element with detail char. */
	@Override
	public boolean charTyped(CharacterEvent event) {
		keyHandled = false;
		if (event.isAllowedChatCharacter()) fireTop("keypress", "char", event.codepointAsString());
		return keyHandled || super.charTyped(event);
	}

	private void fireTop(String type, String key, Object value) {
		Node top = view.root().firstElement();
		if (top == null) return;
		Map<String, Object> detail = new HashMap<>();
		detail.put(key, value);
		bridge.fire(top, type, detail);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		Config.save();
		super.onClose();
	}
}
