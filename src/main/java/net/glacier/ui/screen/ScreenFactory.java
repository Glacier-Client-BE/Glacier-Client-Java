package net.glacier.ui.screen;

import net.minecraft.client.gui.screens.Screen;

/**
 * Single place that decides which implementation opens each Glacier screen. The HTML/CSS versions are the reference
 * design; the native Java ones are experimental until they match them 1:1. Flip {@link #NATIVE} to try them.
 */
public final class ScreenFactory {
	public static final boolean NATIVE = true;

	private ScreenFactory() {
	}

	public static Screen menu(String tab) {
		return NATIVE ? NativeMenuScreen.open(tab) : WebScreen.menu(tab);
	}

	public static Screen quickBar() {
		return NATIVE ? new NativeQuickBar() : new HudQuickScreen();
	}

	public static Screen start() {
		return NATIVE ? new NativeStartScreen() : new StartScreen();
	}

	public static Screen pause(boolean showPauseMenu) {
		return NATIVE ? new NativePauseScreen(showPauseMenu) : new PauseWebScreen(showPauseMenu);
	}

	public static Screen options(Screen parent) {
		return NATIVE ? new NativeOptionsScreen(parent) : new net.minecraft.client.gui.screens.options.OptionsScreen(parent, net.minecraft.client.Minecraft.getInstance().options);
	}

	public static Screen progress(Screen source) {
		return NATIVE ? NativeProgressScreen.wrap(source) : new ProgressWebScreen(source);
	}

	public static Screen play(Screen parent) {
		if (NATIVE) {
			NativePlayScreen.open(parent);
			return null;
		}
		return new PlayWebScreen(parent);
	}
}
