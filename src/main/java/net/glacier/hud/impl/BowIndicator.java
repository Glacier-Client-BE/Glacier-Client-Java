package net.glacier.hud.impl;

import net.glacier.hud.HudModule;
import net.glacier.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.BowItem;

/** A draw-progress bar while a bow is held drawn back. Hidden otherwise. Green at full power. */
public class BowIndicator extends HudModule {
	private static final int BAR_W = 60;
	private static final int BAR_H = 6;
	private static final int TRACK = 0x80000000;

	public BowIndicator() {
		super("Bow Indicator", "Shows how far your bow is drawn back.");
		setPos(0.5f, 0.62f);
	}

	private boolean drawing() {
		var p = mc.player;
		return p != null && p.isUsingItem() && p.getUseItem().getItem() instanceof BowItem;
	}

	@Override
	public void render(GuiGraphicsExtractor g, int sw, int sh) {
		if (drawing()) super.render(g, sw, sh);
	}

	@Override
	public int contentWidth() {
		return BAR_W;
	}

	@Override
	public int contentHeight() {
		return BAR_H;
	}

	@Override
	protected void drawContent(GuiGraphicsExtractor g, int x, int y) {
		float power = mc.player == null ? 0f : BowItem.getPowerForTime(mc.player.getTicksUsingItem());
		power = Math.max(0f, Math.min(1f, power));
		g.fill(x, y, x + BAR_W, y + BAR_H, TRACK);
		int fill = Math.round(BAR_W * power);
		if (fill > 0) g.fill(x, y, x + fill, y + BAR_H, power >= 1f ? Theme.HUD_GOOD : Theme.HUD_WARN);
	}
}
