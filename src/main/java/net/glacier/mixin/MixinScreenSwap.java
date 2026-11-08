package net.glacier.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.glacier.ui.screen.NativePauseScreen;
import net.glacier.ui.screen.ProgressWebScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Swaps the vanilla pause and loading screens for the Glacier pages. Matches exact classes only, so it leaves the
 * title swap ({@link MixinTitleScreenSwap}, TitleScreen only) and its own replacements alone. Off with
 * "customScreens": false in performance.json, read once when the game starts.
 */
@Mixin(Minecraft.class)
public abstract class MixinScreenSwap {
	@Unique
	private static final boolean GLACIER_CUSTOM_SCREENS = glacier$readSwitch();

	@ModifyVariable(method = "setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"), argsOnly = true)
	private Screen glacier$swapScreens(Screen screen) {
		if (!GLACIER_CUSTOM_SCREENS || screen == null) return screen;
		Class<?> type = screen.getClass();
		if (type == PauseScreen.class) return net.glacier.ui.screen.ScreenFactory.pause(((PauseScreen) screen).showsPauseMenu());
		// the wrapper reads progress from the vanilla instance, which keeps receiving the updates
		if (type == LevelLoadingScreen.class || type == ProgressScreen.class) return net.glacier.ui.screen.ScreenFactory.progress(screen);
		return screen;
	}

	@Unique
	private static boolean glacier$readSwitch() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve("glacier").resolve("performance.json");
		try {
			if (!Files.isRegularFile(file)) return true;
			JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
			if (!root.isJsonObject()) return true;
			JsonElement v = ((JsonObject) root).get("customScreens");
			return v == null || !v.isJsonPrimitive() || v.getAsBoolean();
		} catch (Exception e) {
			return true;
		}
	}
}
