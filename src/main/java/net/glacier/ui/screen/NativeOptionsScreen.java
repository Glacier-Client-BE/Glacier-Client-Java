package net.glacier.ui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.glacier.ui.screen.pages.MenuKit;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.glacier.compat.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Glacier's own Options screen. Same visual language as the native mod menu (css px panel at the snapped scale,
 * MenuKit shapes, Glacier UI font): a header with title, search and Done, a section sidebar and a scrolling list of
 * rows. Every row reads and writes the real vanilla {@link Options} / {@link OptionInstance}, which is saved when the
 * screen is closed, so the values are the same ones the vanilla screens show.
 */
public final class NativeOptionsScreen extends Screen {
	private static final float MENU_W = 960f, MENU_H = 600f, MENU_FILL = 0.9f, MIN_W = 320f, MIN_H = 240f;
	private static final float MAX_SCALE = 4f, STEP = 0.25f;
	private static final int CARD = 0xFF2A2B2F, CARD_HOVER = 0xFF303137, CARD_BORDER = 0x0FFFFFFF;
	private static final int MUTED = 0xFF99AAB5, ACCENT = 0xFF7289DA, WHITE = 0xFFFFFFFF, TEXT = 0xFFD6DAE0;
	private static final int PILL = 0x0AFFFFFF, PILL_HOVER = 0x14FFFFFF, PILL_BORDER = 0x1AFFFFFF, RED = 0xFFF04747;
	private static final float ROW_H = 44f, GAP = 8f, HEAD_H = 30f, SIDE_W = 190f;
	private static final int ICON_SEARCH = 0xf002, ICON_CHECK = 0xf00c;

	// ------------------------------------------------------------------ rows

	private interface Row {
	}

	private record Head(String title) implements Row {
	}

	private record Toggle(String label, BooleanSupplier get, Consumer<Boolean> set) implements Row {
	}

	private record Cycle(String label, Supplier<String> text, IntConsumer step) implements Row {
	}

	private record Act(String label, String button, Runnable run) implements Row {
	}

	private record Key(KeyMapping mapping) implements Row {
	}

	private record Lang(String code, String name) implements Row {
	}

	/** A slider over a 0..1 fraction. Deferred sliders only write the option when the mouse is released. */
	private static final class Slide implements Row {
		final String label;
		final DoubleSupplier get;
		final DoubleConsumer set;
		final DoubleFunction<String> text;
		final boolean deferred;
		final Runnable after;
		double pending = Double.NaN;

		Slide(String label, DoubleSupplier get, DoubleConsumer set, DoubleFunction<String> text, boolean deferred,
			  Runnable after) {
			this.label = label;
			this.get = get;
			this.set = set;
			this.text = text;
			this.deferred = deferred;
			this.after = after;
		}

		double frac() {
			return Double.isNaN(pending) ? get.getAsDouble() : pending;
		}

		void drag(double f) {
			f = Math.max(0, Math.min(1, f));
			if (deferred) {
				pending = f;
			} else {
				set.accept(f);
				if (after != null) after.run();
			}
		}

		void release() {
			if (!Double.isNaN(pending)) {
				set.accept(pending);
				pending = Double.NaN;
				if (after != null) after.run();
			}
		}
	}

	private record Section(String name, int icon, List<Row> rows) {
	}

	// ----------------------------------------------------------------- state

	private final Screen parent;
	private final Options opts;
	private final List<Section> sections = new ArrayList<>();
	private int section;
	private float scroll, content, viewH = 400;
	private String query = "";
	private boolean searchFocus;
	private boolean textOn;
	private KeyMapping listening;

	private final List<MenuKit.Hit> hits = new ArrayList<>();
	private MenuKit.Hit active;
	private float scale = 1f;
	private int ox, oy, lw = 960, lh = 600;
	private float px, py, pw = 960, ph = 600;

	public NativeOptionsScreen(Screen parent) {
		super(Component.literal("Options"));
		this.parent = parent;
		this.opts = Minecraft.getInstance().options;
		build();
	}

	// ------------------------------------------------------------- row setup

	private static String pct(double v) {
		return Math.round(v * 100) + "%";
	}

	private static String pretty(Enum<?> e) {
		String n = e.name().toLowerCase(Locale.ROOT).replace('_', ' ');
		StringBuilder b = new StringBuilder();
		boolean up = true;
		for (char c : n.toCharArray()) {
			b.append(up ? Character.toUpperCase(c) : c);
			up = c == ' ';
		}
		return b.toString();
	}

	private static Row toggle(String label, OptionInstance<Boolean> o) {
		return new Toggle(label, o::get, o::set);
	}

	/** Integer slider over min..max in steps; {@code deferred} options (render distance, GUI scale) apply on release. */
	private static Row islider(String label, OptionInstance<Integer> o, int min, int max, int step,
							   IntFunction<String> fmt, boolean deferred, Runnable after) {
		int n = Math.max(1, (max - min) / step);
		DoubleFunction<Integer> at = f -> min + (int) Math.round(Math.max(0, Math.min(1, f)) * n) * step;
		return new Slide(label, () -> (Math.max(min, Math.min(max, o.get())) - min) / (double) (n * step),
				f -> o.set(at.apply(f)), f -> fmt.apply(at.apply(f)), deferred, after);
	}

	private static Row dslider(String label, OptionInstance<Double> o, double min, double max, DoubleFunction<String> fmt) {
		return new Slide(label, () -> (o.get() - min) / (max - min), f -> o.set(min + f * (max - min)),
				f -> fmt.apply(min + f * (max - min)), false, null);
	}

	private static Row unit(String label, OptionInstance<Double> o) {
		return dslider(label, o, 0, 1, NativeOptionsScreen::pct);
	}

	private static <T> Row cycle(String label, OptionInstance<T> o, List<T> vals, Function<T, String> name) {
		return new Cycle(label, () -> name.apply(o.get()), d -> {
			int n = vals.size(), i = vals.indexOf(o.get());
			i = i < 0 ? (d > 0 ? 0 : n - 1) : Math.floorMod(i + d, n);
			o.set(vals.get(i));
		});
	}

	private static <E extends Enum<E>> Row cycleEnum(String label, OptionInstance<E> o, E[] vals) {
		return cycle(label, o, Arrays.asList(vals), NativeOptionsScreen::pretty);
	}

	private void build() {
		Minecraft mc = Minecraft.getInstance();
		Options o = opts;

		List<Row> video = new ArrayList<>();
		video.add(new Head("Display"));
		video.add(cycle("Graphics", o.graphicsPreset(), List.of(net.minecraft.client.GraphicsPreset.FAST,
				net.minecraft.client.GraphicsPreset.FANCY, net.minecraft.client.GraphicsPreset.FABULOUS,
				net.minecraft.client.GraphicsPreset.CUSTOM), NativeOptionsScreen::pretty));
		video.add(islider("Render Distance", o.renderDistance(), 2, 32, 1, v -> v + " chunks", true, null));
		video.add(islider("Simulation Distance", o.simulationDistance(), 5, 32, 1, v -> v + " chunks", true, null));
		video.add(islider("Max Framerate", o.framerateLimit(), 10, 260, 10, v -> v >= 260 ? "Unlimited" : v + " FPS", false, null));
		video.add(toggle("VSync", o.enableVsync()));
		video.add(toggle("Fullscreen", o.fullscreen()));
		int maxScale = Math.max(1, mc.getWindow().calculateScale(0, mc.isEnforceUnicode()));
		video.add(islider("GUI Scale", o.guiScale(), 0, maxScale, 1, v -> v == 0 ? "Auto" : String.valueOf(v), true,
				mc::resizeGui));
		video.add(dslider("Brightness", o.gamma(), 0, 1, f -> f <= 0 ? "Moody" : f >= 1 ? "Bright" : pct(f)));
		video.add(islider("Field of View", o.fov(), 30, 110, 1, v -> v == 70 ? "Normal" : v == 110 ? "Quake Pro" : String.valueOf(v),
				false, null));
		video.add(new Head("Quality"));
		video.add(cycleEnum("Clouds", o.cloudStatus(), net.minecraft.client.CloudStatus.values()));
		video.add(cycleEnum("Particles", o.particles(), net.minecraft.server.level.ParticleStatus.values()));
		video.add(toggle("Smooth Lighting", o.ambientOcclusion()));
		video.add(toggle("Entity Shadows", o.entityShadows()));
		video.add(toggle("Vignette", o.vignette()));
		video.add(toggle("View Bobbing", o.bobView()));
		video.add(islider("Biome Blend", o.biomeBlendRadius(), 0, 7, 1, v -> v == 0 ? "Off" : (2 * v + 1) + "x" + (2 * v + 1), true, null));
		video.add(islider("Mipmap Levels", o.mipmapLevels(), 0, 4, 1, v -> v == 0 ? "Off" : String.valueOf(v), true, null));
		video.add(dslider("Entity Distance", o.entityDistanceScaling(), 0.5, 5, v -> pct(v)));
		video.add(cycleEnum("Attack Indicator", o.attackIndicator(), net.minecraft.client.AttackIndicatorStatus.values()));
		sections.add(new Section("Video", 0xf108, video));

		List<Row> audio = new ArrayList<>();
		audio.add(new Head("Volume"));
		Map<String, String> names = Map.of("master", "Master Volume", "music", "Music", "record", "Jukebox and Note Blocks",
				"weather", "Weather", "block", "Blocks", "hostile", "Hostile Creatures", "neutral", "Friendly Creatures",
				"player", "Players", "ambient", "Ambient and Environment", "voice", "Voice and Speech");
		for (SoundSource src : SoundSource.values()) {
			String n = names.getOrDefault(src.getName(), pretty(src));
			audio.add(dslider(n, o.getSoundSourceOptionInstance(src), 0, 1, f -> f <= 0 ? "Off" : pct(f)));
		}
		audio.add(new Head("Sound"));
		audio.add(toggle("Show Subtitles", o.showSubtitles()));
		audio.add(toggle("Directional Audio", o.directionalAudio()));
		sections.add(new Section("Audio", 0xf028, audio));

		List<Row> controls = new ArrayList<>();
		controls.add(new Head("Mouse and Movement"));
		controls.add(dslider("Mouse Sensitivity", o.sensitivity(), 0, 1,
				f -> f <= 0 ? "*yawn*" : f >= 1 ? "HYPERSPEED!!!" : String.valueOf((int) (f * 200)) + "%"));
		controls.add(toggle("Invert Mouse X", o.invertMouseX()));
		controls.add(toggle("Invert Mouse Y", o.invertMouseY()));
		controls.add(toggle("Discrete Scrolling", o.discreteMouseScroll()));
		controls.add(toggle("Auto-Jump", o.autoJump()));
		controls.add(toggle("Toggle Crouch", o.toggleCrouch()));
		controls.add(toggle("Toggle Sprint", o.toggleSprint()));
		KeyMapping[] maps = o.keyMappings.clone();
		Arrays.sort(maps);
		KeyMapping.Category last = null;
		for (KeyMapping km : maps) {
			if (!km.getCategory().equals(last)) {
				last = km.getCategory();
				controls.add(new Head("Key Binds: " + last.label().getString()));
			}
			controls.add(new Key(km));
		}
		sections.add(new Section("Controls", 0xf11b, controls));

		List<Row> chat = new ArrayList<>();
		chat.add(new Head("Chat"));
		chat.add(cycleEnum("Chat Visibility", o.chatVisibility(), net.minecraft.world.entity.player.ChatVisiblity.values()));
		chat.add(toggle("Colors", o.chatColors()));
		chat.add(toggle("Web Links", o.chatLinks()));
		chat.add(toggle("Prompt on Links", o.chatLinksPrompt()));
		chat.add(toggle("Command Suggestions", o.autoSuggestions()));
		chat.add(toggle("Only Show Secure Chat", o.onlyShowSecureChat()));
		chat.add(toggle("Hide Matched Names", o.hideMatchedNames()));
		chat.add(toggle("Save Chat Drafts", o.saveChatDrafts()));
		chat.add(toggle("Background for Chat Only", o.backgroundForChatOnly()));
		chat.add(new Head("Appearance"));
		chat.add(unit("Opacity", o.chatOpacity()));
		chat.add(unit("Text Background Opacity", o.textBackgroundOpacity()));
		chat.add(unit("Scale", o.chatScale()));
		chat.add(unit("Line Spacing", o.chatLineSpacing()));
		chat.add(unit("Width", o.chatWidth()));
		chat.add(unit("Focused Height", o.chatHeightFocused()));
		chat.add(unit("Unfocused Height", o.chatHeightUnfocused()));
		chat.add(dslider("Message Delay", o.chatDelay(), 0, 6, v -> v <= 0 ? "None" : String.format(Locale.ROOT, "%.1f s", v)));
		sections.add(new Section("Chat", 0xf075, chat));

		List<Row> access = new ArrayList<>();
		access.add(new Head("Vision and Hearing"));
		access.add(cycleEnum("Narrator", o.narrator(), net.minecraft.client.NarratorStatus.values()));
		access.add(toggle("Show Subtitles", o.showSubtitles()));
		access.add(toggle("High Contrast", o.highContrast()));
		access.add(toggle("High Contrast Block Outline", o.highContrastBlockOutline()));
		access.add(toggle("Narrator Hotkey", o.narratorHotkey()));
		access.add(islider("Menu Background Blur", o.menuBackgroundBlurriness(), 0, 10, 1, v -> v == 0 ? "Off" : String.valueOf(v), false, null));
		access.add(new Head("Effects"));
		access.add(unit("Distortion Effects", o.screenEffectScale()));
		access.add(unit("FOV Effects", o.fovEffectScale()));
		access.add(unit("Darkness Pulsing", o.darknessEffectScale()));
		access.add(unit("Damage Tilt", o.damageTiltStrength()));
		access.add(unit("Glint Speed", o.glintSpeed()));
		access.add(unit("Glint Strength", o.glintStrength()));
		access.add(toggle("Hide Lightning Flashes", o.hideLightningFlash()));
		access.add(toggle("Hide Splash Texts", o.hideSplashTexts()));
		access.add(toggle("Dark Background Loading", o.darkMojangStudiosBackground()));
		sections.add(new Section("Accessibility", 0xf29a, access));

		List<Row> lang = new ArrayList<>();
		lang.add(new Head("Game Language"));
		LanguageManager lm = mc.getLanguageManager();
		for (Map.Entry<String, LanguageInfo> e : lm.getLanguages().entrySet()) {
			lang.add(new Lang(e.getKey(), e.getValue().toComponent().getString()));
		}
		sections.add(new Section("Language", 0xf1ab, lang));

		List<Row> skin = new ArrayList<>();
		skin.add(new Head("Skin Customization"));
		String[] partNames = {"Cape", "Jacket", "Left Sleeve", "Right Sleeve", "Left Pants Leg", "Right Pants Leg", "Hat"};
		PlayerModelPart[] parts = PlayerModelPart.values();
		for (int i = 0; i < parts.length; i++) {
			PlayerModelPart part = parts[i];
			skin.add(new Toggle(i < partNames.length && parts.length == partNames.length ? partNames[i] : pretty(part),
					() -> o.isModelPartEnabled(part), v -> o.setModelPart(part, v)));
		}
		skin.add(cycleEnum("Main Hand", o.mainHand(), net.minecraft.world.entity.HumanoidArm.values()));
		sections.add(new Section("Skin", 0xf007, skin));

		List<Row> packs = new ArrayList<>();
		packs.add(new Head("Resource Packs"));
		packs.add(new Act("Resource Packs", "Open", this::openPacks));
		sections.add(new Section("Resource Packs", 0xf1b2, packs));
	}

	private void openPacks() {
		Minecraft mc = Minecraft.getInstance();
		mc.gui.setScreen(new PackSelectionScreen(mc.getResourcePackRepository(), repo -> {
			opts.updateResourcePacks(repo);
			mc.gui.setScreen(this);
		}, mc.getResourcePackDirectory(), Component.translatable("resourcePack.title")));
	}

	private void setLanguage(String code) {
		Minecraft mc = Minecraft.getInstance();
		LanguageManager lm = mc.getLanguageManager();
		if (code.equals(lm.getSelected())) return;
		lm.setSelected(code);
		opts.languageCode = code;
		opts.save();
		mc.reloadResourcePacks();
	}

	// --------------------------------------------------------------- queries

	private static String labelOf(Row r) {
		if (r instanceof Toggle t) return t.label();
		if (r instanceof Slide s) return s.label;
		if (r instanceof Cycle c) return c.label();
		if (r instanceof Act a) return a.label();
		if (r instanceof Key k) return I18n.get(k.mapping().getName());
		if (r instanceof Lang l) return l.name();
		return "";
	}

	private List<Row> visibleRows() {
		if (query.isBlank()) return sections.get(section).rows();
		String q = query.trim().toLowerCase(Locale.ROOT);
		List<Row> out = new ArrayList<>();
		for (Section s : sections) {
			boolean head = false;
			for (Row r : s.rows()) {
				if (r instanceof Head || !labelOf(r).toLowerCase(Locale.ROOT).contains(q)) continue;
				if (!head) {
					out.add(new Head(s.name()));
					head = true;
				}
				out.add(r);
			}
		}
		return out;
	}

	// ------------------------------------------------------------------ fit

	private void fit() {
		float desired = Math.min(width * MENU_FILL / MENU_W, height * MENU_FILL / MENU_H);
		float s = Math.min(Math.min(desired, MAX_SCALE), Math.min(width / MIN_W, height / MIN_H));
		scale = s < 0.5f ? Math.max(0.1f, s) : (float) Math.floor(s / STEP) * STEP;
		lw = (int) Math.floor(width / scale);
		lh = (int) Math.floor(height / scale);
		ox = Math.round((width - lw * scale) / 2f);
		oy = Math.round((height - lh * scale) / 2f);
		pw = Math.min(MENU_W, lw);
		ph = Math.min(MENU_H, lh);
		px = (float) Math.floor((lw - pw) / 2f);
		py = (float) Math.floor((lh - ph) / 2f);
	}

	private double lx(double sx) {
		return (sx - ox) / scale;
	}

	private double ly(double sy) {
		return (sy - oy) / scale;
	}

	// ----------------------------------------------------------------- render

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mxs, int mys, float partial) {
		// 26.x only delivers typed characters while text input is switched on, as an EditBox does when focused.
		if (searchFocus != textOn) {
			textOn = searchFocus;
			var tim = Minecraft.getInstance().textInputManager();
			if (textOn) tim.startTextInput(this);
			else tim.stopTextInput(this);
		}
		fit();
		hits.clear();
		if (Minecraft.getInstance().level == null) {
			g.fillGradient(0, 0, width, height, 0xFF1B1C20, 0xFF121316);
		} else {
			g.fill(0, 0, width, height, 0x99000000);
		}
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(ox, oy);
		pose.scale(scale, scale);
		MenuKit k = new MenuKit(g, lx(mxs), ly(mys), hits);

		k.round(px, py, pw, ph, 18, 0xFF222326);
		k.ring(px, py, pw, ph, 18, 0x14FFFFFF, 1);
		float x0 = px + 1, y0 = py + 1, iw = pw - 2, ih = ph - 2;
		header(k, x0, y0, iw);

		float hy = y0 + 58;
		viewH = ih - 58;
		sidebar(k, x0 + 24, hy + 16);

		float cx = x0 + 24 + SIDE_W + 20, cw = iw - 48 - SIDE_W - 20;
		List<Row> rows = visibleRows();
		k.scissor(cx - 4, hy, cw + 24, viewH);
		k.setClip(hy, hy + viewH);
		float top = hy + 12 - scroll;
		float end = drawRows(k, cx, top, cw, rows);
		if (rows.isEmpty()) k.textCenter("No options match your search", cx + cw / 2f, hy + 60, 13, MUTED, false);
		k.clearClip();
		k.endScissor();
		content = end - top + 12 + 16;
		scroll = Math.max(0, Math.min(scroll, Math.max(0, content - viewH)));
		k.scrollbar(x0 + iw - 8, hy, viewH, content, scroll);
		pose.popMatrix();
	}

	private void header(MenuKit k, float x0, float y0, float iw) {
		float cy = y0 + 20 + 15;
		k.text("Options", x0 + 24, cy, 20, WHITE, true);
		k.text("Saved automatically", x0 + 24 + MenuKit.width("Options", 20, true) + 12, cy + 1, 11, MUTED);

		float doneW = 80, doneX = x0 + iw - 24 - doneW;
		boolean hov = k.hover(doneX, cy - 15, doneW, 30);
		k.round(doneX, cy - 15, doneW, 30, 8, hov ? 0xFF8296E0 : ACCENT);
		k.textCenter("Done", doneX + doneW / 2f, cy, 12, WHITE, false);
		k.hit(doneX, cy - 15, doneW, 30, this::onClose);

		float sw = 240, sx = doneX - 12 - sw;
		boolean sh = k.hover(sx, cy - 15, sw, 30);
		k.box(sx, cy - 15, sw, 30, 8, searchFocus ? 0x14FFFFFF : sh ? PILL_HOVER : PILL, searchFocus ? ACCENT : PILL_BORDER);
		k.icon(ICON_SEARCH, sx + 18, cy, 12, MUTED);
		boolean caret = searchFocus && (System.currentTimeMillis() / 500) % 2 == 0;
		if (query.isEmpty()) {
			k.text("Search options", sx + 34, cy, 12, MUTED);
			if (caret) k.fill(sx + 34, cy - 7, 1, 14, WHITE);
		} else {
			String shown = MenuKit.fit(query, sw - 50, 12, false);
			k.text(shown, sx + 34, cy, 12, WHITE);
			if (caret) k.fill(sx + 34 + MenuKit.width(shown, 12, false) + 1, cy - 7, 1, 14, WHITE);
		}
		k.hit(sx, cy - 15, sw, 30, () -> searchFocus = true);
	}

	private void sidebar(MenuKit k, float x, float y) {
		boolean searching = !query.isBlank();
		for (int i = 0; i < sections.size(); i++) {
			Section s = sections.get(i);
			boolean on = i == section && !searching;
			boolean hov = k.hover(x, y, SIDE_W, 38);
			k.round(x, y, SIDE_W, 38, 10, on ? 0xFF6B7FE0 : hov ? 0x17FFFFFF : 0x0DFFFFFF);
			k.icon(s.icon(), x + 26, y + 19, 13, on ? WHITE : TEXT);
			k.text(s.name(), x + 48, y + 19, 13, on || hov ? WHITE : TEXT);
			final int idx = i;
			k.hit(x, y, SIDE_W, 38, () -> {
				section = idx;
				query = "";
				scroll = 0;
				listening = null;
			});
			y += 38 + 6;
		}
	}

	private float drawRows(MenuKit k, float x, float y, float w, List<Row> rows) {
		boolean first = true;
		for (Row r : rows) {
			if (r instanceof Head h) {
				if (!first) y += 6;
				if (k.visibleY(y, HEAD_H)) k.text(h.title().toUpperCase(Locale.ROOT), x + 4, y + HEAD_H / 2f + 2, 10, MUTED, true);
				y += HEAD_H;
			} else {
				if (k.visibleY(y, ROW_H)) drawRow(k, x, y, w, r);
				y += ROW_H + GAP;
			}
			first = false;
		}
		return y;
	}

	private void drawRow(MenuKit k, float x, float y, float w, Row r) {
		float cy = y + ROW_H / 2f, right = x + w - 16;
		boolean hov = k.hover(x, y, w, ROW_H);
		boolean clickRow = r instanceof Toggle || r instanceof Lang;
		k.box(x, y, w, ROW_H, 12, hov && clickRow ? CARD_HOVER : CARD, CARD_BORDER);
		String label = labelOf(r);
		if (r instanceof Toggle t) {
			k.text(label, x + 16, cy, 13, WHITE);
			boolean on = t.get().getAsBoolean();
			k.toggle(right - 38, cy - 11, 38, on, ACCENT);
			k.hit(x, y, w, ROW_H, () -> t.set().accept(!t.get().getAsBoolean()));
		} else if (r instanceof Slide s) {
			k.text(label, x + 16, cy, 13, WHITE);
			float sw = 240, sx = right - sw;
			k.range(sx, cy - 7, sw, 14, s.frac(), ACCENT);
			k.textRight(s.text.apply(s.frac()), sx - 14, cy, 12, MUTED, false);
			k.drag(sx - 4, cy - 12, sw + 8, 24, lx -> {
				s.drag(MenuKit.fracAt(lx, sx, sw));
			}, s::release);
		} else if (r instanceof Cycle c) {
			k.text(label, x + 16, cy, 13, WHITE);
			float bw = 190, bx = right - bw;
			boolean ph = k.hover(bx, cy - 14, bw, 28);
			k.box(bx, cy - 14, bw, 28, 9, ph ? PILL_HOVER : PILL, PILL_BORDER);
			k.textCenter(MenuKit.fit(c.text().get(), bw - 16, 12, false), bx + bw / 2f, cy, 12, WHITE, false);
			k.hitRight(bx, cy - 14, bw, 28, () -> c.step().accept(1), () -> c.step().accept(-1));
		} else if (r instanceof Act a) {
			k.text(label, x + 16, cy, 13, WHITE);
			float bw = 110, bx = right - bw;
			boolean ph = k.hover(bx, cy - 14, bw, 28);
			k.round(bx, cy - 14, bw, 28, 9, ph ? 0xFF8296E0 : ACCENT);
			k.textCenter(a.button(), bx + bw / 2f, cy, 12, WHITE, false);
			k.hit(bx, cy - 14, bw, 28, a.run());
		} else if (r instanceof Key kb) {
			KeyMapping km = kb.mapping();
			boolean conflict = false;
			if (!km.isUnbound()) {
				for (KeyMapping other : opts.keyMappings) {
					if (other != km && km.same(other)) conflict = true;
				}
			}
			k.text(MenuKit.fit(label, w - 16 - 160 - 80 - 24, 13, false), x + 16, cy, 13, WHITE);
			float bw = 160, bx = right - bw;
			boolean isListening = listening == km;
			boolean ph = k.hover(bx, cy - 14, bw, 28);
			k.box(bx, cy - 14, bw, 28, 9, isListening ? 0x2E7289DA : ph ? PILL_HOVER : PILL, isListening ? ACCENT : conflict ? RED : PILL_BORDER);
			String txt = isListening ? "> Press a key <" : km.getTranslatedKeyMessage().getString();
			k.textCenter(MenuKit.fit(txt, bw - 14, 12, false), bx + bw / 2f, cy, 12, conflict ? RED : WHITE, false);
			k.hitRight(bx, cy - 14, bw, 28, () -> listening = isListening ? null : km, () -> reset(km));
			if (!km.isDefault()) {
				float rw = 56, rx = bx - 8 - rw;
				boolean rh = k.hover(rx, cy - 14, rw, 28);
				k.box(rx, cy - 14, rw, 28, 9, rh ? PILL_HOVER : PILL, PILL_BORDER);
				k.textCenter("Reset", rx + rw / 2f, cy, 11, TEXT, false);
				k.hit(rx, cy - 14, rw, 28, () -> reset(km));
			}
		} else if (r instanceof Lang l) {
			boolean cur = l.code().equals(Minecraft.getInstance().getLanguageManager().getSelected());
			k.text(label, x + 16, cy, 13, cur ? WHITE : TEXT);
			if (cur) k.icon(ICON_CHECK, right - 8, cy, 13, ACCENT);
			k.hit(x, y, w, ROW_H, () -> setLanguage(l.code()));
		}
	}

	private void reset(KeyMapping km) {
		km.setKey(km.getDefaultKey());
		KeyMapping.resetMapping();
		opts.save();
		listening = null;
	}

	private void bind(InputConstants.Key key) {
		listening.setKey(key);
		KeyMapping.resetMapping();
		opts.save();
		listening = null;
	}

	// ------------------------------------------------------------------- input

	private static int btn(MouseButtonEvent e) {
		return net.glacier.compat.Mouse.button(e);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
		if (listening != null) {
			bind(InputConstants.Type.MOUSE.getOrCreate(btn(e)));
			return true;
		}
		searchFocus = false;
		double lx = lx(e.x()), ly = ly(e.y());
		for (int i = hits.size() - 1; i >= 0; i--) {
			MenuKit.Hit h = hits.get(i);
			if (!h.in(lx, ly)) continue;
			if (btn(e) == 1 && h.right() != null) {
				h.right().run();
			} else if (btn(e) == 0) {
				if (h.drag()) active = h;
				h.press().accept(lx);
			}
			return true;
		}
		return super.mouseClicked(e, dbl);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
		if (active != null) {
			active.press().accept(lx(e.x()));
			return true;
		}
		return super.mouseDragged(e, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent e) {
		if (active != null) {
			Runnable r = active.release();
			active = null;
			if (r != null) r.run();
			return true;
		}
		return super.mouseReleased(e);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy) {
		float max = Math.max(0, content - viewH);
		scroll = (float) Math.max(0, Math.min(scroll - sy * 30, max));
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		int key = e.key();
		if (listening != null) {
			if (key == GLFW.GLFW_KEY_ESCAPE) bind(InputConstants.UNKNOWN);
			else bind(InputConstants.getKey(e));
			return true;
		}
		if (searchFocus) {
			if (key == GLFW.GLFW_KEY_BACKSPACE) {
				if (!query.isEmpty()) query = query.substring(0, query.length() - 1);
				scroll = 0;
				return true;
			}
			if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
				searchFocus = false;
				return true;
			}
			return true;
		}
		if (key == GLFW.GLFW_KEY_ESCAPE) {
			if (!query.isEmpty()) query = "";
			else onClose();
			return true;
		}
		return super.keyPressed(e);
	}

	@Override
	public boolean charTyped(CharacterEvent e) {
		if (searchFocus && e.isAllowedChatCharacter() && query.length() < 40) {
			query += e.codepointAsString();
			scroll = 0;
			return true;
		}
		return super.charTyped(e);
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().gui.setScreen(parent);
	}

	@Override
	public void removed() {
		if (textOn) Minecraft.getInstance().textInputManager().stopTextInput(this);
		textOn = false;
		opts.save();
		super.removed();
	}
}
