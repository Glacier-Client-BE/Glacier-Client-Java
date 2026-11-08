package net.glacier.hud.impl;

import net.glacier.hud.HudModule;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/** The held item icon followed by its durability (damageable items) or stack count. Text is rebuilt per tick. */
public class MainhandHud extends HudModule {
	private static final int SLOT = 16;
	private static final int GAP = 4;
	private final ChoiceSetting durability = add(new ChoiceSetting("Durability", "Absolute", "Absolute", "Percentage"));
	private final ToggleSetting showTotal = add(new ToggleSetting("Show Total Durability", false));
	private final ToggleSetting showDecimals = add(new ToggleSetting("Show Decimals", false));
	private final ToggleSetting durabilityColour = add(new ToggleSetting("Enable Durability Colour", false));
	private final ToggleSetting showCount = add(new ToggleSetting("Show Item Count", true));
	private final ToggleSetting showTotalStack = add(new ToggleSetting("Show Total Stack", false));
	private final ToggleSetting showIcon = add(new ToggleSetting("Show Item Icon", true));
	private final ToggleSetting showName = add(new ToggleSetting("Show Name", false));
	private ItemStack stack = ItemStack.EMPTY;
	private String text = "";
	private int color = TEXT;

	public MainhandHud() {
		super("Mainhand HUD", "Shows your held item with its count or durability.");
		setPos(0.92f, 0.85f);
	}

	@Override
	public void onTick() {
		ItemStack st = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
		String v = "";
		int col = TEXT;
		if (!st.isEmpty()) {
			if (st.isDamageableItem()) {
				int max = Math.max(1, st.getMaxDamage());
				int left = max - st.getDamageValue();
				if (durability.is("Percentage")) {
					int tenths = left * 1000 / max;
					v = showDecimals.on() ? tenths / 10 + "." + tenths % 10 + "%" : left * 100 / max + "%";
				} else {
					v = showTotal.on() ? left + "/" + max : String.valueOf(left);
				}
				if (durabilityColour.on()) col = goodHigh(left * 100.0 / max, 50, 20);
			} else {
				int max = Math.max(1, st.getMaxStackSize());
				String c = showCount.on() ? String.valueOf(st.getCount()) : "";
				if (showTotalStack.on()) c = (c.isEmpty() ? "" : c + "/") + max;
				v = c;
			}
			if (showName.on()) v = st.getHoverName().getString() + (v.isEmpty() ? "" : " " + v);
		}
		if (st == stack && v.equals(text) && col == color) return;
		stack = st;
		text = v;
		color = col;
	}

	private boolean iconShown() {
		return showIcon.on() && !stack.isEmpty();
	}

	@Override
	public int contentWidth() {
		if (stack.isEmpty()) return SLOT;
		int w = showIcon.on() ? SLOT : 0;
		if (!text.isEmpty()) w += (w > 0 ? GAP : 0) + textWidth(text);
		return w;
	}

	@Override
	public int contentHeight() {
		return SLOT;
	}

	@Override
	protected void drawContent(GuiGraphics g, int x, int y) {
		if (stack.isEmpty()) return;
		int tx = x;
		if (iconShown()) {
			g.renderItem(stack, x, y);
			tx += SLOT + GAP;
		}
		if (!text.isEmpty()) drawText(g, text, tx, y + (SLOT - lineHeight()) / 2 + 1, color);
	}
}
