package net.glacier.module.registry;

import net.glacier.module.Module;

import java.util.function.Consumer;

/** Registers extra non-HUD modules. Owned by the utility feature work; ModuleManager calls {@link #registerAll}. */
public final class UtilityRegistry {
	private UtilityRegistry() {
	}

	public static void registerAll(Consumer<Module> register) {
		register.accept(new net.glacier.module.impl.TimeChanger());
		register.accept(new net.glacier.module.impl.HitboxModule());
		register.accept(new net.glacier.module.impl.BlockOutline());
		register.accept(new net.glacier.module.impl.HideArmor());
		register.accept(new net.glacier.module.impl.NoAdvancement());
		register.accept(new net.glacier.module.impl.AutoReconnect());
		register.accept(new net.glacier.module.impl.ConfirmDisconnect());
		register.accept(new net.glacier.module.impl.ChatFilter());
		register.accept(new net.glacier.module.impl.StreamerMode());
		register.accept(new net.glacier.module.impl.SmoothChat());
		register.accept(new net.glacier.module.impl.ClearBackground());
		register.accept(new net.glacier.module.impl.HitColor());
		register.accept(new net.glacier.module.impl.ProjectileTrails());
		register.accept(new net.glacier.module.impl.SpinningProjectiles());
		register.accept(new net.glacier.module.impl.BorderlessFullscreen());
	}
}
