package net.glacier.mixin;

import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the progress of a ProgressScreen (saving and stages) for ProgressWebScreen. */
@Mixin(ProgressScreen.class)
public interface MixinProgressScreenAccessor {
	@Accessor("progress")
	int glacier$progress();

	@Accessor("stage")
	Component glacier$stage();

	@Accessor("header")
	Component glacier$header();
}
