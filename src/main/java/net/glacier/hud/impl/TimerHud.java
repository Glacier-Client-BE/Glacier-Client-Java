package net.glacier.hud.impl;

import com.mojang.blaze3d.platform.InputConstants;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Theme;

/**
 * Session timer (time since the module was enabled) or a stopwatch. In Stopwatch mode the Toggle Key
 * starts and pauses it; Shift + Toggle Key resets it. Keys are only read while no screen is open.
 * Hide Hours folds hours into minutes (mm:ss); Hide Seconds shows whole minutes only.
 */
public class TimerHud extends CachedRowsHud {
	private static final int SHIFT = 340; // GLFW_KEY_LEFT_SHIFT
	private final ChoiceSetting mode = add(new ChoiceSetting("Mode", "Session", "Session", "Stopwatch"));
	private final ChoiceSetting key = add(new ChoiceSetting("Toggle Key", "J",
			"None", "G", "H", "J", "K", "L", "N", "P", "U", "Y"));
	private final ToggleSetting hideHours = add(new ToggleSetting("Hide Hours", false));
	private final ToggleSetting hideSeconds = add(new ToggleSetting("Hide Seconds", false));
	private long sessionStart;
	private long elapsedBefore;
	private long runStart;
	private boolean running;
	private boolean keyWasDown;
	private long shownSec = -1;
	private boolean shownRunning;
	private boolean shownHideH;
	private boolean shownHideS;

	public TimerHud() {
		super("Timer HUD", "Session timer or a key-toggled stopwatch.", "00:00");
		stackTopLeft(16);
	}

	@Override
	protected void onEnable() {
		sessionStart = System.currentTimeMillis();
		shownSec = -1;
	}

	@Override
	public void onTick() {
		long now = System.currentTimeMillis();
		boolean stopwatch = mode.is("Stopwatch");
		if (stopwatch) pollKey(now);
		long ms = stopwatch ? elapsedBefore + (running ? now - runStart : 0) : now - sessionStart;
		long sec = ms / 1000;
		boolean active = !stopwatch || running;
		boolean hH = hideHours.on();
		boolean hS = hideSeconds.on();
		if (sec == shownSec && active == shownRunning && hH == shownHideH && hS == shownHideS) return;
		shownSec = sec;
		shownRunning = active;
		shownHideH = hH;
		shownHideS = hS;
		setRows(single(part(format(sec, hH, hS), active ? TEXT : Theme.HUD_WARN)));
	}

	private void pollKey(long now) {
		int code = key.is("None") ? -1 : key.get().charAt(0);
		boolean down = code > 0 && mc.screen == null && InputConstants.isKeyDown(mc.getWindow(), code);
		if (down && !keyWasDown) {
			if (InputConstants.isKeyDown(mc.getWindow(), SHIFT)) {
				running = false;
				elapsedBefore = 0;
			} else if (running) {
				elapsedBefore += now - runStart;
				running = false;
			} else {
				runStart = now;
				running = true;
			}
		}
		keyWasDown = down;
	}

	private static String format(long sec, boolean hideHours, boolean hideSeconds) {
		long h = sec / 3600, m = sec / 60 % 60, s = sec % 60;
		if (hideSeconds) {
			if (hideHours) return String.valueOf(sec / 60);
			return h > 0 ? h + ":" + pad2(m) : String.valueOf(sec / 60);
		}
		if (hideHours) return pad2(sec / 60) + ":" + pad2(s);
		String ms = pad2(m) + ":" + pad2(s);
		return h > 0 ? h + ":" + ms : ms;
	}

	private static String pad2(long v) {
		return (v < 10 ? "0" : "") + v;
	}
}
