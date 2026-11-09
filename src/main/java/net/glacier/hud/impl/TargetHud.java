package net.glacier.hud.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;

/** Name and health of the living entity under the crosshair (not yourself). Hidden when there is none. */
public class TargetHud extends CachedRowsHud {
	private boolean has;
	private String lastName = "";
	private int lastHp = -1;
	private int lastMax = -1;

	public TargetHud() {
		super("Target HUD", "Shows the name and health of the entity under your crosshair.", "");
		setPos(0.5f, 0.38f);
	}

	@Override
	public void onTick() {
		LivingEntity target = null;
		if (mc.player != null && mc.hitResult instanceof EntityHitResult ehr) {
			if (ehr.getEntity() instanceof LivingEntity le && le != mc.player && le.isAlive()) target = le;
		}
		has = target != null;
		if (!has) return;
		String name = target.getName().getString();
		int hp = Math.round(target.getHealth());
		int max = Math.max(1, Math.round(target.getMaxHealth()));
		if (name.equals(lastName) && hp == lastHp && max == lastMax) return;
		lastName = name;
		lastHp = hp;
		lastMax = max;
		int pct = hp * 100 / max;
		setRows(List.<Part[]>of(
				new Part[]{part(name, TEXT)},
				new Part[]{part("HP " + hp + "/" + max, goodHigh(pct, 60, 30))}));
	}

	@Override
	public void render(GuiGraphicsExtractor g, int sw, int sh) {
		if (has) super.render(g, sw, sh);
	}
}
