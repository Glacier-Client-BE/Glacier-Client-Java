package net.glacier.hud.impl;

import net.glacier.hud.HudModule;
import net.glacier.setting.ColorSetting;
import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Draw;
import net.glacier.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayDeque;

/** WASD, LMB/RMB with click-per-second counters, and an optional space bar cap. */
public class Keystrokes extends HudModule {
	private static final int TILE = 20;
	private static final int GAP = 2;
	private static final int ROW = TILE + GAP;
	private static final int WIDTH = TILE * 3 + GAP * 2;
	private static final int WIDE = (WIDTH - GAP) / 2;
	private static final int MOUSE_H = 26;
	private static final int SPACE_H = 12;
	private static final int SPACE_BAR_H = 2;
	/** Small gap between the label and the CPS line under it. */
	private static final int LABEL_CPS_GAP = 2;
	private static final float CPS_SCALE = 0.8f;
	private static final long ONE_SECOND_NS = 1_000_000_000L;

	private final ColorSetting pressedColor = add(new ColorSetting("Pressed Color", Theme.HUD_KEY_PRESSED));
	private final ColorSetting keyColor = add(new ColorSetting("Key Color", Theme.HUD_KEY_IDLE));
	private final ToggleSetting showSpace = add(new ToggleSetting("Show Space Bar", false));

	private final ArrayDeque<Long> leftClicks = new ArrayDeque<>();
	private final ArrayDeque<Long> rightClicks = new ArrayDeque<>();
	private boolean prevLeft;
	private boolean prevRight;

	public Keystrokes() {
		super("Keystrokes", "Shows your movement and click inputs.");
		background.set(false);
		padding.set(0.0);
		setPos(0.85f, 0.6f);
	}

	@Override
	protected void onDisable() {
		leftClicks.clear();
		rightClicks.clear();
		prevLeft = false;
		prevRight = false;
	}

	/**
	 * Polls the left and right mouse button state once per client tick and counts a click on each
	 * up-to-down edge compared with the previous tick. A press shorter than one tick (about 50 ms) can be
	 * missed; this is an accepted approximation.
	 */
	@Override
	public void onTick() {
		if (mc.mouseHandler == null) return;
		long now = System.nanoTime();
		boolean l = mc.mouseHandler.isLeftPressed();
		boolean r = mc.mouseHandler.isRightPressed();
		if (l && !prevLeft) leftClicks.addLast(now);
		if (r && !prevRight) rightClicks.addLast(now);
		prevLeft = l;
		prevRight = r;
		prune(leftClicks, now);
		prune(rightClicks, now);
	}

	private static void prune(ArrayDeque<Long> q, long now) {
		while (!q.isEmpty() && now - q.peekFirst() > ONE_SECOND_NS) q.pollFirst();
	}

	@Override
	public int contentWidth() {
		return WIDTH;
	}

	@Override
	public int contentHeight() {
		int h = ROW + ROW + MOUSE_H;
		return showSpace.on() ? h + GAP + SPACE_H : h;
	}

	/** Idle cap fill: the Glacier smoked glass when Glacier design is on and the colour is still the default. */
	private int idleFill() {
		return glacier() && keyColor.argb() == Theme.HUD_KEY_IDLE ? Theme.GLACIER_KEY_IDLE : keyColor.argb();
	}

	/** Pressed cap fill: the Glacier white when Glacier design is on and the colour is still the default. */
	private int pressedFill() {
		return glacier() && pressedColor.argb() == Theme.HUD_KEY_PRESSED ? Theme.GLACIER_KEY_PRESSED : pressedColor.argb();
	}

	private void cap(GuiGraphics g, int x, int y, int w, int h, String label, boolean pressed) {
		Draw.round(g, x, y, w, h, radius(), pressed ? pressedFill() : idleFill());
		int tx = x + (w - textWidth(label)) / 2;
		int ty = y + (h - lineHeight()) / 2 + 1;
		if (pressed && !glacier()) g.drawString(mc.font, label, tx, ty, Theme.HUD_INK, false);
		else drawText(g, label, tx, ty, pressed ? Theme.HUD_INK : TEXT);
	}

	/** Mouse cap: the button label with the CPS line below, the pair centred vertically in the cap. */
	private void mouseCap(GuiGraphics g, int x, int y, String label, int cps, boolean pressed) {
		Draw.round(g, x, y, WIDE, MOUSE_H, radius(), pressed ? pressedFill() : idleFill());
		int cpsH = Math.round(lineHeight() * CPS_SCALE);
		int blockH = lineHeight() + LABEL_CPS_GAP + cpsH;
		int top = y + (MOUSE_H - blockH) / 2;

		int lx = x + (WIDE - textWidth(label)) / 2;
		drawText(g, label, lx, top, pressed ? Theme.HUD_INK : TEXT);

		String c = cps + " CPS";
		float cx = x + (WIDE - textWidth(c) * CPS_SCALE) / 2f;
		var p = g.pose();
		p.pushMatrix();
		try {
			p.translate(cx, top + lineHeight() + LABEL_CPS_GAP);
			p.scale(CPS_SCALE, CPS_SCALE);
			drawText(g, c, 0, 0, pressed ? Theme.HUD_INK : Theme.HUD_MUTED);
		} finally {
			p.popMatrix();
		}
	}

	@Override
	protected void drawContent(GuiGraphics g, int x, int y) {
		var o = mc.options;
		boolean w = o.keyUp.isDown();
		boolean a = o.keyLeft.isDown();
		boolean s = o.keyDown.isDown();
		boolean d = o.keyRight.isDown();
		boolean lmb = mc.mouseHandler.isLeftPressed();
		boolean rmb = mc.mouseHandler.isRightPressed();

		cap(g, x + ROW, y, TILE, TILE, "W", w);
		cap(g, x, y + ROW, TILE, TILE, "A", a);
		cap(g, x + ROW, y + ROW, TILE, TILE, "S", s);
		cap(g, x + ROW * 2, y + ROW, TILE, TILE, "D", d);

		int my = y + ROW * 2;
		mouseCap(g, x, my, "LMB", leftClicks.size(), lmb);
		mouseCap(g, x + WIDE + GAP, my, "RMB", rightClicks.size(), rmb);

		if (showSpace.on()) {
			boolean jump = o.keyJump.isDown();
			int sy = my + MOUSE_H + GAP;
			Draw.round(g, x, sy, WIDTH, SPACE_H, radius(), jump ? pressedFill() : idleFill());
			int bw = WIDTH / 2;
			int bx = x + (WIDTH - bw) / 2;
			int by = sy + (SPACE_H - SPACE_BAR_H) / 2;
			g.fill(bx, by, bx + bw, by + SPACE_BAR_H, jump ? Theme.HUD_INK : textColor.argb());
		}
	}
}
