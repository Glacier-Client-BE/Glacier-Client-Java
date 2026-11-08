package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;

/**
 * Shows entity hitboxes without opening the debug overlay, by pinning the vanilla F3+B debug entry
 * to ALWAYS_ON. The previous status of that entry is restored on disable. No mixin.
 */
public class HitboxModule extends Module {
	private DebugScreenEntryStatus previous;

	public HitboxModule() {
		super("Hitbox", "Displays entity hitboxes (same as F3+B) without the debug screen.", Category.RENDER);
	}

	@Override
	public void onTick() {
		if (mc.debugEntries == null) return;
		DebugScreenEntryStatus cur = mc.debugEntries.getStatus(DebugScreenEntries.ENTITY_HITBOXES);
		if (cur == DebugScreenEntryStatus.ALWAYS_ON) return;
		if (previous == null) previous = cur;
		mc.debugEntries.setStatus(DebugScreenEntries.ENTITY_HITBOXES, DebugScreenEntryStatus.ALWAYS_ON);
	}

	@Override
	protected void onDisable() {
		if (previous != null && mc.debugEntries != null) {
			mc.debugEntries.setStatus(DebugScreenEntries.ENTITY_HITBOXES, previous);
		}
		previous = null;
	}
}
