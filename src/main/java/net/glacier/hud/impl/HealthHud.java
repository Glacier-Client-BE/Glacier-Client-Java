package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Theme;

import java.util.Locale;

/** "HP: 18.0 / 20" with the absorption (gold hearts) added as "+4" when you have any. */
public class HealthHud extends CachedRowsHud {
	private final ToggleSetting showAbsorption = add(new ToggleSetting("Show Absorption", true));

	public HealthHud() {
		super("Health", "Shows your health as a number, with absorption when you have it.", "HP: --");
		stackTopLeft(28);
	}

	@Override
	protected boolean pill() {
		return true;
	}

	@Override
	public void onTick() {
		if (mc.player == null) {
			setRows(single(part("HP: --", TEXT)));
			return;
		}
		float hp = mc.player.getHealth();
		float max = Math.max(1f, mc.player.getMaxHealth());
		float absorb = mc.player.getAbsorptionAmount();
		int pct = (int) (hp * 100f / max);
		Part hpPart = part(String.format(Locale.ROOT, "%.1f", hp), goodHigh(pct, 60, 30));
		Part head = part("HP: ", TEXT);
		Part of = part(" / " + Math.round(max), TEXT);
		if (showAbsorption.on() && absorb >= 0.5f) {
			setRows(single(head, hpPart, of, part(" +" + Math.round(absorb), Theme.HUD_WARN)));
		} else {
			setRows(single(head, hpPart, of));
		}
	}
}
