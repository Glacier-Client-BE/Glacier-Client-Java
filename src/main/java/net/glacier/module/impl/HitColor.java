package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.ColorSetting;
import net.glacier.setting.SliderSetting;

/**
 * Recolors the red damage flash on hurt entities, like the Bedrock HitColor pack. The hurt tint lives in the top
 * half of the 16x16 overlay texture (see PkOverlayTexture); this module repaints those rows when its settings
 * change and restores vanilla red on disable. Chroma re-uploads the tiny texture every other tick.
 */
public class HitColor extends Module {
	/** Implemented by PkOverlayTexture so the module can repaint the overlay without touching private fields. */
	public interface Recolorable {
		void glacier$paintHurt(int argb);
	}

	private static final int VANILLA = 0xB2FF0000;
	private static HitColor instance;
	private static Recolorable overlay;

	private final ChoiceSetting color = add(new ChoiceSetting("Color", "Blue", "Black", "Blue", "Chroma", "Green",
			"None", "Orange", "Pink", "Purple", "White", "Yellow", "Red", "Custom"));
	private final ColorSetting custom = add(new ColorSetting("Custom Color", 0xFF00FFFF));
	private final SliderSetting opacity = add(new SliderSetting("Opacity %", 70, 10, 100, 5));

	private int lastApplied = VANILLA;
	private int tickCount;

	public HitColor() {
		super("Hit Color", "Changes the color of the flash shown when something is hurt.", Category.RENDER);
		instance = this;
	}

	/** Called from the OverlayTexture constructor mixin once the texture exists. */
	public static void attach(Recolorable o) {
		overlay = o;
		HitColor h = instance;
		if (h != null && h.isEnabled()) h.apply(true);
	}

	@Override
	protected void onEnable() {
		apply(true);
	}

	@Override
	protected void onDisable() {
		Recolorable o = overlay;
		if (o != null) o.glacier$paintHurt(VANILLA);
		lastApplied = VANILLA;
	}

	@Override
	public void onTick() {
		tickCount++;
		if (color.is("Chroma")) {
			if ((tickCount & 1) == 0) apply(false);
		} else {
			apply(false);
		}
	}

	private void apply(boolean force) {
		Recolorable o = overlay;
		if (o == null) return;
		int a = Math.round(opacity.i() * 2.55f);
		int rgb = switch (color.get()) {
			case "Black" -> 0x000000;
			case "Blue" -> 0x3050FF;
			case "Green" -> 0x30E040;
			case "Orange" -> 0xFF8A10;
			case "Pink" -> 0xFF60C0;
			case "Purple" -> 0x9030E0;
			case "White" -> 0xFFFFFF;
			case "Yellow" -> 0xFFE820;
			case "Red" -> 0xFF0000;
			case "Custom" -> custom.argb() & 0xFFFFFF;
			case "Chroma" -> java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 3000L) / 3000f, 0.9f, 1f) & 0xFFFFFF;
			default -> 0;
		};
		if (color.is("None")) a = 0;
		int argb = (a << 24) | rgb;
		if (!force && argb == lastApplied) return;
		lastApplied = argb;
		o.glacier$paintHurt(argb);
	}
}
