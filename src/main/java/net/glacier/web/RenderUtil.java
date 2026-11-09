package net.glacier.web;

import net.glacier.GlacierClient;
import net.glacier.ui.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Drawing helpers for the HTML engine: anti-aliased rounded shapes with per-corner radii,
 * gradients, menu text (Glacier UI fonts and Font Awesome glyphs) and PNG images.
 * Corner order everywhere is top-left, top-right, bottom-right, bottom-left.
 * Rectangles are float and rounded to whole pixels when drawn.
 */
public final class RenderUtil {
	/** Sub-samples per axis for corner anti-aliasing (SS * SS coverage levels). */
	private static final int SS = 4;
	public static final String FA_SOLID = "fa_solid";
	public static final String FA_BRANDS = "fa_brands";
	private static final String UI = "ui";
	private static final String UI_BOLD = "ui_bold";
	private static final String ELLIPSIS = "…";
	private static final int[] NO_IMAGE = {0, 0};
	private static final Map<Identifier, int[]> IMAGE_SIZES = new HashMap<>();
	/** Images already reported as unreadable, so a broken src logs once instead of every frame. */
	private static final Set<Identifier> WARNED = new HashSet<>();

	private RenderUtil() {
	}

	/* ------------------------------------------------------------------ text */

	/**
	 * Font family for a text run: "fa_solid", "fa_brands", or null for the menu UI font.
	 * Icon classes win; otherwise text made only of Private Use Area code points is an icon.
	 */
	public static String family(Node p, String text) {
		if (p != null) {
			if (p.hasClass("fab")) return FA_BRANDS;
			if (p.hasClass("fa") || p.hasClass("fas") || p.hasClass("fa-solid")) return FA_SOLID;
		}
		return isPua(text) ? FA_SOLID : null;
	}

	static boolean isPua(String s) {
		if (s == null || s.isEmpty()) return false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c < 0xE000 || c > 0xF8FF) return false;
		}
		return true;
	}

	private static Font font() {
		return Minecraft.getInstance().font;
	}

	/** Key of the cached text components: a record, so lookups allocate only the key. */
	private record CompKey(String s, boolean bold, String fa) {
	}

	/** Bounded LRU of text components. A component is immutable, so one per (text, weight, font) is enough. */
	private static final int COMPONENT_CACHE_MAX = 512;
	private static final Map<CompKey, Component> COMPONENTS = new LinkedHashMap<>(256, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<CompKey, Component> e) {
			return size() > COMPONENT_CACHE_MAX;
		}
	};

	/** The text component for s in the UI font (or the given Font Awesome family), cached. */
	public static Component textComponent(String s, boolean bold, String fa) {
		CompKey k = new CompKey(s, bold, fa);
		Component c = COMPONENTS.get(k);
		if (c == null) {
			String name = fa != null ? fa : (bold ? UI_BOLD : UI);
			c = Component.literal(s).setStyle(
					Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("glacier", name))));
			COMPONENTS.put(k, c);
		}
		return c;
	}

	/** Scale from the font's line height to CSS font size px. */
	public static float textScale(float px) {
		return px / font().lineHeight;
	}

	/** Width in pixels of s at CSS font size px. A null fa means "auto" (icon if Private Use Area). */
	public static float textWidth(String s, boolean bold, float px, String fa) {
		if (s == null || s.isEmpty()) return 0f;
		Font f = font();
		String family = fa != null ? fa : (isPua(s) ? FA_SOLID : null);
		return f.width(textComponent(s, bold, family)) * px / f.lineHeight;
	}

	/** Draws one line of text with its top-left corner at (x, y), scaled to CSS font size px. */
	public static void drawText(GuiGraphics g, String s, float x, float y, float px, boolean bold, String fa, int argb) {
		if (s == null || s.isEmpty()) return;
		drawPrepared(g, textComponent(s, bold, fa), textScale(px), x, y, argb);
	}

	/**
	 * Draws a component prepared by {@link #textComponent} at (x, y) with scale sc. The same calls as
	 * {@link #drawText}, without the component or the scale lookup: used by the retained display list.
	 */
	public static void drawPrepared(GuiGraphics g, Component c, float sc, float x, float y, int argb) {
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(sc, sc);
		g.drawString(font(), c, 0, 0, argb, false);
		pose.popMatrix();
	}

	/** Draws one line of text centred on (cx, cy). */
	public static void drawTextCentered(GuiGraphics g, String s, float cx, float cy, float px, String fa, int argb) {
		float tw = textWidth(s, false, px, fa);
		drawText(g, s, cx - tw / 2f, cy - px / 2f, px, false, fa, argb);
	}

	/** Shortens s with an ellipsis so it fits in maxW pixels. */
	public static String ellipsize(String s, float maxW, boolean bold, float px, String fa) {
		if (textWidth(s, bold, px, fa) <= maxW) return s;
		for (int k = s.length() - 1; k > 0; k--) {
			String cand = s.substring(0, k).stripTrailing() + ELLIPSIS;
			if (textWidth(cand, bold, px, fa) <= maxW) return cand;
		}
		return ELLIPSIS;
	}

	/* ---------------------------------------------------------------- shapes */

	/** Filled rounded rectangle with anti-aliased corners. */
	public static void fillRounded(GuiGraphics g, float fx0, float fy0, float fx1, float fy1, float[] radii, int argb) {
		int x0 = Math.round(fx0), y0 = Math.round(fy0), x1 = Math.round(fx1), y1 = Math.round(fy1);
		shape(g, x0, y0, x1 - x0, y1 - y0, radii, 0, false, argb);
	}

	/** Anti-aliased rounded ring drawn inside the given bounds. Thickness is rounded to whole pixels, minimum 1. */
	public static void ringRounded(GuiGraphics g, float fx0, float fy0, float fx1, float fy1, float[] radii,
								   float thickness, int argb) {
		if (thickness <= 0f) return;
		int x0 = Math.round(fx0), y0 = Math.round(fy0), x1 = Math.round(fx1), y1 = Math.round(fy1);
		int t = Math.max(1, Math.round(thickness));
		shape(g, x0, y0, x1 - x0, y1 - y0, radii, t, true, argb);
	}

	/** Axis-aligned rectangle with no rounding. Empty rectangles are skipped. */
	public static void rect(GuiGraphics g, float fx0, float fy0, float fx1, float fy1, int argb) {
		int x0 = Math.round(fx0), y0 = Math.round(fy0), x1 = Math.round(fx1), y1 = Math.round(fy1);
		if (x1 <= x0 || y1 <= y0 || (argb >>> 24) == 0) return;
		g.fill(x0, y0, x1, y1, argb);
	}

	/** Rows [top, bottom) of a mask that share the same runs. Each run is {start, end, hit} in shape x. */
	private static final class Band {
		int top, bottom;
		final int[] runs;

		Band(int top, int bottom, int[] runs) {
			this.top = top;
			this.bottom = bottom;
			this.runs = runs;
		}
	}

	/** The fills of one rounded shape, without its position or colour. */
	private static final class Mask {
		final Band[] bands;

		Mask(Band[] bands) {
			this.bands = bands;
		}
	}

	private record MaskKey(int w, int h, float r0, float r1, float r2, float r3, int t, boolean ring) {
	}

	/** Masks by size, radii and thickness. Bounded; used on the render thread only. */
	private static final int MASK_CACHE_MAX = 128;
	private static final Map<MaskKey, Mask> MASKS = new LinkedHashMap<>(64, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<MaskKey, Mask> e) {
			return size() > MASK_CACHE_MAX;
		}
	};

	/**
	 * Shared rounded-shape rasteriser on integer pixels. The coverage of each pixel is computed once per
	 * (size, radii, thickness) into a cached {@link Mask}; a frame only replays its fills. Adjacent pixels with
	 * equal coverage are one fill, and rows with identical fills are one fill.
	 */
	private static void shape(GuiGraphics g, int x, int y, int w, int h, float[] radii, int t, boolean ring, int argb) {
		if (w <= 0 || h <= 0 || (argb >>> 24) == 0) return;
		float[] r = clampRadii(radii, w, h);
		MaskKey key = new MaskKey(w, h, r[0], r[1], r[2], r[3], t, ring);
		Mask mask = MASKS.get(key);
		if (mask == null) {
			mask = buildMask(w, h, r, t, ring);
			MASKS.put(key, mask);
		}
		for (Band b : mask.bands) {
			int py0 = y + b.top, py1 = y + b.bottom;
			int[] runs = b.runs;
			for (int k = 0; k < runs.length; k += 3) {
				int hit = runs[k + 2];
				int col = hit >= SS * SS ? argb : Draw.alpha(argb, hit / (float) (SS * SS));
				g.fill(x + runs[k], py0, x + runs[k + 1], py1, col);
			}
		}
	}

	/** Computes the fills of a w x h shape with clamped radii r. Straight rows are full; corner rows are sampled. */
	private static Mask buildMask(int w, int h, float[] r, int t, boolean ring) {
		if (ring && (w - 2 * t <= 0 || h - 2 * t <= 0)) ring = false;
		float[] ir = new float[4];
		for (int i = 0; i < 4; i++) ir[i] = Math.max(0f, r[i] - t);
		float maxR = Math.max(Math.max(r[0], r[1]), Math.max(r[2], r[3]));
		int rowsTop = (int) Math.ceil(Math.max(r[0], r[1]));
		int rowsBot = (int) Math.ceil(Math.max(r[2], r[3]));
		int edge = Math.min((w + 1) / 2, (int) Math.ceil(maxR) + (ring ? t : 0) + 1);
		int full = SS * SS;
		int[] hits = new int[w];
		List<Band> bands = new ArrayList<>();
		Band cur = null;
		for (int yy = 0; yy < h; yy++) {
			boolean corner = yy < rowsTop || yy >= h - rowsBot;
			boolean hole = ring && yy >= t && yy < h - t;
			Arrays.fill(hits, 0);
			if (!corner) {
				if (!hole) {
					Arrays.fill(hits, full);
				} else {
					Arrays.fill(hits, 0, t, full);
					Arrays.fill(hits, w - t, w, full);
				}
			} else {
				for (int i = 0; i < edge; i++) {
					int c = coverage(i, yy, w, h, r, ring, t, ir);
					if (c <= 0) continue;
					hits[i] = c;
					hits[w - 1 - i] = c;
				}
				if (!(ring && hole) && edge < w - edge) Arrays.fill(hits, edge, w - edge, full);
			}
			int[] runs = runsOf(hits);
			if (cur != null && Arrays.equals(cur.runs, runs)) {
				cur.bottom = yy + 1;
			} else {
				cur = new Band(yy, yy + 1, runs);
				bands.add(cur);
			}
		}
		return new Mask(bands.toArray(new Band[0]));
	}

	/** Flattened {start, end, hit} triples of the non-zero runs in one row of hit counts. */
	private static int[] runsOf(int[] hits) {
		int[] out = new int[hits.length * 3];
		int n = 0;
		int p = 0;
		while (p < hits.length) {
			int v = hits[p];
			if (v == 0) {
				p++;
				continue;
			}
			int s = p;
			while (p < hits.length && hits[p] == v) p++;
			out[n++] = s;
			out[n++] = p;
			out[n++] = v;
		}
		return Arrays.copyOf(out, n);
	}

	/** Number of the SS x SS sub-samples of pixel (xx, yy) that are inside the outer shape and outside the hole. */
	private static int coverage(int xx, int yy, int w, int h, float[] r, boolean ring, int t, float[] ir) {
		int hit = 0;
		for (int sy = 0; sy < SS; sy++) {
			double py = yy + (sy + 0.5) / SS;
			for (int sx = 0; sx < SS; sx++) {
				double px = xx + (sx + 0.5) / SS;
				if (!inRounded(px, py, 0, 0, w, h, r)) continue;
				if (ring && inRounded(px, py, t, t, w - 2 * t, h - 2 * t, ir)) continue;
				hit++;
			}
		}
		return hit;
	}

	/** Point-in-rounded-rect test for a box at (ox, oy) of size rw x rh with radii tl, tr, br, bl. */
	static boolean inRounded(double px, double py, double ox, double oy, double rw, double rh, float[] r) {
		double lx = px - ox, ly = py - oy;
		if (lx < 0 || ly < 0 || lx > rw || ly > rh) return false;
		boolean left = lx < rw / 2.0, top = ly < rh / 2.0;
		double rad = top ? (left ? r[0] : r[1]) : (left ? r[3] : r[2]);
		if (rad <= 0) return true;
		boolean inX = left ? lx < rad : lx > rw - rad;
		boolean inY = top ? ly < rad : ly > rh - rad;
		if (!inX || !inY) return true;
		double cx = left ? rad : rw - rad, cy = top ? rad : rh - rad;
		double dx = lx - cx, dy = ly - cy;
		return dx * dx + dy * dy <= rad * rad;
	}

	/** Radii clamped so that adjacent corners never overlap. */
	static float[] clampRadii(float[] radii, int w, int h) {
		float max = Math.min(w, h) / 2f;
		float[] out = new float[4];
		if (radii != null) {
			for (int i = 0; i < 4; i++) out[i] = Math.max(0f, Math.min(radii[i], max));
		}
		return out;
	}

	/* -------------------------------------------------------------- baked fills */

	private static final int[] NO_RECTS = new int[0];

	/**
	 * The fills of a w x h rounded fill (t = 0) or ring (t = thickness, ring set) in colour argb, as flat
	 * {x0, y0, x1, y1, argb} rectangles relative to the top-left corner, in the order {@link #shape} draws them.
	 * Used by the retained display list: a frame then replays the rectangles at its own origin.
	 */
	public static int[] roundedRects(int w, int h, float[] radii, int t, boolean ring, int argb) {
		if (w <= 0 || h <= 0 || (argb >>> 24) == 0) return NO_RECTS;
		float[] r = clampRadii(radii, w, h);
		MaskKey key = new MaskKey(w, h, r[0], r[1], r[2], r[3], t, ring);
		Mask mask = MASKS.get(key);
		if (mask == null) {
			mask = buildMask(w, h, r, t, ring);
			MASKS.put(key, mask);
		}
		int count = 0;
		for (Band b : mask.bands) count += b.runs.length / 3;
		int[] out = new int[count * 5];
		int k = 0;
		for (Band b : mask.bands) {
			int[] runs = b.runs;
			for (int i = 0; i < runs.length; i += 3) {
				int hit = runs[i + 2];
				out[k++] = runs[i];
				out[k++] = b.top;
				out[k++] = runs[i + 1];
				out[k++] = b.bottom;
				out[k++] = hit >= SS * SS ? argb : Draw.alpha(argb, hit / (float) (SS * SS));
			}
		}
		return out;
	}

	/** Draws {@link #roundedRects} output at origin (ox, oy). */
	public static void fillRects(GuiGraphics g, int ox, int oy, int[] rects) {
		for (int i = 0; i < rects.length; i += 5) {
			g.fill(ox + rects[i], oy + rects[i + 1], ox + rects[i + 2], oy + rects[i + 3], rects[i + 4]);
		}
	}

	/* -------------------------------------------------------------- gradient */

	/**
	 * linear-gradient background with rounded corners. Gradients are drawn as one-pixel
	 * rows (vertical-ish angles) or columns (horizontal-ish angles); corners are not anti-aliased.
	 */
	public static void gradientRounded(GuiGraphics g, float fx0, float fy0, float fx1, float fy1, float[] radii,
									   Css.Gradient gr, float op) {
		int x = Math.round(fx0), y = Math.round(fy0);
		int w = Math.round(fx1) - x, h = Math.round(fy1) - y;
		fillRects(g, x, y, gradientRects(w, h, radii, gr, op));
	}

	/**
	 * The rows or columns of {@link #gradientRounded} for a w x h box, as flat {x0, y0, x1, y1, argb} rectangles
	 * relative to the top-left corner, in draw order.
	 */
	public static int[] gradientRects(int w, int h, float[] radii, Css.Gradient gr, float op) {
		if (w <= 0 || h <= 0 || gr == null || gr.colors.isEmpty() || op <= 0f) return NO_RECTS;
		float[] r = clampRadii(radii, w, h);
		int n = gr.colors.size();
		int[] cols = new int[n];
		float[] pos = new float[n];
		for (int i = 0; i < n; i++) {
			cols[i] = gr.colors.get(i);
			float s = i < gr.stops.size() ? gr.stops.get(i) : Float.NaN;
			pos[i] = Float.isNaN(s) ? (n == 1 ? 0f : i / (float) (n - 1)) : s;
		}
		double a = Math.toRadians(gr.angleDeg);
		double dx = Math.sin(a), dy = -Math.cos(a);
		double len = Math.abs(w * dx) + Math.abs(h * dy);
		if (len < 1e-6) len = 1;
		int[] out = new int[5 * Math.max(w, h)];
		int k = 0;
		if (Math.abs(dy) >= Math.abs(dx)) {
			for (int yy = 0; yy < h; yy++) {
				double py = yy + 0.5;
				double t = (py - h / 2.0) * dy / len + 0.5;
				int x0 = (int) Math.round(Math.max(arc(py, r[0]), arc(h - py, r[3])));
				int x1 = w - (int) Math.round(Math.max(arc(py, r[1]), arc(h - py, r[2])));
				if (x1 > x0) {
					out[k++] = x0;
					out[k++] = yy;
					out[k++] = x1;
					out[k++] = yy + 1;
					out[k++] = gradColor(cols, pos, t, op);
				}
			}
		} else {
			for (int xx = 0; xx < w; xx++) {
				double px = xx + 0.5;
				double t = (px - w / 2.0) * dx / len + 0.5;
				int y0 = (int) Math.round(Math.max(arc(px, r[0]), arc(w - px, r[1])));
				int y1 = h - (int) Math.round(Math.max(arc(px, r[3]), arc(w - px, r[2])));
				if (y1 > y0) {
					out[k++] = xx;
					out[k++] = y0;
					out[k++] = xx + 1;
					out[k++] = y1;
					out[k++] = gradColor(cols, pos, t, op);
				}
			}
		}
		return Arrays.copyOf(out, k);
	}

	/** Horizontal inset of a rounded corner of radius r at distance d from its edge. */
	private static double arc(double d, double r) {
		if (r <= 0 || d >= r) return 0;
		double q = r - d;
		return r - Math.sqrt(Math.max(0, r * r - q * q));
	}

	private static int gradColor(int[] cols, float[] pos, double t, float op) {
		int n = cols.length;
		if (t <= pos[0]) return Draw.alpha(cols[0], op);
		for (int i = 0; i + 1 < n; i++) {
			if (t <= pos[i + 1]) {
				float span = pos[i + 1] - pos[i];
				float f = span <= 0 ? 1f : (float) ((t - pos[i]) / span);
				return Draw.alpha(Draw.mix(cols[i], cols[i + 1], f), op);
			}
		}
		return Draw.alpha(cols[n - 1], op);
	}

	/* ---------------------------------------------------------------- images */

	/** Maps an img src of the form glacier:textures/... to its texture id, or null. */
	public static Identifier imageId(String src) {
		if (src == null || !src.startsWith("glacier:")) return null;
		try {
			return Identifier.fromNamespaceAndPath("glacier", src.substring("glacier:".length()));
		} catch (RuntimeException e) {
			return null;
		}
	}

	/** Registers the size of a dynamically created texture so image() can draw it. */
	public static void putImageSize(Identifier id, int w, int h) {
		IMAGE_SIZES.put(id, new int[]{w, h});
	}

	/** Natural {width, height} of a PNG texture, cached; null when it is missing or not a PNG. */
	public static int[] imageSize(Identifier id) {
		if (id == null) return null;
		int[] s = IMAGE_SIZES.get(id);
		if (s == null) {
			s = readPngSize(id);
			if (s == null) s = NO_IMAGE;
			IMAGE_SIZES.put(id, s);
		}
		return s == NO_IMAGE ? null : s;
	}

	private static int[] readPngSize(Identifier id) {
		try {
			Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(id);
			if (res.isEmpty()) return null;
			try (InputStream in = res.get().open()) {
				byte[] b = in.readNBytes(24);
				if (b.length < 24 || b[1] != 'P' || b[2] != 'N' || b[3] != 'G') return null;
				int w = ((b[16] & 0xFF) << 24) | ((b[17] & 0xFF) << 16) | ((b[18] & 0xFF) << 8) | (b[19] & 0xFF);
				int h = ((b[20] & 0xFF) << 24) | ((b[21] & 0xFF) << 16) | ((b[22] & 0xFF) << 8) | (b[23] & 0xFF);
				return w > 0 && h > 0 ? new int[]{w, h} : null;
			}
		} catch (IOException | RuntimeException e) {
			return null;
		}
	}

	/** Draws a whole PNG stretched to the given box, tinted by opacity op. Logs once when the PNG cannot be read. */
	public static void image(GuiGraphics g, Identifier id, float fx0, float fy0, float fx1, float fy1, float op) {
		int[] nat = imageSize(id);
		if (nat == null) {
			if (WARNED.add(id)) GlacierClient.LOGGER.warn("[ui] image not loaded {}", id);
			return;
		}
		imageUv(g, id, fx0, fy0, fx1, fy1, 0f, 0f, nat[0], nat[1], op);
	}

	/**
	 * Draws the uw x uh texel region at (u, v) of a PNG, stretched to the box, tinted by opacity op. The region is in
	 * texels of the natural size: a region smaller than the texture shows only that part of it (used for panning).
	 * The blit must get the region size as uWidth/uHeight; passing the box size there samples only the top-left corner.
	 */
	public static void imageUv(GuiGraphics g, Identifier id, float fx0, float fy0, float fx1, float fy1,
							   float u, float v, float uw, float uh, float op) {
		int[] nat = imageSize(id);
		if (nat == null || op <= 0f) return;
		int x0 = Math.round(fx0), y0 = Math.round(fy0);
		int w = Math.round(fx1) - x0, h = Math.round(fy1) - y0;
		if (w <= 0 || h <= 0 || uw <= 0f || uh <= 0f) return;
		g.blit(RenderPipelines.GUI_TEXTURED, id, x0, y0, u, v, w, h,
				Math.round(uw), Math.round(uh), nat[0], nat[1], Draw.alpha(0xFFFFFFFF, op));
	}
}
