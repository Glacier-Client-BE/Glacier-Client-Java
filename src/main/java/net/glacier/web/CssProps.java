package net.glacier.web;

import java.util.HashMap;
import java.util.Map;

/** Property names as small integer ids, so the cascade dispatches with an int switch. */
final class CssProps {
	private CssProps() {}

	/** Not modelled by the engine: kept in Node.style only. */
	static final int OTHER = 0;
	/** A custom property (--name): only feeds var() resolution. */
	static final int CUSTOM = 1;

	static final int FONT_SIZE = 2, FONT = 3, FONT_WEIGHT = 4, LINE_HEIGHT = 5, LETTER_SPACING = 6,
			TEXT_ALIGN = 7, WHITE_SPACE = 8, TEXT_OVERFLOW = 9, BOX_SHADOW = 10, COLOR = 11,
			BACKGROUND = 12, BACKGROUND_COLOR = 13, OPACITY = 14, DISPLAY = 15, POSITION = 16,
			TOP = 17, RIGHT = 18, BOTTOM = 19, LEFT = 20, INSET = 21, BOX_SIZING = 22, OVERFLOW = 23,
			WIDTH = 24, HEIGHT = 25, MIN_WIDTH = 26, MIN_HEIGHT = 27, MAX_WIDTH = 28, MAX_HEIGHT = 29,
			MARGIN = 30, MARGIN_TOP = 31, MARGIN_RIGHT = 32, MARGIN_BOTTOM = 33, MARGIN_LEFT = 34,
			PADDING = 35, PADDING_TOP = 36, PADDING_RIGHT = 37, PADDING_BOTTOM = 38, PADDING_LEFT = 39,
			FLEX_DIRECTION = 40, FLEX_WRAP = 41, FLEX_GROW = 42, FLEX_SHRINK = 43, FLEX_BASIS = 44,
			FLEX = 45, JUSTIFY_CONTENT = 46, ALIGN_ITEMS = 47, ALIGN_SELF = 48, GAP = 49, ROW_GAP = 50,
			COLUMN_GAP = 51, GRID_TEMPLATE_COLUMNS = 52, GRID_COLUMN = 53, BORDER_RADIUS = 54,
			BORDER = 55, BORDER_TOP = 56, BORDER_RIGHT = 57, BORDER_BOTTOM = 58, BORDER_LEFT = 59,
			BORDER_WIDTH = 60, BORDER_STYLE = 61, BORDER_COLOR = 62, TRANSFORM = 63;

	private static final Map<String, Integer> IDS = new HashMap<>();

	static {
		put("font-size", FONT_SIZE);
		put("font", FONT);
		put("font-weight", FONT_WEIGHT);
		put("line-height", LINE_HEIGHT);
		put("letter-spacing", LETTER_SPACING);
		put("text-align", TEXT_ALIGN);
		put("white-space", WHITE_SPACE);
		put("text-overflow", TEXT_OVERFLOW);
		put("box-shadow", BOX_SHADOW);
		put("color", COLOR);
		put("background", BACKGROUND);
		put("background-color", BACKGROUND_COLOR);
		put("opacity", OPACITY);
		put("display", DISPLAY);
		put("position", POSITION);
		put("top", TOP);
		put("right", RIGHT);
		put("bottom", BOTTOM);
		put("left", LEFT);
		put("inset", INSET);
		put("box-sizing", BOX_SIZING);
		put("overflow", OVERFLOW);
		put("overflow-x", OVERFLOW);
		put("overflow-y", OVERFLOW);
		put("width", WIDTH);
		put("height", HEIGHT);
		put("min-width", MIN_WIDTH);
		put("min-height", MIN_HEIGHT);
		put("max-width", MAX_WIDTH);
		put("max-height", MAX_HEIGHT);
		put("margin", MARGIN);
		put("margin-top", MARGIN_TOP);
		put("margin-right", MARGIN_RIGHT);
		put("margin-bottom", MARGIN_BOTTOM);
		put("margin-left", MARGIN_LEFT);
		put("padding", PADDING);
		put("padding-top", PADDING_TOP);
		put("padding-right", PADDING_RIGHT);
		put("padding-bottom", PADDING_BOTTOM);
		put("padding-left", PADDING_LEFT);
		put("flex-direction", FLEX_DIRECTION);
		put("flex-wrap", FLEX_WRAP);
		put("flex-grow", FLEX_GROW);
		put("flex-shrink", FLEX_SHRINK);
		put("flex-basis", FLEX_BASIS);
		put("flex", FLEX);
		put("justify-content", JUSTIFY_CONTENT);
		put("align-items", ALIGN_ITEMS);
		put("align-self", ALIGN_SELF);
		put("gap", GAP);
		put("row-gap", ROW_GAP);
		put("column-gap", COLUMN_GAP);
		put("grid-template-columns", GRID_TEMPLATE_COLUMNS);
		put("grid-column", GRID_COLUMN);
		put("border-radius", BORDER_RADIUS);
		put("border", BORDER);
		put("border-top", BORDER_TOP);
		put("border-right", BORDER_RIGHT);
		put("border-bottom", BORDER_BOTTOM);
		put("border-left", BORDER_LEFT);
		put("border-width", BORDER_WIDTH);
		put("border-style", BORDER_STYLE);
		put("border-color", BORDER_COLOR);
		put("transform", TRANSFORM);
	}

	private static void put(String name, int id) {
		IDS.put(name, id);
	}

	/** Id of a lowercased property name; CUSTOM for --names, OTHER for anything not modelled. */
	static int id(String prop) {
		if (prop.startsWith("--")) return CUSTOM;
		Integer i = IDS.get(prop);
		return i == null ? OTHER : i;
	}

	/** Cascade pass: font-size first (em bases), then color (currentColor borders), then everything else. */
	static int pass(int id) {
		if (id == FONT_SIZE || id == FONT) return 1;
		if (id == COLOR) return 2;
		return 3;
	}

	/** Lengths that can be pre-parsed at sheet-parse time (their px value never depends on the em base). */
	static boolean preParsedLength(int id) {
		switch (id) {
			case MARGIN_TOP: case MARGIN_RIGHT: case MARGIN_BOTTOM: case MARGIN_LEFT:
			case PADDING_TOP: case PADDING_RIGHT: case PADDING_BOTTOM: case PADDING_LEFT:
			case TOP: case RIGHT: case BOTTOM: case LEFT: case ROW_GAP: case COLUMN_GAP:
				return true;
			default:
				return false;
		}
	}
}
