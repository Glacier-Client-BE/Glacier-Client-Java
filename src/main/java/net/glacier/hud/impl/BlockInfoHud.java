package net.glacier.hud.impl;

import net.glacier.setting.ToggleSetting;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** "Block: Oak Planks" for the block under the crosshair. Looked up once per tick. */
public class BlockInfoHud extends CachedRowsHud {
	private final ToggleSetting label = add(new ToggleSetting("Show Label", true));

	public BlockInfoHud() {
		super("Block Info", "Shows the name of the block under your crosshair.", "Block: -");
		stackTopLeft(20);
	}

	@Override
	protected boolean pill() {
		return true;
	}

	@Override
	public void onTick() {
		String name = "-";
		HitResult hit = mc.hitResult;
		if (mc.level != null && hit instanceof BlockHitResult bhr) {
			BlockState state = mc.level.getBlockState(bhr.getBlockPos());
			if (!state.isAir()) name = state.getBlock().getName().getString();
		}
		if (label.on()) setRows(single(part("Block: ", TEXT), part(name, TEXT)));
		else setRows(single(part(name, TEXT)));
	}
}
