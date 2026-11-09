package net.glacier.module.registry;

import net.glacier.module.Module;

import java.util.function.Consumer;

/** Registers modules of one feature batch; ModuleManager calls {@link #registerAll}. */
public final class ExtraARegistry {
	private ExtraARegistry() {
	}

	public static void registerAll(Consumer<Module> register) {
		// register.accept(new net.glacier.module.impl.ExampleModule());
		register.accept(new net.glacier.module.impl.GlintColorizer());
		register.accept(new net.glacier.module.impl.MotionBlur());
		register.accept(new net.glacier.module.impl.ColorSaturation());
		register.accept(new net.glacier.module.impl.ItemPhysics());
		register.accept(new net.glacier.module.impl.ItemHighlighter());
		register.accept(new net.glacier.module.impl.ShinyPots());
	}
}
