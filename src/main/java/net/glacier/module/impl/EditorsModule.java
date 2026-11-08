package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.module.ModuleManager;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.gui.GuiGraphics;

/** Backs the Editors page: crosshair, hotbar and EXP bar options. Hooks are read by MixinGui and HudExtras. */
public class EditorsModule extends Module {
	private static final String[] COLOR_NAMES = {
			"Default", "White", "Red", "Orange", "Yellow", "Green", "Aqua", "Blue", "Purple", "Pink"
	};
	private static final int[] COLOR_ARGB = {
			0xFFFFFFFF, 0xFFFFFFFF, 0xFFF04747, 0xFFFAA61A, 0xFFFEE75C,
			0xFF43B581, 0xFF4AD0E0, 0xFF7289DA, 0xFF9B59B6, 0xFFEB459E
	};
	/** {arm length, half gap around centre, thickness} for presets 1..4. */
	private static final int[][] PRESETS = {{4, 2, 1}, {6, 3, 1}, {5, 0, 2}, {3, 1, 1}};
	private static final int OUTLINE_ARGB = 0xE6000000;
	private static final int VANILLA_ARGB = 0xFFFFFFFF;

	public final ChoiceSetting crosshairStyle = add(new ChoiceSetting("Crosshair Style", "Vanilla", "Vanilla", "Preset"));
	public final ToggleSetting crosshairOutline = add(new ToggleSetting("Crosshair Outlines", true));
	public final SliderSetting crosshairPreset = add(new SliderSetting("Crosshair Preset", 1, 1, 4, 1));
	public final SliderSetting crosshairSize = add(new SliderSetting("Crosshair Size", 1.0, 0.5, 3.0, 0.1));
	public final ChoiceSetting crosshairColor = add(new ChoiceSetting("Crosshair Colour", "Default", COLOR_NAMES));
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

	/** True when the Glacier preset crosshair should replace the vanilla one. */
	public static boolean customCrosshair() {
		EditorsModule m = ModuleManager.get(EditorsModule.class);
		return m != null && m.crosshairStyle.is("Preset");
	}

	/** Draws a crosshair centred on (cx, cy). Used by the live HUD and by the Editors page preview. */
	public void drawCrosshair(GuiGraphics g, int cx, int cy, boolean preset) {
		if (!preset) {
			drawPlus(g, cx, cy, 6, 1, 1, VANILLA_ARGB, false);
			return;
		}
		int[] p = PRESETS[Math.max(0, Math.min(PRESETS.length - 1, crosshairPreset.i() - 1))];
		float s = crosshairSize.f();
		int arm = Math.max(1, Math.round(p[0] * s));
		int gap = Math.round(p[1] * s);
		int thick = Math.max(1, Math.round(p[2] * s));
		int idx = Math.max(0, Math.min(COLOR_ARGB.length - 1, crosshairColor.index()));
		drawPlus(g, cx, cy, arm, gap, thick, COLOR_ARGB[idx], crosshairOutline.on());
	}

	private static void drawPlus(GuiGraphics g, int cx, int cy, int arm, int gap, int thick, int argb, boolean outline) {
		if (outline) plus(g, cx, cy, arm, gap, thick, 1, OUTLINE_ARGB);
		plus(g, cx, cy, arm, gap, thick, 0, argb);
	}

	private static void plus(GuiGraphics g, int cx, int cy, int arm, int gap, int thick, int pad, int argb) {
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
