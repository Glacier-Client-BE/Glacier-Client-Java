package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;

/** Backs the Elements page. The toggles are read by MixinGui and MixinBossHealthOverlay to hide vanilla HUD pieces. */
public class ElementsModule extends Module {
	public final ToggleSetting hideItemName = add(new ToggleSetting("Hide Item Name Text", false));
	public final ToggleSetting hideJukebox = add(new ToggleSetting("Hide Jukebox Text", false));
	public final ToggleSetting hideTip = add(new ToggleSetting("Hide Tip Text", false));
	public final ToggleSetting hideActionbar = add(new ToggleSetting("Hide Actionbar Text", false));
	public final ToggleSetting hideBossBar = add(new ToggleSetting("Hide Boss Bar", false));
	public final ToggleSetting hideBossName = add(new ToggleSetting("Hide Boss Name", false));
	public final ToggleSetting showBossPercent = add(new ToggleSetting("Show Boss Percentage", false));
	public final SliderSetting bossPadding = add(new SliderSetting("Boss Bar Vertical Padding", 4, 0, 20, 1));
	public final ToggleSetting hideScoreboard = add(new ToggleSetting("Hide Scoreboard", false));
	public final ToggleSetting hideScores = add(new ToggleSetting("Hide Scores", false));
	public final ToggleSetting hideVignette = add(new ToggleSetting("Hide Vignette", false));
	public final ToggleSetting hideEffects = add(new ToggleSetting("Hide Effects Icon", false));
	public final ToggleSetting useBaseBg = add(new ToggleSetting("Use Base BG", false));
	/** Glacier design for every HUD element: navy panels, pills and the UI font. Off = vanilla look. */
	public final ToggleSetting glacierDesign = add(new ToggleSetting("Glacier Design", true));
	/** Adds the frosted inner edge to every HUD panel. */
	public final ToggleSetting useFrostedGlass = add(new ToggleSetting("Use Frosted Glass", false));
	public final ToggleSetting showSubtitles = add(new ToggleSetting("Show Sound Subtitles", false));

	public ElementsModule() {
		super("Elements", "Hide or show vanilla HUD elements.", Category.SYSTEM);
	}

	@Override
	public void afterLoad() {
		setEnabled(true);
		syncFromOptions();
	}

	/** Vanilla owns the subtitles option, so the setting mirrors it. */
	public void syncFromOptions() {
		if (mc.options != null) showSubtitles.set(mc.options.showSubtitles().get());
	}

	/** Writes the subtitles setting into the vanilla option. */
	public void applyOptions() {
		if (mc.options != null) {
			mc.options.showSubtitles().set(showSubtitles.on());
			mc.options.save();
		}
	}
}
