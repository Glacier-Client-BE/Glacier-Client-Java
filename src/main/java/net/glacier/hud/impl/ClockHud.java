package net.glacier.hud.impl;

import net.glacier.hud.HudModule;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.LocalTime;

/**
 * Real-time clock, HH:mm (or h:mm AM/PM), followed by optional compass and recovery compass item icons.
 * The time is formatted once per minute, in onTick.
 */
public class ClockHud extends HudModule {
	private static final int SLOT = 16;
	private static final int GAP = 3;
	private final ToggleSetting twelveHour = add(new ToggleSetting("12 Hour", false));
	private final ToggleSetting showClock = add(new ToggleSetting("Show Clock", true));
	private final ToggleSetting showCompass = add(new ToggleSetting("Show Compass", false));
	private final ToggleSetting showRecovery = add(new ToggleSetting("Show Recovery Compass", false));
	private int lastKey = -1;
	private String text = "00:00";
	private ItemStack compass;
	private ItemStack recovery;

	public ClockHud() {
		super("Clock & Compass", "Shows the real-world time with compass items.");
		stackTopLeft(13);
	}

	private int itemCount() {
		return (showCompass.on() ? 1 : 0) + (showRecovery.on() ? 1 : 0);
	}

	@Override
	public void onTick() {
		LocalTime t = LocalTime.now();
		int h = t.getHour();
		int m = t.getMinute();
		boolean twelve = twelveHour.on();
		int key = (h * 60 + m) * 2 + (twelve ? 1 : 0);
		if (key == lastKey) return;
		lastKey = key;
		String mm = (m < 10 ? "0" : "") + m;
		if (twelve) {
			int hh = h % 12 == 0 ? 12 : h % 12;
			text = hh + ":" + mm + (h < 12 ? " AM" : " PM");
		} else {
			text = (h < 10 ? "0" : "") + h + ":" + mm;
		}
	}

	@Override
	public int contentWidth() {
		int w = showClock.on() ? textWidth(text) : 0;
		int n = itemCount();
		if (n > 0) w += (w > 0 ? GAP : 0) + n * SLOT + (n - 1) * GAP;
		return w;
	}

	@Override
	public int contentHeight() {
		return itemCount() > 0 ? SLOT : lineHeight();
	}

	@Override
	protected void drawContent(GuiGraphics g, int x, int y) {
		int cx = x;
		int ty = y + (contentHeight() - lineHeight()) / 2;
		if (showClock.on()) {
			drawText(g, text, cx, ty, TEXT);
			cx += textWidth(text) + (itemCount() > 0 ? GAP : 0);
		}
		if (showCompass.on()) {
			if (compass == null) compass = new ItemStack(Items.COMPASS);
			g.renderItem(compass, cx, y);
			cx += SLOT + GAP;
		}
		if (showRecovery.on()) {
			if (recovery == null) recovery = new ItemStack(Items.RECOVERY_COMPASS);
			g.renderItem(recovery, cx, y);
		}
	}
}
