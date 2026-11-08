package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.module.ModuleManager;
import net.glacier.setting.SliderSetting;

/**
 * Brightens the world by overriding the lightmap gamma (see MixinLightTexture). Opt-in: disabled by default, and vanilla
 * lighting is used whenever it is off. Note that some servers' rules restrict brightness mods.
 */
public class Fullbright extends Module {
	private final SliderSetting brightness = add(new SliderSetting("Brightness", 10, 1, 15, 1));

	public Fullbright() {
		super("Fullbright", "Brightens the world so you can see in the dark.", Category.RENDER);
	}

	/** True when the module is enabled and should override the lightmap gamma. */
	public static boolean active() {
		Fullbright f = ModuleManager.get(Fullbright.class);
		return f != null && f.isEnabled();
	}

	/**
	 * Effective lightmap gamma while enabled: the Brightness slider scaled onto the 0..1 range the lightmap shader
	 * expects (vanilla's gamma option is 0..1, and values above 1 wash the whole picture out to white).
	 */
	public static float gammaValue() {
		Fullbright f = ModuleManager.get(Fullbright.class);
		if (f == null) return 0f;
		double max = f.brightness.max();
		return (float) Math.max(0.0, Math.min(1.0, f.brightness.f() / max));
	}
}
