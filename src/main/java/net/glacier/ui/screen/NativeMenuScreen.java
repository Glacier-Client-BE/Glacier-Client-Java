package net.glacier.ui.screen;

import net.glacier.config.Config;
import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.module.ModuleManager;
import net.glacier.module.impl.CosmeticsModule;
import net.glacier.module.impl.EditorsModule;
import net.glacier.module.impl.ElementsModule;
import net.glacier.module.impl.PerformanceMods;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.ColorSetting;
import net.glacier.setting.Setting;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Draw;
import net.glacier.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Glacier mod menu as plain Java widgets drawn straight onto GuiGraphics: no HTML, CSS or script engine.
 * It is immediate mode: every frame the visible rows are laid out and their click regions recorded, so there is no
 * widget tree to keep in sync and nothing is drawn or allocated for rows that are scrolled out of view.
 */
public class NativeMenuScreen extends Screen {
	private static final int[] PALETTE = {
			0xFFFFFFFF, 0xFFF04747, 0xFFFAA61A, 0xFFFFFF55, 0xFF43B581, 0xFF55FFFF, 0xFF7289DA, 0xFFB57BFF, 0xFF000000};
	private static final String[] TABS = {"Modules", "Elements", "Editors", "Cosmetics", "Performance"};
	private static final int SIDE = 104, ROW = 24, GAP = 4, PAD = 10;

	private static int lastTab;

	private int tab;
	private int catIndex = -1;
	private double scroll;
	private final Set<String> expanded = new HashSet<>();
	private final List<Hit> hits = new ArrayList<>();
	private int px, py, pw, ph, cx, cy, cw, ch, contentH;
	private SliderSetting dragging;
	private int dragX, dragW;

	private record Hit(int x, int y, int w, int h, Runnable click, Runnable rightClick) {
		boolean in(double mx, double my) {
			return mx >= x && my >= y && mx < x + w && my < y + h;
		}
	}

	public NativeMenuScreen() {
		super(Component.literal("Glacier"));
		tab = lastTab;
	}

	/** Opens the menu on the named tab (for example "cosmetics"), or on the last used tab when null. */
	public static NativeMenuScreen open(String tabName) {
		NativeMenuScreen s = new NativeMenuScreen();
		if (tabName != null) {
			for (int i = 0; i < TABS.length; i++) {
				if (TABS[i].equalsIgnoreCase(tabName)) s.tab = i;
			}
		}
		return s;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		lastTab = tab;
		Config.save();
		super.onClose();
	}

	// ------------------------------------------------------------------ render

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		hits.clear();
		pw = Math.min(660, width - 24);
		ph = Math.min(400, height - 24);
		px = (width - pw) / 2;
		py = (height - ph) / 2;
		g.fill(0, 0, width, height, 0x88000000);
		Draw.round(g, px, py, pw, ph, 8, Theme.PANEL);

		drawSidebar(g, mx, my);

		cx = px + SIDE + PAD;
		cy = py + PAD;
		cw = pw - SIDE - PAD * 2;
		ch = ph - PAD * 2;
		g.enableScissor(cx, cy, cx + cw, cy + ch);
		int top = cy - (int) scroll;
		int end = switch (tab) {
			case 0 -> drawModules(g, top, mx, my);
			case 1 -> drawSettings(g, ModuleManager.get(ElementsModule.class), top, mx, my);
			case 2 -> drawSettings(g, ModuleManager.get(EditorsModule.class), top, mx, my);
			case 3 -> drawSettings(g, ModuleManager.get(CosmeticsModule.class), top, mx, my);
			default -> drawSettings(g, ModuleManager.get(PerformanceMods.class), top, mx, my);
		};
		contentH = end - top;
		g.disableScissor();
		scroll = Math.max(0, Math.min(scroll, Math.max(0, contentH - ch)));
	}

	private void drawSidebar(GuiGraphics g, int mx, int my) {
		Draw.text(g, "Glacier", px + PAD + 2, py + PAD, Theme.ACCENT);
		int y = py + PAD + 22;
		for (int i = 0; i < TABS.length; i++) {
			final int idx = i;
			boolean on = i == tab;
			boolean hover = mx >= px + 6 && mx < px + SIDE - 2 && my >= y && my < y + ROW;
			if (on) Draw.round(g, px + 6, y, SIDE - 8, ROW, 6, Theme.ACCENT_BG);
			else if (hover) Draw.round(g, px + 6, y, SIDE - 8, ROW, 6, Theme.ITEM_HOVER);
			Draw.text(g, TABS[i], px + 14, y + 8, on ? Theme.TEXT : Theme.DIM);
			hits.add(new Hit(px + 6, y, SIDE - 8, ROW, () -> {
				tab = idx;
				scroll = 0;
			}, null));
			y += ROW + 2;
		}
	}

	// ----------------------------------------------------------------- modules

	private int drawModules(GuiGraphics g, int y, int mx, int my) {
		Category[] cats = {Category.HUD, Category.RENDER, Category.MISC};
		int x = chip(g, "All", cx, y, catIndex < 0, mx, my, () -> catIndex = -1);
		for (int i = 0; i < cats.length; i++) {
			final int idx = i;
			x = chip(g, cats[i].display, x, y, catIndex == i, mx, my, () -> catIndex = idx);
		}
		y += 18 + GAP;
		for (Module m : ModuleManager.all()) {
			if (m.category() == Category.SYSTEM) continue;
			if (catIndex >= 0 && m.category() != cats[catIndex]) continue;
			y = drawModule(g, m, y, mx, my);
		}
		return y + PAD;
	}

	private int chip(GuiGraphics g, String label, int x, int y, boolean on, int mx, int my, Runnable r) {
		int w = Draw.textWidth(label, 1f) + 16;
		boolean hover = mx >= x && mx < x + w && my >= y && my < y + 18;
		Draw.round(g, x, y, w, 18, 9, on ? Theme.ACCENT : hover ? Theme.CARD_BORDER : Theme.CARD);
		Draw.text(g, label, x + 8, y + 5, on ? Theme.TEXT : Theme.DIM);
		hits.add(new Hit(x, y, w, 18, () -> {
			r.run();
			scroll = 0;
		}, null));
		return x + w + 4;
	}

	private int drawModule(GuiGraphics g, Module m, int y, int mx, int my) {
		boolean open = expanded.contains(m.id());
		if (y + ROW >= cy && y <= cy + ch) {
			Draw.round(g, cx, y, cw, ROW, 6, Theme.CARD);
			Draw.text(g, m.name(), cx + 8, y + 8, Theme.TEXT);
			toggle(g, cx + cw - 34, y + 5, m.isEnabled());
			hits.add(new Hit(cx + cw - 38, y, 38, ROW, m::toggle, null));
			if (!m.settings().isEmpty()) {
				Draw.text(g, open ? "v" : ">", cx + cw - 54, y + 8, Theme.DIM);
				hits.add(new Hit(cx, y, cw - 40, ROW, () -> flip(m.id()), null));
			}
		}
		y += ROW + 2;
		if (open) y = drawSettingRows(g, m, y, mx, my, 10);
		return y;
	}

	private void flip(String id) {
		if (!expanded.remove(id)) expanded.add(id);
	}

	private int drawSettings(GuiGraphics g, Module m, int y, int mx, int my) {
		if (m == null) return y;
		return drawSettingRows(g, m, y, mx, my, 0) + PAD;
	}

	// ---------------------------------------------------------------- settings

	private int drawSettingRows(GuiGraphics g, Module m, int y, int mx, int my, int indent) {
		int x = cx + indent, w = cw - indent;
		for (Setting<?> s : m.settings()) {
			if (s.isHidden()) continue;
			if (y + ROW >= cy && y <= cy + ch) drawSetting(g, s, x, y, w, mx);
			y += ROW;
		}
		return y;
	}

	private void drawSetting(GuiGraphics g, Setting<?> s, int x, int y, int w, int mx) {
		Draw.text(g, s.getName(), x + 8, y + 8, Theme.DIM);
		int rx = x + w;
		if (s instanceof ToggleSetting t) {
			toggle(g, rx - 34, y + 5, t.on());
			hits.add(new Hit(rx - 120, y, 120, ROW, t::flip, null));
		} else if (s instanceof SliderSetting sl) {
			int tw = Math.min(120, w / 2), tx = rx - tw - 44;
			int fw = (int) (tw * sl.fraction());
			Draw.round(g, tx, y + 10, tw, 4, 2, Theme.TRACK_OFF);
			Draw.round(g, tx, y + 10, Math.max(4, fw), 4, 2, Theme.ACCENT);
			Draw.round(g, tx + fw - 4, y + 7, 9, 10, 4, Theme.TEXT);
			String v = sl.step() >= 1 ? String.valueOf(sl.i()) : String.format("%.2f", sl.get());
			Draw.text(g, v, rx - 38, y + 8, Theme.TEXT);
			final int ftx = tx, ftw = tw;
			hits.add(new Hit(tx - 4, y, tw + 8, ROW, () -> {
				dragging = sl;
				dragX = ftx;
				dragW = ftw;
				sl.setFraction((mx - ftx) / (double) ftw);
			}, null));
		} else if (s instanceof ChoiceSetting c) {
			String label = c.get();
			int bw = Draw.textWidth(label, 1f) + 16;
			Draw.round(g, rx - bw, y + 3, bw, 18, 9, Theme.CARD_BORDER);
			Draw.text(g, label, rx - bw + 8, y + 8, Theme.TEXT);
			hits.add(new Hit(rx - bw, y, bw, ROW, c::next,
					() -> c.setIndex(c.index() == 0 ? c.options().size() - 1 : c.index() - 1)));
		} else if (s instanceof ColorSetting col) {
			Draw.round(g, rx - 26, y + 4, 26, 16, 5, col.argb() | 0xFF000000);
			hits.add(new Hit(rx - 26, y, 26, ROW, () -> col.set(nextColor(col.argb())), null));
		}
	}

	private static int nextColor(int argb) {
		for (int i = 0; i < PALETTE.length; i++) {
			if ((PALETTE[i] & 0xFFFFFF) == (argb & 0xFFFFFF)) return PALETTE[(i + 1) % PALETTE.length];
		}
		return PALETTE[0];
	}

	private void toggle(GuiGraphics g, int x, int y, boolean on) {
		Draw.round(g, x, y, 28, 14, 7, on ? Theme.ACCENT : Theme.TRACK_OFF);
		Draw.round(g, on ? x + 15 : x + 2, y + 2, 11, 10, 5, Theme.TEXT);
	}

	// ------------------------------------------------------------------- input

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
		for (int i = hits.size() - 1; i >= 0; i--) {
			Hit h = hits.get(i);
			if (!h.in(e.x(), e.y())) continue;
			// Content hits only count inside the scrolled viewport.
			if (h.x() >= cx && (e.y() < cy || e.y() >= cy + ch)) continue;
			Runnable r = e.button() == 1 && h.rightClick() != null ? h.rightClick() : h.click();
			r.run();
			return true;
		}
		return super.mouseClicked(e, dbl);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
		if (dragging != null) {
			dragging.setFraction((e.x() - dragX) / (double) dragW);
			return true;
		}
		return super.mouseDragged(e, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent e) {
		dragging = null;
		return super.mouseReleased(e);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy) {
		scroll = Math.max(0, Math.min(scroll - sy * 24, Math.max(0, contentH - ch)));
		return true;
	}
}
