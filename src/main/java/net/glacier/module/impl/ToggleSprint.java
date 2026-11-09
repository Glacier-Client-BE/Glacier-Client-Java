package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.ToggleSetting;

/** Keeps sprint held while you move forward, so the sprint key never needs to be held. */
public class ToggleSprint extends Module {
	private final ToggleSetting whileSneaking = add(new ToggleSetting("Sprint While Sneaking", false));
	private final ToggleSetting allDirections = add(new ToggleSetting("All Directions", false));
	private final ToggleSetting whileUsingItem = add(new ToggleSetting("Sprint While Using Item", true));
	private boolean forced;

	public ToggleSprint() {
		super("Toggle Sprint", "Automatically sprints while you walk forward.", Category.MISC);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.gui.screen() != null) return;
		boolean forward = mc.options.keyUp.isDown()
				|| (allDirections.on() && (mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown()));
		if (!whileUsingItem.on() && mc.player.isUsingItem()) forward = false;
		boolean sneak = mc.options.keyShift.isDown();
		if (forward && (!sneak || whileSneaking.on())) {
			mc.options.keySprint.setDown(true);
			forced = true;
		} else if (forced) {
			mc.options.keySprint.setDown(false);
			forced = false;
		}
	}

	@Override
	protected void onDisable() {
		if (forced && mc.options != null) mc.options.keySprint.setDown(false);
		forced = false;
	}
}
