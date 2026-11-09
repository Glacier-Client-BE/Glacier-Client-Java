package net.glacier.hud.impl;

import net.glacier.hud.HudModule;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.LocalDateTime;

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
	private final ToggleSetting showSeconds = add(new ToggleSetting("Show Seconds", false));
	private final ToggleSetting showDate = add(new ToggleSetting("Show Date", false));
	private final ChoiceSetting dateFormat = add(new ChoiceSetting("Date Format", "DD/MM", "DD/MM", "MM/DD", "YYYY-MM-DD"));
	private long lastKey = -1;
	private String text = "00:00";
	private ItemStack compass;
	private ItemStack recovery;
	private final WidthCache textWidthCache = new WidthCache();

	public ClockHud() {
		super("Clock & Compass", "Shows the real-world time with compass items.");
		stackTopLeft(13);
	}

	private int itemCount() {
		return (showCompass.on() ? 1 : 0) + (showRecovery.on() ? 1 : 0);
	}

	private static String two(int v) {
		return (v < 10 ? "0" : "") + v;
	}

	@Override
	public void onTick() {
		LocalDateTime t = LocalDateTime.now();
		int h = t.getHour();
		int m = t.getMinute();
		int sec = showSeconds.on() ? t.getSecond() : 0;
		boolean twelve = twelveHour.on();
		boolean date = showDate.on();
		long key = ((t.getDayOfYear() * 24L + h) * 60 + m) * 60 + sec;
		key = key * 32 + (twelve ? 1 : 0) + (showSeconds.on() ? 2 : 0) + (date ? 4 : 0) + dateFormat.index() * 8L;
		if (key == lastKey) return;
		lastKey = key;
		StringBuilder sb = new StringBuilder(24);
		if (twelve) sb.append(h % 12 == 0 ? 12 : h % 12);
		else sb.append(two(h));
		sb.append(':').append(two(m));
		if (showSeconds.on()) sb.append(':').append(two(sec));
		if (twelve) sb.append(h < 12 ? " AM" : " PM");
		if (date) {
			sb.append("  ");
			switch (dateFormat.index()) {
				case 1 -> sb.append(two(t.getMonthValue())).append('/').append(two(t.getDayOfMonth()));
				case 2 -> sb.append(t.getYear()).append('-').append(two(t.getMonthValue())).append('-').append(two(t.getDayOfMonth()));
				default -> sb.append(two(t.getDayOfMonth())).append('/').append(two(t.getMonthValue()));
			}
		}
		text = sb.toString();
	}

	@Override
	public int contentWidth() {
		int w = showClock.on() ? cachedWidth(textWidthCache, text) : 0;
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
			cx += cachedWidth(textWidthCache, text) + (itemCount() > 0 ? GAP : 0);
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
