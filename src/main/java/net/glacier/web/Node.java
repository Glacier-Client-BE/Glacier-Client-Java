package net.glacier.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A DOM node: an element (tag) or a text node (tag "#text").
 * Holds parsed attributes, computed style values and the laid-out box.
 * Box values are border-box pixels; margin/padding/inset use NaN for "auto"/unset.
 *
 * <p>Every keyword-valued computed property exists twice: the String field (read by existing code)
 * and an int mirror ({@code *Id} / {@code displayBits}) that the cascade sets from the same keyword.
 * Hot code should prefer the int mirrors; the constants below define their values.
 */
public class Node {
	/* ---- int keyword ids (mirror the String computed-style fields) ---- */

	/** displayBits: bit set. "none" is its own bit; "inline*" sets INLINE; "flex"/"inline-flex" set FLEX; "grid"/"inline-grid" set GRID. */
	public static final int DISPLAY_NONE = 1, DISPLAY_INLINE = 2, DISPLAY_FLEX = 4, DISPLAY_GRID = 8;

	public static final int POSITION_STATIC = 0, POSITION_RELATIVE = 1, POSITION_ABSOLUTE = 2;

	/** flexDirId: FLEX_DIR_ROW for "row*", FLEX_DIR_COLUMN for "column*", FLEX_DIR_OTHER otherwise. */
	public static final int FLEX_DIR_OTHER = 0, FLEX_DIR_ROW = 1, FLEX_DIR_COLUMN = 2;

	public static final int OVERFLOW_VISIBLE = 0, OVERFLOW_HIDDEN = 1, OVERFLOW_CLIP = 2, OVERFLOW_AUTO = 3,
			OVERFLOW_SCROLL = 4, OVERFLOW_OTHER = 5;

	/** Keyword tables; an id is the index of the keyword, or the table length for any other value. */
	static final String[] TEXT_ALIGN_VALUES = {"left", "right", "center", "justify", "start", "end"};
	static final String[] JUSTIFY_VALUES = {"flex-start", "flex-end", "center", "space-between", "space-around",
			"space-evenly", "start", "end", "left", "right"};
	static final String[] ALIGN_ITEMS_VALUES = {"stretch", "flex-start", "flex-end", "center", "baseline", "start", "end"};
	static final String[] ALIGN_SELF_VALUES = {"auto", "stretch", "flex-start", "flex-end", "center", "baseline", "start", "end"};

	public static final int TEXT_ALIGN_OTHER = TEXT_ALIGN_VALUES.length;
	public static final int JUSTIFY_OTHER = JUSTIFY_VALUES.length;
	public static final int ALIGN_ITEMS_OTHER = ALIGN_ITEMS_VALUES.length;
	public static final int ALIGN_SELF_OTHER = ALIGN_SELF_VALUES.length;

	/* ---- dirty flags for Node.markDirty ---- */

	public static final int DIRTY_STYLE = 1, DIRTY_LAYOUT = 2;

	public String tag;                 // lowercased tag, or "#text"
	public String text = "";           // text for "#text" nodes
	public String id = "";
	public final List<String> classes = new ArrayList<>();
	public final Map<String, String> attrs = new LinkedHashMap<>();
	public final List<Node> children = new ArrayList<>();
	public Node parent;
	/** Document root only: concatenated text of all {@code <style>} elements. */
	public String styleText = "";

	/**
	 * Computed declarations the engine does not model (for example accent-color), after var()
	 * resolution. Modelled properties are read from the typed fields instead.
	 */
	public final Map<String, String> style = new LinkedHashMap<>();

	/** Set by the host's input layer; used by :hover / :active selectors. */
	public boolean hovered, active;

	// ---- dirty flags (set by markDirty, cleared by clearDirty) ----
	/** This node's own style must be recomputed. */
	public boolean needsStyle;
	/** This node's own layout must be recomputed. */
	public boolean needsLayout;
	/** Some descendant has a pending flag. */
	public boolean subtreeDirty;

	/** Custom properties in effect when this node was last styled (copy-on-write snapshot; cascade-internal). */
	Map<String, String> cssVars;
	/** True when this node's computed style can be copied to an identical sibling (cascade-internal). */
	boolean shareable;

	// ---- computed style (filled by Css.apply) ----
	public String display = "block";
	public String position = "static";            // static | relative | absolute
	public String flexDirection = "row";
	/** Int mirrors of the String fields above (set by the cascade; see the constants at the top). */
	public int displayBits, positionId, flexDirId = FLEX_DIR_ROW;
	public int justifyId, alignItemsId, alignSelfId, overflowId, textAlignId;
	public boolean flexWrap;
	public float flexGrow, flexShrink = 1f;
	public float flexBasis = -1;                  // -1 = auto
	public String justifyContent = "flex-start";
	public String alignItems = "stretch";
	public String alignSelf = "auto";
	public float rowGap, columnGap;
	public final float[] margin = new float[4];   // top, right, bottom, left (NaN = auto)
	public final float[] padding = new float[4];  // top, right, bottom, left
	public final float[] inset = new float[4];    // top, right, bottom, left (NaN = unset)
	public float width = -1, height = -1;         // -1 = auto; percent values are fractions when percentWidth/percentHeight
	public boolean percentWidth, percentHeight;
	public float minWidth, minHeight, maxWidth = -1, maxHeight = -1;
	public boolean percentMaxWidth, percentMaxHeight; // max-* stored as fractions when set
	public boolean minWidthSet;                   // false = min-width:auto (flex items keep min-content)
	public boolean borderBox;
	public String overflow = "visible";
	public String gridColumns = "";               // raw grid-template-columns
	public int gridColumnSpan = 1;
	public float opacity = 1f;
	public int color = 0xFFFFFFFF;                // ARGB
	public int background;                        // ARGB, 0 = none
	public Css.Gradient gradient;                 // linear-gradient(...) when set
	public final float[] borderWidth = new float[4]; // top, right, bottom, left
	public final int[] borderColor = new int[4];     // ARGB per side
	public final float[] radius = new float[4];      // top-left, top-right, bottom-right, bottom-left
	public String borderStyle = "none";
	public float fontSize = 16f;
	public boolean bold;
	public float lineHeight;                      // 0 = normal
	public float letterSpacing;                   // stored only
	public String textAlign = "left";
	public boolean nowrap;
	public boolean ellipsis;                      // stored only
	public String boxShadow = "";                 // stored only
	public float translateX, translateY;          // px
	public float translateXPct, translateYPct;    // fraction of own width/height

	// ---- layout geometry (filled by Layout) ----
	public float x, y, w, h;
	public float contentH;                        // natural content height (for scrolling)
	public float scrollY;                         // scroll offset, owned by the renderer
	public boolean visible = true;
	/** Wrapped lines for text nodes, set by Layout. */
	public final List<String> lines = new ArrayList<>();

	// layout caches for text nodes (owned by Layout)
	String normSrc, normText;
	float measW;
	int measPass = -1;

	/** Click handler bound by the screen layer. */
	public Consumer<Node> onClick;

	public Node(String tag) {
		this.tag = tag;
		resetStyle();
	}

	/** Restores every computed style field to its initial value. */
	public void resetStyle() {
		display = "block";
		displayBits = 0;
		position = "static";
		positionId = POSITION_STATIC;
		flexDirection = "row";
		flexDirId = FLEX_DIR_ROW;
		flexWrap = false;
		flexGrow = 0;
		flexShrink = 1f;
		flexBasis = -1;
		justifyContent = "flex-start";
		justifyId = 0;
		alignItems = "stretch";
		alignItemsId = 0;
		alignSelf = "auto";
		alignSelfId = 0;
		rowGap = columnGap = 0;
		Arrays.fill(margin, 0f);
		Arrays.fill(padding, 0f);
		Arrays.fill(inset, Float.NaN);
		width = height = -1;
		percentWidth = percentHeight = false;
		minWidth = minHeight = 0;
		maxWidth = maxHeight = -1;
		percentMaxWidth = percentMaxHeight = minWidthSet = false;
		borderBox = false;
		overflow = "visible";
		overflowId = OVERFLOW_VISIBLE;
		gridColumns = "";
		gridColumnSpan = 1;
		opacity = 1f;
		color = 0xFFFFFFFF;
		background = 0;
		gradient = null;
		Arrays.fill(borderWidth, 0f);
		Arrays.fill(borderColor, 0);
		Arrays.fill(radius, 0f);
		borderStyle = "none";
		fontSize = 16f;
		bold = false;
		lineHeight = 0;
		letterSpacing = 0;
		textAlign = "left";
		textAlignId = 0;
		nowrap = false;
		ellipsis = false;
		boxShadow = "";
		translateX = translateY = translateXPct = translateYPct = 0;
	}

	/**
	 * Copies every cascade-computed field from an identical sibling (same tag, id, classes and
	 * attributes, no state, no custom properties). Geometry and tree links are not touched.
	 */
	void copyStyleFrom(Node s) {
		display = s.display;
		displayBits = s.displayBits;
		position = s.position;
		positionId = s.positionId;
		flexDirection = s.flexDirection;
		flexDirId = s.flexDirId;
		flexWrap = s.flexWrap;
		flexGrow = s.flexGrow;
		flexShrink = s.flexShrink;
		flexBasis = s.flexBasis;
		justifyContent = s.justifyContent;
		justifyId = s.justifyId;
		alignItems = s.alignItems;
		alignItemsId = s.alignItemsId;
		alignSelf = s.alignSelf;
		alignSelfId = s.alignSelfId;
		rowGap = s.rowGap;
		columnGap = s.columnGap;
		System.arraycopy(s.margin, 0, margin, 0, 4);
		System.arraycopy(s.padding, 0, padding, 0, 4);
		System.arraycopy(s.inset, 0, inset, 0, 4);
		width = s.width;
		height = s.height;
		percentWidth = s.percentWidth;
		percentHeight = s.percentHeight;
		minWidth = s.minWidth;
		minHeight = s.minHeight;
		maxWidth = s.maxWidth;
		maxHeight = s.maxHeight;
		percentMaxWidth = s.percentMaxWidth;
		percentMaxHeight = s.percentMaxHeight;
		minWidthSet = s.minWidthSet;
		borderBox = s.borderBox;
		overflow = s.overflow;
		overflowId = s.overflowId;
		gridColumns = s.gridColumns;
		gridColumnSpan = s.gridColumnSpan;
		opacity = s.opacity;
		color = s.color;
		background = s.background;
		gradient = s.gradient;
		System.arraycopy(s.borderWidth, 0, borderWidth, 0, 4);
		System.arraycopy(s.borderColor, 0, borderColor, 0, 4);
		System.arraycopy(s.radius, 0, radius, 0, 4);
		borderStyle = s.borderStyle;
		fontSize = s.fontSize;
		bold = s.bold;
		lineHeight = s.lineHeight;
		letterSpacing = s.letterSpacing;
		textAlign = s.textAlign;
		textAlignId = s.textAlignId;
		nowrap = s.nowrap;
		ellipsis = s.ellipsis;
		boxShadow = s.boxShadow;
		translateX = s.translateX;
		translateY = s.translateY;
		translateXPct = s.translateXPct;
		translateYPct = s.translateYPct;
		style.clear();
		if (!s.style.isEmpty()) style.putAll(s.style);
	}

	/* ---- keyword mapping (used by the cascade) ---- */

	/** displayBits for a lowercased display value. */
	static int displayBitsOf(String v) {
		int bits = 0;
		if (v.equals("none")) return DISPLAY_NONE;
		if (v.startsWith("inline")) bits |= DISPLAY_INLINE;
		if (v.equals("flex") || v.equals("inline-flex")) bits |= DISPLAY_FLEX;
		if (v.equals("grid") || v.equals("inline-grid")) bits |= DISPLAY_GRID;
		return bits;
	}

	static int flexDirIdOf(String v) {
		if (v.startsWith("row")) return FLEX_DIR_ROW;
		if (v.startsWith("column")) return FLEX_DIR_COLUMN;
		return FLEX_DIR_OTHER;
	}

	static int positionIdOf(String v) {
		if (v.equals("relative")) return POSITION_RELATIVE;
		if (v.equals("absolute")) return POSITION_ABSOLUTE;
		return POSITION_STATIC;
	}

	static int overflowIdOf(String v) {
		switch (v) {
			case "visible": return OVERFLOW_VISIBLE;
			case "hidden": return OVERFLOW_HIDDEN;
			case "clip": return OVERFLOW_CLIP;
			case "auto": return OVERFLOW_AUTO;
			case "scroll": return OVERFLOW_SCROLL;
			default: return OVERFLOW_OTHER;
		}
	}

	static int keywordId(String v, String[] table) {
		for (int i = 0; i < table.length; i++) if (table[i].equals(v)) return i;
		return table.length;
	}

	/* ---- dirty flags ---- */

	/**
	 * Marks this node (DIRTY_STYLE and/or DIRTY_LAYOUT) and propagates {@code subtreeDirty} to every ancestor.
	 * Propagation stops at the first ancestor that is already dirty, since its ancestors are too.
	 */
	public void markDirty(int flags) {
		if ((flags & DIRTY_STYLE) != 0) needsStyle = true;
		if ((flags & DIRTY_LAYOUT) != 0) needsLayout = true;
		for (Node p = parent; p != null && !p.subtreeDirty; p = p.parent) p.subtreeDirty = true;
	}

	/** Clears every dirty flag in this subtree. */
	public void clearDirty() {
		needsStyle = needsLayout = subtreeDirty = false;
		for (Node c : children) c.clearDirty();
	}

	/* ---- tree helpers ---- */

	public boolean isText() {
		return "#text".equals(tag);
	}

	/* ---- class helpers ---- */

	public boolean hasClass(String c) {
		return classes.contains(c);
	}

	public void addClass(String c) {
		if (c != null && !c.isEmpty() && !classes.contains(c)) {
			classes.add(c);
			syncClassAttr();
		}
	}

	public void removeClass(String c) {
		if (classes.remove(c)) syncClassAttr();
	}

	private void syncClassAttr() {
		attrs.put("class", String.join(" ", classes));
	}

	/* ---- attribute helpers ---- */

	public String attr(String name) {
		return attrs.getOrDefault(name, "");
	}

	public boolean hasAttr(String name) {
		return attrs.containsKey(name);
	}

	public void setAttr(String name, String value) {
		attrs.put(name, value);
		if (name.equals("id")) {
			id = value;
		} else if (name.equals("class")) {
			classes.clear();
			for (String cl : value.trim().split("\\s+")) if (!cl.isEmpty()) classes.add(cl);
		}
	}

	/* ---- tree helpers ---- */

	public static Node text(String t) {
		Node n = new Node("#text");
		n.text = t;
		return n;
	}

	public void add(Node child) {
		child.parent = this;
		children.add(child);
	}

	/** Depth-first walk including this node. */
	public void walk(Consumer<Node> visitor) {
		visitor.accept(this);
		for (Node c : children) c.walk(visitor);
	}

	/** Concatenated text of this subtree. */
	public String innerText() {
		if (isText()) return text;
		StringBuilder sb = new StringBuilder();
		for (Node c : children) sb.append(c.innerText());
		return sb.toString();
	}

	public Node firstElement() {
		for (Node c : children) if (!c.isText()) return c;
		return null;
	}
}
