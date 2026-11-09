package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.SliderSetting;

/**
 * Forces the time of day you see, client side only. Re-applies the chosen time every tick through
 * the client side world clock of the current dimension (26.x: ClientClockManager, written through
 * MixinClockInstance), which simply overrides the server sync packets. The real time is tracked while active so it can
 * be handed back when the module is switched off.
 */
public class TimeChanger extends Module {
	private static final long DAY = 24000L;

	private final ChoiceSetting preset = add(new ChoiceSetting("Time", "Noon",
			"Sunrise", "Day", "Noon", "Sunset", "Night", "Midnight", "Custom"));
	private final SliderSetting custom = add(new SliderSetting("Custom Time", 6000, 0, 24000, 100));
	private final SliderSetting cycle = add(new SliderSetting("Cycle Speed", 0, 0, 20, 0.5));

	private long realDayTime = -1;
	private double cycled;

	public TimeChanger() {
		super("Time Changer", "Locks the time of day you see; the server time is unaffected.", Category.RENDER);
	}

	@Override
	protected void onEnable() {
		realDayTime = mc.level != null ? mc.level.getDefaultClockTime() : -1;
		cycled = 0;
	}

	@Override
	public void onTick() {
		if (mc.level == null) {
			realDayTime = -1;
			return;
		}
		if (realDayTime < 0) realDayTime = mc.level.getDefaultClockTime();
		else realDayTime++;
		cycled += cycle.get();
		long t = Math.floorMod(base() + (long) cycled, DAY);
		// keep the day count of the real clock so the moon phase stays sane, only the time of day is pinned
		setClock(Math.floorDiv(mc.level.getDefaultClockTime(), DAY) * DAY + t);
	}

	/** Writes the total tick count of the current dimension's world clock (client side only). */
	private void setClock(long totalTicks) {
		var clock = mc.level.dimensionType().defaultClock();
		if (clock.isEmpty()) return;
		if (mc.level.clockManager().getInstance(clock.get()) instanceof net.glacier.mixin.extraa.MixinClockInstance w) {
			w.glacier$setTotalTicks(totalTicks);
		}
	}

	private long base() {
		return switch (preset.get()) {
			case "Sunrise" -> 23000L;
			case "Day" -> 1000L;
			case "Noon" -> 6000L;
			case "Sunset" -> 12000L;
			case "Night" -> 13000L;
			case "Midnight" -> 18000L;
			default -> custom.i();
		};
	}

	@Override
	protected void onDisable() {
		if (mc.level != null && realDayTime >= 0) {
			setClock(realDayTime);
		}
		realDayTime = -1;
		cycled = 0;
	}
}
