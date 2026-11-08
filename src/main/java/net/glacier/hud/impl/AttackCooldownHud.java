package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;

/** "Attack: 100%", how charged your next melee swing is. 100% means a full-damage hit. */
public class AttackCooldownHud extends CachedRowsHud {
	private final ToggleSetting label = add(new ToggleSetting("Show Label", true));

	public AttackCooldownHud() {
		super("Attack Cooldown", "Shows how charged your next melee attack is.", "Attack: --");
		stackTopLeft(27);
	}

	@Override
	protected boolean pill() {
		return true;
	}

	@Override
	public void onTick() {
		if (mc.player == null) {
			setRows(single(part("Attack: --", TEXT)));
			return;
		}
		int pct = Math.round(Math.min(1f, mc.player.getAttackStrengthScale(0f)) * 100f);
		int color = goodHigh(pct, 95, 50);
		if (label.on()) setRows(single(part("Attack: ", TEXT), part(pct + "%", color)));
		else setRows(single(part(pct + "%", color)));
	}
}
