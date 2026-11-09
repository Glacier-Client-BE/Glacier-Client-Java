package net.glacier.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immediate-mode drawing primitives used by the native UI. */
public final class Draw {
	/** Sub-samples per axis for edge anti-aliasing (SS*SS coverage levels). */
	private static final int SS = 4;
	private static final Identifier UI_FONT = Identifier.fromNamespaceAndPath("glacier", "ui");
	private static final Identifier FA_SOLID = Identifier.fromNamespaceAndPath("glacier", "fa_solid");

	private Draw() {
	}

	/** Filled rectangle with anti-aliased rounded corners. */
	public static void round(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int argb) {
		int r = Math.min(Math.min(radius, 16), Math.min(w, h) / 2);
		if (r >= 1 && w >= 2 * r + 1 && h >= 2 * r + 1) {
			sprite(g, "r" + r, x, y, w, h, argb);
			return;
		}
		shape(g, x, y, w, h, radius, 0, false, argb);
	}

	/** Source texture scale: the PNGs hold 4 texels per logical (GUI) pixel. */
	private static final int NINE_SCALE = 4;
	private static final java.util.Map<String, NineSlice> NINES = new java.util.HashMap<>();

	/** One nine-slice texture: its id, size in texels and corner size in logical pixels. */
	private record NineSlice(net.minecraft.resources.Identifier id, int tex, int border) {
	}

	private static NineSlice nine(String name) {
		NineSlice n = NINES.get(name);
		if (n != null) return n;
		int logical, border;
		if (name.startsWith("ring")) {
			border = Integer.parseInt(name.substring(4, name.indexOf('_')));
			logical = 2 * border + 2;
		} else if (name.startsWith("r") && Character.isDigit(name.charAt(1))) {
			border = Integer.parseInt(name.substring(1));
			logical = 2 * border + 2;
		} else if (name.equals("base_bg") || name.equals("secondary_bg")) {
			logical = 80;
			border = 8;
		} else {
			logical = 32;
			border = name.equals("mm_panel") ? 10 : 8;
		}
		n = new NineSlice(Identifier.fromNamespaceAndPath("glacier", "textures/ui_nine/" + name + ".png"),
				logical * NINE_SCALE, border);
		NINES.put(name, n);
		return n;
	}

	/**
	 * Draws one of the nine-slice panel textures in assets/glacier/textures/ui_nine (the Glacier v7 pack panels and
	 * generated rounded shapes) stretched to w x h: four fixed corners, four stretched edges and a stretched centre, so
	 * nine quads per panel. argb tints the texture (its alpha is the opacity). w and h must be at least 2x the corner
	 * size; smaller shapes should use {@link #round}.
	 */
	public static void sprite(GuiGraphicsExtractor g, String name, int x, int y, int w, int h, int argb) {
		if (w <= 0 || h <= 0 || (argb >>> 24) == 0) return;
		NineSlice n = nine(name);
		int b = Math.min(n.border, Math.min(w, h) / 2);
		int sb = n.border * NINE_SCALE;
		int t = n.tex;
		int[] dx = {x, x + b, x + w - b};
		int[] dw = {b, w - 2 * b, b};
		int[] dy = {y, y + b, y + h - b};
		int[] dh = {b, h - 2 * b, b};
		int[] su = {0, sb, t - sb};
		int[] sw = {sb, t - 2 * sb, sb};
		for (int j = 0; j < 3; j++) {
			if (dh[j] <= 0) continue;
			for (int i = 0; i < 3; i++) {
				if (dw[i] <= 0) continue;
				g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, n.id, dx[i], dy[j],
						(float) su[i], (float) su[j], dw[i], dh[j], sw[i], sw[j], t, t, argb);
			}
		}
	}

	/** Anti-aliased rounded rectangle ring, drawn inside the given bounds. */
	public static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int argb, int thickness) {
		if (thickness <= 0) return;
		int r = Math.min(Math.min(radius, 16), Math.min(w, h) / 2);
		if (thickness <= 2 && r > thickness && w >= 2 * r + 1 && h >= 2 * r + 1) {
			sprite(g, "ring" + r + "_" + thickness, x, y, w, h, argb);
			return;
		}
		shape(g, x, y, w, h, radius, thickness, true, argb);
	}

	public static void text(GuiGraphicsExtractor g, String s, int x, int y, int argb) {
		g.text(font(), uiText(s), x, y, argb, false);
	}

	public static void text(GuiGraphicsExtractor g, String s, float x, float y, float scale, int argb) {
		var p = g.pose();
		p.pushMatrix();
		p.translate(x, y);
		p.scale(scale, scale);
		g.text(font(), uiText(s), 0, 0, argb, false);
		p.popMatrix();
	}

	/** Bounded cache of menu text components: HUD and menu code draws the same strings every frame. */
	private static final int UI_TEXT_MAX = 256;
	private static final Map<String, Component> UI_TEXT = new LinkedHashMap<>(128, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Component> e) {
			return size() > UI_TEXT_MAX;
		}
	};

	/** Menu text in the Glacier UI font (Plus Jakarta Sans). Components are immutable, so they are cached. */
	public static Component uiText(String s) {
		Component c = UI_TEXT.get(s);
		if (c == null) {
			c = Component.literal(s).setStyle(Style.EMPTY.withFont(new FontDescription.Resource(UI_FONT)));
			UI_TEXT.put(s, c);
		}
		return c;
	}

	public static int textWidth(String s, float scale) {
		return Math.round(font().width(uiText(s)) * scale);
	}

	public static int textHeight(float scale) {
		return Math.round(8 * scale);
	}

	public static void centered(GuiGraphicsExtractor g, String s, int cx, int y, float scale, int argb) {
		text(g, s, cx - textWidth(s, scale) / 2f, y, scale, argb);
	}

	/** Font Awesome solid glyph by codepoint. */
	public static void icon(GuiGraphicsExtractor g, int codepoint, float x, float y, float scale, int argb) {
		var p = g.pose();
		p.pushMatrix();
		p.translate(x, y);
		p.scale(scale, scale);
		g.text(font(), iconText(codepoint), 0, 0, argb, false);
		p.popMatrix();
	}

	public static int iconWidth(int codepoint, float scale) {
		return Math.round(font().width(iconText(codepoint)) * scale);
	}

	/** Multiplies the alpha channel of an ARGB colour by mul in [0,1]. */
	public static int alpha(int argb, float mul) {
		int a = Math.round(((argb >>> 24) & 0xFF) * mul);
		a = Math.max(0, Math.min(255, a));
		return (a << 24) | (argb & 0x00FFFFFF);
	}

	/** Linear blend of two ARGB colours, t=0 gives a, t=1 gives b. */
	public static int mix(int a, int b, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int aa = lerp((a >>> 24) & 0xFF, (b >>> 24) & 0xFF, t);
		int rr = lerp((a >> 16) & 0xFF, (b >> 16) & 0xFF, t);
		int gg = lerp((a >> 8) & 0xFF, (b >> 8) & 0xFF, t);
		int bb = lerp(a & 0xFF, b & 0xFF, t);
		return (aa << 24) | (rr << 16) | (gg << 8) | bb;
	}

	private static int lerp(int a, int b, float t) {
		return Math.round(a + (b - a) * t);
	}

	private static Font font() {
		return Minecraft.getInstance().font;
	}

	/** Bounded cache of icon components, keyed by codepoint. Cleared when full. */
	private static final Map<Integer, Component> ICON_TEXT = new HashMap<>();
	private static final int ICON_TEXT_MAX = 512;

	private static Component iconText(int codepoint) {
		Component c = ICON_TEXT.get(codepoint);
		if (c == null) {
			if (ICON_TEXT.size() >= ICON_TEXT_MAX) ICON_TEXT.clear();
			c = Component.literal(new String(Character.toChars(codepoint)))
					.setStyle(Style.EMPTY.withFont(new FontDescription.Resource(FA_SOLID)));
			ICON_TEXT.put(codepoint, c);
		}
		return c;
	}

	/** Per-row edge coverage scratch buffer, grown on demand. Only touched on the render thread. */
	private static float[] edgeCoverage = new float[0];

	/**
	 * Shared rounded-shape rasteriser. Fills the outer rounded rect, or, when ring is set, the
	 * outer rect minus an inner rounded rect inset by t. Edge pixels use SS x SS sub-sampling.
	 * Only the left and right edge bands are sampled per row; the straight middle is one fill,
	 * and the shape is horizontally symmetric so the right band mirrors the left.
	 */
	private static void shape(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int t, boolean ring, int argb) {
		if (w <= 0 || h <= 0 || (argb >>> 24) == 0) return;
		int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
		if (r == 0 && !ring) {
			g.fill(x, y, x + w, y + h, argb);
			return;
		}
		if (!ring) {
			filled(g, x, y, w, h, r, argb);
			return;
		}
		boolean innerOk = ring && w - 2 * t > 0 && h - 2 * t > 0;
		int iw = w - 2 * t;
		int ih = h - 2 * t;
		int ir = Math.max(0, r - t);
		int edge = Math.min((w + 1) / 2, Math.max(r, innerOk ? t : 0) + 1);
		if (edgeCoverage.length < edge) edgeCoverage = new float[edge];
		float[] c = edgeCoverage;
		for (int yy = 0; yy < h; yy++) {
			for (int i = 0; i < edge; i++) {
				c[i] = cover(i, yy, w, h, r, innerOk, t, iw, ih, ir);
			}
			boolean innerRow = innerOk && yy >= t && yy < h - t;
			if (!innerRow) {
				int full = edge;
				while (full > 0 && c[full - 1] >= 0.999f) full--;
				if (full < w - full) g.fill(x + full, y + yy, x + w - full, y + yy + 1, argb);
				for (int i = 0; i < full; i++) plot(g, x, y, w, yy, i, c[i], argb);
			} else {
				for (int i = 0; i < edge; i++) plot(g, x, y, w, yy, i, c[i], argb);
			}
		}
	}

	/** Edge coverage of filled rounded shapes, cached per size: HUD panels repeat the same size every frame. */
	private static final Map<Long, float[]> FILL_CACHE = new LinkedHashMap<>(64, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Long, float[]> e) {
			return size() > 96;
		}
	};

	private static void filled(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int argb) {
		int edge = Math.min((w + 1) / 2, r + 1);
		long key = ((long) w << 40) | ((long) h << 20) | r;
		float[] cov = FILL_CACHE.get(key);
		if (cov == null) {
			cov = new float[h * edge];
			for (int yy = 0; yy < h; yy++) {
				for (int i = 0; i < edge; i++) cov[yy * edge + i] = cover(i, yy, w, h, r, false, 0, w, h, 0);
			}
			FILL_CACHE.put(key, cov);
		}
		for (int yy = 0; yy < h; yy++) {
			int base = yy * edge;
			int full = edge;
			while (full > 0 && cov[base + full - 1] >= 0.999f) full--;
			if (full < w - full) g.fill(x + full, y + yy, x + w - full, y + yy + 1, argb);
			for (int i = 0; i < full; i++) plot(g, x, y, w, yy, i, cov[base + i], argb);
		}
	}

	private static void plot(GuiGraphicsExtractor g, int x, int y, int w, int yy, int i, float cov, int argb) {
		if (cov <= 0f) return;
		int col = cov >= 0.999f ? argb : alpha(argb, cov);
		g.fill(x + i, y + yy, x + i + 1, y + yy + 1, col);
		int mirror = w - 1 - i;
		if (mirror != i) g.fill(x + mirror, y + yy, x + mirror + 1, y + yy + 1, col);
	}

	/** Fraction of sub-samples of pixel (xx,yy) inside the outer shape and outside the inner one. */
	private static float cover(int xx, int yy, int w, int h, int r, boolean innerOk, int t, int iw, int ih, int ir) {
		int hit = 0;
		for (int sy = 0; sy < SS; sy++) {
			double py = yy + (sy + 0.5) / SS;
			for (int sx = 0; sx < SS; sx++) {
				double px = xx + (sx + 0.5) / SS;
				if (!inside(px, py, 0, 0, w, h, r)) continue;
				if (innerOk && inside(px, py, t, t, iw, ih, ir)) continue;
				hit++;
			}
		}
		return hit / (float) (SS * SS);
	}

	private static boolean inside(double px, double py, double rx, double ry, double rw, double rh, double rr) {
		double lx = px - rx;
		double ly = py - ry;
		if (lx < 0 || ly < 0 || lx > rw || ly > rh) return false;
		double cx = Math.max(rr, Math.min(rw - rr, lx));
		double cy = Math.max(rr, Math.min(rh - rr, ly));
		double dx = lx - cx;
		double dy = ly - cy;
		return dx * dx + dy * dy <= rr * rr;
	}
}
