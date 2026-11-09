package net.glacier.module.impl;

/**
 * Implemented onto the vanilla ChatComponent by the extrac chat mixin. It lives outside the mixin package on purpose,
 * because classes inside a mixin package cannot be referenced by normal code at runtime.
 */
public interface ChatTextLookup {
	/** Plain text of the chat message under the given GUI-scaled mouse position, or null when there is none. */
	String glacier$textAt(double mouseX, double mouseY);
}
