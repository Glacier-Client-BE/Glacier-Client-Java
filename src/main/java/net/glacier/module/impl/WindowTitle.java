package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.ChoiceSetting;

/** Sets a custom game window title. */
public class WindowTitle extends Module {
	private final ChoiceSetting title = add(new ChoiceSetting("Title", "Glacier Client", "Glacier Client", "Minecraft", "Glacier", "Playing Glacier"));
	private String applied;

	public WindowTitle() {
		super("Window Title", "Renames the game window.", Category.MISC);
	}

	@Override
	public void onTick() {
		String t = title.get();
		if (t.equals(applied) || mc.getWindow() == null) return;
		mc.getWindow().setTitle(t);
		applied = t;
	}

	@Override
	protected void onDisable() {
		applied = null;
		// Back to the title vanilla would show right now (version, world, server).
		if (mc.getWindow() != null) mc.updateTitle();
	}
}
