package net.glacier.setting;

import com.google.gson.JsonElement;

/**
 * A configurable value owned by a module. Every setting serialises itself to
 * JSON so the whole config round-trips through config.json.
 */
public abstract class Setting<T> {
	private final String name;
	protected T value;
	private T defaultValue;
	private boolean hidden;

	protected Setting(String name, T def) {
		this.name = name;
		this.value = def;
		this.defaultValue = def;
	}

	/** Makes the current value the default; for subclasses that normalise the value in their constructor. */
	protected void rememberDefault() {
		defaultValue = value;
	}

	/** Restores the default value in place, without re-running the owning module's constructor. */
	public void resetToDefault() {
		value = defaultValue;
	}

	public String getName() {
		return name;
	}

	/** Stable key used in config.json: the name lowercased with non-alphanumerics removed. */
	public String getKey() {
		StringBuilder sb = new StringBuilder();
		for (char c : name.toCharArray()) {
			if (Character.isLetterOrDigit(c)) sb.append(Character.toLowerCase(c));
		}
		return sb.toString();
	}

	public T get() {
		return value;
	}

	public void set(T v) {
		value = v;
	}

	/** Hidden settings persist to config.json but are not shown in the menu. */
	public Setting<T> hide() {
		hidden = true;
		return this;
	}

	public boolean isHidden() {
		return hidden;
	}

	public abstract JsonElement save();

	public abstract void load(JsonElement e);
}
