package net.glacier.hud.impl;

import net.glacier.hud.SlotHudModule;
import net.glacier.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/** The item in the off hand with its count (stackables) or remaining durability. Hidden when empty. */
public class OffhandHud extends SlotHudModule {
	private static final int GAP = 4;
	private ItemStack stack = ItemStack.EMPTY;
	private String text = "";
	private final WidthCache textW = new WidthCache();

	public OffhandHud() {
		super("Offhand HUD", "Shows the item in your off hand with its count or durability.", 22);
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
	public void render(GuiGraphicsExtractor g, int sw, int sh) {
		if (!stack.isEmpty()) super.render(g, sw, sh);
	}

	@Override
	public int contentWidth() {
		return slot() + (text.isEmpty() ? 0 : GAP + cachedWidth(textW, text));
	}

	@Override
	public int contentHeight() {
		return slot();
	}

	@Override
	protected void drawContent(GuiGraphicsExtractor g, int x, int y) {
		if (stack.isEmpty()) return;
		drawSlot(g, stack, x, y);
		if (!text.isEmpty()) drawText(g, text, x + slot() + GAP, y + (slot() - lineHeight()) / 2 + 1, TEXT);
	}
}
