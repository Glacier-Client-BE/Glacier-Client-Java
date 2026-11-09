package net.glacier.ui.screen;

import net.glacier.GlacierClient;
import net.glacier.config.Config;
import net.glacier.web.JsBridge;
import net.glacier.web.Node;
import net.glacier.web.WebApi;
import net.glacier.web.WebView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.glacier.compat.GLFW;

/**
 * Right Shift screen, like the Glacier DLL: the live HUD stays visible and every HUD module can be dragged (and
 * scaled with the wheel), with the web quick bar (logo, Back / MODS / Grid) centred on top. Clicks on a quick bar
 * button go to the page, everything else drives the HUD drag. Right Shift or Esc closes.
 */
public class HudQuickScreen extends Screen {
	/** Quick bar height in css px: its scale follows the screen height (same as WebScreen). */
	private static final float BAR_H = 420f;
	private static final float MAX_SCALE = 4f;
	private static final float STEP = 0.25f;

	private final WebView view;
	private final JsBridge bridge;
	private final HudEditorScreen.Dragger dragger = new HudEditorScreen.Dragger();
	private Node gridButton;
	private boolean webPressed;

	private float scale = 1f;
	private int originX;
	private int originY;
	private int fitW = -1;
	private int fitH = -1;

	public HudQuickScreen() {
		super(Component.literal("Glacier"));
		view = new WebView(asset("quickbar.html"), asset("quickbar.css"));
		bridge = new JsBridge(view);
		new WebApi(this).install(bridge);
		bridge.loadScripts("quickbar.js");
	}

	private static String asset(String name) {
		String text = JsBridge.readAsset(name);
		if (text == null) {
			GlacierClient.LOGGER.error("[ui] missing web asset {}", name);
			return "";
		}
		return text;
	}

	/** Recomputes the page placement only when the screen size changes. */
	private void fit() {
		if (fitW == width && fitH == height) return;
		fitW = width;
		fitH = height;
		float s = Math.min(Math.min(height / BAR_H, MAX_SCALE), Math.min(width / 320f, height / 240f));
		scale = s < 0.5f ? Math.max(0.1f, s) : (float) Math.floor(s / STEP) * STEP;
		int lw = (int) Math.floor(width / scale);
		int lh = (int) Math.floor(height / scale);
		originX = Math.round((width - lw * scale) / 2f);
		originY = Math.round((height - lh * scale) / 2f);
		view.setSize(lw, lh);
	}

	private double lx(double sx) {
		return (sx - originX) / scale;
	}

	private double ly(double sy) {
		return (sy - originY) / scale;
	}

	/** True when the point is over a quick bar button (or anything inside one). */
	private boolean overButton(double sx, double sy) {
		fit();
		for (Node n = view.hit(lx(sx), ly(sy)); n != null; n = n.parent) {
			if (n.hasClass("qb-btn")) return true;
		}
		return false;
	}

	/** The Grid toggle state, as shown by the page (quickbar.js marks #qb-grid active). */
	private boolean gridOn() {
		if (gridButton == null) {
			view.root().walk(n -> {
				if ("qb-grid".equals(n.id)) gridButton = n;
			});
		}
		return gridButton != null && gridButton.hasClass("active");
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		fit();
		bridge.tick();
		boolean grid = gridOn();
		if (grid) HudEditorScreen.Dragger.drawGrid(g, width, height);
		boolean onBar = dragger.dragging() == null && overButton(mouseX, mouseY);
		dragger.renderModules(g, width, height, onBar ? -1 : mouseX, onBar ? -1 : mouseY, grid);

		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(originX, originY);
		pose.scale(scale, scale);
		view.render(g, (int) Math.round(lx(mouseX)), (int) Math.round(ly(mouseY)));
		pose.popMatrix();
	}

	/** No blur or dim: the game and the HUD stay fully visible. */
	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (overButton(event.x(), event.y())) {
			webPressed = true;
			return view.mouseClicked(lx(event.x()), ly(event.y()), net.glacier.compat.Mouse.button(event));
		}
		return net.glacier.compat.Mouse.button(event) == 0 && dragger.begin(event.x(), event.y(), width, height);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		return dragger.drag(event.x(), event.y(), width, height, gridOn());
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragger.release()) return true;
		if (!webPressed) return false;
		webPressed = false;
		fit();
		return view.mouseReleased(lx(event.x()), ly(event.y()), net.glacier.compat.Mouse.button(event));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		return dragger.scale(mouseX, mouseY, width, height, scrollY);
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
}
