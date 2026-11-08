package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Theme;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

/** One row per active potion effect: "Speed II  1:23". Time turns yellow under 30 s and red under 10 s. */
public class PotionTimersHud extends CachedRowsHud {
	private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

	private final ToggleSetting showTime = add(new ToggleSetting("Show Time", true));

	public PotionTimersHud() {
		super("Potion Timers", "Lists your active potion effects with the time they have left.", "No effects");
		stackTopLeft(31);
	}

	@Override
	public void onTick() {
		if (mc.player == null) {
			setRows(single(part("No effects", Theme.HUD_MUTED)));
			return;
		}
		List<Part[]> rows = new ArrayList<>();
		for (MobEffectInstance e : mc.player.getActiveEffects()) {
			int amp = Math.max(0, e.getAmplifier());
			String name = e.getEffect().value().getDisplayName().getString()
					+ " " + (amp < ROMAN.length ? ROMAN[amp] : String.valueOf(amp + 1));
			if (!showTime.on()) {
				rows.add(new Part[]{part(name, TEXT)});
				continue;
			}
			if (e.isInfiniteDuration()) {
				rows.add(new Part[]{part(name + "  ", TEXT), part("inf", TEXT)});
				continue;
			}
			int sec = Math.max(0, e.getDuration()) / 20;
			int color = sec < 10 ? Theme.HUD_BAD : sec < 30 ? Theme.HUD_WARN : TEXT;
			String time = (sec / 60) + ":" + (sec % 60 < 10 ? "0" : "") + (sec % 60);
			rows.add(new Part[]{part(name + "  ", TEXT), part(time, color)});
		}
		if (rows.isEmpty()) rows = single(part("No effects", Theme.HUD_MUTED));
		setRows(rows);
	}
}
