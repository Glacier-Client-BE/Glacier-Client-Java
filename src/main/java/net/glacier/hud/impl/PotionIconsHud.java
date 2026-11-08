package net.glacier.hud.impl;

import net.glacier.hud.HudModule;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.List;

/** Active potion effects as their vanilla icons with the remaining time and level, rebuilt once per tick. */
public class PotionIconsHud extends HudModule {
	private static final int ICON = 18;
	private static final int GAP = 2;
	private static final int TEXT_GAP = 3;
	private static final String INFINITE = "inf";

	private final ChoiceSetting orientation = add(new ChoiceSetting("Orientation", "Horizontal", "Horizontal", "Vertical"));
	private final ToggleSetting showTime = add(new ToggleSetting("Show Time", true));
	private final ToggleSetting showAmplifier = add(new ToggleSetting("Show Amplifier", true));
	private final ToggleSetting colorByTime = add(new ToggleSetting("Color By Time", true));
	private final ToggleSetting hideWhenEmpty = add(new ToggleSetting("Hide When Empty", true));
	private final ChoiceSetting sort = add(new ChoiceSetting("Sort", "Default", "Default", "Duration", "Name"));

	private Identifier[] sprites = new Identifier[0];
	private String[] times = new String[0];
	private String[] levels = new String[0];
	private int[] colors = new int[0];
	private int count;

	public PotionIconsHud() {
		super("Potion Icons", "Shows your active potion effects as icons with their time and level.");
		setPos(0.02f, 0.9f);
	}

	private boolean horizontal() {
		return orientation.is("Horizontal");
	}

	@Override
	public void onTick() {
		if (mc.player == null) {
			count = 0;
			return;
		}
		List<MobEffectInstance> list = PotionTimersHud.sorted(mc.player.getActiveEffects(), sort.get());
		count = list.size();
		if (sprites.length < count) {
			sprites = new Identifier[count];
			times = new String[count];
			levels = new String[count];
			colors = new int[count];
		}
		for (int i = 0; i < count; i++) {
			MobEffectInstance e = list.get(i);
			sprites[i] = Gui.getMobEffectSprite(e.getEffect());
			levels[i] = showAmplifier.on() && e.getAmplifier() > 0 ? PotionTimersHud.level(e.getAmplifier()) : null;
			if (!showTime.on()) {
				times[i] = null;
			} else if (e.isInfiniteDuration()) {
				times[i] = INFINITE;
				colors[i] = TEXT;
			} else {
				int sec = Math.max(0, e.getDuration()) / 20;
				times[i] = PotionTimersHud.clock(sec);
				colors[i] = colorByTime.on() ? PotionTimersHud.timeColor(sec) : TEXT;
			}
		}
	}

	/** Width of one cell: just the icon, or wider when the time text under it is wider (horizontal layout). */
	private int cellWidth(int i) {
		return horizontal() && times[i] != null ? Math.max(ICON, textWidth(times[i])) : ICON;
	}

	@Override
	public int contentWidth() {
		int w = ICON;
		if (horizontal()) {
			w = 0;
			for (int i = 0; i < count; i++) w += cellWidth(i) + (i > 0 ? GAP : 0);
			return Math.max(ICON, w);
		}
		for (int i = 0; i < count; i++) {
			if (times[i] != null) w = Math.max(w, ICON + TEXT_GAP + textWidth(times[i]));
		}
		return w;
	}

	@Override
	public int contentHeight() {
		if (horizontal()) return ICON + (showTime.on() ? lineHeight() + 1 : 0);
		int n = Math.max(1, count);
		return n * ICON + (n - 1) * GAP;
	}

	@Override
	public void render(GuiGraphics g, int sw, int sh) {
		if (count == 0 && hideWhenEmpty.on()) return;
		super.render(g, sw, sh);
	}

	@Override
	protected void drawContent(GuiGraphics g, int x, int y) {
		boolean hor = horizontal();
		int cx = x;
		int cy = y;
		for (int i = 0; i < count; i++) {
			int cell = cellWidth(i);
			int ix = hor ? cx + (cell - ICON) / 2 : cx;
			g.blitSprite(RenderPipelines.GUI_TEXTURED, sprites[i], ix, cy, ICON, ICON);
			if (levels[i] != null) drawText(g, levels[i], ix + ICON - textWidth(levels[i]), cy, TEXT);
			if (times[i] != null) {
				if (hor) drawText(g, times[i], cx + (cell - textWidth(times[i])) / 2, cy + ICON + 1, colors[i]);
				else drawText(g, times[i], cx + ICON + TEXT_GAP, cy + (ICON - lineHeight()) / 2 + 1, colors[i]);
			}
			if (hor) cx += cell + GAP;
			else cy += ICON + GAP;
		}
	}
}
