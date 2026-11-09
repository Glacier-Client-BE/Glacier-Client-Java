package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Action bar text tweaks (NRC actionbar module): hide, scale, vertical offset and how long a message stays. Hooks are
 * in {@code mixin.extrac.ExtraCGui} (renderOverlayMessage and setOverlayMessage). The jukebox "now playing" text
 * uses the same overlay and follows these settings.
 */
public class ActionBarPlus extends Module {
	private static ActionBarPlus instance;

	public final ToggleSetting hide = add(new ToggleSetting("Hide", false));
	public final SliderSetting scale = add(new SliderSetting("Scale %", 100, 50, 250, 5));
	/** Pixels; negative moves the text up. */
	public final SliderSetting offsetY = add(new SliderSetting("Y Offset", 0, -300, 100, 2));
	/** Ticks the message stays; vanilla is 60. */
	public final SliderSetting duration = add(new SliderSetting("Duration (ticks)", 60, 20, 300, 10));

	public ActionBarPlus() {
		super("Action Bar", "Scale, position, duration and hiding for the action bar text.", Category.RENDER);
		instance = this;
	}

	/** The enabled module, or null for vanilla. */
	public static ActionBarPlus active() {
		ActionBarPlus a = instance;
		return a != null && a.isEnabled() ? a : null;
	}

	public static boolean hidden() {
		ActionBarPlus a = active();
		return a != null && a.hide.on();
	}

	public static int durationOr(int vanilla) {
		ActionBarPlus a = active();
		return a == null ? vanilla : a.duration.i();
	}

	/** Pushes scale and offset about the text anchor (centre, 68 px above the bottom). True when a matrix was pushed. */
	public static boolean push(GuiGraphicsExtractor g) {
		ActionBarPlus a = active();
		if (a == null) return false;
		float k = a.scale.f() / 100f;
		float dy = a.offsetY.f();
		if (k == 1f && dy == 0f) return false;
		float cx = g.guiWidth() / 2f;
		float cy = g.guiHeight() - 68f;
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(cx, cy + dy);
		pose.scale(k, k);
		pose.translate(-cx, -cy);
		return true;
	}
}
