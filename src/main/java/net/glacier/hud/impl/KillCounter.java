package net.glacier.hud.impl;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.glacier.setting.ToggleSetting;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;

import java.util.Arrays;

/**
 * "Kills: 3" counts your killstreak and resets when you die. A kill is a living entity you attacked that is then
 * seen dying; attacks come from Fabric's AttackEntityCallback, deaths are polled on tick.
 */
public class KillCounter extends CachedRowsHud {
	/** How many recently attacked entities are watched at once. */
	private static final int WATCH = 8;
	/** Ticks after the last hit during which a death still counts as your kill. */
	private static final int WINDOW_TICKS = 200;

	private final ToggleSetting showBest = add(new ToggleSetting("Show Best", false));

	private final LivingEntity[] watched = new LivingEntity[WATCH];
	private final int[] hitAge = new int[WATCH];
	private int next;
	private int kills;
	private int best;
	private boolean wasDead;
	private int shownKills = -1;
	private int shownBest = -1;
	private boolean shownBestOn;

	/** Fabric callbacks cannot be unregistered, so the listener is added once for the whole process. */
	private static boolean listenerRegistered;

	public KillCounter() {
		super("Kill Counter", "Counts your killstreak; resets when you die.", "Kills: 0");
		stackTopLeft(26);
		if (!listenerRegistered) {
			listenerRegistered = true;
			AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
				if (isEnabled() && mc.player != null && player == mc.player && entity instanceof LivingEntity le) watch(le);
				return InteractionResult.PASS;
			});
		}
	}

	private void watch(LivingEntity e) {
		for (int i = 0; i < WATCH; i++) {
			if (watched[i] == e) {
				hitAge[i] = 0;
				return;
			}
		}
		watched[next] = e;
		hitAge[next] = 0;
		next = (next + 1) % WATCH;
	}

	@Override
	protected boolean pill() {
		return true;
	}

	@Override
	protected void onDisable() {
		Arrays.fill(watched, null);
		kills = 0;
	}

	@Override
	public void onTick() {
		boolean dead = mc.player != null && mc.player.isDeadOrDying();
		if (dead && !wasDead) {
			kills = 0;
			Arrays.fill(watched, null);
		}
		wasDead = dead;
		for (int i = 0; i < WATCH; i++) {
			LivingEntity e = watched[i];
			if (e == null) continue;
			if (e.isDeadOrDying()) {
				watched[i] = null;
				kills++;
				best = Math.max(best, kills);
			} else if (e.isRemoved() || ++hitAge[i] > WINDOW_TICKS) {
				watched[i] = null;
			}
		}
		boolean bestOn = showBest.on();
		if (kills == shownKills && best == shownBest && bestOn == shownBestOn) return;
		shownKills = kills;
		shownBest = best;
		shownBestOn = bestOn;
		Part name = part("Kills: ", TEXT);
		Part value = part(String.valueOf(kills), goodHigh(kills, 10, 3));
		setRows(bestOn ? single(name, value, part("  Best " + best, TEXT)) : single(name, value));
	}
}
