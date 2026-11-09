package net.glacier.hud.impl;

import net.glacier.hud.SlotHudModule;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.ColorSetting;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Worn armour in Bedrock-style bevelled slots with a durability bar, cached once per game tick. Optional extras:
 * reversed order, the offhand slot, durability as a percentage, and a warning colour below a durability percentage.
 */
public class ArmorHud extends SlotHudModule {
	private static final EquipmentSlot[] ORDER = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};
	private static final Identifier[] EMPTY_SPRITES = {
			Identifier.withDefaultNamespace("container/slot/helmet"),
			Identifier.withDefaultNamespace("container/slot/chestplate"),
			Identifier.withDefaultNamespace("container/slot/leggings"),
			Identifier.withDefaultNamespace("container/slot/boots")
	};
	private static final int MAX = 6;
	private static final int NUM_GAP = 3;
	private static final int SILHOUETTE = 0x59FFFFFF;

	private final ChoiceSetting orientation = add(new ChoiceSetting("Orientation", "Vertical", "Vertical", "Horizontal"));
	private final ToggleSetting hideEmpty = add(new ToggleSetting("Hide Empty", true));
	private final ToggleSetting showNumber = add(new ToggleSetting("Show Durability Number", false));
	private final ChoiceSetting numberFormat = add(new ChoiceSetting("Number Format", "Remaining", "Remaining", "Percent"));
	private final ToggleSetting reversed = add(new ToggleSetting("Reversed Order", false));
	/** Durability percentage under which the warning colour is used; 0 turns the warning off. */
	private final SliderSetting warnBelow = add(new SliderSetting("Warning Below %", 0, 0, 100, 5));
	private final ColorSetting warnColor = add(new ColorSetting("Warning Color", Theme.HUD_BAD));

	private final ItemStack[] stacks = new ItemStack[MAX];
	private final int[] sprite = new int[MAX];
	private final String[] durText = new String[MAX];
	private final int[] durW = new int[MAX];
	private final int[] durKey = new int[MAX];
	private int count;
	private int numW;
	private int lastTick = Integer.MIN_VALUE;
	private long lastFrame = -1;

	public ArmorHud() {
		super("Armor HUD", "Displays your equipped armor and its durability.", 22);
		setPos(0.92f, 0.45f);
	}

	/** Rebuilds the cached slot list at most once per game tick (or immediately when settings change the shape). */
	private void refresh() {
		long frame = System.nanoTime() >> 20;
		if (frame == lastFrame) return;
		lastFrame = frame;
		int tick = mc.player == null ? -1 : mc.player.tickCount;
		int key = tick * 64 + (hideEmpty.on() ? 1 : 0) + (showNumber.on() ? 4 : 0)
				+ (reversed.on() ? 8 : 0) + (numberFormat.is("Percent") ? 32 : 0);
		if (key == lastTick) return;
		lastTick = key;
		count = 0;
		numW = 0;
		if (mc.player == null) return;
		for (int n = 0; n < ORDER.length; n++) {
			int i = reversed.on() ? ORDER.length - 1 - n : n;
			ItemStack st = mc.player.getItemBySlot(ORDER[i]);
			if (st.isEmpty() && hideEmpty.on()) continue;
			put(st, i);
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
		boolean pct = numberFormat.is("Percent");
		int v = pct ? Math.round(frac(st) * 100f) : st.getMaxDamage() - st.getDamageValue();
		// the text and its width are only rebuilt when the number shown in this slot changes
		int k = pct ? ~v : v;
		if (durText[i] == null || durKey[i] != k) {
			durKey[i] = k;
			durText[i] = pct ? v + "%" : String.valueOf(v);
			durW[i] = textWidth(durText[i]);
		}
		numW = Math.max(numW, durW[i]);
	}

	private boolean horizontal() {
		return orientation.is("Horizontal");
	}

	private int size() {
		return slot();
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
	protected void drawContent(GuiGraphicsExtractor g, int x, int y) {
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
			g.item(st, ix, iy);
			if (showBar.on() && st.isDamageableItem()) drawBar(g, st, sx, sy, s);
			String t = durText[i];
			if (t != null) {
				int col = colorFor(st);
				if (hor) drawText(g, t, sx + (s - durW[i]) / 2, sy + s + NUM_GAP, col);
				else drawText(g, t, sx + s + NUM_GAP, sy + (s - lineHeight()) / 2 + 1, col);
			}
		}
	}

	/** The warning colour under the Warning Below percentage, otherwise the gradient colour. */
	private int colorFor(ItemStack st) {
		if (warnBelow.i() > 0 && frac(st) * 100f < warnBelow.i()) return warnColor.argb();
		return durabilityColor(st);
	}
}
