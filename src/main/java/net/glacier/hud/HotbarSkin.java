package net.glacier.hud;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.glacier.module.ModuleManager;
import net.glacier.module.impl.EditorsModule;
import net.glacier.ui.Draw;
import net.glacier.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Hotbar and EXP bar skins built from the Glacier pack textures. Called from the hotbar and XP bar mixins. */
public final class HotbarSkin {
	private static final RenderPipeline PIPE = RenderPipelines.GUI_TEXTURED;
	private static final Identifier SLOT = tex("hotbar/default_hotbar");
	private static final Identifier SELECTED = tex("hotbar/default_hotbar_selected");
	private static final Identifier CONNECTED = tex("hotbar/connected_hotbar");
	private static final int WHITE = 0xFFFFFFFF;
	private static final java.util.Map<String, Identifier[]> XP_IDS = new java.util.HashMap<>();

	private HotbarSkin() {
	}

	private static Identifier tex(String name) {
		return Identifier.fromNamespaceAndPath("glacier", "textures/" + name + ".png");
	}

	private static EditorsModule ed() {
		return ModuleManager.get(EditorsModule.class);
	}

	/** Rainbow colour that cycles over time. */
	public static int chroma() {
		float hue = (System.currentTimeMillis() % 4000L) / 4000f;
		return 0xFF000000 | java.awt.Color.HSBtoRGB(hue, 0.55f, 1f);
	}

	/** True when the hotbar background should be drawn by Glacier instead of vanilla. */
	public static boolean customHotbar() {
		EditorsModule e = ed();
		return e != null && !e.hotbarStyle.is("Vanilla");
	}

	/** Draws the hotbar background at the vanilla position (x, y = top-left of the 182x22 bar). */
	public static void hotbarBackground(GuiGraphicsExtractor g, int x, int y) {
		EditorsModule e = ed();
		if (e == null) return;
		String style = e.hotbarStyle.get();
		switch (style) {
			case "Connected" -> g.blit(PIPE, CONNECTED, x + 1, y, 0f, 0f, 180, 22, 180, 22);
			case "Glacier" -> {
				Draw.round(g, x, y, 182, 22, 6, 0xE62C2F33);
				Draw.outline(g, x, y, 182, 22, 6, 0xFF393C41, 1);
			}
			default -> {
				int tint = style.equals("Chroma") ? chroma() : WHITE;
				for (int i = 0; i < 9; i++) {
					g.blit(PIPE, SLOT, x + 1 + i * 20, y, 0f, 0f, 20, 22, 20, 22, 20, 22, tint);
				}
			}
		}
	}

	/** Draws the selected-slot highlight. (x, y) is the top-left of the vanilla 24x23 selection sprite. */
	public static void hotbarSelection(GuiGraphicsExtractor g, int x, int y) {
		EditorsModule e = ed();
		if (e == null) return;
		if (e.hotbarStyle.is("Glacier")) {
			Draw.round(g, x + 3, y + 2, 18, 18, 4, Draw.alpha(Theme.ACCENT, 0.35f));
			Draw.outline(g, x + 3, y + 2, 18, 18, 4, Theme.ACCENT, 1);
			return;
		}
		int tint = e.hotbarStyle.is("Chroma") ? chroma() : WHITE;
		g.blit(PIPE, SELECTED, x + 2, y + 1, 0f, 0f, 20, 22, 20, 22, 20, 22, tint);
	}

	/** True when the EXP bar should be drawn by Glacier instead of vanilla. */
	public static boolean customExp() {
		EditorsModule e = ed();
		return e != null && !e.expColor.is("Default");
	}

	/** Draws a 13x5 pack bar texture as a three-slice, so it can be any width. */
	public static void expBar(GuiGraphicsExtractor g, boolean full, int x, int y, int width) {
		EditorsModule e = ed();
		if (e == null || width < 2) return;
		boolean chroma = e.expColor.is("Chroma");
		String name = chroma ? "white" : e.expColor.get();
		Identifier[] pair = XP_IDS.computeIfAbsent(name, n -> new Identifier[] {
				tex("xp/" + n.toLowerCase() + "_empty"), tex("xp/" + n.toLowerCase() + "_full")});
		Identifier id = pair[full ? 1 : 0];
		int tint = full && chroma ? chroma() : WHITE;
		g.blit(PIPE, id, x, y, 0f, 0f, 1, 5, 1, 5, 13, 5, tint);
		g.blit(PIPE, id, x + 1, y, 1f, 0f, width - 2, 5, 11, 5, 13, 5, tint);
		g.blit(PIPE, id, x + width - 1, y, 12f, 0f, 1, 5, 1, 5, 13, 5, tint);
	}
}
