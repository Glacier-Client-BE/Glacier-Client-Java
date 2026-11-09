package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Screen title and subtitle tweaks (NRC titles module): hide either, scale, vertical offset and how long the title
 * stays or fades. Hooks are in {@code mixin.extrac.ExtraCGui} (renderTitle and setTimes).
 */
public class TitlesPlus extends Module {
	private static TitlesPlus instance;

	public final ToggleSetting hideTitle = add(new ToggleSetting("Hide Title", false));
	public final ToggleSetting hideSubtitle = add(new ToggleSetting("Hide Subtitle", false));
	public final SliderSetting scale = add(new SliderSetting("Scale %", 100, 25, 200, 5));
	/** Pixels; negative moves the title up. */
	public final SliderSetting offsetY = add(new SliderSetting("Y Offset", 0, -200, 200, 2));
	public final SliderSetting stay = add(new SliderSetting("Stay Time %", 100, 25, 300, 5));
	public final SliderSetting fade = add(new SliderSetting("Fade Time %", 100, 25, 300, 5));

	public TitlesPlus() {
		super("Titles", "Scale, position, duration and hiding for /title text.", Category.RENDER);
		instance = this;
	}

	/** The enabled module, or null for vanilla. */
	public static TitlesPlus active() {
		TitlesPlus t = instance;
		return t != null && t.isEnabled() ? t : null;
	}

	/** True to skip the whole title draw (both parts hidden). */
	public static boolean hideAll() {
		TitlesPlus t = active();
		return t != null && t.hideTitle.on() && t.hideSubtitle.on();
	}

	public static boolean hideTitleNow() {
		TitlesPlus t = active();
		return t != null && t.hideTitle.on();
	}

	public static boolean hideSubtitleNow() {
		TitlesPlus t = active();
		return t != null && t.hideSubtitle.on();
	}

	/** Pushes scale and offset about the screen centre. Returns true when a matrix was pushed. */
	public static boolean push(GuiGraphicsExtractor g) {
		TitlesPlus t = active();
		if (t == null) return false;
		float k = t.scale.f() / 100f;
		float dy = t.offsetY.f();
		if (k == 1f && dy == 0f) return false;
		float cx = g.guiWidth() / 2f;
		float cy = g.guiHeight() / 2f;
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(cx, cy + dy);
		pose.scale(k, k);
		pose.translate(-cx, -cy);
		return true;
	}

	/** Scales a fade-in or fade-out duration in ticks; negative values mean "keep" and pass through. */
	public static int fadeOr(int ticks) {
		TitlesPlus t = active();
		return t == null || ticks < 0 ? ticks : Math.round(ticks * t.fade.f() / 100f);
	}

	/** Scales the stay duration in ticks; negative values mean "keep" and pass through. */
	public static int stayOr(int ticks) {
		TitlesPlus t = active();
		return t == null || ticks < 0 ? ticks : Math.round(ticks * t.stay.f() / 100f);
	}
}
