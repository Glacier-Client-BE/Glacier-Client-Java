package net.glacier.ui.screen;

import net.glacier.config.Config;
import net.glacier.hud.HudModule;
import net.glacier.module.ModuleManager;
import net.glacier.ui.Draw;
import net.glacier.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Drag enabled HUD elements to move them, scroll over one to scale it, Esc returns to the parent screen. */
public class HudEditorScreen extends Screen {
	private static final int DIM = 0x70000000;

	private final Screen parent;
	private final Dragger dragger = new Dragger();

	public HudEditorScreen(Screen parent) {
		super(Component.literal("HUD Editor"));
		this.parent = parent;
	}

	/**
	 * Hit-test, drag, snap, scale and draw logic for enabled HUD modules, shared by the HUD editor and the Right
	 * Shift quick screen. Positions are stored by HudModule as clamped 0..1 fractions.
	 */
	public static final class Dragger {
		public static final int GRID = 8;
		/** Drag snap in screen px. Kept finer than the visual grid so placement stays precise. */
		public static final int SNAP = 4;
		private static final int GRID_LINE = 0x14FFFFFF;
		private static final int AXIS_LINE = 0x30FFFFFF;
		private static final int IDLE_BORDER = 0x55FFFFFF;

		private HudModule dragging;
		private int dragDx;
		private int dragDy;
		/** Set by drags and scales; the config is written once on release or close, not on every wheel notch. */
		private boolean dirty;

		public HudModule dragging() {
			return dragging;
		}

		/** Topmost enabled module under the point, or null. */
		public HudModule moduleAt(double mx, double my, int sw, int sh) {
			List<HudModule> list = ModuleManager.hud();
			for (int i = list.size() - 1; i >= 0; i--) {
				HudModule h = list.get(i);
				if (!h.isEnabled()) continue;
				int px = h.screenX(sw);
				int py = h.screenY(sh);
				if (mx >= px && mx < px + h.pixelW() && my >= py && my < py + h.pixelH()) return h;
			}
			return null;
		}

		/** Starts dragging the module under the point; false when there is none. */
		public boolean begin(double mx, double my, int sw, int sh) {
			HudModule h = moduleAt(mx, my, sw, sh);
			if (h == null) return false;
			dragging = h;
			dragDx = (int) mx - h.screenX(sw);
			dragDy = (int) my - h.screenY(sh);
			return true;
		}

		public boolean drag(double mx, double my, int sw, int sh, boolean snap) {
			if (dragging == null) return false;
			int nx = (int) mx - dragDx;
			int ny = (int) my - dragDy;
			if (snap) {
				nx = snap(nx);
				ny = snap(ny);
			}
			dragging.setScreenPos(nx, ny, sw, sh);
			dirty = true;
			return true;
		}

		/** Ends a drag and saves; false when nothing was being dragged. */
		public boolean release() {
			if (dragging == null) return false;
			dragging = null;
			saveIfDirty();
			return true;
		}

		/** Writes the config if a drag or scale changed anything since the last save. */
		public void saveIfDirty() {
			if (!dirty) return;
			dirty = false;
			Config.save();
		}

		public boolean scale(double mx, double my, int sw, int sh, double scrollY) {
			HudModule h = moduleAt(mx, my, sw, sh);
			if (h == null) return false;
			h.setScale(h.scale() + (float) scrollY * 0.1f);
			dirty = true;
			return true;
		}

		/** Draws every enabled module with an outline; the hovered one gets the accent. Returns the hovered one. */
		public HudModule renderModules(GuiGraphics g, int sw, int sh, int mouseX, int mouseY, boolean idleOutline) {
			HudModule hover = dragging != null ? dragging : moduleAt(mouseX, mouseY, sw, sh);
			List<HudModule> list = ModuleManager.hud();
			for (int i = 0, n = list.size(); i < n; i++) {
				HudModule h = list.get(i);
				if (!h.isEnabled()) continue;
				int px = h.screenX(sw);
				int py = h.screenY(sh);
				try {
					h.renderAt(g, px, py);
				} catch (RuntimeException ignored) {
				}
				if (h == hover) {
					Draw.outline(g, px - 2, py - 2, h.pixelW() + 4, h.pixelH() + 4, Theme.R_SM, Theme.ACCENT, 1);
				} else if (idleOutline) {
					Draw.outline(g, px - 2, py - 2, h.pixelW() + 4, h.pixelH() + 4, Theme.R_SM, IDLE_BORDER, 1);
				}
			}
			return hover;
		}

		public static void drawGrid(GuiGraphics g, int sw, int sh) {
			for (int x = 0; x < sw; x += GRID) g.fill(x, 0, x + 1, sh, GRID_LINE);
			for (int y = 0; y < sh; y += GRID) g.fill(0, y, sw, y + 1, GRID_LINE);
			g.fill(sw / 2, 0, sw / 2 + 1, sh, AXIS_LINE);
			g.fill(0, sh / 2, sw, sh / 2 + 1, AXIS_LINE);
		}

		private static int snap(int v) {
			return Math.round(v / (float) SNAP) * SNAP;
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, width, height, DIM);
		Dragger.drawGrid(g, width, height);
		HudModule hover = dragger.renderModules(g, width, height, mouseX, mouseY, true);

		Draw.centered(g, "HUD Editor", width / 2, 8, 1f, Theme.TEXT);
		Draw.centered(g, "Drag to move   |   Scroll to scale   |   Esc to finish", width / 2, 20, 1f, Theme.DIM);

		if (hover != null) {
			String label = hover.name() + "  " + String.format("%.2fx", hover.scale());
			Draw.text(g, label, hover.screenX(width), hover.screenY(height) - 12, 1f, Theme.TEXT);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0 && dragger.begin(event.x(), event.y(), width, height)) return true;
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		return dragger.drag(event.x(), event.y(), width, height, true) || super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		return dragger.release() || super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		return dragger.scale(mouseX, mouseY, width, height, scrollY)
				|| super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void onClose() {
		dragger.saveIfDirty();
		minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
