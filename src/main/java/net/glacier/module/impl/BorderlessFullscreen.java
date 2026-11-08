package net.glacier.module.impl;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import net.glacier.module.Category;
import net.glacier.module.Module;
import org.lwjgl.glfw.GLFW;

/**
 * Borderless windowed fullscreen: drops the window decorations and stretches the window over the monitor it is
 * on, without the video-mode switch of real fullscreen (instant alt-tab, no flicker). Uses plain GLFW calls, no
 * mixin. The previous window position, size and decorations are restored when the module is switched off; if
 * real fullscreen (F11) is entered the module simply stands down.
 */
public class BorderlessFullscreen extends Module {
	private boolean applied;
	private int oldX, oldY, oldW, oldH;

	public BorderlessFullscreen() {
		super("Borderless Fullscreen", "Fills the monitor with an undecorated window instead of exclusive fullscreen.", Category.RENDER);
	}

	@Override
	public void onTick() {
		Window window = mc.getWindow();
		if (window == null) return;
		if (window.isFullscreen()) {
			// Real fullscreen owns the window now; forget our state without touching it.
			applied = false;
			return;
		}
		if (!applied) apply(window);
	}

	private void apply(Window window) {
		Monitor monitor = window.findBestMonitor();
		if (monitor == null) return;
		VideoMode mode = monitor.getCurrentMode();
		long h = window.handle();
		int[] x = new int[1], y = new int[1], w = new int[1], ht = new int[1];
		GLFW.glfwGetWindowPos(h, x, y);
		GLFW.glfwGetWindowSize(h, w, ht);
		oldX = x[0];
		oldY = y[0];
		oldW = w[0];
		oldH = ht[0];
		GLFW.glfwSetWindowAttrib(h, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
		GLFW.glfwSetWindowPos(h, monitor.getX(), monitor.getY());
		GLFW.glfwSetWindowSize(h, mode.getWidth(), mode.getHeight());
		applied = true;
	}

	@Override
	protected void onDisable() {
		Window window = mc.getWindow();
		if (window != null && applied && !window.isFullscreen()) {
			long h = window.handle();
			GLFW.glfwSetWindowAttrib(h, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
			GLFW.glfwSetWindowSize(h, Math.max(oldW, 320), Math.max(oldH, 240));
			GLFW.glfwSetWindowPos(h, oldX, oldY);
		}
		applied = false;
	}
}
