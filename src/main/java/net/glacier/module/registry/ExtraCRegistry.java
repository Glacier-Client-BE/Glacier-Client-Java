package net.glacier.module.registry;

import net.glacier.module.Module;

import java.util.function.Consumer;

/** Registers modules of one feature batch; ModuleManager calls {@link #registerAll}. */
public final class ExtraCRegistry {
	private ExtraCRegistry() {
	}

	public static void registerAll(Consumer<Module> register) {
		register.accept(new net.glacier.module.impl.ChatPlus());
		register.accept(new net.glacier.module.impl.PlayerTabList());
		register.accept(new net.glacier.module.impl.ScoreboardPlus());
		register.accept(new net.glacier.module.impl.TitlesPlus());
		register.accept(new net.glacier.module.impl.ActionBarPlus());
		register.accept(new net.glacier.module.impl.LoadingTips());
		register.accept(new net.glacier.hud.impl.MouseTracker());
	}
}
