package net.glacier.hud;

import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * Shared look for every HUD that shows item slots (Armor, Mainhand, Offhand, Inventory): Bedrock-style bevelled or
 * plain frames, a durability bar under the item, and the same Frame Style / Slot Size / Durability Bar settings.
 */
public abstract class SlotHudModule extends HudModule {
	private static final int OUTLINE = 0xFF000000;
	private static final int BEVEL_MID = 0xFF8B8B8B;
	private static final int BEVEL_LIGHT = 0xFFC6C6C6;
	private static final int BEVEL_DARK = 0xFF555555;
	private static final int INNER = 0xCC2A2A14;

	protected final ChoiceSetting frame = add(new ChoiceSetting("Frame Style", "Framed", "Framed", "Plain", "None"));
	protected final SliderSetting slotSize;
	protected final ToggleSetting showBar = add(new ToggleSetting("Show Durability Bar", true));

	protected SlotHudModule(String name, String desc, int defaultSlotSize) {
		super(name, desc);
		slotSize = add(new SliderSetting("Slot Size", defaultSlotSize, 18, 32, 1));
	}

	protected final int slot() {
		return slotSize.i();
	}

	/** Draws a framed slot with the item centred and, for damageable items, the durability bar. */
	protected final void drawSlot(GuiGraphics g, ItemStack st, int x, int y) {
		int s = slot();
		drawFrame(g, x, y, s);
		if (st.isEmpty()) return;
		g.renderItem(st, x + (s - 16) / 2, y + (s - 16) / 2 - (showBar.on() ? 1 : 0));
		if (showBar.on() && st.isDamageableItem()) drawBar(g, st, x, y, s);
	}

	protected final void drawFrame(GuiGraphics g, int x, int y, int s) {
		if (frame.is("None")) return;
		int x2 = x + s, y2 = y + s;
		g.fill(x, y, x2, y2, OUTLINE);
		if (frame.is("Plain")) {
			g.fill(x + 1, y + 1, x2 - 1, y2 - 1, INNER);
			return;
		}
		g.fill(x + 1, y + 1, x2 - 1, y2 - 1, BEVEL_MID);
		g.fill(x + 1, y + 1, x2 - 1, y + 2, BEVEL_LIGHT);
		g.fill(x + 1, y + 2, x + 2, y2 - 1, BEVEL_LIGHT);
		g.fill(x + 2, y2 - 2, x2 - 1, y2 - 1, BEVEL_DARK);
		g.fill(x2 - 2, y + 2, x2 - 1, y2 - 2, BEVEL_DARK);
		g.fill(x + 3, y + 3, x2 - 3, y2 - 3, OUTLINE);
		g.fill(x + 4, y + 4, x2 - 4, y2 - 4, INNER);
	}

	protected final void drawBar(GuiGraphics g, ItemStack st, int x, int y, int s) {
		int inset = 5;
		int bx = x + inset, bw = s - inset * 2;
		int by = y + s - 7;
		g.fill(bx, by, bx + bw, by + 3, OUTLINE);
		int fw = Math.round(bw * frac(st));
		if (fw > 0) g.fill(bx, by + 1, bx + fw, by + 2, durabilityColor(st));
	}

	/** Remaining durability, 0..1. */
	protected static float frac(ItemStack st) {
		int max = Math.max(1, st.getMaxDamage());
		return Math.max(0f, Math.min(1f, (max - st.getDamageValue()) / (float) max));
	}

	/** Green (#7CFC00) at full, yellow at half, red when broken. */
	protected static int durabilityColor(ItemStack st) {
		float f = frac(st);
		int r, gr;
		if (f >= 0.5f) {
			float t = (f - 0.5f) * 2f;
			r = (int) (255 + (0x7C - 255) * t);
			gr = (int) (255 + (0xFC - 255) * t);
		} else {
			r = 255;
			gr = (int) (255 * f * 2f);
		}
		return 0xFF000000 | r << 16 | gr << 8;
	}
}
