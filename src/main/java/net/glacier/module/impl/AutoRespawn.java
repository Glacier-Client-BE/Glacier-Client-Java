package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.SliderSetting;
import net.minecraft.client.gui.screens.DeathScreen;

/** Presses Respawn for you once the death screen has been up for the delay. Runs on client ticks, no mixin. */
public class AutoRespawn extends Module {
	private final SliderSetting delay = add(new SliderSetting("Delay (ticks)", 10, 0, 60, 1));
	private int waited;

	public AutoRespawn() {
		super("Auto Respawn", "Respawns for you shortly after you die.", Category.MISC);
	}

	@Override
	public void onTick() {
		if (mc.player == null || !(mc.screen instanceof DeathScreen) || !mc.player.isDeadOrDying()) {
			waited = 0;
			return;
		}
		if (waited++ < delay.i()) return;
		waited = 0;
		mc.player.respawn();
	}

	@Override
	protected void onDisable() {
		waited = 0;
	}
}
