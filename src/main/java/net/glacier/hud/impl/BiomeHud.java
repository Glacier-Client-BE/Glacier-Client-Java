package net.glacier.hud.impl;

import net.glacier.ui.Theme;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/** "Biome: Dark Forest", the biome at your feet. The name is rebuilt only when the biome changes. */
public class BiomeHud extends LabelValueHud {
	private Holder<Biome> lastBiome;
	private String name = "Unknown";

	public BiomeHud() {
		super("Biome", "Shows the name of the biome you are in.", "Biome", 42);
	}

	@Override
	protected String value() {
		if (mc.player == null || mc.level == null) return null;
		Holder<Biome> biome = mc.level.getBiome(mc.player.blockPosition());
		if (biome != lastBiome) {
			lastBiome = biome;
			name = biome.unwrapKey().map(k -> pretty(k.identifier().getPath())).orElse("Unknown");
		}
		return name;
	}

	@Override
	protected int valueColor() {
		return Theme.HUD_BIOME;
	}
}
