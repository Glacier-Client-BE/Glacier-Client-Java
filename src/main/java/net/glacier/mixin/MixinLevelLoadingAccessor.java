package net.glacier.mixin;

import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the level load tracker of a LevelLoadingScreen for ProgressWebScreen. */
@Mixin(LevelLoadingScreen.class)
public interface MixinLevelLoadingAccessor {
	@Accessor("loadTracker")
	LevelLoadTracker glacier$loadTracker();
}
