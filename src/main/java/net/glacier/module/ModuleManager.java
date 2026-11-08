package net.glacier.module;

import com.mojang.blaze3d.platform.InputConstants;
import net.glacier.GlacierClient;
import net.minecraft.client.Minecraft;
import net.glacier.hud.HudModule;
import net.glacier.hud.impl.ArmorHud;
import net.glacier.hud.impl.CoordinatesHud;
import net.glacier.hud.impl.DirectionHud;
import net.glacier.hud.impl.FpsCounter;
import net.glacier.hud.impl.Keystrokes;
import net.glacier.hud.impl.PingCounter;
import net.glacier.hud.impl.Speedometer;
import net.glacier.module.impl.CosmeticsModule;
import net.glacier.module.impl.EditorsModule;
import net.glacier.module.impl.ElementsModule;
import net.glacier.module.impl.Fullbright;
import net.glacier.module.impl.PerformanceMods;
import net.glacier.social.GlacierPlayersModule;

import net.glacier.hud.impl.AirSupplyHud;
import net.glacier.hud.impl.AttackCooldownHud;
import net.glacier.hud.impl.BlockInfoHud;
import net.glacier.hud.impl.BowIndicator;
import net.glacier.hud.impl.ChunkHud;
import net.glacier.hud.impl.ClockHud;
import net.glacier.hud.impl.CpsHud;
import net.glacier.hud.impl.CrosshairCount;
import net.glacier.hud.impl.DayCounter;
import net.glacier.hud.impl.DayEvent;
import net.glacier.hud.impl.DaysPlayed;
import net.glacier.hud.impl.DeathCoordinates;
import net.glacier.hud.impl.ElytraPitch;
import net.glacier.hud.impl.EntityCountHud;
import net.glacier.hud.impl.FallDistanceHud;
import net.glacier.hud.impl.FoodStats;
import net.glacier.hud.impl.FpsStatsHud;
import net.glacier.hud.impl.GameModeHud;
import net.glacier.hud.impl.HealthHud;
import net.glacier.hud.impl.HitComboHud;
import net.glacier.hud.impl.ItemsCounter;
import net.glacier.hud.impl.LightLevelHud;
import net.glacier.hud.impl.LowDurability;
import net.glacier.hud.impl.MainhandHud;
import net.glacier.hud.impl.MemoryUsage;
import net.glacier.hud.impl.MobIndicator;
import net.glacier.hud.impl.OffhandHud;
import net.glacier.hud.impl.PlayersOnlineHud;
import net.glacier.hud.impl.PotionTimersHud;
import net.glacier.hud.impl.ReachDisplay;
import net.glacier.hud.impl.RotationHud;
import net.glacier.hud.impl.ServerDisplay;
import net.glacier.hud.impl.SessionTimeHud;
import net.glacier.hud.impl.SlotCounter;
import net.glacier.hud.impl.StatusHud;
import net.glacier.hud.impl.TargetHud;
import net.glacier.hud.impl.TimerHud;
import net.glacier.hud.impl.TntTimerHud;
import net.glacier.hud.impl.VerticalSpeedHud;
import net.glacier.hud.impl.WalkDistance;
import net.glacier.hud.impl.WeatherHud;
import net.glacier.hud.impl.XpCalculator;
import net.glacier.module.impl.AutoRespawn;
import net.glacier.module.impl.MenuFpsLimit;
import net.glacier.module.impl.NoHurtCam;
import net.glacier.module.impl.ParticleLimiter;
import net.glacier.module.impl.ToggleSprint;
import net.glacier.module.impl.WeatherChanger;
import net.glacier.module.impl.ZoomModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleManager {
	private static final List<Module> MODULES = new ArrayList<>();
	/** The HUD subset of MODULES, kept in registration order. */
	private static final List<HudModule> HUD = new ArrayList<>();

	private ModuleManager() {
	}

	/** Registers every module. Must run once, before Config.load(). */
	public static void init() {
		MODULES.clear();
		HUD.clear();
		register(new FpsCounter());
		register(new PingCounter());
		register(new CoordinatesHud());
		register(new DirectionHud());
		register(new Speedometer());
		register(new ArmorHud());
		register(new Keystrokes());
		register(new Fullbright());
		register(new PerformanceMods());
		register(new ElementsModule());
		register(new EditorsModule());
		register(new CosmeticsModule());
		register(new GlacierPlayersModule());
		register(new DeathCoordinates());
		register(new DayCounter());
		register(new ServerDisplay());
		register(new ClockHud());
		register(new MemoryUsage());
		register(new LowDurability());
		register(new WalkDistance());
		register(new MainhandHud());
		register(new TimerHud());

		register(new ZoomModule());
		register(new NoHurtCam());
		register(new ParticleLimiter());
		register(new MenuFpsLimit());

		register(new StatusHud());
		register(new DaysPlayed());
		register(new DayEvent());
		register(new XpCalculator());
		register(new ElytraPitch());
		register(new SlotCounter());
		register(new ItemsCounter());
		register(new FoodStats());
		register(new OffhandHud());
		register(new CrosshairCount());
		register(new BowIndicator());
		register(new TargetHud());
		register(new MobIndicator());

		register(new ReachDisplay());
		register(new LightLevelHud());
		register(new FallDistanceHud());
		register(new BlockInfoHud());
		register(new PlayersOnlineHud());
		register(new EntityCountHud());
		register(new WeatherHud());
		register(new HitComboHud());
		register(new VerticalSpeedHud());
		register(new AirSupplyHud());
		register(new AttackCooldownHud());
		register(new HealthHud());
		register(new FpsStatsHud());
		register(new GameModeHud());
		register(new PotionTimersHud());
		register(new AutoRespawn());
		register(new ToggleSprint());
		register(new WeatherChanger());
		register(new CpsHud());
		register(new SessionTimeHud());
		register(new RotationHud());
		register(new ChunkHud());
		register(new TntTimerHud());

		register(new net.glacier.module.impl.FreeLook());
		register(new net.glacier.module.impl.FovChanger());
		register(new net.glacier.module.impl.LowFire());
		register(new net.glacier.module.impl.NoFog());
		register(new net.glacier.module.impl.WindowTitle());
		register(new net.glacier.hud.impl.NearestPlayerHud());

		get(ElementsModule.class).setEnabled(true);
		get(EditorsModule.class).setEnabled(true);

		GlacierClient.LOGGER.info("Registered {} modules", MODULES.size());
	}

	private static void register(Module m) {
		MODULES.add(m);
		if (m instanceof HudModule h) HUD.add(h);
	}

	private static boolean[] keyWasDown = new boolean[0];

	/** Toggles modules on the rising edge of their hotkey; ignored while any screen is open. */
	private static void pollHotkeys() {
		int n = MODULES.size();
		if (keyWasDown.length != n) keyWasDown = new boolean[n];
		Minecraft mc = Minecraft.getInstance();
		boolean active = mc.screen == null && mc.getWindow() != null;
		for (int i = 0; i < n; i++) {
			Module m = MODULES.get(i);
			int key = m.keyCode();
			boolean down = active && key > 0 && InputConstants.isKeyDown(mc.getWindow(), key);
			if (down && !keyWasDown[i]) {
				try {
					m.toggle();
				} catch (RuntimeException e) {
					GlacierClient.LOGGER.error("Hotkey toggle failed for {}", m.name(), e);
				}
			}
			keyWasDown[i] = down;
		}
	}

	public static List<Module> all() {
		return Collections.unmodifiableList(MODULES);
	}

	public static List<HudModule> hud() {
		return Collections.unmodifiableList(HUD);
	}

	public static Module byId(String id) {
		for (Module m : MODULES) {
			if (m.id().equals(id)) return m;
		}
		return null;
	}

	/** Returns the registered module of the given class, or null if none is registered. */
	public static <T extends Module> T get(Class<T> c) {
		for (Module m : MODULES) {
			if (c.isInstance(m)) return c.cast(m);
		}
		return null;
	}

	/** Ticks every enabled module, isolating failures so one module cannot break the others. */
	public static void tick() {
		pollHotkeys();
		for (Module m : MODULES) {
			if (!m.isEnabled()) continue;
			try {
				m.onTick();
			} catch (RuntimeException e) {
				GlacierClient.LOGGER.error("Tick error in module {}", m.name(), e);
			}
		}
		HudModule.refreshStack();
	}
}
