package net.glacier.hud.impl;

import net.glacier.hud.HudModule;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Draw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Technical readout in two columns. The left column describes the world and your player, the right one the game and
 * the machine. Every line has its own Show toggle and Opacity slider. Values are rebuilt on tick, never per frame.
 */
public class DebugHud extends HudModule {
	private static final int COLUMN_GAP = 12;
	private static final int LINE_GAP = 1;
	private static final int LEFT = 8;
	private static final int RIGHT = 6;

	private static final String[] LABELS = {
			"World Type", "World Name", "Day Counter", "Moon Phase", "Game Mode", "XP Level", "Item ID", "Aux ID",
			"Glacier Version", "Minecraft Version", "OS", "Graphics", "Platform", "UI Type"
	};
	private static final int WORLD_TYPE = 0, WORLD_NAME = 1, DAY = 2, MOON = 3, MODE = 4, XP = 5, ITEM = 6, AUX = 7;
	private static final int GLACIER = 8, MC = 9, OS = 10, GFX = 11, PLATFORM = 12, UI = 13;

	private final ToggleSetting[] show = new ToggleSetting[LABELS.length];
	private final SliderSetting[] lineOpacity = new SliderSetting[LABELS.length];
	/** The finished "Label: value" line per entry. */
	private final String[] text = new String[LABELS.length];

	public DebugHud() {
		super("Debug HUD", "Advanced technical info and game statistics.");
		for (int i = 0; i < LABELS.length; i++) {
			show[i] = add(new ToggleSetting("Show " + LABELS[i], true));
			lineOpacity[i] = add(new SliderSetting(LABELS[i] + " Opacity", 1.0, 0.0, 1.0, 0.1));
			text[i] = LABELS[i] + ": -";
		}
		text[GLACIER] = LABELS[GLACIER] + ": " + HudPopup.VERSION;
		text[OS] = LABELS[OS] + ": " + System.getProperty("os.name");
		text[PLATFORM] = LABELS[PLATFORM] + ": Java " + System.getProperty("java.version") + " "
				+ System.getProperty("os.arch");
		setPos(0.01f, 0.3f);
	}

	private static String line(int entry, String value) {
		return LABELS[entry] + ": " + value;
	}

	@Override
	public void onTick() {
		var p = mc.player;
		if (p == null || mc.level == null) return;
		ServerData sd = mc.getCurrentServer();
		boolean local = mc.isLocalServer() || sd == null;
		var server = mc.getSingleplayerServer();
		text[WORLD_TYPE] = line(WORLD_TYPE, local ? "Singleplayer" : "Multiplayer");
		text[WORLD_NAME] = line(WORLD_NAME, local && server != null ? server.getWorldData().getLevelName()
				: sd != null ? sd.name : "Unknown");
		long day = mc.level.getDayTime() / 24000L;
		text[DAY] = line(DAY, "Day " + day);
		text[MOON] = line(MOON, DayEvent.moonPhase(day));
		text[MODE] = line(MODE, mc.gameMode == null ? "Unknown" : mc.gameMode.getPlayerMode().getName());
		text[XP] = line(XP, String.valueOf(p.experienceLevel));
		ItemStack held = p.getMainHandItem();
		text[ITEM] = line(ITEM, BuiltInRegistries.ITEM.getKey(held.getItem()).toString());
		text[AUX] = line(AUX, String.valueOf(held.getDamageValue()));
		text[MC] = line(MC, mc.getLaunchedVersion());
		text[GFX] = line(GFX, mc.options.graphicsPreset().get().getSerializedName().toUpperCase(Locale.ROOT));
		text[UI] = line(UI, "GUI Scale " + mc.getWindow().getGuiScale());
	}

	/** Width of the visible lines in entries [from, to), or 0 when none is shown. */
	private int columnWidth(int from, int to) {
		int w = 0;
		for (int i = from; i < to; i++) {
			if (show[i].on()) w = Math.max(w, textWidth(text[i]));
		}
		return w;
	}

	private int columnLines(int from, int to) {
		int n = 0;
		for (int i = from; i < to; i++) {
			if (show[i].on()) n++;
		}
		return n;
	}

	@Override
	public int contentWidth() {
		int l = columnWidth(0, LEFT);
		int r = columnWidth(LEFT, LEFT + RIGHT);
		return l > 0 && r > 0 ? l + COLUMN_GAP + r : Math.max(Math.max(l, r), 8);
	}

	@Override
	public int contentHeight() {
		int n = Math.max(columnLines(0, LEFT), columnLines(LEFT, LEFT + RIGHT));
		return n == 0 ? lineHeight() : n * (lineHeight() + LINE_GAP) - LINE_GAP;
	}

	/** Draws the visible lines of entries [from, to) down from (x, y). */
	private void drawColumn(GuiGraphics g, int from, int to, int x, int y) {
		int cy = y;
		for (int i = from; i < to; i++) {
			if (!show[i].on()) continue;
			drawText(g, text[i], x, cy, Draw.alpha(textColor.argb(), lineOpacity[i].f()));
			cy += lineHeight() + LINE_GAP;
		}
	}

	@Override
	protected void drawContent(GuiGraphics g, int x, int y) {
		int l = columnWidth(0, LEFT);
		drawColumn(g, 0, LEFT, x, y);
		drawColumn(g, LEFT, LEFT + RIGHT, l > 0 ? x + l + COLUMN_GAP : x, y);
	}
}
