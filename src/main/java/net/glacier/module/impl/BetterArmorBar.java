package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.ColorSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;

/**
 * Replaces the vanilla armor icons above the hearts with a slim bar (0-20 armor points) and an optional number.
 * MixinBetterArmorBar cancels Gui.renderArmor (verified with javap: private static, called once from
 * renderPlayerHealth with the same x/y it uses for the icons) and calls {@link #render} instead.
 */
public class BetterArmorBar extends Module {
	private static BetterArmorBar instance;
	private static final String[] NUMBERS = new String[41];

	private final ChoiceSetting style = add(new ChoiceSetting("Style", "Segmented", "Segmented", "Solid"));
	private final ToggleSetting number = add(new ToggleSetting("Show Number", true));
	private final ColorSetting color = add(new ColorSetting("Color", 0xFF9FD8FF));
	private final ColorSetting fullColor = add(new ColorSetting("Full Color", 0xFF55E6C1));

	public BetterArmorBar() {
		super("Better Armor Bar", "Replaces the armor icons with a clean armor bar.", Category.HUD);
		instance = this;
	}

	/** True when the vanilla armor icons should be skipped in favour of {@link #render}. */
	public static boolean replacesVanilla() {
		BetterArmorBar b = instance;
		return b != null && b.isEnabled();
	}

	private static String text(int v) {
		if (v < 0 || v >= NUMBERS.length) return Integer.toString(v);
		String s = NUMBERS[v];
		if (s == null) NUMBERS[v] = s = Integer.toString(v);
		return s;
	}

	/** Draws the bar in the 81x9 area the vanilla armor row occupies; x,y is its top-left corner. */
	public static void render(GuiGraphicsExtractor g, Player player, int x, int y) {
		BetterArmorBar b = instance;
		if (b == null) return;
		int armor = player.getArmorValue();
		if (armor <= 0) return;
		int w = 81;
		int top = y + 2;
		int h = 5;
		int fill = armor >= 20 ? b.fullColor.argb() : b.color.argb();
		g.fill(x - 1, top - 1, x + w + 1, top + h + 1, 0xCC000000);
		g.fill(x, top, x + w, top + h, 0xFF2A2D33);
		int filled = Math.min(w, armor * w / 20);
		if (b.style.is("Segmented")) {
			for (int i = 0; i < 10; i++) {
				int pts = armor - i * 2;
				if (pts <= 0) break;
				int sx = x + i * 8;
				int segEnd = pts >= 2 ? sx + 8 : sx + 4;
				g.fill(sx, top, Math.min(segEnd, x + w), top + h, fill);
			}
			for (int i = 1; i < 10; i++) g.fill(x + i * 8, top, x + i * 8 + 1, top + h, 0x66000000);
		} else {
			g.fill(x, top, x + filled, top + h, fill);
			g.fill(x, top, x + filled, top + 1, 0x55FFFFFF);
		}
		if (b.number.on()) {
			Minecraft mc = Minecraft.getInstance();
			String t = text(armor);
			g.text(mc.font, t, x - 2 - mc.font.width(t), y, 0xFFFFFFFF, true);
		}
	}
}
