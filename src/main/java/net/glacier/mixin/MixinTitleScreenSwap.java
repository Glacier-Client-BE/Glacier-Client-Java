package net.glacier.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.glacier.ui.screen.NativeStartScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Swaps the vanilla title screen for {@link NativeStartScreen}. Off with "customTitle": false in performance.json. */
@Mixin(Minecraft.class)
public abstract class MixinTitleScreenSwap {
	@Unique
	private static final boolean GLACIER_CUSTOM_TITLE = glacier$readSwitch();

	@ModifyVariable(method = "setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"), argsOnly = true)
	private Screen glacier$swapTitle(Screen screen) {
		if (!GLACIER_CUSTOM_TITLE) return screen;
		if (screen != null && screen.getClass() == TitleScreen.class) return net.glacier.ui.screen.ScreenFactory.start();
		// vanilla turns a null screen into a TitleScreen when no world is loaded
		if (screen == null && ((Minecraft) (Object) this).level == null) return net.glacier.ui.screen.ScreenFactory.start();
		return screen;
	}

	@Unique
	private static boolean glacier$readSwitch() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve("glacier").resolve("performance.json");
		try {
			if (!Files.isRegularFile(file)) return true;
			JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
			if (!root.isJsonObject()) return true;
			JsonElement v = ((JsonObject) root).get("customTitle");
			return v == null || !v.isJsonPrimitive() || v.getAsBoolean();
		} catch (Exception e) {
			return true;
		}
	}
}
