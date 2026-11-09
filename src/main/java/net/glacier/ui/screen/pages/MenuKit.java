package net.glacier.ui.screen.pages;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.glacier.ui.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleConsumer;

/**
 * Per-frame drawing and hit-test context of the native mod menu. Everything is laid out in css px (the units of
 * menu.css) with float geometry; the screen applies the page scale through the pose stack, so sizes here are the
 * numbers from the stylesheets. Text follows the web engine: a run of font-size px is the Glacier UI font scaled by
 * px / 9, its glyph box is px tall and centred in a line box of 1.25 * px.
 */
public final class MenuKit {
	private static final RenderPipeline PIPE = RenderPipelines.GUI_TEXTURED;
	public static final float LINE = 1.25f;

	/** One click region. {@code press} receives the logical mouse x; a {@code drag} region keeps receiving it while held. */
	public record Hit(float x, float y, float w, float h, float clipTop, float clipBottom, DoubleConsumer press,
					  boolean drag, Runnable release, Runnable right) {
		public boolean in(double px, double py) {
			return px >= x && py >= y && px < x + w && py < y + h && py >= clipTop && py < clipBottom;
		}
	}

	public final GuiGraphicsExtractor g;
	public final double mx, my;
	public final List<Hit> hits;
	private float clipTop = -1e9f, clipBottom = 1e9f;

	public MenuKit(GuiGraphicsExtractor g, double mx, double my, List<Hit> hits) {
		this.g = g;
		this.mx = mx;
		this.my = my;
		this.hits = hits;
	}

	// ------------------------------------------------------------------ clip & hits

	/** Mouse and click regions only count inside [top, bottom) (the visible part of a scroll area). */
	public void setClip(float top, float bottom) {
		clipTop = top;
		clipBottom = bottom;
	}

	public void clearClip() {
		clipTop = -1e9f;
		clipBottom = 1e9f;
	}

	public void scissor(float x, float y, float w, float h) {
		g.enableScissor(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h));
	}

	public void endScissor() {
		g.disableScissor();
	}

	/** True when any part of [y, y + h) lies inside the current scroll clip. */
	public boolean visibleY(float y, float h) {
		return y + h > clipTop && y < clipBottom;
	}

	public boolean hover(float x, float y, float w, float h) {
		return mx >= x && my >= y && mx < x + w && my < y + h && my >= clipTop && my < clipBottom;
	}

	public void hit(float x, float y, float w, float h, Runnable click) {
		if (y + h < clipTop || y > clipBottom) return;
		hits.add(new Hit(x, y, w, h, clipTop, clipBottom, d -> click.run(), false, null, null));
	}

	public void hitRight(float x, float y, float w, float h, Runnable click, Runnable right) {
		if (y + h < clipTop || y > clipBottom) return;
		hits.add(new Hit(x, y, w, h, clipTop, clipBottom, d -> click.run(), false, null, right));
	}

	public void drag(float x, float y, float w, float h, DoubleConsumer press, Runnable release) {
		if (y + h < clipTop || y > clipBottom) return;
		hits.add(new Hit(x, y, w, h, clipTop, clipBottom, press, true, release, null));
	}

	/** A region that swallows clicks without doing anything (the scrim under an open sheet). */
	public void block(float x, float y, float w, float h, Runnable click) {
		hits.add(new Hit(x, y, w, h, -1e9f, 1e9f, d -> {
			if (click != null) click.run();
		}, false, null, null));
	}

	// ----------------------------------------------------------------------- shapes

	private static int r(float v) {
		return Math.round(v);
	}

	public void fill(float x, float y, float w, float h, int argb) {
		int x0 = r(x), y0 = r(y), x1 = r(x + w), y1 = r(y + h);
		if (x1 <= x0 || y1 <= y0 || (argb >>> 24) == 0) return;
		g.fill(x0, y0, x1, y1, argb);
	}

	public void round(float x, float y, float w, float h, float radius, int argb) {
		int x0 = r(x), y0 = r(y), x1 = r(x + w), y1 = r(y + h);
		if (x1 <= x0 || y1 <= y0 || (argb >>> 24) == 0) return;
		Draw.round(g, x0, y0, x1 - x0, y1 - y0, Math.round(radius), argb);
	}

	public void ring(float x, float y, float w, float h, float radius, int argb, int thickness) {
		int x0 = r(x), y0 = r(y), x1 = r(x + w), y1 = r(y + h);
		if (x1 <= x0 || y1 <= y0 || (argb >>> 24) == 0) return;
		Draw.outline(g, x0, y0, x1 - x0, y1 - y0, Math.round(radius), argb, thickness);
	}

	/** Background plus a 1px border drawn over it: the box model of every card, pill and input in the css. */
	public void box(float x, float y, float w, float h, float radius, int fill, int border) {
		round(x, y, w, h, radius, fill);
		if ((border >>> 24) != 0) ring(x, y, w, h, radius, border, 1);
	}

	/**
	 * css linear-gradient(135deg, c1, c2) in a rounded box. At 135deg the gradient position is (x + y) / (w + h), which is
	 * linear in both axes, so each vertical strip is one exact vertical gradient. Corners are cut per strip.
	 */
	public void gradient(float fx, float fy, float fw, float fh, float radius, int c1, int c2) {
		int x0 = r(fx), y0 = r(fy), w = r(fx + fw) - x0, h = r(fy + fh) - y0;
		if (w <= 0 || h <= 0) return;
		int rad = Math.max(0, Math.min((int) radius, Math.min(w, h) / 2));
		float total = w + h;
		int dx = 0;
		while (dx < w) {
			int edge = Math.min(dx, w - 1 - dx);
			int sw = (dx < rad || dx >= w - rad) ? 1 : Math.min(3, (w - rad) - dx);
			int inset = 0;
			if (edge < rad) {
				double d = rad - edge - 0.5;
				inset = (int) Math.round(rad - Math.sqrt(Math.max(0, (double) rad * rad - d * d)));
			}
			int top = y0 + inset, bot = y0 + h - inset;
			if (bot > top) {
				float mid = dx + sw / 2f;
				int ca = Draw.mix(c1, c2, mid / total);
				int cb = Draw.mix(c1, c2, (mid + h) / total);
				g.fillGradient(x0 + dx, top, x0 + dx + sw, bot, ca, cb);
			}
			dx += sw;
		}
	}

	/** Soft drop shadow approximated by a few translucent rounded layers (css box-shadow blur). */
	public void shadow(float x, float y, float w, float h, float radius, int argb, int spread) {
		float each = ((argb >>> 24) / 255f) / spread;
		for (int i = spread; i >= 1; i--) {
			round(x - i, y - i, w + 2 * i, h + 2 * i, radius + i, Draw.alpha(argb, each));
		}
	}

	// ------------------------------------------------------------------------- text

	private static final Identifier UI = Identifier.fromNamespaceAndPath("glacier", "ui");
	private static final Identifier UI_BOLD = Identifier.fromNamespaceAndPath("glacier", "ui_bold");
	private static final Identifier FA = Identifier.fromNamespaceAndPath("glacier", "fa_solid");

	private record Key(String s, int kind) {
	}

	private static final Map<Key, Component> COMPONENTS = new LinkedHashMap<>(256, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Key, Component> e) {
			return size() > 768;
		}
	};

	private static Component comp(String s, int kind) {
		Key k = new Key(s, kind);
		Component c = COMPONENTS.get(k);
		if (c == null) {
			Identifier f = kind == 2 ? FA : kind == 1 ? UI_BOLD : UI;
			c = Component.literal(s).setStyle(Style.EMPTY.withFont(new FontDescription.Resource(f)));
			COMPONENTS.put(k, c);
		}
		return c;
	}

	private static Font font() {
		return Minecraft.getInstance().font;
	}

	public static float width(String s, float px, boolean bold) {
		if (s == null || s.isEmpty()) return 0f;
		Font f = font();
		return f.width(comp(s, bold ? 1 : 0)) * px / f.lineHeight;
	}

	public static float iconWidth(int cp, float px) {
		Font f = font();
		return f.width(comp(new String(Character.toChars(cp)), 2)) * px / f.lineHeight;
	}

	/** Draws a run with its glyph box centred vertically on cy. */
	public void text(String s, float x, float cy, float px, int argb, boolean bold) {
		if (s == null || s.isEmpty() || (argb >>> 24) == 0) return;
		put(comp(s, bold ? 1 : 0), x, cy - px / 2f, px, argb);
	}

	public void text(String s, float x, float cy, float px, int argb) {
		text(s, x, cy, px, argb, false);
	}

	public void textRight(String s, float right, float cy, float px, int argb, boolean bold) {
		text(s, right - width(s, px, bold), cy, px, argb, bold);
	}

	public void textCenter(String s, float cx, float cy, float px, int argb, boolean bold) {
		text(s, cx - width(s, px, bold) / 2f, cy, px, argb, bold);
	}

	public void icon(int cp, float cx, float cy, float px, int argb) {
		if ((argb >>> 24) == 0) return;
		float w = iconWidth(cp, px);
		put(comp(new String(Character.toChars(cp)), 2), cx - w / 2f, cy - px / 2f, px, argb);
	}

	private void put(Component c, float x, float y, float px, int argb) {
		float sc = px / font().lineHeight;
		var p = g.pose();
		p.pushMatrix();
		p.translate(x, y);
		p.scale(sc, sc);
		g.text(font(), c, 0, 0, argb, false);
		p.popMatrix();
	}

	/** Shortens s with an ellipsis so it fits maxW (css text-overflow: ellipsis). */
	public static String fit(String s, float maxW, float px, boolean bold) {
		if (width(s, px, bold) <= maxW) return s;
		for (int k = s.length() - 1; k > 0; k--) {
			String cand = s.substring(0, k).stripTrailing() + "…";
			if (width(cand, px, bold) <= maxW) return cand;
		}
		return "…";
	}

	public static List<String> wrap(String s, float maxW, float px, boolean bold) {
		List<String> lines = new ArrayList<>();
		StringBuilder cur = new StringBuilder();
		for (String word : s.split(" ")) {
			String cand = cur.length() == 0 ? word : cur + " " + word;
			if (cur.length() > 0 && width(cand, px, bold) > maxW) {
				lines.add(cur.toString());
				cur = new StringBuilder(word);
			} else {
				cur = new StringBuilder(cand);
			}
		}
		if (cur.length() > 0) lines.add(cur.toString());
		return lines;
	}

	// ------------------------------------------------------------------------ images

	/** Draws the u,v,rw,rh region of a texture of tw x th scaled into the dw x dh box at (x, y). */
	public void image(Identifier id, float x, float y, float dw, float dh, int u, int v, int rw, int rh, int tw, int th) {
		g.blit(PIPE, id, r(x), r(y), (float) u, (float) v, r(dw), r(dh), rw, rh, tw, th);
	}

	// ------------------------------------------------------------------- components

	public static int rgb(String hex) {
		return 0xFF000000 | Integer.parseInt(hex.substring(1), 16);
	}

	/** The css switch: 38x22 (40x22 in the sheet), padding 3, knob 16. */
	public void toggle(float x, float y, float w, boolean on, int onColor) {
		round(x, y, w, 22, 11, on ? onColor : 0xFF383B40);
		round(on ? x + w - 19 : x + 3, y + 3, 16, 16, 8, 0xFFFFFFFF);
	}

	/** An input[type=range]: 4px track inset by half the 12px thumb, accent fill, white 12px thumb. */
	public float range(float x, float y, float w, float h, double frac, int accent) {
		float cy = y + h / 2f;
		float tx0 = x + 6f, tx1 = x + w - 6f;
		float fx = tx0 + (float) Math.max(0, Math.min(1, frac)) * (tx1 - tx0);
		round(tx0, cy - 2, tx1 - tx0, 4, 2, 0xFF383B40);
		round(tx0, cy - 2, fx - tx0, 4, 2, accent);
		round(fx - 6, cy - 6, 12, 12, 6, 0xFFFFFFFF);
		return tx0;
	}

	/** Slider fraction for a logical mouse x over a range drawn at x..x+w. */
	public static double fracAt(double lx, float x, float w) {
		return (lx - (x + 6.0)) / Math.max(1.0, w - 12.0);
	}

	/** The thin scroll thumb the engine paints at the right edge of a scrolled box. */
	public void scrollbar(float x1, float y0, float h, float content, float scroll) {
		float max = content - h;
		if (max <= 0) return;
		float trackH = h - 4;
		float thumbH = Math.min(trackH, Math.max(16f, trackH * h / content));
		float ty = y0 + 2 + (trackH - thumbH) * (scroll / max);
		round(x1 - 5, ty, 3, thumbH, 1, 0x66FFFFFF);
	}
}
