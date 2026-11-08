package net.glacier.hud;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.module.ModuleManager;
import net.glacier.module.impl.ElementsModule;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.ColorSetting;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Draw;
import net.glacier.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An on-screen element anchored by a fractional position (0..1 on each axis of the free space),
 * with a shared panel style. Subclasses describe only their content.
 *
 * The global Elements "Glacier Design" toggle switches every element between the vanilla flat box and
 * the Glacier navy panel or pill, and between the vanilla and the UI font. Modules only decide their
 * content, and call {@link #pill()} when they are a single-line readout.
 */
public abstract class HudModule extends Module {
	/** Colour sentinel: a part drawn in the module's Text Color setting. */
	protected static final int TEXT = 0;

	private final SliderSetting posX;
	private final SliderSetting posY;
	protected final SliderSetting scaleSetting;
	protected final ToggleSetting background;
	/** Panel opacity, 0.0 to 1.0. */
	protected final SliderSetting opacity;
	/** BG Fill: Base or Frosted (Frosted adds a light inner edge). */
	protected final ChoiceSetting bgFill;
	/** BG Shape: Square, Soft, Round or Pill. Sets the panel corner radius in both design modes. */
	protected final ChoiceSetting bgShape;
	protected final ColorSetting textColor;
	protected final ToggleSetting textShadow;
	protected final SliderSetting padding;
	/** NoRisk-style accent strip: None, Left, Top or Bottom, drawn in Accent Color. */
	protected final ChoiceSetting accent;
	protected final ColorSetting accentColor;
	protected final ToggleSetting border;
	/** Hidden. On while the element sits at its default stack slot; a manual drag turns it off. */
	private final ToggleSetting autoPlace;
	/** Pixel gap between stacked default elements and from the screen edge. */
	private static final int STACK_MARGIN = 4;
	/** Order in the top-left default stack, or -1 when the element has no stack slot. */
	private int stackOrder = -1;
	/** Pixel height and width at the last {@link #refreshStack()}. */
	private int stackH;
	private int stackW;

	/** Cached lookup of the Elements module, which holds the global design toggles. */
	private static ElementsModule elements;
	/** Components for Glacier-font text, so per-frame drawing does not rebuild them. */
	private static final Map<String, Component> FONT_CACHE = new HashMap<>();

	protected HudModule(String name, String desc) {
		super(name, desc, Category.HUD);
		posX = add(new SliderSetting("Pos X", 0, 0, 1, 0));
		posY = add(new SliderSetting("Pos Y", 0, 0, 1, 0));
		posX.hide();
		posY.hide();
		autoPlace = add(new ToggleSetting("Auto Place", true));
		autoPlace.hide();
		scaleSetting = add(new SliderSetting("Scale", 1.0, 0.5, 3.0, 0.05));
		background = add(new ToggleSetting("Background", true));
		opacity = add(new SliderSetting("Opacity", 0.6, 0.0, 1.0, 0.1));
		bgFill = add(new ChoiceSetting("BG Fill", "Base", "Base", "Frosted"));
		bgShape = add(new ChoiceSetting("BG Shape", "Soft", "Square", "Soft", "Round", "Pill"));
		textColor = add(new ColorSetting("Text Color", 0xFFFFFFFF));
		textShadow = add(new ToggleSetting("Text Shadow", true));
		padding = add(new SliderSetting("Padding", 3, 0, 12, 1));
		accent = add(new ChoiceSetting("Accent", "None", "None", "Left", "Top", "Bottom"));
		accentColor = add(new ColorSetting("Accent Color", Theme.ACCENT));
		border = add(new ToggleSetting("Border", false));
	}

	/**
	 * A run of text in one colour, or a Font Awesome glyph when cp is a codepoint (text is then unused).
	 * Use {@link #TEXT} as the colour for the Text Color setting.
	 */
	protected static final class Part {
		final String text;
		final int argb;
		final int cp;

		Part(String text, int argb, int cp) {
			this.text = text;
			this.argb = argb;
			this.cp = cp;
		}
	}

	protected static Part part(String text, int argb) {
		return new Part(text, argb, -1);
	}

	/** A Font Awesome solid glyph by codepoint, drawn in the given colour. */
	protected static Part icon(int codepoint, int argb) {
		return new Part(null, argb, codepoint);
	}

	/** True when the Elements "Glacier Design" toggle is on. */
	protected static boolean glacier() {
		ElementsModule e = elements();
		return e != null && e.glacierDesign.on();
	}

	private static ElementsModule elements() {
		if (elements == null) elements = ModuleManager.get(ElementsModule.class);
		return elements;
	}

	/** Glacier pill style: a fully rounded box. Override in small single-line readouts. */
	protected boolean pill() {
		return false;
	}

	/** Corner radius from BG Shape: Square 0, Soft 6, Round 10, Pill fully rounded. */
	protected final int radius() {
		return switch (bgShape.index()) {
			case 0 -> 0;
			case 2 -> 10;
			case 3 -> Theme.R_PILL;
			default -> 4;
		};
	}

	private boolean frosted() {
		ElementsModule e = elements();
		return bgFill.is("Frosted") || (e != null && e.useFrostedGlass.on());
	}

	/** Unscaled content width in pixels, excluding padding. */
	public abstract int contentWidth();

	/** Unscaled content height in pixels, excluding padding. */
	public abstract int contentHeight();

	/** Draws the content with its top-left corner at (x, y) in unscaled box coordinates. */
	protected abstract void drawContent(GuiGraphics g, int x, int y);

	protected final int pad() {
		return padding.i();
	}

	private int boxW() {
		return contentWidth() + pad() * 2;
	}

	private int boxH() {
		return contentHeight() + pad() * 2;
	}

	/** Scaled width in screen pixels. */
	public int pixelW() {
		return Math.round(boxW() * scaleSetting.f());
	}

	/** Scaled height in screen pixels. */
	public int pixelH() {
		return Math.round(boxH() * scaleSetting.f());
	}

	public float posX() {
		return posX.f();
	}

	public float posY() {
		return posY.f();
	}

	public void setPos(float fx, float fy) {
		posX.set((double) Math.max(0f, Math.min(1f, fx)));
		posY.set((double) Math.max(0f, Math.min(1f, fy)));
	}

	public float scale() {
		return scaleSetting.f();
	}

	public void setScale(float s) {
		scaleSetting.set((double) s);
	}

	/** Left edge in screen pixels for a screen of the given width. */
	public int screenX(int sw) {
		return Math.round(posX.f() * Math.max(0, sw - pixelW()));
	}

	/** Top edge in screen pixels for a screen of the given height. */
	public int screenY(int sh) {
		return Math.round(posY.f() * Math.max(0, sh - pixelH()));
	}

	/** Moves the element so its top-left corner lands on the given screen pixel. Ends auto placement. */
	public void setScreenPos(int px, int py, int sw, int sh) {
		int maxX = Math.max(1, sw - pixelW());
		int maxY = Math.max(1, sh - pixelH());
		setPos(px / (float) maxX, py / (float) maxY);
		autoPlace.set(false);
	}

	/**
	 * Puts the element in the top-left default stack at the given order (lower is higher up). Positions are
	 * computed in pixels from the real heights of the enabled elements above it, so the defaults never overlap
	 * at any GUI scale. Ignored once the element has been dragged.
	 */
	protected final void stackTopLeft(int order) {
		stackOrder = order;
	}

	/** Enabled auto-placed elements in stack order, with their pixel sizes. Rebuilt once per tick by {@link #refreshStack()}. */
	private static List<HudModule> stack = List.of();

	/** Caches the stack order and pixel sizes of the auto-placed elements. Called once per tick after modules tick. */
	public static void refreshStack() {
		List<HudModule> list = new ArrayList<>();
		for (HudModule m : ModuleManager.hud()) {
			if (m.stackOrder < 0 || !m.isEnabled() || !m.autoPlace.on()) continue;
			m.stackH = m.pixelH();
			m.stackW = m.pixelW();
			list.add(m);
		}
		list.sort(Comparator.comparingInt(m -> m.stackOrder));
		stack = list;
	}

	/**
	 * Recomputes the stacked top-left position in pixels while auto placement is on. When an element would fall
	 * below the screen bottom, the stack continues in a new column to the right.
	 */
	private void applyAutoPlace(int sw, int sh) {
		if (stackOrder < 0 || !autoPlace.on()) return;
		int x = STACK_MARGIN;
		int y = STACK_MARGIN;
		int colW = 0;
		for (HudModule m : stack) {
			if (y + m.stackH > sh - STACK_MARGIN && y > STACK_MARGIN) {
				x += colW + STACK_MARGIN;
				y = STACK_MARGIN;
				colW = 0;
			}
			if (m == this) break;
			y += m.stackH + STACK_MARGIN;
			colW = Math.max(colW, m.stackW);
		}
		int maxX = Math.max(1, sw - pixelW());
		int maxY = Math.max(1, sh - pixelH());
		setPos(x / (float) maxX, y / (float) maxY);
	}

	public void render(GuiGraphics g, int sw, int sh) {
		applyAutoPlace(sw, sh);
		renderAt(g, screenX(sw), screenY(sh));
	}

	public void renderAt(GuiGraphics g, int px, int py) {
		var pose = g.pose();
		float s = scaleSetting.f();
		pose.pushMatrix();
		try {
			pose.translate(px, py);
			pose.scale(s, s);
			if (panelVisible()) drawPanel(g, boxW(), boxH());
			drawContent(g, pad(), pad());
		} finally {
			pose.popMatrix();
		}
	}

	/** The box behind the content: vanilla flat black, or the Glacier navy panel or pill. */
	private void drawPanel(GuiGraphics g, int w, int h) {
		int r;
		int fill;
		if (glacier()) {
			r = radius();
			fill = Draw.alpha(Theme.GLACIER_PANEL, opacity.f());
		} else {
			r = radius();
			fill = Draw.alpha(Theme.HUD_BOX, opacity.f());
		}
		Draw.round(g, 0, 0, w, h, r, fill);
		if (frosted()) Draw.outline(g, 0, 0, w, h, r, Theme.GLACIER_EDGE, 1);
		if (border.on()) Draw.outline(g, 0, 0, w, h, r, Draw.alpha(accentColor.argb(), 0.6f), 1);
		int ac = accentColor.argb();
		int inset = Math.min(r / 2, Math.min(w, h) / 4);
		switch (accent.index()) {
			case 1 -> Draw.round(g, 0, inset, 2, h - inset * 2, 1, ac);
			case 2 -> Draw.round(g, inset, 0, w - inset * 2, 2, 1, ac);
			case 3 -> Draw.round(g, inset, h - 2, w - inset * 2, 2, 1, ac);
			default -> {
			}
		}
	}

	/** The module's own Background toggle, or the Elements "Use Base BG" override. */
	private boolean panelVisible() {
		if (background.on()) return true;
		ElementsModule e = elements();
		return e != null && e.useBaseBg.on();
	}

	/** Argb for a part: the Text Color setting when the part asks for {@link #TEXT}. */
	protected final int resolve(int argb) {
		return argb == TEXT ? textColor.argb() : argb;
	}

	/** Components for Glacier-font text, cached so per-frame drawing does not rebuild them. */
	private static Component fontText(String s) {
		Component c = FONT_CACHE.get(s);
		if (c == null) {
			if (FONT_CACHE.size() >= 256) FONT_CACHE.clear();
			c = Draw.uiText(s);
			FONT_CACHE.put(s, c);
		}
		return c;
	}

	protected final void drawText(GuiGraphics g, String s, int x, int y) {
		drawText(g, s, x, y, TEXT);
	}

	/** Glacier design draws the UI font, with the Text Shadow setting applied as in the vanilla design. */
	protected final void drawText(GuiGraphics g, String s, int x, int y, int argb) {
		int c = resolve(argb);
		if (glacier()) g.drawString(mc.font, fontText(s), x, y, c, textShadow.on());
		else g.drawString(mc.font, s, x, y, c, textShadow.on());
	}

	/** Draws parts left to right with their top at y. */
	protected final void drawParts(GuiGraphics g, int x, int y, Part... parts) {
		int cx = x;
		for (Part p : parts) {
			if (p.cp >= 0) {
				Draw.icon(g, p.cp, cx, y, 1f, resolve(p.argb));
				cx += Draw.iconWidth(p.cp, 1f);
			} else {
				drawText(g, p.text, cx, y, p.argb);
				cx += textWidth(p.text);
			}
		}
	}

	protected final int partsWidth(Part... parts) {
		int w = 0;
		for (Part p : parts) w += p.cp >= 0 ? Draw.iconWidth(p.cp, 1f) : textWidth(p.text);
		return w;
	}

	protected final int textWidth(String s) {
		return glacier() ? mc.font.width(fontText(s)) : mc.font.width(s);
	}

	protected final int lineHeight() {
		return mc.font.lineHeight;
	}

	/** Status colour for a value where higher is better: green at or above good, yellow at or above warn. */
	protected static int goodHigh(double v, double good, double warn) {
		if (v >= good) return Theme.HUD_GOOD;
		if (v >= warn) return Theme.HUD_WARN;
		return Theme.HUD_BAD;
	}

	/** Status colour for a value where lower is better: green below good, yellow below warn. */
	protected static int goodLow(double v, double good, double warn) {
		if (v < good) return Theme.HUD_GOOD;
		if (v < warn) return Theme.HUD_WARN;
		return Theme.HUD_BAD;
	}
}
