package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.module.ModuleManager;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Sidebar scoreboard tweaks (NRC scoreboard module and the Bedrock scoreboard options): hide the red numbers, text
 * shadow, background style and opacity, scale and position offsets. The hooks are in {@code mixin.extrac.ExtraCGui}
 * around Gui.displayScoreboardSidebar. The numbers are hidden through the existing Elements "Hide Scores" setting
 * (MixinGuiElements owns that draw call), which this module mirrors while it is enabled.
 */
public class ScoreboardPlus extends Module {
	private static ScoreboardPlus instance;

	public final ToggleSetting hideNumbers = add(new ToggleSetting("Hide Numbers", false));
	public final ToggleSetting shadow = add(new ToggleSetting("Text Shadow", false));
	public final ChoiceSetting background = add(new ChoiceSetting("Background", "Vanilla", "Vanilla", "None", "Custom"));
	public final SliderSetting opacity = add(new SliderSetting("Background Opacity %", 30, 0, 100, 5));
	public final SliderSetting scale = add(new SliderSetting("Scale %", 100, 50, 200, 5));
	/** Pixels; negative moves the sidebar left. */
	public final SliderSetting offsetX = add(new SliderSetting("X Offset", 0, -600, 600, 2));
	/** Pixels; negative moves the sidebar up. */
	public final SliderSetting offsetY = add(new SliderSetting("Y Offset", 0, -300, 300, 2));

	/** True while this module has switched the Elements "Hide Scores" setting on. */
	private boolean lastHide;

	public ScoreboardPlus() {
		super("Scoreboard", "Hide numbers, shadow, background, scale and position of the sidebar scoreboard.", Category.RENDER);
		instance = this;
	}

	/** The enabled module, or null for vanilla. */
	public static ScoreboardPlus active() {
		ScoreboardPlus s = instance;
		return s != null && s.isEnabled() ? s : null;
	}

	@Override
	public void onTick() {
		if (lastHide == hideNumbers.on()) return;
		ElementsModule e = ModuleManager.get(ElementsModule.class);
		if (e == null) return;
		lastHide = hideNumbers.on();
		e.hideScores.set(lastHide);
	}

	@Override
	protected void onDisable() {
		if (!lastHide) return;
		lastHide = false;
		ElementsModule e = ModuleManager.get(ElementsModule.class);
		if (e != null) e.hideScores.set(false);
	}

	/** Pushes the scale and offset transform around the sidebar. Returns true when a matrix was pushed. */
	public static boolean push(GuiGraphicsExtractor g) {
		ScoreboardPlus s = active();
		if (s == null) return false;
		float k = s.scale.f() / 100f;
		float dx = s.offsetX.f();
		float dy = s.offsetY.f();
		if (k == 1f && dx == 0f && dy == 0f) return false;
		float w = g.guiWidth() - 1f;
		float h = g.guiHeight() / 2f;
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(w + dx, h + dy);
		pose.scale(k, k);
		pose.translate(-w, -h);
		return true;
	}

	/** ARGB for a sidebar background that vanilla draws with the given base alpha. */
	public static int backgroundOr(int vanilla) {
		ScoreboardPlus s = active();
		if (s == null || s.background.is("Vanilla")) return vanilla;
		if (s.background.is("None")) return 0;
		return Math.round(s.opacity.f() / 100f * 255f) << 24;
	}

	/** True when sidebar names and title are drawn with a shadow. */
	public static boolean shadowOr(boolean vanilla) {
		ScoreboardPlus s = active();
		return s != null && s.shadow.on() || vanilla;
	}
}
