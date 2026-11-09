package net.glacier.hud.impl;

import net.glacier.hud.HudModule;
import net.glacier.ui.Draw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Mob;

/** A sword icon while the crosshair is on a mob. Hidden otherwise. */
public class MobIndicator extends HudModule {
	/** Font Awesome "sword" glyph. */
	private static final int SWORD = 0xf71c;

	public MobIndicator() {
		super("Mob Indicator", "Shows a sword icon while your crosshair is on a mob.");
		setPos(0.5f, 0.42f);
	}

	@Override
	public void render(GuiGraphicsExtractor g, int sw, int sh) {
		if (mc.crosshairPickEntity instanceof Mob) super.render(g, sw, sh);
	}

	@Override
	public int contentWidth() {
		return Draw.iconWidth(SWORD, 1f);
	}

	@Override
	public int contentHeight() {
		return lineHeight();
	}

	@Override
	protected void drawContent(GuiGraphicsExtractor g, int x, int y) {
		drawParts(g, x, y, icon(SWORD, TEXT));
	}
}
