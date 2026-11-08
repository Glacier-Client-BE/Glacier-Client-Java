package net.glacier.module.registry;

import net.glacier.module.Module;

import java.util.function.Consumer;

/** Registers extra HUD modules. Owned by the HUD feature work; ModuleManager calls {@link #registerAll}. */
public final class HudRegistry {
	private HudRegistry() {
	}

	public static void registerAll(Consumer<Module> register) {
		register.accept(new net.glacier.hud.impl.TpsHud());
		register.accept(new net.glacier.hud.impl.ShieldStatusHud());
		register.accept(new net.glacier.hud.impl.BiomeHud());
		register.accept(new net.glacier.hud.impl.PortalCoordsHud());
		register.accept(new net.glacier.hud.impl.PotionIconsHud());
		register.accept(new net.glacier.hud.impl.InventoryHud());
		register.accept(new net.glacier.hud.impl.ItemCounterHud());
		register.accept(new net.glacier.hud.impl.CrosshairHud());
		register.accept(new net.glacier.hud.impl.ResourcePackHud());
		register.accept(new net.glacier.hud.impl.CooldownHud());
		register.accept(new net.glacier.hud.impl.KillCounter());
		register.accept(new net.glacier.hud.impl.HudPopup());
		register.accept(new net.glacier.hud.impl.SafeZoneViewer());
		register.accept(new net.glacier.hud.impl.OreVisualizer());
		register.accept(new net.glacier.hud.impl.ChunkMap());
		register.accept(new net.glacier.hud.impl.DebugHud());
	}
}
