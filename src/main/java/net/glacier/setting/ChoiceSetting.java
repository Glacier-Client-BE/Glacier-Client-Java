package net.glacier.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

/** A value picked from a fixed list of string options. */
public class ChoiceSetting extends Setting<String> {
	private final List<String> choices;

	public ChoiceSetting(String name, String def, String... options) {
		super(name, def);
		this.choices = List.of(options);
		if (!choices.contains(def) && !choices.isEmpty()) {
			this.value = choices.get(0);
		}
		rememberDefault();
	}

	public List<String> options() {
		return choices;
	}

	public int index() {
		return Math.max(0, choices.indexOf(value));
	}

	public boolean is(String option) {
		return value.equals(option);
	}

	public void setIndex(int i) {
		if (choices.isEmpty()) return;
		value = choices.get(Math.max(0, Math.min(choices.size() - 1, i)));
	}

	/** Advances to the next option, wrapping around. */
	public void next() {
		if (choices.isEmpty()) return;
		setIndex((index() + 1) % choices.size());
	}

	@Override
	public void set(String v) {
		if (v != null && choices.contains(v)) value = v;
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(value);
	}

	@Override
	public void load(JsonElement e) {
		if (e != null && e.isJsonPrimitive()) {
			set(e.getAsString());
		}
	}
}
