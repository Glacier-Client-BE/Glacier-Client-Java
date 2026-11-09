package net.glacier.hud.impl;

import net.glacier.setting.SliderSetting;
import net.glacier.ui.Theme;

/**
 * Jump reset timing: after you take a hit and jump, shows how many ticks passed between the two ("Jump Reset: 2t").
 * Jumping on the same tick as the hit or one tick later is the ideal reset (green), up to 3 ticks is fine (yellow),
 * later is late (red). A jump is detected as leaving the ground with upward speed, a hit as hurtTime rising.
 */
public class JumpResetHud extends CachedRowsHud {
	/** Longest gap after a hit that still counts as a reset attempt. */
	private static final int WINDOW = 10;

	private final SliderSetting display = add(new SliderSetting("Display Time (s)", 3.0, 1.0, 10.0, 0.5));

	private int lastTick = Integer.MIN_VALUE;
	private int hitTick = Integer.MIN_VALUE;
	private boolean wasOnGround;
	private int prevHurt;
	private long shownUntilMs;

	public JumpResetHud() {
		super("Jump Reset", "Shows how many ticks after being hit you jumped.", "Jump Reset: --");
		stackTopLeft(62);
	}

	@Override
	protected void onDisable() {
		lastTick = Integer.MIN_VALUE;
		hitTick = Integer.MIN_VALUE;
		shownUntilMs = 0;
	}

	@Override
	protected boolean hidden() {
		return System.currentTimeMillis() > shownUntilMs;
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		int tick = mc.player.tickCount;
		if (tick == lastTick) return;
		lastTick = tick;
		boolean ground = mc.player.onGround();
		int hurt = mc.player.hurtTime;
		if (hurt > prevHurt) hitTick = tick;
		prevHurt = hurt;
		boolean jumped = wasOnGround && !ground && mc.player.getDeltaMovement().y > 0.2;
		wasOnGround = ground;
		if (jumped && hitTick != Integer.MIN_VALUE && tick - hitTick <= WINDOW) {
			int diff = tick - hitTick;
			int color = diff <= 1 ? Theme.HUD_GOOD : diff <= 3 ? Theme.HUD_WARN : Theme.HUD_BAD;
			setRows(single(part("Jump Reset: ", TEXT), part(diff + "t", color)));
			shownUntilMs = System.currentTimeMillis() + (long) (display.get() * 1000.0);
			hitTick = Integer.MIN_VALUE;
		}
	}
}
