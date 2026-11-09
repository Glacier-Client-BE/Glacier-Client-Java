package net.glacier.mixin.extraa;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Write access to the client side world clock (26.x moved the day time out of the level data into per-dimension
 * clocks). Used by Time Changer to pin the time of day the player sees.
 */
@Mixin(targets = "net.minecraft.client.ClientClockManager$ClientClockInstance")
public interface MixinClockInstance {
	@Accessor("totalTicks")
	void glacier$setTotalTicks(long ticks);
}
