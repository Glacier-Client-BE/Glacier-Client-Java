package net.glacier.ui.screen;

import net.glacier.web.ScreenApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.glacier.compat.GLFW;

/**
 * The pause menu (pause.html) that replaces the vanilla {@link PauseScreen}. It extends PauseScreen so existing
 * {@code instanceof PauseScreen} checks (MenuFps) still match. The vanilla widgets are not built; every action goes
 * to the same vanilla screen or call through {@link ScreenApi}. Replaced by {@link MixinScreenSwap}.
 */
public final class PauseWebScreen extends PauseScreen {
	/** 50% black dim over the world, as in the pack's pause screen. */
	private static final int DIM = 0x80000000;

	private final WebPage page;

	public PauseWebScreen(boolean showPauseMenu) {
		super(showPauseMenu);
		ScreenApi api = new ScreenApi(this, () -> Minecraft.getInstance().gui.setScreen(null));
		this.page = new WebPage(this, api, "pause.html", "pause.css", "pause.js");
	}

	/** The vanilla pause widgets are replaced by the page, so nothing is built here. */
	@Override
	protected void init() {
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public void tick() {
		page.tick();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, width, height, DIM);
		page.extractRenderState(g, mouseX, mouseY, width, height);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		return page.click(event);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		return page.release(event);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		return page.drag(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		return page.scroll(mouseX, mouseY, scrollY);
	}

	/** The page sees the key first. Esc resumes the game unless the page consumed it (closing its confirm box). */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (page.key(event.key())) return true;
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (event.isAllowedChatCharacter()) page.typed(event.codepointAsString());
		return true;
	}
}
