package net.glacier.hud.impl;

import net.glacier.setting.ColorSetting;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Theme;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * "Light: 12" on the block you stand in. Below 8, hostile mobs can spawn there. With a Scan Range above zero it
 * also counts the unsafe floor spots nearby (air above a solid block with no block light and little sky light),
 * rescanned twice a second so rendering never allocates.
 */
public class LightLevelHud extends CachedRowsHud {
	private static final int SCAN_INTERVAL = 10;
	private static final int SCAN_HEIGHT = 2;
	private static final int SKY_SAFE = 8;

	private final ToggleSetting label = add(new ToggleSetting("Show Label", true));
	private final SliderSetting scanRange = add(new SliderSetting("Scan Range", 0, 0, 8, 1));
	private final ColorSetting safeColor = add(new ColorSetting("Safe Color", Theme.HUD_GOOD));
	private final ColorSetting warnColor = add(new ColorSetting("Warning Color", Theme.HUD_WARN));
	private final ColorSetting dangerColor = add(new ColorSetting("Danger Color", Theme.HUD_BAD));

	private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
	private int unsafe;
	private int lastKey = Integer.MIN_VALUE;

	public LightLevelHud() {
		super("Light Level", "Shows the light level of the block you are standing in.", "Light: --");
		stackTopLeft(18);
	}

	@Override
	protected boolean pill() {
		return true;
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.level == null) {
			setRows(single(part("Light: --", TEXT)));
			lastKey = Integer.MIN_VALUE;
			return;
		}
		int range = scanRange.i();
		if (range > 0 && mc.player.tickCount % SCAN_INTERVAL == 0) unsafe = scan(range);
		int light = mc.level.getMaxLocalRawBrightness(mc.player.blockPosition());
		int key = light * 4096 + (range > 0 ? 1 + unsafe : 0) * 2 + (label.on() ? 1 : 0);
		if (key == lastKey) return;
		lastKey = key;
		int color = light >= 8 ? safeColor.argb() : light >= 1 ? warnColor.argb() : dangerColor.argb();
		setReadout(label.on(), "Light", String.valueOf(light), color, "");
		if (range > 0) {
			Part[] row = rows().get(0);
			Part[] withScan = new Part[row.length + 1];
			System.arraycopy(row, 0, withScan, 0, row.length);
			withScan[row.length] = part("  Unsafe " + unsafe, unsafe > 0 ? dangerColor.argb() : safeColor.argb());
			setRows(single(withScan));
		}
	}

	/** Counts floor spots within the range (and two blocks up and down) where a monster could spawn. */
	private int scan(int range) {
		BlockPos center = mc.player.blockPosition();
		int count = 0;
		for (int dx = -range; dx <= range; dx++) {
			for (int dz = -range; dz <= range; dz++) {
				for (int dy = -SCAN_HEIGHT; dy <= SCAN_HEIGHT; dy++) {
					cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
					if (!mc.level.getBlockState(cursor).isAir()) continue;
					if (mc.level.getBrightness(LightLayer.BLOCK, cursor) > 0) continue;
					if (mc.level.getBrightness(LightLayer.SKY, cursor) >= SKY_SAFE) continue;
					cursor.setY(cursor.getY() - 1);
					BlockState floor = mc.level.getBlockState(cursor);
					if (floor.isFaceSturdy(mc.level, cursor, Direction.UP)) count++;
				}
			}
		}
		return count;
	}
}
