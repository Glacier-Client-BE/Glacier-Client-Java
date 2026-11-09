package net.glacier.module.registry;

import net.glacier.module.Module;

import java.util.function.Consumer;

/** Registers modules of one feature batch; ModuleManager calls {@link #registerAll}. */
public final class ExtraBRegistry {
	private ExtraBRegistry() {
	}

	public static void registerAll(Consumer<Module> register) {
		register.accept(new net.glacier.module.impl.OldAnimations());
		register.accept(new net.glacier.module.impl.BetterArmorBar());
		register.accept(new net.glacier.hud.impl.JumpResetHud());
	}
}
