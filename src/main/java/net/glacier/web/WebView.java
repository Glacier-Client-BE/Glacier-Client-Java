package net.glacier.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.glacier.ui.Draw;
import net.glacier.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * An HTML document rendered with GuiGraphics. Owns the Node tree, the stylesheets and the layout,
 * paints it, and turns mouse and key input into DOM-style events.
 *
 * <p>Coordinates passed in and out are relative to the WebView's top-left corner; the caller must
 * leave {@code g.pose()} as it is. Styles come from {@code css} first, then from the document's own
 * {@code <style>} elements (later rules win at equal specificity).
 *
 * <p>Painting goes through a retained display list. After a cascade and a layout, the painted nodes are
 * recorded once into flat arrays: each node's own paint commands (fills with pre-resolved colours and
 * pre-rasterised rounded shapes, gradient rows, text lines with prepared components) and its children in
 * paint order. A frame only replays it, applying the scroll offsets as it goes and skipping whatever lies
 * outside the current scissor. A hover or active change re-bakes only the restyled subtrees.
 *
 * <p>Events are delivered through {@link WebEvents}. Each event is fired once per node on the path
 * from the target up to the root, with the node being fired at as the first argument and the
 * deepest target under the key "origin" in the detail map. Bubbling types: click, mousedown, mouseup,
 * keydown, input, change. Non-bubbling: mouseover, mouseout (fired on the target only).
 * Detail keys: x, y, button for pointer events; key for keydown; value for input/change on sliders;
 * checked for checkbox change. A node's {@link Node#onClick} is also invoked on click.
 * Click fires on the deepest common ancestor of the press and release targets.
 */
public class WebView {

	/** Receives DOM-style events. See the class comment for the bubbling rules. */
	public interface WebEvents {
		void fire(Node target, String type, Map<String, Object> detail);
	}

	/**
	 * Renderer timings and counters. The times are microseconds of the last frame (style, layout, build)
	 * or of the last build; commands is the size of the display list, fills and culled count the last frame.
	 * Only filled in when profiling is enabled, see {@link #drawProfile}.
	 */
	public record Profile(long styleUs, long layoutUs, long buildUs, long replayUs, long frameUs,
						  int commands, int fills, int culled, int builds, int patches) {
	}

	/** Line height as a multiple of the CSS font size, used when line-height is normal. */
	private static final float LINE_FACTOR = 1.25f;
	private static final float SCROLL_STEP = 20f;
	/** Range slider thumb diameter; the track runs from THUMB/2 to width - THUMB/2. */
	private static final float THUMB = 12f;
	private static final float RANGE_W = 160f, RANGE_H = 14f;
	private static final float BOX_SIZE = 14f;
	private static final float[] R_TRACK = {2, 2, 2, 2};
	private static final float[] R_THUMB = {6, 6, 6, 6};
	private static final float[] R_BOX = {3, 3, 3, 3};
	private static final float[] R_BAR = {1.5f, 1.5f, 1.5f, 1.5f};
	/**
	 * Padding around element and text bounds. Culling skips a child of a scroll container only when its bounds,
	 * padded by this much, lie outside the visible window, so glyph and ring overhang never gets cut.
	 */
	private static final float PAD = 16f;
	/** Padding around a text line's measured box for the scissor test. */
	private static final float TEXT_PAD = 8f;
	/** Text widths by (string, bold, size, family). Font metrics only, so entries never go stale. */
	private static final int TEXT_CACHE_MAX = 512;
	private static final Map<TextKey, Float> TEXT_WIDTHS = new LinkedHashMap<>(256, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<TextKey, Float> e) {
			return size() > TEXT_CACHE_MAX;
		}
	};

	private record TextKey(String s, boolean bold, float px, String fa) {
	}
	/** Font Awesome check (fa-check). */
	private static final String CHECK_GLYPH = "";

	/** Command kinds. */
	private static final byte FILL = 1, GRAD = 2, TEXT = 3, DIRECT = 4, IMAGE = 5;
	private static final int[] NO_KIDS = new int[0];

	/**
	 * Profiling is on with -Dglacier.uiProfile=true or with "uiProfile": true in config/glacier/performance.json.
	 * Read once; the render path takes no timestamps when it is off.
	 */
	private static final boolean PROFILE = profileEnabled();
	private static long tStyle, tLayout, tBuild, tReplay, tFrame;
	private static int nCommands, nFills, nCulled, nBuilds, nPatches;

	/**
	 * One paint command of the display list, in base (layout) coordinates. FILL and GRAD boxes hold the baked
	 * fills for their size; when a frame's size differs, they fall back to the RenderUtil call with the same
	 * arguments as the cascade-era painter.
	 */
	private static final class Cmd {
		final byte kind;
		/** Box top-left in layout coordinates (TEXT: the line's origin before the scroll offset). */
		final float x, y, w, h;
		/** Opacity the command was baked with. */
		final float op;
		/** FILL, GRAD: colour (with opacity applied), radii, ring thickness, and the baked box size and fills. */
		int argb;
		float[] radii;
		boolean ring;
		float thick;
		Css.Gradient grad;
		int bw, bh;
		int[] rects;
		/** DIRECT: the node, whether its border is painted, and whether its range or checkbox is painted. */
		Node node;
		boolean border, special;
		/** IMAGE: the texture. */
		Identifier image;
		/** TEXT: the component, its scale, the colour, the line offsets, the measured width and the font size. */
		Component comp;
		float scale, half, lineOff, lw, px;

		Cmd(byte kind, float x, float y, float w, float h, float op) {
			this.kind = kind;
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.op = op;
		}
	}

	/** One painted node of the display list (an element or a text node), with its own commands and children. */
	private static final class Entry {
		final Node node;
		final boolean text;
		final int index;
		/** The node's own commands: cmds[cmdStart, cmdEnd). */
		int cmdStart, cmdEnd;
		/** Painted children, in order, as entry indices. */
		int[] kids = NO_KIDS;
		/** The node has DOM children (even hidden ones). Decides whether the scroll and clip work runs. */
		boolean hasChildren, clipping, scroll;
		/** Border box in layout coordinates. */
		float x, y, w, h;
		/** Opacity of this node (the input opacity times its own). */
		float op;
		/** Bounds of this subtree in layout y, padded by PAD. */
		float subMin, subMax;
		/** Scroll containers: prefix maximum of the kids' subMax and suffix minimum of their subMin. */
		float[] preMax, sufMin;
		/** Scroll containers: the scrollbar's rectangles, cached for the thumb size and colour last drawn. */
		int sbW = -1, sbH = -1, sbArgb;
		int[] sbRects;

		Entry(Node node, boolean text, int index) {
			this.node = node;
			this.text = text;
			this.index = index;
		}
	}

	private final Node root;
	private final List<Css.Stylesheet> sheets = new ArrayList<>();
	private final boolean usesHover, usesActive;
	private final Layout.TextMeasurer measurer = new Layout.TextMeasurer() {
		@Override
		public float width(String text, boolean bold, float fontPx) {
			return measure(text, bold, fontPx, null);
		}

		@Override
		public float lineHeight(float fontPx) {
			return fontPx * LINE_FACTOR;
		}
	};

	private WebEvents sink;
	private int width, height;
	/** Set by size, DOM and class changes: the cascade and the layout must run again. */
	private boolean layoutDirty = true;
	private int layoutGen, scrollGen;

	private Node hoverNode, pressed, focus, dragging;

	private List<Node> cachedPath = List.of();
	private float cachedX, cachedY;
	private int cachedLayout = -1, cachedScroll = -1;

	/** Nodes whose hovered or active flag flipped since the last cascade. */
	private final List<Node> stateNodes = new ArrayList<>();
	/** Nodes restyled by a state change whose display-list subtrees must be re-baked. */
	private final List<Node> patchNodes = new ArrayList<>();
	private final List<Node> oldChain = new ArrayList<>(), newChain = new ArrayList<>();

	/** The display list: entries in pre-order, commands grouped per entry. Rebuilt after layout, patched after state. */
	private Entry[] entries = new Entry[0];
	private Cmd[] cmds = new Cmd[0];
	private int rootEntry = -1;
	private boolean displayDirty = true;
	private final Map<Node, Integer> entryOf = new HashMap<>();
	/** Build scratch, reused between builds. */
	private final List<Entry> entryList = new ArrayList<>();
	private final List<Cmd> cmdList = new ArrayList<>();
	private final List<Cmd> tmpCmds = new ArrayList<>();
	/** The graphics the current render writes to; set only while {@link #render} runs. */
	private GuiGraphics gui;

	/** Parses the document; css is the author stylesheet, applied before the document's own styles. */
	public WebView(String html, String css) {
		this.root = HtmlParser.parse(html);
		sheets.add(Css.parseSheet(css));
		sheets.add(Css.parseSheet(root.styleText));
		this.usesHover = usesPseudo("hover");
		this.usesActive = usesPseudo("active");
	}

	public Node root() {
		return root;
	}

	public void setEventSink(WebEvents sink) {
		this.sink = sink;
	}

	/** Sets the viewport size. Layout is redone on the next render or hit test. */
	public void setSize(int w, int h) {
		if (w != width || h != height) {
			width = w;
			height = h;
			layoutDirty = true;
		}
	}

	/** Re-runs the cascade and layout on the next render. Call after any DOM or attribute change. */
	public void invalidate() {
		layoutDirty = true;
	}

	/** Renderer timings and counters; see {@link Profile}. */
	public static Profile profile() {
		return new Profile(tStyle, tLayout, tBuild, tReplay, tFrame, nCommands, nFills, nCulled, nBuilds, nPatches);
	}

	/**
	 * Draws the profiler lines in the top-left corner of the screen. Does nothing unless profiling is enabled
	 * (-Dglacier.uiProfile=true, or "uiProfile": true in config/glacier/performance.json).
	 */
	public static void drawProfile(GuiGraphics g) {
		if (!PROFILE) return;
		Profile p = profile();
		String[] lines = {
				"ui frame " + p.frameUs() + " us  replay " + p.replayUs() + " us",
				"style " + p.styleUs() + " us  layout " + p.layoutUs() + " us  build " + p.buildUs() + " us",
				"commands " + p.commands() + "  fills " + p.fills() + "  culled " + p.culled(),
				"builds " + p.builds() + "  patches " + p.patches()};
		g.fill(2, 2, 230, 4 + lines.length * 10, 0xAA000000);
		int y = 5;
		for (String s : lines) {
			Draw.text(g, s, 5, y, 0xFFFFFFFF);
			y += 10;
		}
	}

	private static boolean profileEnabled() {
		if (Boolean.getBoolean("glacier.uiProfile")) return true;
		try {
			Path file = FabricLoader.getInstance().getGameDir().resolve("config").resolve("glacier")
					.resolve("performance.json");
			if (!java.nio.file.Files.exists(file)) return false;
			JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
			if (!parsed.isJsonObject()) return false;
			JsonElement v = parsed.getAsJsonObject().get("uiProfile");
			return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean() && v.getAsBoolean();
		} catch (Throwable t) {
			return false;
		}
	}

	/* ---------------------------------------------------------------- render */

	/** Draws the page. Runs the cascade and layout only when they are dirty, then replays the display list. */
	public void render(GuiGraphics g, int mx, int my) {
		if (root == null || width <= 0 || height <= 0) return;
		long f0 = 0L;
		if (PROFILE) {
			f0 = System.nanoTime();
			tStyle = tLayout = tBuild = 0L;
		}
		gui = g;
		ensureLayout();
		updateHover(mx, my);
		ensureLayout();
		ensureDisplay();
		long r0 = PROFILE ? System.nanoTime() : 0L;
		nFills = 0;
		nCulled = 0;
		if (rootEntry >= 0) replay(rootEntry, 0f, 0f, 0f, width, height, false);
		gui = null;
		if (PROFILE) {
			long now = System.nanoTime();
			tReplay = (now - r0) / 1000L;
			tFrame = (now - f0) / 1000L;
		}
	}

	/* ------------------------------------------------------------ display list */

	/** Builds the display list when it is dirty, or re-bakes the restyled subtrees when only paint changed. */
	private void ensureDisplay() {
		if (!displayDirty && patchNodes.isEmpty()) return;
		long t0 = PROFILE ? System.nanoTime() : 0L;
		boolean patched = !displayDirty;
		if (patched) {
			for (Node n : patchNodes) {
				if (!patchNode(n, opBefore(n))) {
					patched = false;
					break;
				}
			}
		}
		patchNodes.clear();
		if (patched) {
			if (PROFILE) {
				tBuild += (System.nanoTime() - t0) / 1000L;
				nPatches++;
			}
			return;
		}
		buildDisplay();
		if (PROFILE) tBuild = (System.nanoTime() - t0) / 1000L;
	}

	private void buildDisplay() {
		entryList.clear();
		cmdList.clear();
		entryOf.clear();
		rootEntry = buildNode(root, 1f);
		entries = entryList.toArray(new Entry[0]);
		cmds = cmdList.toArray(new Cmd[0]);
		entryList.clear();
		cmdList.clear();
		displayDirty = false;
		nCommands = cmds.length;
		nBuilds++;
	}

	/** Records n and its painted descendants. Returns the entry index, or -1 when n is not painted. */
	private int buildNode(Node n, float opIn) {
		if (n.isText()) {
			Entry e = addEntry(n, true);
			e.op = opIn;
			e.cmdStart = cmdList.size();
			bakeText(n, opIn, cmdList);
			e.cmdEnd = cmdList.size();
			Node p = n.parent;
			float px = p != null ? p.fontSize : 16f;
			float lh = p != null ? lineHeight(p) : px * LINE_FACTOR;
			e.subMin = n.y - PAD - px;
			e.subMax = n.y + n.lines.size() * lh + px + PAD;
			return e.index;
		}
		if (!n.visible || "none".equals(n.display)) return -1;
		float o = opIn * n.opacity;
		if (o <= 0f) return -1;
		Entry e = addEntry(n, false);
		e.op = o;
		e.clipping = isClipping(n);
		e.scroll = isScrollable(n);
		e.hasChildren = !n.children.isEmpty();
		e.x = n.x;
		e.y = n.y;
		e.w = n.w;
		e.h = n.h;
		e.cmdStart = cmdList.size();
		bakeBox(n, o, cmdList);
		e.cmdEnd = cmdList.size();
		if (e.hasChildren) {
			int[] buf = new int[n.children.size()];
			int k = 0;
			for (Node c : n.children) {
				int ci = buildNode(c, o);
				if (ci >= 0) buf[k++] = ci;
			}
			e.kids = k == buf.length ? buf : Arrays.copyOf(buf, k);
		}
		float lo = n.y - PAD, hi = n.y + n.h + PAD;
		for (int ci : e.kids) {
			Entry c = entryList.get(ci);
			lo = Math.min(lo, c.subMin);
			hi = Math.max(hi, c.subMax);
		}
		e.subMin = lo;
		e.subMax = hi;
		if (e.scroll) {
			int k = e.kids.length;
			e.preMax = new float[k];
			e.sufMin = new float[k];
			float m = Float.NEGATIVE_INFINITY;
			for (int i = 0; i < k; i++) {
				m = Math.max(m, entryList.get(e.kids[i]).subMax);
				e.preMax[i] = m;
			}
			float s = Float.POSITIVE_INFINITY;
			for (int i = k - 1; i >= 0; i--) {
				s = Math.min(s, entryList.get(e.kids[i]).subMin);
				e.sufMin[i] = s;
			}
		}
		return e.index;
	}

	private Entry addEntry(Node n, boolean text) {
		Entry e = new Entry(n, text, entryList.size());
		entryList.add(e);
		entryOf.put(n, e.index);
		return e;
	}

	/**
	 * Re-bakes the commands of n and its painted subtree from their current style. Returns false when the
	 * subtree's painted structure or command count changed; the caller then rebuilds the whole list.
	 */
	private boolean patchNode(Node n, float opIn) {
		Integer idx = entryOf.get(n);
		if (idx == null) return false;
		Entry e = entries[idx];
		if (n.isText()) {
			tmpCmds.clear();
			bakeText(n, opIn, tmpCmds);
			if (tmpCmds.size() != e.cmdEnd - e.cmdStart) return false;
			for (int i = 0; i < tmpCmds.size(); i++) cmds[e.cmdStart + i] = tmpCmds.get(i);
			e.op = opIn;
			return true;
		}
		if (!n.visible || "none".equals(n.display)) return false;
		float o = opIn * n.opacity;
		if (o <= 0f || e.clipping != isClipping(n) || e.scroll != isScrollable(n)
				|| e.hasChildren != !n.children.isEmpty()) {
			return false;
		}
		tmpCmds.clear();
		bakeBox(n, o, tmpCmds);
		if (tmpCmds.size() != e.cmdEnd - e.cmdStart) return false;
		for (int i = 0; i < tmpCmds.size(); i++) cmds[e.cmdStart + i] = tmpCmds.get(i);
		e.op = o;
		for (Node c : n.children) {
			boolean painted = c.isText() || (c.visible && !"none".equals(c.display) && o * c.opacity > 0f);
			if (painted) {
				if (!patchNode(c, o)) return false;
			} else if (entryOf.containsKey(c)) {
				return false;
			}
		}
		return true;
	}

	/** The opacity a node is painted with before its own: the product down its ancestor chain, root first. */
	private static float opBefore(Node n) {
		if (n.parent == null) return 1f;
		return opBefore(n.parent) * n.parent.opacity;
	}

	/**
	 * Commands of a node's box: the background (gradient or fill), a uniform border as a ring, and a direct paint
	 * for everything else (non-uniform border, range, checkbox), then an image. The sizes are those at the
	 * origin; a frame whose rounded size matches uses the baked fills.
	 */
	private static void bakeBox(Node n, float o, List<Cmd> out) {
		int bw = Math.round(n.x + n.w) - Math.round(n.x);
		int bh = Math.round(n.y + n.h) - Math.round(n.y);
		if (n.gradient != null) {
			Cmd c = new Cmd(GRAD, n.x, n.y, n.w, n.h, o);
			c.grad = n.gradient;
			c.radii = n.radius.clone();
			c.bw = bw;
			c.bh = bh;
			c.rects = RenderUtil.gradientRects(bw, bh, c.radii, n.gradient, o);
			out.add(c);
		} else if ((n.background >>> 24) != 0) {
			Cmd c = new Cmd(FILL, n.x, n.y, n.w, n.h, o);
			c.argb = Draw.alpha(n.background, o);
			c.radii = n.radius.clone();
			c.bw = bw;
			c.bh = bh;
			c.rects = RenderUtil.roundedRects(bw, bh, c.radii, 0, false, c.argb);
			out.add(c);
		}
		float[] bwd = n.borderWidth;
		int[] bc = n.borderColor;
		boolean border = false;
		if (bwd[0] > 0 || bwd[1] > 0 || bwd[2] > 0 || bwd[3] > 0) {
			boolean uniform = bwd[0] == bwd[1] && bwd[1] == bwd[2] && bwd[2] == bwd[3]
					&& bc[0] == bc[1] && bc[1] == bc[2] && bc[2] == bc[3];
			if (uniform) {
				Cmd c = new Cmd(FILL, n.x, n.y, n.w, n.h, o);
				c.ring = true;
				c.thick = bwd[0];
				c.argb = Draw.alpha(bc[0], o);
				c.radii = n.radius.clone();
				c.bw = bw;
				c.bh = bh;
				c.rects = RenderUtil.roundedRects(bw, bh, c.radii, Math.max(1, Math.round(bwd[0])), true, c.argb);
				out.add(c);
			} else {
				border = true;
			}
		}
		if ("img".equals(n.tag)) {
			if (border) out.add(direct(n, o, true, false));
			Identifier id = RenderUtil.imageId(n.attr("src"));
			if (id != null) {
				Cmd c = new Cmd(IMAGE, n.x, n.y, n.w, n.h, o);
				c.image = id;
				c.node = n;
				out.add(c);
			}
		} else if (isRange(n) || isCheckbox(n)) {
			out.add(direct(n, o, border, true));
		} else if (border) {
			out.add(direct(n, o, true, false));
		}
	}

	private static Cmd direct(Node n, float o, boolean border, boolean special) {
		Cmd c = new Cmd(DIRECT, n.x, n.y, n.w, n.h, o);
		c.node = n;
		c.border = border;
		c.special = special;
		return c;
	}

	/** Commands of a text node: one per line, with the parent's font, colour and alignment. */
	private static void bakeText(Node t, float opIn, List<Cmd> out) {
		Node p = t.parent;
		if (p == null || t.lines.isEmpty()) return;
		float px = p.fontSize;
		float lh = lineHeight(p);
		String fa = RenderUtil.family(p, t.text);
		int color = Draw.alpha(p.color, opIn);
		if ((color >>> 24) == 0) return;
		float half = (lh - px) / 2f;
		float scale = RenderUtil.textScale(px);
		int count = t.lines.size();
		for (int i = 0; i < count; i++) {
			String line = t.lines.get(i);
			float lw = measure(line, p.bold, px, fa);
			if (p.ellipsis && t.w > 0 && lw > t.w) {
				line = RenderUtil.ellipsize(line, t.w, p.bold, px, fa);
				lw = measure(line, p.bold, px, fa);
			}
			float xo = 0f;
			if (count > 1) {
				switch (p.textAlign) {
					case "center" -> xo = (t.w - lw) / 2f;
					case "right", "end" -> xo = t.w - lw;
					default -> { }
				}
			}
			if (line == null || line.isEmpty()) continue;
			Cmd c = new Cmd(TEXT, t.x + xo, t.y, 0f, 0f, opIn);
			c.comp = RenderUtil.textComponent(line, p.bold, fa);
			c.scale = scale;
			c.argb = color;
			c.half = half;
			c.lineOff = i * lh;
			c.lw = lw;
			c.px = px;
			out.add(c);
		}
	}

	/* ------------------------------------------------------------------ replay */

	/**
	 * Paints entry ei with scroll offset dy inside the clip (cx0, cy0, cx1, cy1). Same order and clipping as the
	 * cascade-era painter: a clipping node outside the clip is skipped whole, its children are painted inside its
	 * clip, and a scroll container paints its scrollbar after its children. The clip is only a real scissor when sc.
	 */
	private void replay(int ei, float dy, float cx0, float cy0, float cx1, float cy1, boolean sc) {
		Entry e = entries[ei];
		if (e.text) {
			for (int i = e.cmdStart; i < e.cmdEnd; i++) runCmd(cmds[i], dy, cx0, cy0, cx1, cy1, sc);
			return;
		}
		float x0 = e.x, y0 = e.y + dy, x1 = x0 + e.w, y1 = y0 + e.h;
		if (e.clipping && !overlaps(cx0, cy0, cx1, cy1, x0, y0, x1, y1)) return;
		for (int i = e.cmdStart; i < e.cmdEnd; i++) runCmd(cmds[i], dy, cx0, cy0, cx1, cy1, sc);
		if (!e.hasChildren) return;
		Node n = e.node;
		float childDy = dy;
		if (e.scroll) {
			clampScroll(n);
			childDy = dy - n.scrollY;
		}
		float ax0 = cx0, ay0 = cy0, ax1 = cx1, ay1 = cy1;
		boolean asc = sc;
		if (e.clipping) {
			ax0 = Math.max(cx0, x0);
			ay0 = Math.max(cy0, y0);
			ax1 = Math.min(cx1, x1);
			ay1 = Math.min(cy1, y1);
			asc = true;
		}
		if (ax1 <= ax0 || ay1 <= ay0) return;
		if (e.clipping) gui.enableScissor((int) Math.floor(ax0), (int) Math.floor(ay0),
				(int) Math.ceil(ax1), (int) Math.ceil(ay1));
		try {
			if (e.scroll) {
				replayScrollKids(e, childDy, ax0, ay0, ax1, ay1, asc);
				paintScrollbar(e, x0, y0, x1, y1);
			} else {
				for (int k : e.kids) replay(k, childDy, ax0, ay0, ax1, ay1, asc);
			}
		} finally {
			if (e.clipping) gui.disableScissor();
		}
	}

	/**
	 * The children of a scroll container that can show inside its window. The children are in document order:
	 * a binary search on the prefix maximum finds the first one that reaches the window, and the suffix minimum
	 * ends the walk. Each child is then tested on its own bounds.
	 */
	private void replayScrollKids(Entry e, float dy, float ax0, float ay0, float ax1, float ay1, boolean asc) {
		int[] kids = e.kids;
		float[] pre = e.preMax;
		int lo = 0, hi = kids.length;
		while (lo < hi) {
			int mid = (lo + hi) >>> 1;
			if (pre[mid] + dy > ay0) hi = mid;
			else lo = mid + 1;
		}
		for (int i = lo; i < kids.length; i++) {
			if (e.sufMin[i] + dy >= ay1) break;
			Entry c = entries[kids[i]];
			if (c.subMax + dy <= ay0 || c.subMin + dy >= ay1) {
				nCulled++;
				continue;
			}
			replay(kids[i], dy, ax0, ay0, ax1, ay1, asc);
		}
	}

	private void runCmd(Cmd c, float dy, float cx0, float cy0, float cx1, float cy1, boolean sc) {
		switch (c.kind) {
			case FILL, GRAD -> runBox(c, dy, cx0, cy0, cx1, cy1, sc);
			case TEXT -> runText(c, dy, cx0, cy0, cx1, cy1, sc);
			case DIRECT -> runDirect(c, dy);
			default -> runImage(c, dy);
		}
	}

	/**
	 * A fill, gradient or ring. The device box is rounded exactly as the RenderUtil call would round it, so a
	 * frame whose rounded size equals the baked size replays the baked fills at its origin; otherwise the call is
	 * made with the float box, as before.
	 */
	private void runBox(Cmd c, float dy, float cx0, float cy0, float cx1, float cy1, boolean sc) {
		float x0 = c.x, y0 = c.y + dy, x1 = x0 + c.w, y1 = y0 + c.h;
		int ix0 = Math.round(x0), iy0 = Math.round(y0), ix1 = Math.round(x1), iy1 = Math.round(y1);
		if (sc && (ix1 <= cx0 || ix0 >= cx1 || iy1 <= cy0 || iy0 >= cy1)) {
			nCulled++;
			return;
		}
		if (ix1 - ix0 == c.bw && iy1 - iy0 == c.bh) {
			RenderUtil.fillRects(gui, ix0, iy0, c.rects);
			nFills += c.rects.length / 5;
		} else {
			if (c.kind == GRAD) RenderUtil.gradientRounded(gui, x0, y0, x1, y1, c.radii, c.grad, c.op);
			else if (c.ring) RenderUtil.ringRounded(gui, x0, y0, x1, y1, c.radii, c.thick, c.argb);
			else RenderUtil.fillRounded(gui, x0, y0, x1, y1, c.radii, c.argb);
			nFills++;
		}
	}

	/** A text line, drawn at the same float origin as the cascade-era painter; skipped when its box is off the scissor. */
	private void runText(Cmd c, float dy, float cx0, float cy0, float cx1, float cy1, boolean sc) {
		float top = c.y + dy + c.half;
		float y = top + c.lineOff;
		if (sc && (c.x + c.lw + TEXT_PAD <= cx0 || c.x - TEXT_PAD >= cx1
				|| y + 2f * c.px + TEXT_PAD <= cy0 || y - TEXT_PAD >= cy1)) {
			nCulled++;
			return;
		}
		RenderUtil.drawPrepared(gui, c.comp, c.scale, c.x, y, c.argb);
	}

	/** Border and special paints of a node, which need the float box. */
	private void runDirect(Cmd c, float dy) {
		float x0 = c.x, y0 = c.y + dy, x1 = x0 + c.w, y1 = y0 + c.h;
		if (c.border) paintBorder(gui, c.node, x0, y0, x1, y1, c.op);
		if (c.special) {
			if (isRange(c.node)) paintRange(gui, c.node, x0, y0, x1, y1, c.op);
			else paintCheckbox(gui, c.node, x0, y0, x1, y1, c.op);
		}
	}

	private void runImage(Cmd c, float dy) {
		float x0 = c.x, y0 = c.y + dy, x1 = x0 + c.w, y1 = y0 + c.h;
		RenderUtil.image(gui, c.image, x0, y0, x1, y1, c.op);
	}

	private static void paintBorder(GuiGraphics g, Node n, float x0, float y0, float x1, float y1, float o) {
		float[] bw = n.borderWidth;
		int[] bc = n.borderColor;
		if (bw[0] <= 0 && bw[1] <= 0 && bw[2] <= 0 && bw[3] <= 0) return;
		if (bw[0] == bw[1] && bw[1] == bw[2] && bw[2] == bw[3]
				&& bc[0] == bc[1] && bc[1] == bc[2] && bc[2] == bc[3]) {
			RenderUtil.ringRounded(g, x0, y0, x1, y1, n.radius, bw[0], Draw.alpha(bc[0], o));
			return;
		}
		RenderUtil.rect(g, x0, y0, x1, y0 + bw[0], Draw.alpha(bc[0], o));
		RenderUtil.rect(g, x1 - bw[1], y0, x1, y1, Draw.alpha(bc[1], o));
		RenderUtil.rect(g, x0, y1 - bw[2], x1, y1, Draw.alpha(bc[2], o));
		RenderUtil.rect(g, x0, y0, x0 + bw[3], y1, Draw.alpha(bc[3], o));
	}

	private void paintRange(GuiGraphics g, Node n, float x0, float y0, float x1, float y1, float o) {
		float min = attrF(n, "min", 0f), max = attrF(n, "max", 100f);
		float val = attrF(n, "value", min);
		float t = max > min ? clamp01((val - min) / (max - min)) : 0f;
		int accent = accentOf(n);
		float cy = (y0 + y1) / 2f;
		float tx0 = x0 + THUMB / 2f, tx1 = x1 - THUMB / 2f;
		float fx = tx0 + t * (tx1 - tx0);
		RenderUtil.fillRounded(g, tx0, cy - 2f, tx1, cy + 2f, R_TRACK, Draw.alpha(Theme.TRACK_OFF, o));
		RenderUtil.fillRounded(g, tx0, cy - 2f, fx, cy + 2f, R_TRACK, Draw.alpha(accent, o));
		RenderUtil.fillRounded(g, fx - THUMB / 2f, cy - THUMB / 2f, fx + THUMB / 2f, cy + THUMB / 2f,
				R_THUMB, Draw.alpha(0xFFFFFFFF, o));
	}

	private void paintCheckbox(GuiGraphics g, Node n, float x0, float y0, float x1, float y1, float o) {
		if (n.hasAttr("checked")) {
			RenderUtil.fillRounded(g, x0, y0, x1, y1, R_BOX, Draw.alpha(accentOf(n), o));
			float px = Math.min(x1 - x0, y1 - y0) * 0.75f;
			RenderUtil.drawTextCentered(g, CHECK_GLYPH, (x0 + x1) / 2f, (y0 + y1) / 2f, px,
					RenderUtil.FA_SOLID, Draw.alpha(0xFFFFFFFF, o));
			return;
		}
		boolean styled = n.gradient != null || (n.background >>> 24) != 0;
		boolean bordered = n.borderWidth[0] > 0 || n.borderWidth[1] > 0 || n.borderWidth[2] > 0 || n.borderWidth[3] > 0;
		if (!styled) RenderUtil.fillRounded(g, x0, y0, x1, y1, R_BOX, Draw.alpha(Theme.INSET, o));
		if (!bordered) RenderUtil.ringRounded(g, x0, y0, x1, y1, R_BOX, 1f, Draw.alpha(Theme.CARD_BORDER, o));
	}

	/**
	 * The scrollbar thumb. Its rectangles are kept while the thumb keeps its size and colour, so scrolling
	 * only moves the thumb and allocates nothing.
	 */
	private void paintScrollbar(Entry e, float x0, float y0, float x1, float y1) {
		Node n = e.node;
		float max = maxScroll(n);
		if (max <= 0f) return;
		float trackH = n.h - 4f;
		float content = n.h + max;
		float thumbH = Math.min(trackH, Math.max(16f, trackH * n.h / content));
		float ty = y0 + 2f + (trackH - thumbH) * (n.scrollY / max);
		float fx0 = x1 - 5f, fy0 = ty, fx1 = x1 - 2f, fy1 = ty + thumbH;
		int argb = Draw.alpha(0x66FFFFFF, e.op);
		int ix = Math.round(fx0), iy = Math.round(fy0);
		int w = Math.round(fx1) - ix, h = Math.round(fy1) - iy;
		if (e.sbRects == null || e.sbW != w || e.sbH != h || e.sbArgb != argb) {
			e.sbRects = RenderUtil.roundedRects(w, h, R_BAR, 0, false, argb);
			e.sbW = w;
			e.sbH = h;
			e.sbArgb = argb;
		}
		RenderUtil.fillRects(gui, ix, iy, e.sbRects);
	}

	/* ------------------------------------------------------------- hit test */

	/** The deepest element under (x, y), or null. Coordinates are relative to the WebView. */
	public Node hit(double x, double y) {
		ensureLayout();
		List<Node> path = pathAt((float) x, (float) y);
		return path.isEmpty() ? null : path.get(path.size() - 1);
	}

	/**
	 * Root-to-target path of elements under (x, y). Callers must not modify the result. Cached by position,
	 * layout and scroll generation, so a still mouse costs nothing per frame.
	 */
	private List<Node> pathAt(float x, float y) {
		if (cachedLayout == layoutGen && cachedScroll == scrollGen && cachedX == x && cachedY == y) {
			return cachedPath;
		}
		List<Node> out = new ArrayList<>();
		if (width > 0 && height > 0) hitTest(root, x, y, 0f, new float[]{0, 0, width, height}, out);
		cachedPath = out;
		cachedX = x;
		cachedY = y;
		cachedLayout = layoutGen;
		cachedScroll = scrollGen;
		return out;
	}

	private boolean hitTest(Node n, float x, float y, float dy, float[] clip, List<Node> out) {
		if (n.isText() || !n.visible || "none".equals(n.display)) return false;
		if (!contains(clip, x, y)) return false;
		float x0 = n.x, y0 = n.y + dy, x1 = x0 + n.w, y1 = y0 + n.h;
		boolean clipping = isClipping(n);
		float[] cc = clipping ? intersect(clip, x0, y0, x1, y1) : clip;
		if (isScrollable(n)) clampScroll(n);
		float cdy = isScrollable(n) ? dy - n.scrollY : dy;
		for (int i = n.children.size() - 1; i >= 0; i--) {
			if (hitTest(n.children.get(i), x, y, cdy, cc, out)) {
				out.add(0, n);
				return true;
			}
		}
		if (x >= x0 && x < x1 && y >= y0 && y < y1) {
			out.add(n);
			return true;
		}
		return false;
	}

	/* ---------------------------------------------------------------- input */

	public boolean mouseClicked(double x, double y, int button) {
		float fx = (float) x, fy = (float) y;
		ensureLayout();
		updateHover(fx, fy);
		List<Node> path = pathAt(fx, fy);
		if (path.isEmpty()) {
			pressed = null;
			focus = null;
			return false;
		}
		Node target = path.get(path.size() - 1);
		focus = target;
		pressed = target;
		setActive(target, true);
		dispatch(target, "mousedown", pointer(fx, fy, button), true);
		if (button == 0 && isRange(target)) {
			dragging = target;
			setRangeFromX(target, fx);
		}
		return true;
	}

	public boolean mouseReleased(double x, double y, int button) {
		float fx = (float) x, fy = (float) y;
		ensureLayout();
		Node down = pressed;
		pressed = null;
		if (down != null) setActive(down, false);
		List<Node> path = pathAt(fx, fy);
		Node up = path.isEmpty() ? null : path.get(path.size() - 1);
		if (up != null) dispatch(up, "mouseup", pointer(fx, fy, button), true);

		Node drag = dragging;
		dragging = null;
		if (drag != null) dispatch(drag, "change", kv("value", (double) attrF(drag, "value", 0f)), true);

		if (button == 0 && down != null && up != null) {
			Node common = commonAncestor(down, up);
			if (common != null) dispatch(common, "click", pointer(fx, fy, button), true);
			if (down == up && isCheckbox(up)) toggleCheckbox(up);
		}
		return up != null;
	}

	public boolean mouseDragged(double x, double y, int button) {
		if (dragging == null) return false;
		ensureLayout();
		setRangeFromX(dragging, (float) x);
		return true;
	}

	/** Scrolls the innermost overflow:auto/scroll element under (x, y) that can move. */
	public boolean mouseScrolled(double x, double y, double dy) {
		ensureLayout();
		List<Node> path = pathAt((float) x, (float) y);
		for (int i = path.size() - 1; i >= 0; i--) {
			Node n = path.get(i);
			float max = maxScroll(n);
			if (isScrollable(n) && max > 0f) {
				n.scrollY = clamp(n.scrollY - (float) dy * SCROLL_STEP, 0f, max);
				scrollGen++;
				return true;
			}
		}
		return false;
	}

	/** Dispatches keydown to the last pressed element. Returns false when nothing has focus. */
	public boolean keyPressed(int key) {
		if (focus == null) return false;
		dispatch(focus, "keydown", kv("key", key), true);
		return true;
	}

	/* ------------------------------------------------------- hover and state */

	private void updateHover(float x, float y) {
		List<Node> path = pathAt(x, y);
		Node target = path.isEmpty() ? null : path.get(path.size() - 1);
		Node old = hoverNode;
		if (target == old) return;
		hoverNode = target;
		moveHover(old, target);
		Map<String, Object> detail = pointer(x, y, -1);
		if (old != null) dispatch(old, "mouseout", detail, false);
		if (target != null) dispatch(target, "mouseover", detail, false);
	}

	/**
	 * Moves the hovered flag from the old target's ancestors to the new target's. Only the nodes on one side of
	 * the common ancestor flip, and only those are noted for restyling.
	 */
	private void moveHover(Node old, Node target) {
		oldChain.clear();
		for (Node p = old; p != null; p = p.parent) oldChain.add(p);
		newChain.clear();
		for (Node p = target; p != null; p = p.parent) newChain.add(p);
		for (Node p : oldChain) {
			if (!newChain.contains(p)) {
				p.hovered = false;
				noteState(p, usesHover);
			}
		}
		for (Node p : newChain) {
			if (!oldChain.contains(p)) {
				p.hovered = true;
				noteState(p, usesHover);
			}
		}
	}

	private void setActive(Node n, boolean on) {
		for (Node p = n; p != null; p = p.parent) {
			if (p.active != on) {
				p.active = on;
				noteState(p, usesActive);
			}
		}
	}

	private void noteState(Node n, boolean styleMatters) {
		if (styleMatters) stateNodes.add(n);
	}

	private void setRangeFromX(Node n, float x) {
		float min = attrF(n, "min", 0f), max = attrF(n, "max", 100f);
		float step = attrF(n, "step", 0f);
		float x0 = n.x + THUMB / 2f, x1 = n.x + n.w - THUMB / 2f;
		float t = x1 > x0 ? clamp01((x - x0) / (x1 - x0)) : 0f;
		float v = min + t * (max - min);
		if (step > 0f) v = min + Math.round((v - min) / step) * step;
		v = clamp(v, Math.min(min, max), Math.max(min, max));
		String s = format(v);
		if (s.equals(n.attr("value"))) return;
		n.setAttr("value", s);
		dispatch(n, "input", kv("value", (double) v), true);
	}

	private void toggleCheckbox(Node n) {
		boolean on = !n.hasAttr("checked");
		if (on) n.setAttr("checked", "");
		else n.attrs.remove("checked");
		dispatch(n, "change", kv("checked", on), true);
	}

	/* ------------------------------------------------------------- layout */

	/**
	 * Brings style and layout up to date. A dirty layout runs the full cascade and the layout. Otherwise the
	 * pending state changes are restyled: a paint-only change keeps the layout and queues the subtrees for
	 * re-baking, a layout-changing one re-lays-out the page.
	 */
	private void ensureLayout() {
		if (width <= 0 || height <= 0) return;
		if (layoutDirty) {
			layoutDirty = false;
			stateNodes.clear();
			patchNodes.clear();
			long t0 = PROFILE ? System.nanoTime() : 0L;
			Css.apply(root, sheets);
			applyControlDefaults(root);
			long t1 = PROFILE ? System.nanoTime() : 0L;
			Layout.layout(root, width, height, measurer);
			if (PROFILE) {
				tStyle += (t1 - t0) / 1000L;
				tLayout += (System.nanoTime() - t1) / 1000L;
			}
			layoutGen++;
			displayDirty = true;
			return;
		}
		if (!stateNodes.isEmpty()) applyStateChange();
	}

	private void applyStateChange() {
		long t0 = PROFILE ? System.nanoTime() : 0L;
		int bits = Css.restyleState(root, sheets, stateNodes);
		long t1 = PROFILE ? System.nanoTime() : 0L;
		if ((bits & Css.LAYOUT_CHANGED) != 0) {
			applyControlDefaults(root);
			long t2 = PROFILE ? System.nanoTime() : 0L;
			Layout.layout(root, width, height, measurer);
			if (PROFILE) {
				tLayout += (System.nanoTime() - t2) / 1000L;
			}
			layoutGen++;
			displayDirty = true;
			patchNodes.clear();
		} else {
			for (Node n : stateNodes) applyControlDefaults(n);
			if (!displayDirty) patchNodes.addAll(stateNodes);
		}
		if (PROFILE) tStyle += (t1 - t0) / 1000L;
		stateNodes.clear();
	}

	/** Sizes images and form controls that the stylesheet leaves on auto. Runs after each cascade. */
	private static void applyControlDefaults(Node n) {
		if (n.isText()) return;
		switch (n.tag) {
			case "img" -> sizeImage(n);
			case "input" -> sizeInput(n);
			default -> { }
		}
		for (Node c : n.children) applyControlDefaults(c);
	}

	private static void sizeImage(Node n) {
		boolean cssW = n.width >= 0, cssH = n.height >= 0;
		float aw = cssW ? n.width : attrF(n, "width", -1f);
		float ah = cssH ? n.height : attrF(n, "height", -1f);
		int[] nat = RenderUtil.imageSize(RenderUtil.imageId(n.attr("src")));
		if (nat != null) {
			if (aw < 0 && ah < 0) {
				aw = nat[0];
				ah = nat[1];
			} else if (aw < 0) {
				aw = ah * nat[0] / (float) nat[1];
			} else if (ah < 0) {
				ah = aw * nat[1] / (float) nat[0];
			}
		}
		if (!cssW && aw >= 0) n.width = aw;
		if (!cssH && ah >= 0) n.height = ah;
	}

	private static void sizeInput(Node n) {
		if (isRange(n)) {
			if (n.width < 0) n.width = RANGE_W;
			if (n.height < 0) n.height = RANGE_H;
		} else if (isCheckbox(n)) {
			if (n.width < 0) n.width = BOX_SIZE;
			if (n.height < 0) n.height = BOX_SIZE;
		}
	}

	/** Line height of a text run's parent, matching the Layout measurer. */
	private static float lineHeight(Node p) {
		return p.lineHeight > 0 ? p.lineHeight : p.fontSize * LINE_FACTOR;
	}

	/** Text width in pixels, cached by everything the width depends on. */
	private static float measure(String s, boolean bold, float px, String fa) {
		TextKey k = new TextKey(s, bold, px, fa);
		Float w = TEXT_WIDTHS.get(k);
		if (w == null) {
			w = RenderUtil.textWidth(s, bold, px, fa);
			TEXT_WIDTHS.put(k, w);
		}
		return w;
	}

	private boolean usesPseudo(String name) {
		for (Css.Stylesheet s : sheets) {
			for (Css.Rule r : s.rules) {
				for (Css.Selector sel : r.selectors) {
					for (Css.Compound c : sel.parts) {
						if (c.pseudos.contains(name)) return true;
					}
				}
			}
		}
		return false;
	}

	/* ------------------------------------------------------------- dispatch */

	private void dispatch(Node origin, String type, Map<String, Object> detail, boolean bubble) {
		List<Node> path = new ArrayList<>();
		for (Node n = origin; n != null; n = n.parent) {
			path.add(n);
			if (!bubble) break;
		}
		for (Node n : path) {
			if ("click".equals(type) && n.onClick != null) n.onClick.accept(n);
			if (sink != null) {
				Map<String, Object> d = new LinkedHashMap<>(detail);
				d.put("origin", origin);
				sink.fire(n, type, d);
			}
		}
	}

	private static Map<String, Object> pointer(float x, float y, int button) {
		return kv("x", (double) x, "y", (double) y, "button", button);
	}

	private static Map<String, Object> kv(Object... pairs) {
		Map<String, Object> m = new LinkedHashMap<>();
		for (int i = 0; i + 1 < pairs.length; i += 2) m.put((String) pairs[i], pairs[i + 1]);
		return m;
	}

	private static Node commonAncestor(Node a, Node b) {
		Set<Node> seen = new HashSet<>();
		for (Node n = a; n != null; n = n.parent) seen.add(n);
		for (Node n = b; n != null; n = n.parent) {
			if (seen.contains(n)) return n;
		}
		return null;
	}

	/* ---------------------------------------------------------- style helpers */

	private static boolean isClipping(Node n) {
		return switch (n.overflow) {
			case "hidden", "clip", "auto", "scroll" -> true;
			default -> false;
		};
	}

	private static boolean isScrollable(Node n) {
		return "auto".equals(n.overflow) || "scroll".equals(n.overflow);
	}

	private static float maxScroll(Node n) {
		float pad = n.borderWidth[0] + n.borderWidth[2] + n.padding[0] + n.padding[2];
		return Math.max(0f, n.contentH + pad - n.h);
	}

	private static void clampScroll(Node n) {
		n.scrollY = clamp(n.scrollY, 0f, maxScroll(n));
	}

	private static boolean isRange(Node n) {
		return "input".equals(n.tag) && "range".equals(n.attr("type").toLowerCase(Locale.ROOT));
	}

	private static boolean isCheckbox(Node n) {
		return "input".equals(n.tag) && "checkbox".equals(n.attr("type").toLowerCase(Locale.ROOT));
	}

	private static int accentOf(Node n) {
		String v = n.style.get("accent-color");
		Integer c = v == null ? null : Css.parseColor(v);
		return c != null ? c : Theme.ACCENT;
	}

	private static float attrF(Node n, String name, float def) {
		String v = n.attr(name);
		if (v == null || v.isEmpty()) return def;
		try {
			float f = Float.parseFloat(v.trim());
			return Float.isNaN(f) ? def : f;
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String format(float v) {
		if (v == Math.rint(v)) return Long.toString((long) v);
		return String.format(Locale.ROOT, "%.3f", v);
	}

	private static float clamp(float v, float lo, float hi) {
		return Math.max(lo, Math.min(hi, v));
	}

	private static float clamp01(float v) {
		return clamp(v, 0f, 1f);
	}

	private static boolean contains(float[] r, float x, float y) {
		return x >= r[0] && x < r[2] && y >= r[1] && y < r[3];
	}

	/** True when the box (x0, y0)-(x1, y1) overlaps the clip (cx0, cy0)-(cx1, cy1). */
	private static boolean overlaps(float cx0, float cy0, float cx1, float cy1, float x0, float y0, float x1, float y1) {
		return x1 > cx0 && x0 < cx1 && y1 > cy0 && y0 < cy1;
	}

	private static float[] intersect(float[] c, float x0, float y0, float x1, float y1) {
		return new float[]{Math.max(c[0], x0), Math.max(c[1], y0), Math.min(c[2], x1), Math.min(c[3], y1)};
	}
}
