package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;

/** "Level 12  340 XP to next": the experience still needed to reach the next level, with an optional flask icon. */
public class XpCalculator extends CachedRowsHud {
	/** Font Awesome "flask" glyph, standing in for the experience bottle. */
	private static final int XP_ICON = 0xf0c3;
	private final ToggleSetting showIcon = add(new ToggleSetting("Show XP Icon", false));
	private String last = "";
	private boolean lastIcon;

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
		boolean ic = showIcon.on();
		if (s.equals(last) && ic == lastIcon) return;
		last = s;
		lastIcon = ic;
		setRows(ic ? single(icon(XP_ICON, TEXT), part(" " + s, TEXT)) : single(part(s, TEXT)));
	}
}
