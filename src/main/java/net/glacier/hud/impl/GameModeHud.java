package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;

/** "Survival, Hard": your game mode and the world difficulty. */
public class GameModeHud extends CachedRowsHud {
	private final ToggleSetting showDifficulty = add(new ToggleSetting("Show Difficulty", true));

	public GameModeHud() {
		super("Game Mode", "Shows your game mode and the world difficulty.", "Survival");
		stackTopLeft(30);
	}

	@Override
	protected boolean pill() {
		return true;
	}

	@Override
	public void onTick() {
		String text = "-";
		if (mc.gameMode != null) {
			String mode = mc.gameMode.getPlayerMode().getName();
			if (!mode.isEmpty()) text = Character.toUpperCase(mode.charAt(0)) + mode.substring(1);
		}
		if (showDifficulty.on() && mc.level != null) {
			text += ", " + mc.level.getDifficulty().getDisplayName().getString();
		}
		setRows(single(part(text, TEXT)));
	}
}
