package net.glacier.hud.impl;

import net.glacier.hud.HudModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** The held item icon with how many of that item the inventory holds in total. Counted once per tick. */
public class CrosshairCount extends HudModule {
	private static final int SLOT = 16;
	private static final int GAP = 3;
	private ItemStack held = ItemStack.EMPTY;
	private int total;
	/** "x" + total, rebuilt only when the total changes. */
	private String countText = "x0";
	private final WidthCache countWidth = new WidthCache();

	public CrosshairCount() {
		super("Crosshair Count", "Shows how many of your held item you carry in total.");
		setPos(0.5f, 0.56f);
	}

	@Override
	public void onTick() {
		ItemStack st = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
		int n = 0;
		if (!st.isEmpty() && mc.player != null) {
			Inventory inv = mc.player.getInventory();
			for (int i = 0; i < inv.getContainerSize(); i++) {
				ItemStack s = inv.getItem(i);
				if (s.is(st.getItem())) n += s.getCount();
			}
		}
		if (st == held && n == total) return;
		held = st;
		total = n;
		countText = "x" + n;
	}

	@Override
	public void render(GuiGraphicsExtractor g, int sw, int sh) {
		if (!held.isEmpty()) super.render(g, sw, sh);
	}

	@Override
	public int contentWidth() {
		return SLOT + GAP + cachedWidth(countWidth, countText);
	}

	@Override
	public int contentHeight() {
		return SLOT;
	}

	@Override
	protected void drawContent(GuiGraphicsExtractor g, int x, int y) {
		if (held.isEmpty()) return;
		g.item(held, x, y);
		drawText(g, countText, x + SLOT + GAP, y + (SLOT - lineHeight()) / 2 + 1, TEXT);
	}
}
