package net.glacier.compat;

import com.mojang.blaze3d.platform.InputConstants;

/** Key and button constants under their old GLFW names, mapped to the 26.x input codes. */
public final class GLFW {
	private GLFW() {}
	public static final int GLFW_PRESS = 1, GLFW_RELEASE = 0;
	public static final int GLFW_KEY_ESCAPE = InputConstants.KEY_ESCAPE;
	public static final int GLFW_KEY_RIGHT_SHIFT = InputConstants.KEY_RSHIFT;
	public static final int GLFW_KEY_LEFT_SHIFT = InputConstants.KEY_LSHIFT;
	public static final int GLFW_KEY_BACKSPACE = InputConstants.KEY_BACKSPACE;
	public static final int GLFW_KEY_LEFT_ALT = InputConstants.KEY_LALT;
	public static final int GLFW_KEY_RIGHT_ALT = InputConstants.KEY_RALT;
	public static final int GLFW_KEY_LEFT_CONTROL = InputConstants.KEY_LCONTROL;
	public static final int GLFW_KEY_RIGHT_CONTROL = InputConstants.KEY_RCONTROL;
	public static final int GLFW_KEY_KP_0 = InputConstants.KEY_NUMPAD0;
	public static final int GLFW_KEY_KP_9 = InputConstants.KEY_NUMPAD9;
	public static final int GLFW_KEY_F1 = InputConstants.KEY_F1;
	public static final int GLFW_KEY_ENTER = InputConstants.KEY_RETURN;
	public static final int GLFW_KEY_DELETE = InputConstants.KEY_DELETE;
	public static final int GLFW_KEY_UP = InputConstants.KEY_UP;
	public static final int GLFW_KEY_DOWN = InputConstants.KEY_DOWN;
	public static final int GLFW_KEY_LEFT = InputConstants.KEY_LEFT;
	public static final int GLFW_KEY_RIGHT = InputConstants.KEY_RIGHT;
	public static final int GLFW_KEY_TAB = InputConstants.KEY_TAB;
	public static final int GLFW_KEY_SPACE = InputConstants.KEY_SPACE;
	public static final int GLFW_KEY_PAGE_UP = InputConstants.KEY_PAGEUP;
	public static final int GLFW_KEY_PAGE_DOWN = InputConstants.KEY_PAGEDOWN;
	public static final int GLFW_KEY_INSERT = InputConstants.KEY_INSERT;
	public static final int GLFW_KEY_HOME = InputConstants.KEY_HOME;
	public static final int GLFW_KEY_END = InputConstants.KEY_END;
	public static final int GLFW_KEY_CAPS_LOCK = InputConstants.KEY_CAPSLOCK;
	public static final int GLFW_KEY_C = InputConstants.KEY_C;
	public static final int GLFW_MOUSE_BUTTON_LEFT = 0, GLFW_MOUSE_BUTTON_RIGHT = 1;
}
