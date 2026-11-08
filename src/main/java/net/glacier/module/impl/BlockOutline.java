package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.ColorSetting;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.util.Mth;

/** Recolours and thickens the outline drawn around the block you look at (see MixinBlockOutline). */
public class BlockOutline extends Module {
	private static BlockOutline instance;

	private final ColorSetting color = add(new ColorSetting("Color", 0xCCFFFFFF));
	private final ToggleSetting rainbow = add(new ToggleSetting("Rainbow", false));
	private final SliderSetting width = add(new SliderSetting("Width (0 = vanilla)", 0, 0, 8, 0.5));

	public BlockOutline() {
		super("Block Outline", "Custom colour and width for the targeted block outline.", Category.RENDER);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	/** ARGB colour to draw with. */
	public static int color() {
		BlockOutline b = instance;
		if (b.rainbow.on()) {
			float h = (System.currentTimeMillis() % 4000L) / 4000f;
			return 0xFF000000 | Mth.hsvToRgb(h, 0.8f, 1f);
		}
		return b.color.argb();
	}

	/** Line width, or the vanilla value when the width setting is 0. */
	public static float width(float vanilla) {
		float w = instance.width.f();
		return w <= 0f ? vanilla : w;
	}
}
