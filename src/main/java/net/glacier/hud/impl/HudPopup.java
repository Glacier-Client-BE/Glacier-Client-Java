package net.glacier.hud.impl;

import net.fabricmc.loader.api.FabricLoader;
import net.glacier.setting.SliderSetting;
import net.glacier.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.List;

/** Start-up card with the client name, version and a bug-report hint, shown for a few seconds after joining a world. */
public class HudPopup extends CachedRowsHud {
	/** Glacier mod version, read once; "dev" when the metadata is unavailable. */
	static final String VERSION = FabricLoader.getInstance().getModContainer("glacier")
			.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");

	private final SliderSetting duration = add(new SliderSetting("Duration (s)", 5, 1, 15, 1));
	private ClientLevel shownFor;
	private int ticksLeft;

	public HudPopup() {
		super("HUD Popup", "Start-up message and branding.", "Glacier Client");
		setPos(0.5f, 0.12f);
		setRows(List.of(
				new Part[]{part("Glacier Client ", TEXT), part("v" + VERSION, Theme.ACCENT)},
				new Part[]{part("Found any bugs? Report them in our Server.", Theme.HUD_MUTED)}));
	}

	@Override
	public void onTick() {
		if (mc.level != shownFor) {
			shownFor = mc.level;
			ticksLeft = mc.level == null ? 0 : duration.i() * 20;
		} else if (ticksLeft > 0) {
			ticksLeft--;
		}
	}

	@Override
	protected void onEnable() {
		ticksLeft = duration.i() * 20;
	}

	@Override
	protected boolean hidden() {
		return ticksLeft <= 0;
	}
}
