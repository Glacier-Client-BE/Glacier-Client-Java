package net.glacier.hud.impl;

import net.glacier.hud.HudModule;
import net.glacier.setting.ColorSetting;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Draw;
import net.glacier.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayDeque;

/** WASD, LMB/RMB with click-per-second counters, and an optional space bar cap. */
public class Keystrokes extends HudModule {
	/** Extra height of a mouse cap over a key cap, which holds the CPS line. */
	private static final int CPS_EXTRA = 6;
	private static final int SPACE_H = 12;
	private static final int SPACE_BAR_H = 2;
	/** Small gap between the label and the CPS line under it. */
	private static final int LABEL_CPS_GAP = 2;
	private static final float CPS_SCALE = 0.8f;
	private static final long ONE_SECOND_NS = 1_000_000_000L;

	private final ColorSetting pressedColor = add(new ColorSetting("Pressed Color", Theme.HUD_KEY_PRESSED));
	private final ColorSetting keyColor = add(new ColorSetting("Key Color", Theme.HUD_KEY_IDLE));
	private final ToggleSetting showSpace = add(new ToggleSetting("Show Space Bar", false));
	private final ToggleSetting showMouse = add(new ToggleSetting("Show Mouse Buttons", true));
	private final ToggleSetting showCps = add(new ToggleSetting("Show CPS In Keys", true));
	private final ToggleSetting showSneak = add(new ToggleSetting("Show Sneak", false));
	private final ToggleSetting showSprint = add(new ToggleSetting("Show Sprint", false));
	private final SliderSetting keySize = add(new SliderSetting("Key Size", 20, 12, 32, 1));
	private final SliderSetting keyGap = add(new SliderSetting("Key Gap", 2, 0, 8, 1));

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

	private int tile() {
		return keySize.i();
	}

	private int gap() {
		return keyGap.i();
	}

	private int width() {
		return tile() * 3 + gap() * 2;
	}

	private int wide() {
		return (width() - gap()) / 2;
	}

	private int mouseH() {
		return showCps.on() ? tile() + CPS_EXTRA : tile();
	}

	@Override
	public int contentWidth() {
		return width();
	}

	@Override
	public int contentHeight() {
		int h = (tile() + gap()) * 2 - gap();
		if (showMouse.on()) h += gap() + mouseH();
		if (showSpace.on()) h += gap() + SPACE_H;
		if (showSneak.on() || showSprint.on()) h += gap() + SPACE_H;
		return h;
	}

	/** Idle cap fill: the Glacier smoked glass when Glacier design is on and the colour is still the default. */
	private int idleFill() {
		return glacier() && keyColor.argb() == Theme.HUD_KEY_IDLE ? Theme.GLACIER_KEY_IDLE : keyColor.argb();
	}

	/** Pressed cap fill: the Glacier white when Glacier design is on and the colour is still the default. */
	private int pressedFill() {
		return glacier() && pressedColor.argb() == Theme.HUD_KEY_PRESSED ? Theme.GLACIER_KEY_PRESSED : pressedColor.argb();
	}

	private void cap(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, boolean pressed) {
		Draw.round(g, x, y, w, h, radius(), pressed ? pressedFill() : idleFill());
		int tx = x + (w - textWidth(label)) / 2;
		int ty = y + (h - lineHeight()) / 2 + 1;
		if (pressed && !glacier()) g.text(mc.font, label, tx, ty, Theme.HUD_INK, false);
		else drawText(g, label, tx, ty, pressed ? Theme.HUD_INK : TEXT);
	}

	/** Mouse cap: the button label with the CPS line below, the pair centred vertically in the cap. */
	private void mouseCap(GuiGraphicsExtractor g, int x, int y, String label, int cps, boolean pressed) {
		int wide = wide();
		int mh = mouseH();
		Draw.round(g, x, y, wide, mh, radius(), pressed ? pressedFill() : idleFill());
		if (!showCps.on()) {
			drawText(g, label, x + (wide - textWidth(label)) / 2, y + (mh - lineHeight()) / 2 + 1, pressed ? Theme.HUD_INK : TEXT);
			return;
		}
		int cpsH = Math.round(lineHeight() * CPS_SCALE);
		int blockH = lineHeight() + LABEL_CPS_GAP + cpsH;
		int top = y + (mh - blockH) / 2;

		int lx = x + (wide - textWidth(label)) / 2;
		drawText(g, label, lx, top, pressed ? Theme.HUD_INK : TEXT);

		String c = cps + " CPS";
		float cx = x + (wide - textWidth(c) * CPS_SCALE) / 2f;
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
	protected void drawContent(GuiGraphicsExtractor g, int x, int y) {
		var o = mc.options;
		boolean w = o.keyUp.isDown();
		boolean a = o.keyLeft.isDown();
		boolean s = o.keyDown.isDown();
		boolean d = o.keyRight.isDown();

		int t = tile();
		int gp = gap();
		int row = t + gp;
		int width = width();
		cap(g, x + row, y, t, t, "W", w);
		cap(g, x, y + row, t, t, "A", a);
		cap(g, x + row, y + row, t, t, "S", s);
		cap(g, x + row * 2, y + row, t, t, "D", d);

		int cy = y + row * 2;
		if (showMouse.on()) {
			boolean lmb = mc.mouseHandler.isLeftPressed();
			boolean rmb = mc.mouseHandler.isRightPressed();
			mouseCap(g, x, cy, "LMB", leftClicks.size(), lmb);
			mouseCap(g, x + wide() + gp, cy, "RMB", rightClicks.size(), rmb);
			cy += mouseH() + gp;
		}

		if (showSpace.on()) {
			boolean jump = o.keyJump.isDown();
			Draw.round(g, x, cy, width, SPACE_H, radius(), jump ? pressedFill() : idleFill());
			int bw = width / 2;
			int bx = x + (width - bw) / 2;
			int by = cy + (SPACE_H - SPACE_BAR_H) / 2;
			g.fill(bx, by, bx + bw, by + SPACE_BAR_H, jump ? Theme.HUD_INK : textColor.argb());
			cy += SPACE_H + gp;
		}
		if (showSneak.on() || showSprint.on()) {
			boolean both = showSneak.on() && showSprint.on();
			int w2 = both ? wide() : width;
			int cx = x;
			if (showSneak.on()) {
				cap(g, cx, cy, w2, SPACE_H, "Sneak", o.keyShift.isDown());
				cx += w2 + gp;
			}
			if (showSprint.on()) {
				cap(g, cx, cy, w2, SPACE_H, "Sprint", mc.player != null && mc.player.isSprinting());
			}
		}
	}
}
