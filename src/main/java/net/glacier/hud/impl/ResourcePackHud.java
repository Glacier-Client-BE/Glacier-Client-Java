package net.glacier.hud.impl;

import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Theme;
import net.minecraft.server.packs.repository.Pack;

import java.util.ArrayList;
import java.util.List;

/** Lists the active resource packs, highest priority first. Refreshed once a second. */
public class ResourcePackHud extends CachedRowsHud {
	private static final int REFRESH_TICKS = 20;
	private final ToggleSetting onlyCustom = add(new ToggleSetting("Only Custom Packs", true));
	private final ToggleSetting header = add(new ToggleSetting("Show Header", true));
	private final SliderSetting max = add(new SliderSetting("Max Packs", 5, 1, 15, 1));
	private int tick;
	private String last = "";

	public ResourcePackHud() {
		super("Resource Packs", "Lists your active resource packs.", "Packs");
		stackTopLeft(42);
	}

	@Override
	public void onTick() {
		if (tick++ % REFRESH_TICKS != 0) return;
		List<String> names = new ArrayList<>();
		for (Pack p : mc.getResourcePackRepository().getSelectedPacks()) {
			String id = p.getId();
			if (onlyCustom.on() && !id.startsWith("file/") && !id.startsWith("server/")) continue;
			names.add(p.getTitle().getString());
		}
		StringBuilder key = new StringBuilder();
		key.append(header.on()).append(max.i());
		for (String n : names) key.append('|').append(n);
		String k = key.toString();
		if (k.equals(last)) return;
		last = k;

		List<Part[]> rows = new ArrayList<>();
		if (header.on()) rows.add(new Part[]{part("Resource Packs", TEXT)});
		int shown = 0;
		for (int i = names.size() - 1; i >= 0 && shown < max.i(); i--, shown++) {
			rows.add(new Part[]{part(names.get(i), Theme.HUD_MUTED)});
		}
		if (names.isEmpty()) rows.add(new Part[]{part("None", Theme.HUD_MUTED)});
		setRows(rows);
	}
}
