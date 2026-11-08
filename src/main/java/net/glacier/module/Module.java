package net.glacier.module;

import net.glacier.setting.Setting;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Base class for every feature the client offers. */
public abstract class Module {
	protected static final Minecraft mc = Minecraft.getInstance();

	private final String name;
	private final String description;
	private final Category category;
	private final List<Setting<?>> settings = new ArrayList<>();
	private boolean enabled;
	private int keyCode;

	protected Module(String name, String description, Category category) {
		this.name = name;
		this.description = description;
		this.category = category;
	}

	protected <T extends Setting<?>> T add(T setting) {
		settings.add(setting);
		return setting;
	}

	public List<Setting<?>> settings() {
		return settings;
	}

	public String name() {
		return name;
	}

	public String description() {
		return description;
	}

	public Category category() {
		return category;
	}

	/** Stable id used in config.json and UI lookups: the name lowercased with non-alphanumerics removed. */
	public String id() {
		StringBuilder sb = new StringBuilder();
		for (char c : name.toCharArray()) {
			if (Character.isLetterOrDigit(c)) sb.append(Character.toLowerCase(c));
		}
		return sb.toString();
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) return;
		this.enabled = enabled;
		if (enabled) onEnable();
		else onDisable();
	}

	/** GLFW key code of this module's toggle hotkey, 0 = none. */
	public int keyCode() {
		return keyCode;
	}

	public void setKeyCode(int keyCode) {
		this.keyCode = Math.max(0, keyCode);
	}

	/** Human readable hotkey name, e.g. "B", "LSHIFT", "F5" or "None". */
	public String keyName() {
		int k = keyCode;
		if (k <= 0) return "None";
		String n = null;
		try {
			n = GLFW.glfwGetKeyName(k, 0);
		} catch (Throwable ignored) {
		}
		if (n != null && !n.isEmpty()) return n.toUpperCase(java.util.Locale.ROOT);
		if (k >= GLFW.GLFW_KEY_F1 && k <= GLFW.GLFW_KEY_F25) return "F" + (k - GLFW.GLFW_KEY_F1 + 1);
		if (k >= GLFW.GLFW_KEY_KP_0 && k <= GLFW.GLFW_KEY_KP_9) return "NUM" + (k - GLFW.GLFW_KEY_KP_0);
		return switch (k) {
			case GLFW.GLFW_KEY_SPACE -> "SPACE";
			case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
			case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
			case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
			case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
			case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
			case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
			case GLFW.GLFW_KEY_TAB -> "TAB";
			case GLFW.GLFW_KEY_CAPS_LOCK -> "CAPS";
			case GLFW.GLFW_KEY_ENTER -> "ENTER";
			case GLFW.GLFW_KEY_BACKSPACE -> "BACKSPACE";
			case GLFW.GLFW_KEY_INSERT -> "INSERT";
			case GLFW.GLFW_KEY_DELETE -> "DELETE";
			case GLFW.GLFW_KEY_HOME -> "HOME";
			case GLFW.GLFW_KEY_END -> "END";
			case GLFW.GLFW_KEY_PAGE_UP -> "PGUP";
			case GLFW.GLFW_KEY_PAGE_DOWN -> "PGDN";
			case GLFW.GLFW_KEY_UP -> "UP";
			case GLFW.GLFW_KEY_DOWN -> "DOWN";
			case GLFW.GLFW_KEY_LEFT -> "LEFT";
			case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
			case GLFW.GLFW_KEY_ESCAPE -> "ESC";
			default -> "KEY " + k;
		};
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	protected void onEnable() {
	}

	protected void onDisable() {
	}

	/** Called every client tick while the module is enabled. */
	public void onTick() {
	}

	/** Called by Config once the saved settings have been applied. */
	public void afterLoad() {
	}
}
