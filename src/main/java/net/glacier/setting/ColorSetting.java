package net.glacier.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** An ARGB colour. Serialised as "#AARRGGBB". */
public class ColorSetting extends Setting<Integer> {
	public ColorSetting(String name, int argb) {
		super(name, argb);
	}

	public int argb() {
		return value;
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(String.format("#%08X", value));
	}

	@Override
	public void load(JsonElement e) {
		if (e == null || !e.isJsonPrimitive()) return;
		String s = e.getAsString().trim();
		if (s.startsWith("#")) s = s.substring(1);
		if (s.length() != 6 && s.length() != 8) return;
		try {
			long v = Long.parseLong(s, 16);
			if (s.length() == 6) v |= 0xFF000000L;
			value = (int) v;
		} catch (NumberFormatException ignored) {
		}
	}
}
