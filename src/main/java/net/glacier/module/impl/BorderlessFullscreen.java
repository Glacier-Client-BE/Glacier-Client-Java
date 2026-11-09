package net.glacier.module.impl;

import com.mojang.blaze3d.platform.Window;
import net.glacier.module.Category;
import net.glacier.module.Module;

/**
 * Borderless windowed fullscreen (26.3). The window system moved from GLFW to SDL3 and vanilla now ships its own
 * borderless fullscreen (Window#setFullscreen with the "Exclusive Fullscreen" option off, verified with javap:
 * useBorderlessFullscreenWindow is true on Windows when exclusiveFullscreen is false). The module therefore only drives
 * those two options: it turns exclusive fullscreen off and fullscreen on, and puts both back when it is disabled.
 */
public class BorderlessFullscreen extends Module {
	private boolean applied;
	private boolean prevFullscreen;
	private boolean prevExclusive;

	public BorderlessFullscreen() {
		super("Borderless Fullscreen", "Fills the monitor with an undecorated window instead of exclusive fullscreen.", Category.RENDER);
	}

	@Override
	public void onTick() {
		if (applied || mc.getWindow() == null || mc.options == null) return;
		prevFullscreen = mc.options.fullscreen().get();
		prevExclusive = mc.options.exclusiveFullscreen().get();
		if (prevExclusive) mc.options.exclusiveFullscreen().set(false);
		if (!prevFullscreen) mc.options.fullscreen().set(true);
		applied = true;
	}

	@Override
	protected void onDisable() {
		Window window = mc.getWindow();
		if (applied && window != null && mc.options != null) {
			if (!prevFullscreen) mc.options.fullscreen().set(false);
			if (prevExclusive) mc.options.exclusiveFullscreen().set(true);
		}
		applied = false;
	}
}
