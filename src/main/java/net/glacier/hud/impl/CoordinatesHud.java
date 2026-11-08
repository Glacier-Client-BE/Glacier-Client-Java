package net.glacier.hud.impl;

import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Draw;
import net.glacier.ui.Theme;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Position lines (X, Y, Z), an optional chunk line, an optional Nether line (scaled by 8 between the Overworld
 * and the Nether), and the biome and dimension. Each group has its own text opacity. Rows are rebuilt once per
 * tick, never per frame.
 */
public class CoordinatesHud extends CachedRowsHud {
	private static final int NETHER_SCALE = 8;

	private final ToggleSetting showVanilla = add(new ToggleSetting("Show Vanilla", true));
	private final SliderSetting vanillaOpacity = add(new SliderSetting("Vanilla Opacity", 0.7, 0.0, 1.0, 0.1));
	private final ToggleSetting showChunk = add(new ToggleSetting("Show Chunk", false));
	private final SliderSetting chunkOpacity = add(new SliderSetting("Chunk Opacity", 0.7, 0.0, 1.0, 0.1));
	private final ToggleSetting showNether = add(new ToggleSetting("Show Nether", false));
	private final SliderSetting netherOpacity = add(new SliderSetting("Nether Opacity", 0.7, 0.0, 1.0, 0.1));
	private final ToggleSetting showInOverworld = add(new ToggleSetting("Show In Overworld", false));
	private final SliderSetting overworldOpacity = add(new SliderSetting("In Overworld Opacity", 0.7, 0.0, 1.0, 0.1));
	private final ToggleSetting showBiome = add(new ToggleSetting("Show Biome", true));
	private final ToggleSetting showDimension = add(new ToggleSetting("Show Dimension", false));

	public CoordinatesHud() {
		super("Coordinates HUD", "Shows your XYZ position and current biome.", "X: 0");
		stackTopLeft(2);
	}

	private String biomeName() {
		if (mc.player == null || mc.level == null) return "Unknown";
		BlockPos pos = mc.player.blockPosition();
		return mc.level.getBiome(pos).unwrapKey()
				.map(k -> prettify(k.identifier().getPath()))
				.orElse("Unknown");
	}

	private static String prettify(String id) {
		StringBuilder sb = new StringBuilder();
		for (String part : id.replace('_', ' ').split(" ")) {
			if (part.isEmpty()) continue;
			sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
		}
		return sb.toString().trim();
	}

	/** Text colour with the line's own opacity applied. */
	private int tint(SliderSetting op) {
		return Draw.alpha(textColor.argb(), op.f());
	}

	@Override
	public void onTick() {
		if (mc.player == null) {
			setRows(single(part("X: -", TEXT)));
			return;
		}
		List<Part[]> out = new ArrayList<>();
		int x = (int) Math.floor(mc.player.getX());
		int y = (int) Math.floor(mc.player.getY());
		int z = (int) Math.floor(mc.player.getZ());
		String dim = mc.level == null ? "overworld" : mc.level.dimension().identifier().getPath();
		boolean overworld = "overworld".equals(dim);

		if (showVanilla.on()) {
			int c = tint(vanillaOpacity);
			out.add(new Part[]{part("X: " + x, c)});
			out.add(new Part[]{part("Y: " + y, c)});
			out.add(new Part[]{part("Z: " + z, c)});
		}
		if (showChunk.on()) {
			out.add(new Part[]{part("C: " + (x >> 4) + "/" + (z >> 4), tint(chunkOpacity))});
		}
		if (showNether.on() || (showInOverworld.on() && overworld)) {
			int c = tint(showNether.on() ? netherOpacity : overworldOpacity);
			if (overworld) {
				out.add(new Part[]{part("Nether: " + Math.floorDiv(x, NETHER_SCALE) + " " + Math.floorDiv(z, NETHER_SCALE), c)});
			} else {
				out.add(new Part[]{part("Overworld: " + x * NETHER_SCALE + " " + z * NETHER_SCALE, c)});
			}
		}
		if (showBiome.on()) {
			out.add(new Part[]{part("Biome: ", TEXT), part(biomeName(), Theme.HUD_BIOME)});
		}
		if (showDimension.on() && mc.level != null) {
			out.add(new Part[]{part("Dim: " + prettify(dim), TEXT)});
		}
		setRows(out.isEmpty() ? List.of() : out);
	}
}
