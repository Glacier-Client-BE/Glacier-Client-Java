package net.glacier.module.impl;

import net.glacier.mixin.FogAccess;
import net.glacier.module.Category;
import net.glacier.module.Module;

/** Turns world fog off by clearing FogRenderer's debug fog flag every tick. */
public class NoFog extends Module {
	public NoFog() {
		super("No Fog", "Removes world fog for a clearer view.", Category.RENDER);
	}

	@Override
	public void onTick() {
		FogAccess.glacier$setFogEnabled(false);
	}

	@Override
	protected void onDisable() {
		FogAccess.glacier$setFogEnabled(true);
	}
}
