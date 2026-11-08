package net.glacier.hud.impl;

import net.glacier.hud.HudModule;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Worn armour in Bedrock-style bevelled slots with a durability bar, cached once per game tick. */
public class ArmorHud extends HudModule {
	private static final EquipmentSlot[] ORDER = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};
	private static final Identifier[] EMPTY_SPRITES = {
			Identifier.withDefaultNamespace("container/slot/helmet"),
			Identifier.withDefaultNamespace("container/slot/chestplate"),
			Identifier.withDefaultNamespace("container/slot/leggings"),
			Identifier.withDefaultNamespace("container/slot/boots")
	};
	private static final int MAX = 5;
	private static final int NUM_GAP = 3;
	private static final int OUTLINE = 0xFF000000;
	private static final int BEVEL_MID = 0xFF8B8B8B;
	private static final int BEVEL_LIGHT = 0xFFC6C6C6;
	private static final int BEVEL_DARK = 0xFF555555;
	private static final int INNER = 0xCC2A2A14;
	private static final int SILHOUETTE = 0x59FFFFFF;

	private final ChoiceSetting orientation = add(new ChoiceSetting("Orientation", "Vertical", "Vertical", "Horizontal"));
	private final ToggleSetting showHeld = add(new ToggleSetting("Show Held Item", false));
	private final ToggleSetting hideEmpty = add(new ToggleSetting("Hide Empty", true));
	private final ToggleSetting showBar = add(new ToggleSetting("Show Durability Bar", true));
	private final ToggleSetting showNumber = add(new ToggleSetting("Show Durability Number", false));
	private final ChoiceSetting frame = add(new ChoiceSetting("Frame Style", "Framed", "Framed", "Plain", "None"));
	private final SliderSetting slotSize = add(new SliderSetting("Slot Size", 22, 18, 32, 1));

	private final ItemStack[] stacks = new ItemStack[MAX];
	private final int[] sprite = new int[MAX];
	private final int[] lastDur = new int[MAX];
	private final String[] durText = new String[MAX];
	private final int[] durW = new int[MAX];
	private int count;
	private int numW;
	private int lastTick = Integer.MIN_VALUE;

	public ArmorHud() {
		super("Armor HUD", "Displays your equipped armor and its durability.");
		setPos(0.92f, 0.45f);
		for (int i = 0; i < MAX; i++) lastDur[i] = -1;
	}

	/** Rebuilds the cached slot list at most once per game tick (or immediately when settings change the shape). */
	private void refresh() {
		int tick = mc.player == null ? -1 : mc.player.tickCount;
		int key = tick * 8 + (hideEmpty.on() ? 1 : 0) + (showHeld.on() ? 2 : 0) + (showNumber.on() ? 4 : 0);
		if (key == lastTick) return;
		lastTick = key;
		count = 0;
		numW = 0;
		if (mc.player == null) return;
		for (int i = 0; i < ORDER.length; i++) {
			ItemStack st = mc.player.getItemBySlot(ORDER[i]);
			if (st.isEmpty() && hideEmpty.on()) continue;
			put(st, i);
		}
		if (showHeld.on()) {
			ItemStack main = mc.player.getMainHandItem();
			if (!main.isEmpty() || !hideEmpty.on()) put(main, -1);
		}
	}

	private void put(ItemStack st, int spriteIdx) {
		int i = count++;
		stacks[i] = st;
		sprite[i] = spriteIdx;
		if (!showNumber.on() || st.isEmpty() || !st.isDamageableItem()) {
			durText[i] = null;
			return;
		}
		int d = st.getMaxDamage() - st.getDamageValue();
		if (d != lastDur[i] || durText[i] == null) {
			lastDur[i] = d;
			durText[i] = String.valueOf(d);
			durW[i] = textWidth(durText[i]);
		}
		numW = Math.max(numW, durW[i]);
	}

	private boolean horizontal() {
		return orientation.is("Horizontal");
	}

	private int size() {
		return slotSize.i();
	}

	@Override
	public int contentWidth() {
		refresh();
		int n = Math.max(1, count);
		if (horizontal()) return n * size();
		return size() + (numW > 0 ? NUM_GAP + numW : 0);
	}

	@Override
	public int contentHeight() {
		refresh();
		int n = Math.max(1, count);
		if (horizontal()) return size() + (numW > 0 ? NUM_GAP + lineHeight() : 0);
		return n * size();
	}

	@Override
	protected void drawContent(GuiGraphics g, int x, int y) {
		refresh();
		int s = size();
		boolean hor = horizontal();
		for (int i = 0; i < count; i++) {
			int sx = hor ? x + i * s : x;
			int sy = hor ? y : y + i * s;
			drawFrame(g, sx, sy, s);
			ItemStack st = stacks[i];
			int ix = sx + (s - 16) / 2;
			int iy = sy + (s - 16) / 2 - (showBar.on() ? 1 : 0);
			if (st.isEmpty()) {
				if (sprite[i] >= 0)
					g.blitSprite(RenderPipelines.GUI_TEXTURED, EMPTY_SPRITES[sprite[i]], ix, iy, 16, 16, SILHOUETTE);
				continue;
			}
			g.renderItem(st, ix, iy);
			if (showBar.on() && st.isDamageableItem()) drawBar(g, st, sx, sy, s);
			String t = durText[i];
			if (t != null) {
				int col = durabilityColor(st);
				if (hor) drawText(g, t, sx + (s - durW[i]) / 2, sy + s + NUM_GAP, col);
				else drawText(g, t, sx + s + NUM_GAP, sy + (s - lineHeight()) / 2 + 1, col);
			}
		}
	}

	private void drawFrame(GuiGraphics g, int x, int y, int s) {
		if (frame.is("None")) return;
		int x2 = x + s, y2 = y + s;
		if (frame.is("Plain")) {
			g.fill(x, y, x2, y2, OUTLINE);
			g.fill(x + 1, y + 1, x2 - 1, y2 - 1, INNER);
			return;
		}
		g.fill(x, y, x2, y2, OUTLINE);
		g.fill(x + 1, y + 1, x2 - 1, y2 - 1, BEVEL_MID);
		g.fill(x + 1, y + 1, x2 - 1, y + 2, BEVEL_LIGHT);
		g.fill(x + 1, y + 2, x + 2, y2 - 1, BEVEL_LIGHT);
		g.fill(x + 2, y2 - 2, x2 - 1, y2 - 1, BEVEL_DARK);
		g.fill(x2 - 2, y + 2, x2 - 1, y2 - 2, BEVEL_DARK);
		g.fill(x + 3, y + 3, x2 - 3, y2 - 3, OUTLINE);
		g.fill(x + 4, y + 4, x2 - 4, y2 - 4, INNER);
	}

	private static void drawBar(GuiGraphics g, ItemStack st, int x, int y, int s) {
		int inset = 5;
		int bx = x + inset, bw = s - inset * 2;
		int by = y + s - 7;
		g.fill(bx, by, bx + bw, by + 3, OUTLINE);
		int fw = Math.round(bw * frac(st));
		if (fw > 0) g.fill(bx, by + 1, bx + fw, by + 2, durabilityColor(st));
	}

	private static float frac(ItemStack st) {
		int max = Math.max(1, st.getMaxDamage());
		return Math.max(0f, Math.min(1f, (max - st.getDamageValue()) / (float) max));
	}

	/** Green (#7CFC00) at full, yellow at half, red when broken. */
	private static int durabilityColor(ItemStack st) {
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
