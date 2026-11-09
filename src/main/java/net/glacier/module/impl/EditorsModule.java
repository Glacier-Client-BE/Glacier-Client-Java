package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.module.ModuleManager;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Backs the Editors page: crosshair, hotbar and EXP bar options. Hooks are read by MixinGui and HudExtras. */
public class EditorsModule extends Module {
	/** Colour names of the pack's crosshair colour picker, in its order. */
	public static final String[] COLOR_NAMES = {
			"Default", "Black", "Red", "Blue", "Yellow", "Orange", "Purple", "Green", "Chroma"
	};
	private static final int[] COLOR_ARGB = {
			0xFFFFFFFF, 0xFF000000, 0xFFFF3333, 0xFF4073FF, 0xFFFFFF33, 0xFFFFA500, 0xFFA833FF, 0xFF33FF4D, 0xFFFFFFFF
	};
	/** Number of pack crosshair textures (textures/editors/crosshairs/1.png .. 33.png). */
	public static final int PRESET_COUNT = 33;
	/** Textures 1 and a few others are 15x15, the rest 16x16. */
	private static final boolean[] SMALL = new boolean[PRESET_COUNT + 1];
	private static final Identifier[] PRESET_TEX = new Identifier[PRESET_COUNT + 1];
	/** Default drawn crosshair: a small plus on the 16x16 grid, one row mask per row (bit c is column c). */
	private static final double[] DRAWN_DEFAULT = {0, 0, 0, 384, 384, 384, 384, 4080, 4080, 384, 384, 384, 384, 0, 0, 0};
	private static final int OUTLINE_ARGB = 0xE6000000;
	private static final int VANILLA_ARGB = 0xFFFFFFFF;

	static {
		for (int i : new int[]{1, 19, 21, 22, 23, 24, 25, 27}) SMALL[i] = true;
		for (int i = 1; i <= PRESET_COUNT; i++) {
			PRESET_TEX[i] = Identifier.fromNamespaceAndPath("glacier", "textures/editors/crosshairs/" + i + ".png");
		}
	}

	/** Vanilla, a pack crosshair texture (Preset) or the 16x16 pixel drawing (Drawn). */
	public final ChoiceSetting crosshairStyle = add(new ChoiceSetting("Crosshair Style", "Vanilla", "Vanilla", "Preset", "Drawn"));
	public final ToggleSetting crosshairOutline = add(new ToggleSetting("Crosshair Outlines", true));
	public final SliderSetting crosshairPreset = add(new SliderSetting("Crosshair Preset", 1, 1, PRESET_COUNT, 1));
	/** Size in GUI pixels of the texture / drawing, like the pack's 0..80 slider. */
	public final SliderSetting crosshairSize = add(new SliderSetting("Crosshair Pixels", 16, 8, 80, 4));
	public final ChoiceSetting crosshairColor = add(new ChoiceSetting("Crosshair Colour", "Default", COLOR_NAMES));
	private final SliderSetting[] drawnRows = new SliderSetting[16];
	public final ChoiceSetting hotbarStyle = add(new ChoiceSetting("Hotbar Style", "Vanilla",
			"Vanilla", "Default", "Connected", "Glacier", "Chroma"));
	public final ToggleSetting hideItemCount = add(new ToggleSetting("Hide Item Count", false));
	public final ChoiceSetting expColor = add(new ChoiceSetting("EXP Colour", "Default",
			"Default", "White", "Red", "Orange", "Yellow", "Blue", "Purple", "Pink", "Chroma"));
	public final ChoiceSetting displayColor = add(new ChoiceSetting("Display Colour", "Bright",
			"Bright", "Red", "Orange", "Yellow", "Green", "Aqua", "Blue", "Purple"));
	public final SliderSetting displayIntensity = add(new SliderSetting("Display Intensity", 3, 0, 10, 1));
	public final ToggleSetting slotNumbers = add(new ToggleSetting("Show Slot Numbers", false));
	public final ToggleSetting xpPercent = add(new ToggleSetting("Show XP Percentage", false));

	public EditorsModule() {
		super("Editors", "Crosshair, hotbar and EXP bar options.", Category.SYSTEM);
		for (int i = 0; i < 16; i++) {
			drawnRows[i] = add(new SliderSetting("Drawn Row " + i, DRAWN_DEFAULT[i], 0, 65535, 1));
			drawnRows[i].hide();
		}
	}

	@Override
	public void afterLoad() {
		setEnabled(true);
	}

	/** Full-screen display tint as ARGB; alpha 0 when intensity is 0. */
	public int displayTint() {
		int rgb = switch (displayColor.get()) {
			case "Red" -> 0xF04747;
			case "Orange" -> 0xFAA61A;
			case "Yellow" -> 0xFEE75C;
			case "Green" -> 0x43B581;
			case "Aqua" -> 0x4AD0E0;
			case "Blue" -> 0x4F7CE0;
			case "Purple" -> 0x9B59B6;
			default -> 0xFFFFFF;
		};
		int a = Math.round(255 * 0.03f * Math.max(0, Math.min(10, displayIntensity.i())));
		return (a << 24) | rgb;
	}

	/** True when a Glacier crosshair (texture or drawing) should replace the vanilla one. */
	public static boolean customCrosshair() {
		EditorsModule m = ModuleManager.get(EditorsModule.class);
		return m != null && !m.crosshairStyle.is("Vanilla");
	}

	public static int colorArgb(String name) {
		if ("Chroma".equals(name)) return chroma();
		for (int i = 0; i < COLOR_NAMES.length; i++) if (COLOR_NAMES[i].equals(name)) return COLOR_ARGB[i];
		return VANILLA_ARGB;
	}

	private static int chroma() {
		return 0xFF000000 | (java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 3000L) / 3000f, 0.8f, 1f) & 0xFFFFFF);
	}

	public static Identifier presetTexture(int idx) {
		return PRESET_TEX[Math.max(1, Math.min(PRESET_COUNT, idx))];
	}

	/** Pixel size of the source texture of a preset (15 or 16). */
	public static int presetTexSize(int idx) {
		return SMALL[Math.max(1, Math.min(PRESET_COUNT, idx))] ? 15 : 16;
	}

	public boolean drawnPixel(int row, int col) {
		return ((drawnRows[row].i() >> col) & 1) != 0;
	}

	public void toggleDrawnPixel(int row, int col) {
		drawnRows[row].set((double) (drawnRows[row].i() ^ (1 << col)));
	}

	public void clearDrawn() {
		for (SliderSetting r : drawnRows) r.set(0.0);
	}

	/** Draws a crosshair centred on (cx, cy). Used by the live HUD (MixinGui) and by the Editors page preview. */
	public void drawCrosshair(GuiGraphicsExtractor g, int cx, int cy, boolean custom) {
		if (!custom || crosshairStyle.is("Vanilla")) {
			plus(g, cx, cy, 6, 1, 1, 0, VANILLA_ARGB);
			return;
		}
		int size = Math.max(4, crosshairSize.i());
		int argb = colorArgb(crosshairColor.get());
		boolean drawn = crosshairStyle.is("Drawn");
		int x0 = cx - size / 2, y0 = cy - size / 2;
		if (crosshairOutline.on()) {
			stamp(g, drawn, x0 - 1, y0, size, OUTLINE_ARGB);
			stamp(g, drawn, x0 + 1, y0, size, OUTLINE_ARGB);
			stamp(g, drawn, x0, y0 - 1, size, OUTLINE_ARGB);
			stamp(g, drawn, x0, y0 + 1, size, OUTLINE_ARGB);
		}
		stamp(g, drawn, x0, y0, size, argb);
	}

	private void stamp(GuiGraphicsExtractor g, boolean drawn, int x0, int y0, int size, int argb) {
		if (!drawn) {
			int idx = crosshairPreset.i();
			int t = presetTexSize(idx);
			g.blit(RenderPipelines.GUI_TEXTURED, presetTexture(idx), x0, y0, 0f, 0f, size, size, t, t, t, t, argb);
			return;
		}
		for (int r = 0; r < 16; r++) {
			int mask = drawnRows[r].i();
			if (mask == 0) continue;
			int ya = y0 + Math.round(r * size / 16f), yb = y0 + Math.round((r + 1) * size / 16f);
			for (int c = 0; c < 16; c++) {
				if (((mask >> c) & 1) == 0) continue;
				int xa = x0 + Math.round(c * size / 16f), xb = x0 + Math.round((c + 1) * size / 16f);
				g.fill(xa, ya, xb, yb, argb);
			}
		}
	}

	private static void plus(GuiGraphicsExtractor g, int cx, int cy, int arm, int gap, int thick, int pad, int argb) {
		int lo = thick / 2;
		int x0 = cx - lo - pad;
		int x1 = cx - lo + thick + pad;
		int y0 = cy - lo - pad;
		int y1 = cy - lo + thick + pad;
		g.fill(x0, cy - gap - arm - pad, x1, cy - gap + pad, argb);
		g.fill(x0, cy + gap + 1 - pad, x1, cy + gap + 1 + arm + pad, argb);
		g.fill(cx - gap - arm - pad, y0, cx - gap + pad, y1, argb);
		g.fill(cx + gap + 1 - pad, y0, cx + gap + 1 + arm + pad, y1, argb);
	}
}
