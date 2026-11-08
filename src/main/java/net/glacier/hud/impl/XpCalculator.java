package net.glacier.hud.impl;

/** "Level 12  340 XP to next": the experience still needed to reach the next level. */
public class XpCalculator extends CachedRowsHud {
	private String last = "";

	public XpCalculator() {
		super("XP Calculator", "Shows the experience still needed for your next level.", "XP to next: 0");
		stackTopLeft(7);
	}

	@Override
	public void onTick() {
		var p = mc.player;
		if (p == null) return;
		int need = p.getXpNeededForNextLevel();
		int have = Math.round(p.experienceProgress * need);
		String s = "Level " + p.experienceLevel + "  " + Math.max(0, need - have) + " XP to next";
		if (s.equals(last)) return;
		last = s;
		setRows(single(part(s, TEXT)));
	}
}
