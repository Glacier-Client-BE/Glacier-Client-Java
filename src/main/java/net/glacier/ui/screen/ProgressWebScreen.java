package net.glacier.ui.screen;

import net.glacier.mixin.MixinLevelLoadingAccessor;
import net.glacier.mixin.MixinProgressScreenAccessor;
import net.glacier.web.ScreenApi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.network.chat.Component;
import net.glacier.compat.GLFW;

/**
 * The loading and saving screen (progress.html). It wraps the vanilla screen it replaces ({@code LevelLoadingScreen}
 * or {@code ProgressScreen}) and reads that screen's progress through the accessor mixins, so the vanilla code keeps
 * updating its own instance. Percent -1 means unknown and the bar sweeps. Closing follows the vanilla screen: Exit
 * and Esc only close when the vanilla screen allows it. Replaced by {@link MixinScreenSwap}.
 */
public final class ProgressWebScreen extends Screen {
	private final Screen source;
	private final WebPage page;
	private int lastPct = -2;
	private String lastTitle = "";
	private boolean lastCanClose;

	public ProgressWebScreen(Screen source) {
		super(Component.literal("Loading"));
		this.source = source;
		ScreenApi api = new ScreenApi(this, this::closeSource);
		this.page = new WebPage(this, api, "progress.html", "progress.css", "progress.js");
	}

	/** Closes the vanilla screen the same way Esc does. Saving a world cannot be cancelled, so nothing happens there. */
	private void closeSource() {
		if (source.shouldCloseOnEsc()) source.onClose();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		WebPage.drawPanorama(g, width, height);
		page.extractRenderState(g, mouseX, mouseY, width, height);
	}

	@Override
	public void tick() {
		// the vanilla LevelLoadingScreen closes itself in tick() once the level is ready; this wrapper is what the
		// game sees, so it has to do the same or the loading screen never goes away
		if (source instanceof MixinLevelLoadingAccessor level && this.minecraft != null) {
			LevelLoadTracker tracker = level.glacier$loadTracker();
			if (tracker != null && tracker.isLevelReady()) {
				this.minecraft.gui.setScreen(null);
				return;
			}
		}
		page.tick();
		syncProgress();
	}

	/** Pushes the vanilla progress to the page, only when the value or the title changed (ticks, not frames). */
	private void syncProgress() {
		int pct = -1;
		String title = "Loading";
		try {
			if (source instanceof MixinLevelLoadingAccessor level) {
				LevelLoadTracker tracker = level.glacier$loadTracker();
				if (tracker != null && tracker.hasProgress()) pct = Math.round(tracker.serverProgress() * 100f);
				title = "Loading world";
			} else if (source instanceof MixinProgressScreenAccessor progress) {
				pct = progress.glacier$progress();
				Component stage = progress.glacier$stage();
				Component header = progress.glacier$header();
				if (stage != null) title = stage.getString();
				else if (header != null) title = header.getString();
				else title = "Saving world";
			}
		} catch (RuntimeException e) {
			return;
		}
		boolean canClose = source.shouldCloseOnEsc();
		if (pct == lastPct && title.equals(lastTitle) && canClose == lastCanClose) return;
		lastPct = pct;
		lastTitle = title;
		lastCanClose = canClose;
		page.run("glacierSetProgress(" + pct + ", " + jsString(title) + ", " + canClose + ")");
	}

	/** A JS string literal for text from the game. Control characters become spaces. */
	private static String jsString(String s) {
		StringBuilder b = new StringBuilder(s.length() + 2).append('\'');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\\' || c == '\'') b.append('\\').append(c);
			else if (c < 0x20) b.append(' ');
			else b.append(c);
		}
		return b.append('\'').toString();
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

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (page.key(event.key())) return true;
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) closeSource();
		return true;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		return true;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return source.shouldCloseOnEsc();
	}

	@Override
	public boolean isPauseScreen() {
		return source.isPauseScreen();
	}
}
