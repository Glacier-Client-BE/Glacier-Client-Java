package net.glacier.module.impl;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.glacier.GlacierClient;
import net.glacier.module.Category;
import net.glacier.module.Module;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * Hold the key (default Left Alt) to look around with the camera while your body keeps facing the same way.
 * Mouse movement is diverted in MixinFreeLookTurn; the camera angles are applied in MixinFreeLookCamera.
 */
public class FreeLook extends Module {
	private static FreeLook instance;
	private final KeyMapping key = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.glacier.freelook", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT,
			new KeyMapping.Category(Identifier.fromNamespaceAndPath(GlacierClient.MOD_ID, "main"))));
	private boolean looking;
	private float yaw, pitch;

	public FreeLook() {
		super("Free Look", "Hold Left Alt to look around without turning.", Category.RENDER);
		instance = this;
	}

	@Override
	public void onTick() {
		boolean down = mc.screen == null && mc.player != null && key.isDown();
		if (down && !looking) {
			yaw = mc.player.getYRot();
			pitch = mc.player.getXRot();
		}
		looking = down;
	}

	@Override
	protected void onDisable() {
		looking = false;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled() && instance.looking;
	}

	/** Consumes a mouse turn while free looking. */
	public static void turn(double dYaw, double dPitch) {
		instance.yaw += (float) dYaw * 0.15f;
		instance.pitch = Mth.clamp(instance.pitch + (float) dPitch * 0.15f, -90f, 90f);
	}

	public static float yaw() {
		return instance.yaw;
	}

	public static float pitch() {
		return instance.pitch;
	}
}
