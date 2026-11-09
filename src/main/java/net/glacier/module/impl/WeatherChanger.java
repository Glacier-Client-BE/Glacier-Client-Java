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

	/** The level whose real weather is saved below, so turning the module off puts it back. */
	private net.minecraft.client.multiplayer.ClientLevel savedLevel;
	private float savedRain;
	private float savedThunder;

	@Override
	public void onTick() {
		if (mc.level == null) return;
		if (mc.level != savedLevel) {
			savedLevel = mc.level;
			savedRain = mc.level.getRainLevel(1f);
			savedThunder = mc.level.getThunderLevel(1f);
		}
		float rain = weather.is("Clear") ? 0f : 1f;
		float thunder = weather.is("Thunder") ? 1f : 0f;
		mc.level.setRainLevel(rain);
		mc.level.setThunderLevel(thunder);
	}

	@Override
	protected void onDisable() {
		if (savedLevel != null && savedLevel == mc.level) {
			savedLevel.setRainLevel(savedRain);
			savedLevel.setThunderLevel(savedThunder);
		}
		savedLevel = null;
	}
}
