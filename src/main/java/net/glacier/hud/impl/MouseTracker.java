package net.glacier.hud.impl;

import net.glacier.hud.HudModule;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Draw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Mouse tracker (NRC mousetracker): a small arena with a dot that is pushed by your camera movement and drifts back
 * to the centre, with an optional crosshair and a fading trail. Movement is read from the player's rotation change
 * between frames, so no input hook is needed. Preallocated trail arrays, nothing is allocated per frame.
 */
public class MouseTracker extends HudModule {
	private static final int SIZE = 60;
	private static final int MAX_TRAIL = 32;

	private final ChoiceSetting shape = add(new ChoiceSetting("Shape", "Circle", "Circle", "Square"));
	private final SliderSetting dotSize = add(new SliderSetting("Dot Size", 3, 1, 8, 1));
	private final ToggleSetting crosshair = add(new ToggleSetting("Show Crosshair", true));
	private final ToggleSetting trail = add(new ToggleSetting("Show Trail", true));
	private final SliderSetting trailLength = add(new SliderSetting("Trail Length", 12, 2, MAX_TRAIL, 1));
	/** Pixels of dot movement per degree of camera movement. */
	private final SliderSetting sensitivity = add(new SliderSetting("Sensitivity", 1.5, 0.2, 5.0, 0.1));
	/** How fast the dot returns to the centre, per second. */
	private final SliderSetting recentre = add(new SliderSetting("Return Speed", 3.0, 0.0, 10.0, 0.5));

	private final float[] trailX = new float[MAX_TRAIL];
	private final float[] trailY = new float[MAX_TRAIL];
	private int trailHead;
	private float dotX;
	private float dotY;
	private float lastYaw;
	private float lastPitch;
	private long lastNanos;
	private boolean primed;

	public MouseTracker() {
		super("Mouse Tracker", "Shows your camera movement as a dot in a small arena.");
		stackTopLeft(90);
	}

	@Override
	protected void onDisable() {
		primed = false;
		dotX = 0f;
		dotY = 0f;
		for (int i = 0; i < MAX_TRAIL; i++) {
			trailX[i] = 0f;
			trailY[i] = 0f;
		}
	}

	@Override
	public int contentWidth() {
		return SIZE;
	}

	@Override
	public int contentHeight() {
		return SIZE;
	}

	/** Moves the dot by the rotation since the last frame, then eases it back towards the centre. */
	private void step() {
		long now = System.nanoTime();
		float dt = primed ? Math.min(0.25f, (now - lastNanos) / 1.0e9f) : 0f;
		lastNanos = now;
		if (mc.player == null) {
			primed = false;
			return;
		}
		float yaw = mc.player.getYRot();
		float pitch = mc.player.getXRot();
		if (primed) {
			dotX += Mth.wrapDegrees(yaw - lastYaw) * sensitivity.f();
			dotY += (pitch - lastPitch) * sensitivity.f();
		}
		lastYaw = yaw;
		lastPitch = pitch;
		primed = true;
		float keep = Math.max(0f, 1f - recentre.f() * dt);
		dotX *= keep;
		dotY *= keep;
		float half = SIZE / 2f - dotSize.f() - 1f;
		if (shape.is("Circle")) {
			float len = Mth.sqrt(dotX * dotX + dotY * dotY);
			if (len > half) {
				dotX *= half / len;
				dotY *= half / len;
			}
		} else {
			dotX = Mth.clamp(dotX, -half, half);
			dotY = Mth.clamp(dotY, -half, half);
		}
		trailHead = (trailHead + 1) % MAX_TRAIL;
		trailX[trailHead] = dotX;
		trailY[trailHead] = dotY;
	}

	@Override
	protected void drawContent(GuiGraphics g, int x, int y) {
		step();
		int c = resolve(TEXT);
		int cx = x + SIZE / 2;
		int cy = y + SIZE / 2;
		boolean circle = shape.is("Circle");
		Draw.round(g, x, y, SIZE, SIZE, circle ? SIZE / 2 : 3, Draw.alpha(0xFF000000, 0.25f));
		if (crosshair.on()) {
			int line = Draw.alpha(c, 0.35f);
			g.fill(cx, y + 3, cx + 1, y + SIZE - 3, line);
			g.fill(x + 3, cy, x + SIZE - 3, cy + 1, line);
		}
		int r = dotSize.i();
		if (trail.on()) {
			int n = Math.min(trailLength.i(), MAX_TRAIL);
			for (int i = n - 1; i >= 1; i--) {
				int idx = Math.floorMod(trailHead - i, MAX_TRAIL);
				float a = 0.5f * (1f - i / (float) n);
				int tr = Math.max(1, r - 1);
				Draw.round(g, Math.round(cx + trailX[idx] - tr), Math.round(cy + trailY[idx] - tr), tr * 2, tr * 2,
						tr, Draw.alpha(c, a));
			}
		}
		Draw.round(g, Math.round(cx + dotX - r), Math.round(cy + dotY - r), r * 2, r * 2, r, c);
	}
}
