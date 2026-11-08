package net.glacier.ui.screen;

import net.glacier.web.ScreenApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * The Glacier play screen (play.html): a chooser for worlds, friends and servers, in the pack's layout. Worlds open
 * the vanilla world browser, servers come from the saved list and join through vanilla ConnectScreen. Opened with
 * {@link #open(Screen)}; StartApi's Play chooser can call it to replace the vanilla world list.
 */
public final class PlayWebScreen extends Screen {
	private final Screen parent;
	private final WebPage page;

	public PlayWebScreen(Screen parent) {
		super(Component.literal("Play"));
		this.parent = parent;
		ScreenApi api = new ScreenApi(this, this::back);
		this.page = new WebPage(this, api, "play.html", "play.css", "play.js");
	}

	/** Shows the play screen on top of parent; closing it returns to parent. */
	public static void open(Screen parent) {
		Minecraft.getInstance().setScreen(new PlayWebScreen(parent));
	}

	private void back() {
		Minecraft.getInstance().setScreen(parent);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		WebPage.drawPanorama(g, width, height);
		page.render(g, mouseX, mouseY, width, height);
	}

	@Override
	public void tick() {
		page.tick();
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

	/** The page sees the key first. Esc goes back to the parent screen unless the page consumed it. */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (page.key(event.key())) return true;
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
			back();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (event.isAllowedChatCharacter()) page.typed(event.codepointAsString());
		return true;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
