package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.ChoiceSetting;

/** Forces the weather you see on the client only; the server's real weather is untouched. */
public class WeatherChanger extends Module {
	private final ChoiceSetting weather = add(new ChoiceSetting("Weather", "Clear", "Clear", "Rain", "Thunder"));

	public WeatherChanger() {
		super("Weather Changer", "Client-side weather: always clear, rain or thunder.", Category.RENDER);
	}

	@Override
	public void onTick() {
		if (mc.level == null) return;
		float rain = weather.is("Clear") ? 0f : 1f;
		float thunder = weather.is("Thunder") ? 1f : 0f;
		mc.level.setRainLevel(rain);
		mc.level.setThunderLevel(thunder);
	}
}
