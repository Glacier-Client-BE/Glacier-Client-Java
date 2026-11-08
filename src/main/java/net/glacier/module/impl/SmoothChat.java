package net.glacier.module.impl;

import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.setting.SliderSetting;

/**
 * Smooth chat: when a new line arrives the chat glides up into place instead of jumping. The mixin
 * (Nr2SmoothChat) records the arrival time and shifts the chat drawing down by one line that eases to zero.
 */
public class SmoothChat extends Module {
	private static SmoothChat instance;
	private static long lastMessage;

	private final SliderSetting duration = add(new SliderSetting("Duration (ms)", 150, 50, 500, 10));

	public SmoothChat() {
		super("Smooth Chat", "New chat lines slide up smoothly instead of popping in.", Category.RENDER);
		instance = this;
	}

	/** Called by the mixin whenever a chat message is added. */
	public static void onMessage() {
		lastMessage = System.currentTimeMillis();
	}

	/** Vertical offset in GUI pixels (0 when idle or disabled). */
	public static float offset() {
		SmoothChat s = instance;
		if (s == null || !s.isEnabled()) return 0f;
		float t = (System.currentTimeMillis() - lastMessage) / (float) s.duration.get().doubleValue();
		if (t >= 1f || t < 0f) return 0f;
		float inv = 1f - t;
		return 9f * inv * inv * inv;
	}
}
