package net.glacier.ui.screen;

import net.glacier.config.Config;
import net.glacier.ui.screen.pages.CosmeticsPage;
import net.glacier.ui.screen.pages.EditorsPage;
import net.glacier.ui.screen.pages.ElementsPage;
import net.glacier.ui.screen.pages.MenuKit;
import net.glacier.ui.screen.pages.ModulesPage;
import net.glacier.ui.screen.pages.MusicPage;
import net.glacier.ui.screen.pages.PerformancePage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.glacier.compat.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The Glacier mod menu as native Java, a 1:1 of menu.html / menu.css and the page scripts. Everything is laid out in
 * css px and drawn through the pose stack at the same snapped scale WebScreen uses (the menu fills about 85% of the
 * screen), so every width, gap, radius and font size is the number from the stylesheets. Immediate mode: each frame
 * records the click regions of what it drew.
 */
public class NativeMenuScreen extends Screen {
	private static final String[][] TABS = {{"modules", "Modules", "f009"}, {"elements", "Elements", "f0c9"},
			{"editors", "Editors", "f044"}, {"music", "Music", "f001"}, {"cosmetics", "Cosmetics", "f553"},
			{"performance", "Performance", "f3fd"}};
	private static final Identifier LOGO = Identifier.fromNamespaceAndPath("glacier", "textures/ui/logo.png");
	private static final float MENU_W = 960f, MENU_H = 600f, MENU_FILL = 0.85f, MIN_W = 320f, MIN_H = 240f;
	private static final float MAX_SCALE = 4f, STEP = 0.25f;
	private static final int DIM = 0x99000000;

	private static int lastTab;
	private static final long ANIM_MS = 170;

	private long animStart = System.currentTimeMillis();
	private long closeStart;
	private boolean closing;

	private int tab;
	// static so the menu reopens exactly where it was left (tab, scroll, search, selected group, cosmetics tab)
	private static final float[] scrolls = new float[TABS.length];
	private static final float[] contents = new float[TABS.length];
	private float hostH = 540;
	private final List<MenuKit.Hit> hits = new ArrayList<>();
	private MenuKit.Hit active;
	private static final ModulesPage modules = new ModulesPage();
	private static final ElementsPage elements = new ElementsPage();
	private static final EditorsPage editors = new EditorsPage(() -> Minecraft.getInstance().gui.setScreen(new HudEditorScreen(Minecraft.getInstance().gui.screen())));
	private static final MusicPage music = new MusicPage();
	private static final CosmeticsPage cosmetics = new CosmeticsPage();
	private static final PerformancePage performance = new PerformancePage();

	/** Page placement of the current screen size: scale, origin and logical viewport (WebScreen.Fit). */
	private float scale = 1f;
	private int ox, oy, lw = 960, lh = 600;
	private float px, py, pw = 960, ph = 600;

	public NativeMenuScreen() {
		super(Component.literal("Glacier"));
		tab = lastTab;
	}

	/** Opens the menu on the named tab (for example "cosmetics"), or on the last used tab when null. */
	public static NativeMenuScreen open(String tabName) {
		NativeMenuScreen s = new NativeMenuScreen();
		if (tabName != null) {
			for (int i = 0; i < TABS.length; i++) {
				if (TABS[i][0].equalsIgnoreCase(tabName)) s.tab = i;
			}
		}
		s.enter(s.tab);
		return s;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		lastTab = tab;
		if (!closing) {
			closing = true;
			closeStart = System.currentTimeMillis();
		}
	}

	private void finishClose() {
		lastTab = tab;
		Config.save();
		net.glacier.module.impl.CosmeticsModule.clearPreview();
		super.onClose();
	}

	@Override
	public void removed() {
		net.glacier.module.impl.CosmeticsModule.clearPreview();
		super.removed();
	}

	private void enter(int t) {
		if (t == 1) elements.enter();
		else if (t == 5) performance.enter();
		else if (t == 0) modules.enter();
	}

	private void show(int t) {
		modules.closeSheet();
		if (t == tab) return;
		tab = t;
		enter(t);
	}

	// ------------------------------------------------------------------ fit

	/** WebScreen.fit / pickScale: desired scale snapped to 0.25 steps, integer logical viewport, rounded origin. */
	private void fit() {
		float desired = Math.min(width * MENU_FILL / MENU_W, height * MENU_FILL / MENU_H);
		float s = Math.min(Math.min(desired, MAX_SCALE), Math.min(width / MIN_W, height / MIN_H));
		scale = s < 0.5f ? Math.max(0.1f, s) : (float) Math.floor(s / STEP) * STEP;
		lw = (int) Math.floor(width / scale);
		lh = (int) Math.floor(height / scale);
		ox = Math.round((width - lw * scale) / 2f);
		oy = Math.round((height - lh * scale) / 2f);
		pw = Math.min(MENU_W, lw);
		ph = Math.min(MENU_H, lh);
		px = (float) Math.floor((lw - pw) / 2f);
		py = (float) Math.floor((lh - ph) / 2f);
	}

	private double lx(double sx) {
		return (sx - ox) / scale;
	}

	private double ly(double sy) {
		return (sy - oy) / scale;
	}

	// ----------------------------------------------------------------- render

	/** The menu draws its own dim; skipping the vanilla blurred background saves a full-screen blur pass every frame. */
	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mxs, int mys, float partial) {
		fit();
		hits.clear();
		long now = System.currentTimeMillis();
		float t = closing ? 1f - (now - closeStart) / (float) ANIM_MS : (now - animStart) / (float) ANIM_MS;
		if (closing && t <= 0f) {
			finishClose();
			return;
		}
		t = Math.max(0f, Math.min(1f, t));
		float ease = 1f - (1f - t) * (1f - t) * (1f - t);
		g.fill(0, 0, width, height, ((int) (0x99 * ease) << 24));
		var pose = g.pose();
		pose.pushMatrix();
		float a = 0.94f + 0.06f * ease;
		pose.translate(width / 2f, height / 2f + (1f - ease) * 10f);
		pose.scale(a, a);
		pose.translate(-width / 2f, -height / 2f);
		pose.translate(ox, oy);
		pose.scale(scale, scale);
		MenuKit k = new MenuKit(g, lx(mxs), ly(mys), hits);

		// .panel: 1px border over #222326, radius 18
		k.round(px, py, pw, ph, 18, 0xFF222326);
		k.ring(px, py, pw, ph, 18, 0x14FFFFFF, 1);
		float x0 = px + 1, y0 = py + 1, iw = pw - 2, ih = ph - 2;
		header(k, x0, y0, iw);

		float hy = y0 + 58;
		hostH = ih - 58;
		float cw = iw - 48;
		if (tab == 0) {
			modules.draw(k, x0 + 24, hy + 16, cw, hostH - 36);
		} else {
			float sc = scrolls[tab];
			k.scissor(x0, hy, iw, hostH);
			k.setClip(hy, hy + hostH);
			float top = hy + 16 - sc;
			float end = switch (tab) {
				case 1 -> elements.draw(k, x0 + 24, top, cw);
				case 2 -> editors.draw(k, x0 + 24, top, cw);
				case 3 -> music.draw(k, x0 + 24, top, cw);
				case 4 -> cosmetics.draw(k, x0 + 24, top, cw);
				default -> performance.draw(k, x0 + 24, top, cw);
			};
			k.clearClip();
			k.endScissor();
			float content = end - top + 16 + 20;
			contents[tab] = content;
			scrolls[tab] = Math.max(0, Math.min(sc, Math.max(0, content - hostH)));
			k.scrollbar(x0 + iw, hy, hostH, content, scrolls[tab]);
		}
		if (tab == 0 && modules.sheetOpen()) modules.drawSheet(k, x0, y0, iw, ih);
		pose.popMatrix();
	}

	/** .header: padding 20 24 8, row of brand, tab pills, spacer and the round close button. */
	private void header(MenuKit k, float x0, float y0, float iw) {
		float cy = y0 + 20 + 15;
		float bx = x0 + 24;
		k.image(LOGO, bx, cy - 13, 26, 26, 0, 0, 256, 256, 256, 256);
		float nx = bx + 26 + 8;
		k.text("Glacier", nx, cy, 20, 0xFFFFFFFF, true);
		float vx = nx + MenuKit.width("Glacier", 20, true) + 8;
		float vw = MenuKit.width("v1.0.0", 10, false) + 14;
		k.box(vx, cy - 9.25f, vw, 18.5f, 5, 0x2443B581, 0x5943B581);
		k.text("v1.0.0", vx + 7, cy, 10, 0xFF43B581);

		float tx = vx + vw + 20 + 12;
		for (int i = 0; i < TABS.length; i++) {
			int cp = Integer.parseInt(TABS[i][2], 16);
			float icw = MenuKit.iconWidth(cp, 12), lw2 = MenuKit.width(TABS[i][1], 12, false);
			float tw = 24 + icw + 6 + lw2;
			boolean on = i == tab;
			boolean hov = k.hover(tx, cy - 15, tw, 30);
			k.round(tx, cy - 15, tw, 30, 8, on ? 0xFF6B7FE0 : hov ? 0x17FFFFFF : 0x0DFFFFFF);
			int fg = on || hov ? 0xFFFFFFFF : 0xFFD6DAE0;
			k.icon(cp, tx + 12 + icw / 2f, cy, 12, on ? 0xFFFFFFFF : 0xFFD6DAE0);
			k.text(TABS[i][1], tx + 12 + icw + 6, cy, 12, fg);
			final int idx = i;
			k.hit(tx, cy - 15, tw, 30, () -> show(idx));
			tx += tw + 8;
		}

		float cx = x0 + iw - 24 - 28;
		boolean hov = k.hover(cx, cy - 14, 28, 28);
		k.box(cx, cy - 14, 28, 28, 14, hov ? 0x1FFFFFFF : 0x0FFFFFFF, 0x1AFFFFFF);
		k.icon(0xF00D, cx + 14, cy, 13, 0xFFD6DAE0);
		k.hit(cx, cy - 14, 28, 28, this::onClose);
	}

	// ------------------------------------------------------------------- input

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
		double lx = lx(e.x()), ly = ly(e.y());
		modules.blur();
		for (int i = hits.size() - 1; i >= 0; i--) {
			MenuKit.Hit h = hits.get(i);
			if (!h.in(lx, ly)) continue;
			if (net.glacier.compat.Mouse.button(e) == 1 && h.right() != null) {
				h.right().run();
			} else {
				if (h.drag()) active = h;
				h.press().accept(lx);
			}
			return true;
		}
		return super.mouseClicked(e, dbl);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
		if (active != null) {
			active.press().accept(lx(e.x()));
			return true;
		}
		return super.mouseDragged(e, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent e) {
		if (active != null) {
			Runnable r = active.release();
			active = null;
			if (r != null) r.run();
			return true;
		}
		return super.mouseReleased(e);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy) {
		if (tab == 0) {
			boolean sheetBody = modules.sheetOpen() && lx(mx) >= px + 1 + (pw - 2) - 360;
			modules.scroll(sy, sheetBody);
			return true;
		}
		if (tab == 4 && cosmetics.scrollZoom(lx(mx), ly(my), sy)) return true;
		float max = Math.max(0, contents[tab] - hostH);
		scrolls[tab] = (float) Math.max(0, Math.min(scrolls[tab] - sy * 20, max));
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		if (tab == 0 && modules.key(e.key())) return true;
		if (e.key() == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}
		return super.keyPressed(e);
	}

	@Override
	public boolean charTyped(CharacterEvent e) {
		if (tab == 0 && e.isAllowedChatCharacter() && modules.typed(e.codepointAsString())) return true;
		return super.charTyped(e);
	}
}
