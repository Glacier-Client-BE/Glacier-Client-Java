package net.glacier.hud.impl;

import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Theme;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * One row per active potion effect: "Speed II  1:23". With Color By Time the time turns yellow under 30 s and
 * red under 10 s. Also holds the effect formatting helpers shared with {@link PotionIconsHud}.
 */
public class PotionTimersHud extends CachedRowsHud {
	private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
	private static final int WARN_SEC = 30;
	private static final int BAD_SEC = 10;

	private final ToggleSetting showTime = add(new ToggleSetting("Show Time", true));
	private final ToggleSetting showAmplifier = add(new ToggleSetting("Show Amplifier", true));
	private final ToggleSetting colorByTime = add(new ToggleSetting("Color By Time", true));
	private final ToggleSetting hideWhenEmpty = add(new ToggleSetting("Hide When Empty", false));
	private final ChoiceSetting sort = add(new ChoiceSetting("Sort", "Default", "Default", "Duration", "Name"));

	private boolean empty = true;

	public PotionTimersHud() {
		super("Potion Timers", "Lists your active potion effects with the time they have left.", "No effects");
		stackTopLeft(31);
	}

	/** Roman numeral for an amplifier, falling back to the plain level above ten. */
	static String level(int amplifier) {
		int amp = Math.max(0, amplifier);
		return amp < ROMAN.length ? ROMAN[amp] : String.valueOf(amp + 1);
	}

	/** Remaining time as m:ss. */
	static String clock(int seconds) {
		return (seconds / 60) + ":" + (seconds % 60 < 10 ? "0" : "") + (seconds % 60);
	}

	/** Yellow under 30 s, red under 10 s, otherwise the text colour. */
	static int timeColor(int seconds) {
		return seconds < BAD_SEC ? Theme.HUD_BAD : seconds < WARN_SEC ? Theme.HUD_WARN : TEXT;
	}

	/** The given effects in the chosen order: "Default" keeps game order, "Duration" shortest first, "Name" A to Z. */
	static List<MobEffectInstance> sorted(Iterable<MobEffectInstance> effects, String mode) {
		List<MobEffectInstance> list = new ArrayList<>();
		for (MobEffectInstance e : effects) list.add(e);
		if (mode.equals("Duration")) {
			list.sort(Comparator.comparingInt(e -> e.isInfiniteDuration() ? Integer.MAX_VALUE : e.getDuration()));
		} else if (mode.equals("Name")) {
			list.sort(Comparator.comparing(e -> e.getEffect().value().getDisplayName().getString()));
		}
		return list;
	}

	@Override
	protected boolean hidden() {
		return empty && hideWhenEmpty.on();
	}

	@Override
	public void onTick() {
		empty = mc.player == null || mc.player.getActiveEffects().isEmpty();
		if (empty) {
			setRows(single(part("No effects", Theme.HUD_MUTED)));
			return;
		}
		List<Part[]> rows = new ArrayList<>();
		for (MobEffectInstance e : sorted(mc.player.getActiveEffects(), sort.get())) {
			String name = e.getEffect().value().getDisplayName().getString();
			if (showAmplifier.on()) name += " " + level(e.getAmplifier());
			if (!showTime.on()) {
				rows.add(new Part[]{part(name, TEXT)});
			} else if (e.isInfiniteDuration()) {
				rows.add(new Part[]{part(name + "  ", TEXT), part("inf", TEXT)});
			} else {
				int sec = Math.max(0, e.getDuration()) / 20;
				int color = colorByTime.on() ? timeColor(sec) : TEXT;
				rows.add(new Part[]{part(name + "  ", TEXT), part(clock(sec), color)});
			}
		}
		setRows(rows);
	}
}
