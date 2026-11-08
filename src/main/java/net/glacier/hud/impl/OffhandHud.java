package net.glacier.hud.impl;

import net.glacier.hud.HudModule;
import net.glacier.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/** The item in the off hand with its count (stackables) or remaining durability. Hidden when empty. */
public class OffhandHud extends HudModule {
	private static final int SLOT = 16;
	private static final int GAP = 4;
	private ItemStack stack = ItemStack.EMPTY;
	private String text = "";

	public OffhandHud() {
		super("Offhand HUD", "Shows the item in your off hand with its count or durability.");
		setPos(0.08f, 0.92f);
	}

	@Override
	public void onTick() {
		ItemStack st = mc.player == null ? ItemStack.EMPTY : mc.player.getOffhandItem();
		String v = "";
		if (st.isDamageableItem()) {
			int max = Math.max(1, st.getMaxDamage());
			v = String.valueOf(max - st.getDamageValue());
		} else if (st.getCount() > 1) {
			v = String.valueOf(st.getCount());
		}
		if (st == stack && v.equals(text)) return;
		stack = st;
		text = v;
	}

	@Override
	public void render(GuiGraphics g, int sw, int sh) {
		if (!stack.isEmpty()) super.render(g, sw, sh);
	}

	@Override
	public int contentWidth() {
		return SLOT + (text.isEmpty() ? 0 : GAP + textWidth(text));
	}

	@Override
	public int contentHeight() {
		return SLOT;
	}

	@Override
	protected void drawContent(GuiGraphics g, int x, int y) {
		if (stack.isEmpty()) return;
		g.renderItem(stack, x, y);
		if (!text.isEmpty()) drawText(g, text, x + SLOT + GAP, y + (SLOT - lineHeight()) / 2 + 1, TEXT);
	}
}
