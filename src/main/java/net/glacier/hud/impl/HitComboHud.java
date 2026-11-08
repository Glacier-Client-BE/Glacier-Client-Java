package net.glacier.hud.impl;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.world.InteractionResult;

/**
 * "Combo: 7" counts your hits on entities in a row. The streak ends when no hit lands within the timeout.
 * Hits come from Fabric's AttackEntityCallback, which fires only for attacks on entities.
 */
public class HitComboHud extends CachedRowsHud {
	private final SliderSetting timeout = add(new SliderSetting("Timeout (s)", 2.0, 0.5, 10.0, 0.5));
	private final ToggleSetting showBest = add(new ToggleSetting("Show Best", false));

	private int combo;
	private int best;
	private long lastHitMs;

	/** Fabric callbacks cannot be unregistered, so the listener is added once for the whole process. */
	private static boolean hitListenerRegistered;

	public HitComboHud() {
		super("Hit Combo", "Counts consecutive hits on entities until you pause.", "Combo: 0");
		stackTopLeft(24);
		if (!hitListenerRegistered) {
			hitListenerRegistered = true;
			AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
				if (isEnabled() && mc.player != null && player == mc.player) onHit();
				return InteractionResult.PASS;
			});
		}
	}

	private void onHit() {
		combo++;
		best = Math.max(best, combo);
		lastHitMs = System.currentTimeMillis();
	}

	@Override
	protected boolean pill() {
		return true;
	}

	@Override
	protected void onDisable() {
		combo = 0;
	}

	@Override
	public void onTick() {
		if (combo > 0 && System.currentTimeMillis() - lastHitMs > (long) (timeout.f() * 1000f)) combo = 0;
		Part name = part("Combo: ", TEXT);
		Part value = part(String.valueOf(combo), goodHigh(combo, 10, 3));
		if (showBest.on()) setRows(single(name, value, part("  Best " + best, TEXT)));
		else setRows(single(name, value));
	}
}
