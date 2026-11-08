package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.SliderSetting;

/** Sets the field of view beyond the vanilla slider's steps and restores your original on disable. */
public class FovChanger extends Module {
	private final SliderSetting fov = add(new SliderSetting("FOV", 90, 30, 110, 1));
	private int original = -1;

	public FovChanger() {
		super("FOV Changer", "Sets your field of view from the Glacier menu.", Category.RENDER);
	}

	@Override
	protected void onEnable() {
		original = mc.options != null ? mc.options.fov().get() : -1;
	}

	@Override
	public void onTick() {
		int v = (int) fov.get().doubleValue();
		if (mc.options.fov().get() != v) mc.options.fov().set(v);
	}

	@Override
	protected void onDisable() {
		if (original > 0 && mc.options != null) mc.options.fov().set(original);
		original = -1;
	}
}
