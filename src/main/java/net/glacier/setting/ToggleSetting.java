package net.glacier.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ToggleSetting extends Setting<Boolean> {
	public ToggleSetting(String name, boolean def) {
		super(name, def);
	}

	public boolean on() {
		return value;
	}

	public void flip() {
		value = !value;
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(value);
	}

	@Override
	public void load(JsonElement e) {
		if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) {
			value = e.getAsBoolean();
		}
	}
}
