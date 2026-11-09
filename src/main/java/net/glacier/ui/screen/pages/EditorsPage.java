package net.glacier.ui.screen.pages;

import net.glacier.module.ModuleManager;
import net.glacier.module.impl.EditorsModule;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.glacier.ui.Draw;
import net.glacier.hud.HotbarSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;

/** Editors tab (pages/editors.js): tab pills, the live preview card and the controls of the selected editor. */
public final class EditorsPage {
	private static final String[][] TABS = {{"crosshairs", "Crosshairs", "f05b"}, {"hotbar", "Hotbar", "f00a"},
			{"expbar", "EXP Bar", "f080"}, {"display", "Display", "f26c"}};
	private static final String[][] BIOMES = {{"Overworld", "#3b5a3a"}, {"Nether", "#4a1f1a"}, {"Mangrove", "#2e3d2a"},
			{"End City", "#2a2438"}};
	private static final String[][] CROSS = {{"Default", "#ffffff"}, {"Black", "#000000"}, {"Red", "#ff3333"},
			{"Blue", "#4073ff"}, {"Yellow", "#ffff33"}, {"Orange", "#ffa500"}, {"Purple", "#a833ff"}, {"Green", "#33ff4d"},
			{"Chroma", "#ff6ad5"}};
	private static final String[] BIOME_TEX = {"overworld", "nether", "fortress", "end"};
	private static final Identifier[] BIOME_ID = new Identifier[4];
	private static final String[] STYLES = {"Vanilla", "Preset", "Drawn"};
	private static final String[] STYLE_LABELS = {"Vanilla", "Presets", "Drawn"};
	private static final String[][] EXP = {{"Default", "#80ff20"}, {"Blue", "#4f7ce0"}, {"Red", "#f04747"},
			{"Pink", "#eb459e"}, {"Yellow", "#fee75c"}, {"Orange", "#faa61a"}, {"Purple", "#9b59b6"}, {"White", "#ffffff"},
			{"Chroma", "#ff6ad5"}};
	private static final String[][] DISPLAY = {{"Bright", "#ffffff"}, {"Aqua", "#4ad0e0"}, {"Red", "#f04747"},
			{"Blue", "#4f7ce0"}, {"Yellow", "#fee75c"}, {"Orange", "#faa61a"}, {"Purple", "#9b59b6"}, {"Green", "#43b581"}};
	/** name, background, radius, gap */
	private static final Object[][] HOTBAR = {{"Vanilla", 0x59000000, 2, 2}, {"Default", 0x8C000000, 4, 2},
			{"Connected", 0x8C000000, 0, 0}, {"Glacier", 0xE62C2F33, 8, 3}, {"Chroma", 0x737289DA, 4, 2}};
	private static final String[][] HOTBAR_PAIRS = hotbarPairs();

	static {
		for (int i = 0; i < 4; i++) {
			BIOME_ID[i] = Identifier.fromNamespaceAndPath("glacier", "textures/editors/biome_" + BIOME_TEX[i] + ".png");
		}
	}
	private static final int INSET = 0xFF1F2127, EDGE = 0xFF383B40;
	private static final int ON_FILL = 0x297289DA, ON_LINE = 0x997289DA;
	private static final int MUTED = 0xFF99AAB5;

	private String tab = "crosshairs";
	private String biome = "Overworld";
	private final Runnable openHud;

	public EditorsPage(Runnable openHud) {
		this.openHud = openHud;
	}

	private static EditorsModule mod() {
		return ModuleManager.get(EditorsModule.class);
	}

	private static String named(String[][] pairs, String name, String dflt) {
		for (String[] p : pairs) if (p[0].equals(name)) return p[1];
		return dflt;
	}

	private static int argb(String hex, float alpha) {
		return Draw.alpha(MenuKit.rgb(hex), alpha);
	}

	private static void set(ChoiceSetting s, String v) {
		s.set(v);
		ModulesPage.changed(mod(), s.getKey());
	}

	private static void flip(ToggleSetting s) {
		s.flip();
		ModulesPage.changed(mod(), s.getKey());
	}

	// ------------------------------------------------------------------ page

	public float draw(MenuKit k, float x, float y, float w) {
		EditorsModule m = mod();
		if (m == null) return y;

		// tabs: grid 4, gap 4, height 30
		float tw = (w - 12) / 4f;
		for (int i = 0; i < TABS.length; i++) {
			float tx = x + i * (tw + 4);
			boolean on = TABS[i][0].equals(tab);
			k.box(tx, y, tw, 30, 15, on ? 0xFF6B7FE0 : 0xFF2C2F33, on ? 0xFF6B7FE0 : 0xFF393C41);
			int cp = Integer.parseInt(TABS[i][2], 16);
			float iw = MenuKit.iconWidth(cp, 11), lw = MenuKit.width(TABS[i][1], 12, false);
			float sx = tx + (tw - (iw + 6 + lw)) / 2f;
			int fg = on ? 0xFFFFFFFF : MUTED;
			k.icon(cp, sx + iw / 2f, y + 15, 11, fg);
			k.text(TABS[i][1], sx + iw + 6, y + 15, 12, fg);
			final String id = TABS[i][0];
			k.hit(tx, y, tw, 30, () -> tab = id);
		}
		y += 30 + 10;
		preview(k, m, x, y, w);
		y += 302 + 10;
		return switch (tab) {
			case "hotbar" -> hotbar(k, m, x, y, w);
			case "expbar" -> exp(k, m, x, y, w);
			case "display" -> display(k, m, x, y, w);
			default -> crosshairs(k, m, x, y, w);
		};
	}

	// --------------------------------------------------------------- preview

	private void preview(MenuKit k, EditorsModule m, float x, float y, float w) {
		k.box(x, y, w, 302, 18, 0xFF2C2F33, 0xFF393C41);
		float ix = x + 13, iw = w - 26;
		float top = y + 11;
		// head: title (card-h margin-bottom 6 centres it 3px high), LIVE pill, HUD Editor button
		k.text("Editor Preview", ix, top + 11, 14, 0xFFFFFFFF, true);
		float bw = 2 + 24 + MenuKit.iconWidth(0xF0B2, 14) + 6 + MenuKit.width("HUD Editor", 11, false);
		float bx = ix + iw - bw;
		boolean hov = k.hover(bx, top, bw, 28);
		k.box(bx, top, bw, 28, 9, hov ? 0x4D7289DA : 0x3D7289DA, 0xBF7289DA);
		k.icon(0xF0B2, bx + 13 + MenuKit.iconWidth(0xF0B2, 14) / 2f, top + 14, 14, 0xFFFFFFFF);
		k.text("HUD Editor", bx + 13 + MenuKit.iconWidth(0xF0B2, 14) + 6, top + 14, 11, 0xFFFFFFFF);
		k.hit(bx, top, bw, 28, openHud);

		// biome segmented control
		float sy = top + 28 + 8;
		k.box(ix, sy, iw, 32, 16, INSET, EDGE);
		float bwid = (iw - 8) / 4f;
		String bg = BIOMES[0][1];
		int biomeIdx = 0;
		for (int i = 0; i < BIOMES.length; i++) {
			float bxx = ix + 4 + i * bwid;
			boolean on = BIOMES[i][0].equals(biome);
			if (on) {
				bg = BIOMES[i][1];
				biomeIdx = i;
				k.round(bxx, sy + 4, bwid, 24, 12, 0xFF6B7FE0);
			}
			k.textCenter(BIOMES[i][0], bxx + bwid / 2f, sy + 16, 11, on ? 0xFFFFFFFF : MUTED, false);
			final String n = BIOMES[i][0];
			k.hit(bxx, sy + 4, bwid, 24, () -> biome = n);
		}

		// screen (200px + 1px border)
		float cy0 = sy + 32 + 8;
		k.box(ix, cy0, iw, 202, 12, MenuKit.rgb(bg), EDGE);
		float px0 = ix + 1, py0 = cy0 + 1, pw = iw - 2;
		k.scissor(px0, py0, pw, 200);
		biomeImage(k, BIOME_ID[biomeIdx], px0, py0, pw, 200);
		if (tab.equals("display")) {
			float a = (float) (Math.max(0, Math.min(10, m.displayIntensity.get())) * 0.03);
			k.fill(px0, py0, pw, 200, argb(named(DISPLAY, m.displayColor.get(), "#ffffff"), a));
		}
		// bottom shade (black, 30%, 46px) behind the hotbar like the pack's preview_shade
		k.fill(px0, py0 + 200 - 46, pw, 46, 0x4D000000);
		if (tab.equals("crosshairs")) {
			boolean custom = !m.crosshairStyle.is("Vanilla");
			var pose = k.g.pose();
			pose.pushMatrix();
			pose.translate(Math.round(px0 + pw / 2f), Math.round(py0 + 100));
			pose.scale(2f, 2f);
			m.drawCrosshair(k.g, 0, 0, custom);
			pose.popMatrix();
		}
		// LIVE pill, top left of the screen (pack: 40x14 at 7,7)
		k.round(px0 + 7, py0 + 7, 40, 14, 7, 0xFF43B581);
		k.textCenter("LIVE", px0 + 27, py0 + 14, 8, 0xFFFFFFFF, true);
		hudPreview(k, m, px0 + pw / 2f, py0 + 200 - 10);
		k.endScissor();
	}

	private static final Identifier VANILLA_BAR = Identifier.withDefaultNamespace("hud/hotbar");
	private static final Identifier VANILLA_SEL = Identifier.withDefaultNamespace("hud/hotbar_selection");
	private static final Identifier VANILLA_XP_BG = Identifier.withDefaultNamespace("hud/experience_bar_background");
	private static final Identifier VANILLA_XP = Identifier.withDefaultNamespace("hud/experience_bar_progress");
	private static ItemStack[] stacks;

	private static ItemStack[] stacks() {
		if (stacks == null) {
			stacks = new ItemStack[]{new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.DIAMOND_PICKAXE),
					new ItemStack(Items.BOW), new ItemStack(Items.GOLDEN_APPLE, 12), new ItemStack(Items.COOKED_BEEF, 64),
					new ItemStack(Items.OAK_PLANKS, 64), new ItemStack(Items.TORCH, 32), new ItemStack(Items.ARROW, 64),
					new ItemStack(Items.SHIELD)};
		}
		return stacks;
	}

	/** The in-game hotbar + EXP bar drawn with the same HotbarSkin textures, scaled 1.3x, bottom centre at (cx, bottom). */
	private void hudPreview(MenuKit k, EditorsModule m, float cx, float bottom) {
		var g = k.g;
		var font = Minecraft.getInstance().font;
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(Math.round(cx), Math.round(bottom));
		pose.scale(1.3f, 1.3f);
		final int bx = -91, by = -22;
		// EXP bar (vanilla position: 7px above the hotbar) with the level number above it
		int xy = by - 7, fill = Math.round(182 * 0.64f);
		if (HotbarSkin.customExp()) {
			HotbarSkin.expBar(g, false, bx, xy, 182);
			HotbarSkin.expBar(g, true, bx, xy, fill);
		} else {
			g.blitSprite(RenderPipelines.GUI_TEXTURED, VANILLA_XP_BG, bx, xy, 182, 5);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, VANILLA_XP, 182, 5, 0, 0, bx, xy, fill, 5);
		}
		int lvl = "Chroma".equals(m.expColor.get()) ? HotbarSkin.chroma() : MenuKit.rgb(named(EXP, m.expColor.get(), "#80ff20"));
		g.text(font, "7", -font.width("7") / 2, xy - 8, lvl, true);
		if (tab.equals("expbar") && m.xpPercent.on()) {
			g.text(font, "64%", -font.width("64%") / 2, xy - 18, 0xFFFFFFFF, true);
		}
		// hotbar background and selected slot
		if (HotbarSkin.customHotbar()) HotbarSkin.hotbarBackground(g, bx, by);
		else g.blitSprite(RenderPipelines.GUI_TEXTURED, VANILLA_BAR, bx, by, 182, 22);
		if (HotbarSkin.customHotbar()) HotbarSkin.hotbarSelection(g, bx - 1, by - 1);
		else g.blitSprite(RenderPipelines.GUI_TEXTURED, VANILLA_SEL, bx - 1, by - 1, 24, 23);
		ItemStack[] items = stacks();
		boolean hideCount = m.hideItemCount.on();
		boolean numbers = tab.equals("hotbar") && m.slotNumbers.on();
		for (int i = 0; i < 9; i++) {
			int ix = bx + 3 + i * 20, iy = by + 3;
			g.item(items[i], ix, iy);
			if (hideCount) g.itemDecorations(font, items[i], ix, iy, "");
			else g.itemDecorations(font, items[i], ix, iy);
			if (numbers) g.text(font, String.valueOf(i + 1), ix, iy - 1, 0xFFFFFFFF, true);
		}
		pose.popMatrix();
	}

	/** Draws a biome picture cover-fitted (centred crop) into the w x h box. The source is 512x291. */
	private static void biomeImage(MenuKit k, Identifier id, float x, float y, float w, float h) {
		final int tw = 512, th = 291;
		float srcAspect = (float) tw / th, dstAspect = w / h;
		int rw = tw, rh = th;
		if (dstAspect > srcAspect) rh = Math.min(th, Math.round(tw / dstAspect));
		else rw = Math.min(tw, Math.round(th * dstAspect));
		int u = (tw - rw) / 2, v = (th - rh) / 2;
		k.image(id, x, y, w, h, u, v, rw, rh, tw, th);
	}

	/** One pack crosshair texture (white pixel art, tinted by argb) into a size x size box. */
	private static void tex(MenuKit k, int idx, float x, float y, float size, int argb) {
		int t = EditorsModule.presetTexSize(idx);
		k.g.blit(RenderPipelines.GUI_TEXTURED, EditorsModule.presetTexture(idx), Math.round(x), Math.round(y), 0f, 0f,
				Math.round(size), Math.round(size), t, t, t, t, argb);
	}

	// -------------------------------------------------------------- controls

	private static float cardTop(MenuKit k, float x, float y, float w, float h) {
		k.box(x, y, w, h, 18, 0xFF2C2F33, 0xFF393C41);
		return y + 13;
	}

	/** label (12px 600) over sub (11px muted), left, vertically centred in a row whose content is h tall. */
	private static void labels(MenuKit k, String title, String sub, float x, float ry, float h, float maxW) {
		float off = (h - 30.75f) / 2f;
		k.text(MenuKit.fit(title, maxW, 12, false), x, ry + off + 7.5f, 12, 0xFFFFFFFF);
		k.text(MenuKit.fit(sub, maxW, 11, false), x, ry + off + 15 + 2 + 6.875f, 11, MUTED);
	}

	private static void switchRow(MenuKit k, ToggleSetting s, String title, String sub, float x, float ry, float w, boolean first) {
		if (!first) k.fill(x, ry, w, 1, 0x0DFFFFFF);
		float c = ry + (first ? 0 : 1) + 8;
		labels(k, title, sub, x, c, 30.75f, w - 38 - 12);
		k.toggle(x + w - 38, c + 15.375f - 11, 38, s.on(), 0xFF7289DA);
		k.hit(x + w - 38, c + 15.375f - 11, 38, 22, () -> flip(s));
	}

	private static float pickGrid(MenuKit k, ChoiceSetting s, String[][] pairs, boolean swatches, float x, float y, float w) {
		float cw = (w - 24) / 5f;
		for (int i = 0; i < pairs.length; i++) {
			float px = x + (i % 5) * (cw + 6), py = y + (i / 5) * 36;
			boolean on = pairs[i][0].equals(s.get());
			k.box(px, py, cw, 30, 8, on ? ON_FILL : INSET, on ? ON_LINE : EDGE);
			float tx = px + 1 + 8;
			if (swatches) {
				k.round(tx, py + 10, 10, 10, 3, MenuKit.rgb(pairs[i][1]));
				k.ring(tx, py + 10, 10, 10, 3, 0x33FFFFFF, 1);
				tx += 16;
			}
			k.text(MenuKit.fit(pairs[i][0], px + cw - 9 - tx, 11, false), tx, py + 15, 11, on ? 0xFFFFFFFF : MUTED);
			final String n = pairs[i][0];
			k.hit(px, py, cw, 30, () -> set(s, n));
		}
		int rows = (pairs.length + 4) / 5;
		return y + rows * 30 + (rows - 1) * 6;
	}

	private static String[][] hotbarPairs() {
		String[][] p = new String[HOTBAR.length][2];
		for (int i = 0; i < HOTBAR.length; i++) p[i] = new String[]{(String) HOTBAR[i][0], ""};
		return p;
	}

	private float crosshairs(MenuKit k, EditorsModule m, float x, float y, float w) {
		boolean preset = m.crosshairStyle.is("Preset"), drawn = m.crosshairStyle.is("Drawn");
		boolean custom = preset || drawn;
		float h = 26 + 48 + 2;
		if (preset) h += 151 + 2;
		if (drawn) h += 215 + 2;
		if (custom) h += 106 + 2 + 54 + 2 + 47.75f;
		float cy = cardTop(k, x, y, w, h);
		float ix = x + 15, iw = w - 30;
		// style row: label + segmented Vanilla | Presets | Drawn
		labels(k, "Crosshair Style", "Vanilla, a Glacier preset or your own drawing.", ix, cy + 8, 32, iw - 300);
		float segW = 8;
		for (String o : STYLE_LABELS) segW += MenuKit.width(o, 11, false) + 28;
		float sx = ix + iw - segW, sy = cy + 8;
		k.box(sx, sy, segW, 32, 16, INSET, EDGE);
		float bx = sx + 4;
		for (int i = 0; i < STYLES.length; i++) {
			float bw = MenuKit.width(STYLE_LABELS[i], 11, false) + 28;
			boolean on = STYLES[i].equals(m.crosshairStyle.get());
			if (on) k.round(bx, sy + 4, bw, 24, 12, 0xFF6B7FE0);
			k.textCenter(STYLE_LABELS[i], bx + bw / 2f, sy + 16, 11, on ? 0xFFFFFFFF : MUTED, false);
			final String o = STYLES[i];
			k.hit(bx, sy + 4, bw, 24, () -> set(m.crosshairStyle, o));
			bx += bw;
		}
		float ry = cy + 48 + 2;
		if (preset) {
			k.fill(ix, ry, iw, 1, 0x0DFFFFFF);
			k.textCenter("Crosshairs", ix + iw / 2f, ry + 1 + 8 + 7.5f, 12, 0xFFFFFFFF, false);
			float gy = ry + 1 + 8 + 15 + 8;
			float cw = (iw - 10 * 4) / 11f;
			int cur = m.crosshairPreset.i();
			for (int i = 1; i <= EditorsModule.PRESET_COUNT; i++) {
				int col = (i - 1) % 11, row = (i - 1) / 11;
				float px = ix + col * (cw + 4), py = gy + row * (33 + 6);
				boolean on = cur == i;
				boolean hov = k.hover(px, py, cw, 33);
				k.box(px, py, cw, 33, 8, on ? ON_FILL : INSET, on ? ON_LINE : hov ? 0xFF5A5F66 : EDGE);
				tex(k, i, px + cw / 2f - 9, py + 16.5f - 9, 18, on ? 0xFFFFFFFF : 0xCCFFFFFF);
				final int v = i;
				k.hit(px, py, cw, 33, () -> {
					m.crosshairPreset.set((double) v);
					ModulesPage.changed(m, m.crosshairPreset.getKey());
				});
			}
			ry += 151 + 2;
		}
		if (drawn) {
			k.fill(ix, ry, iw, 1, 0x0DFFFFFF);
			k.text("Draw Crosshair", ix, ry + 1 + 8 + 7.5f, 12, 0xFFFFFFFF);
			float gy = ry + 1 + 8 + 15 + 8;
			final int cell = 10, gap = 1;
			for (int r = 0; r < 16; r++) {
				for (int c = 0; c < 16; c++) {
					float px = ix + c * (cell + gap), py = gy + r * (cell + gap);
					boolean on = m.drawnPixel(r, c);
					boolean hov = k.hover(px, py, cell, cell);
					k.round(px, py, cell, cell, 2, on ? 0xFFFFFFFF : hov ? 0xFF3A3D43 : INSET);
					final int rr = r, cc = c;
					k.hit(px, py, cell, cell, () -> {
						m.toggleDrawnPixel(rr, cc);
						ModulesPage.changed(m, null);
					});
				}
			}
			float gridW = 16 * cell + 15 * gap;
			float bx2 = ix + gridW + 16;
			float cw = MenuKit.width("Clear", 11, false) + 28;
			boolean ch = k.hover(bx2, gy, cw, 30);
			k.box(bx2, gy, cw, 30, 8, INSET, ch ? 0xFF5A5F66 : EDGE);
			k.textCenter("Clear", bx2 + cw / 2f, gy + 15, 11, ch ? 0xFFFFFFFF : MUTED, false);
			k.hit(bx2, gy, cw, 30, () -> {
				m.clearDrawn();
				ModulesPage.changed(m, null);
			});
			ry += 215 + 2;
		}
		if (custom) {
			k.fill(ix, ry, iw, 1, 0x0DFFFFFF);
			k.text("Crosshair Colour", ix, ry + 1 + 8 + 7.5f, 12, 0xFFFFFFFF);
			pickGrid(k, m.crosshairColor, CROSS, true, ix, ry + 1 + 8 + 15 + 8, iw);
			ry += 106 + 2;
			k.fill(ix, ry, iw, 1, 0x0DFFFFFF);
			float c2 = ry + 1 + 8 + 7.5f;
			k.text("Crosshair Size", ix, c2, 12, 0xFFFFFFFF);
			k.textRight(m.crosshairSize.i() + "px", ix + iw, c2, 12, 0xFF8EA0E0, false);
			float rgy = ry + 1 + 8 + 15 + 8;
			SliderSetting sz = m.crosshairSize;
			k.range(ix, rgy, iw, 14, sz.fraction(), 0xFF7289DA);
			k.drag(ix, rgy - 4, iw, 22, lx -> sz.setFraction(MenuKit.fracAt(lx, ix, iw)),
					() -> ModulesPage.changed(m, sz.getKey()));
			ry += 54 + 2;
			switchRow(k, m.crosshairOutline, "Crosshair Outlines", "Draws a dark outline around the crosshair.", ix, ry, iw, false);
		}
		return y + h;
	}

	private float hotbar(MenuKit k, EditorsModule m, float x, float y, float w) {
		float ix = x + 15, iw = w - 30;
		float h1 = 2 + 24 + 21 + 2 + 30;
		float cy = cardTop(k, x, y, w, h1);
		k.text("Style", ix, cy + 7.5f, 12, 0xFFFFFFFF);
		pickGrid(k, m.hotbarStyle, HOTBAR_PAIRS, false, ix, cy + 15 + 6 + 2, iw);
		y += h1 + 10;
		float h2 = 2 + 24 + 46.75f + 2 + 47.75f;
		cy = cardTop(k, x, y, w, h2);
		switchRow(k, m.hideItemCount, "Hide Item Count", "Hides the stack count on hotbar items.", ix, cy, iw, true);
		switchRow(k, m.slotNumbers, "Show Slot Numbers", "Shows the slot number on each hotbar slot.", ix, cy + 46.75f + 2, iw, false);
		return y + h2;
	}

	private float exp(MenuKit k, EditorsModule m, float x, float y, float w) {
		float ix = x + 15, iw = w - 30;
		float h1 = 2 + 24 + 21 + 2 + 66;
		float cy = cardTop(k, x, y, w, h1);
		k.text("Colors", ix, cy + 7.5f, 12, 0xFFFFFFFF);
		pickGrid(k, m.expColor, EXP, true, ix, cy + 15 + 6 + 2, iw);
		y += h1 + 10;
		float h2 = 2 + 24 + 46.75f;
		cy = cardTop(k, x, y, w, h2);
		switchRow(k, m.xpPercent, "Show XP Percentage", "Shows your experience progress as a percentage.", ix, cy, iw, true);
		return y + h2;
	}

	private float display(MenuKit k, EditorsModule m, float x, float y, float w) {
		float ix = x + 15, iw = w - 30;
		float h1 = 2 + 24 + 21 + 2 + 66;
		float cy = cardTop(k, x, y, w, h1);
		k.text("Colors", ix, cy + 7.5f, 12, 0xFFFFFFFF);
		pickGrid(k, m.displayColor, DISPLAY, true, ix, cy + 15 + 6 + 2, iw);
		y += h1 + 10;
		float h2 = 2 + 24 + 46.75f;
		cy = cardTop(k, x, y, w, h2);
		labels(k, "Intensity", "Strength of the colour tint, 0 to 10.", ix, cy + 8, 30.75f, iw - 130);
		SliderSetting in = m.displayIntensity;
		float c = cy + 8 + 15.375f;
		float bx = ix + iw - 112;
		step(k, bx, c, 0xF068, () -> {
			in.set(in.get() - 1);
			ModulesPage.changed(m, in.getKey());
		});
		k.textCenter(String.valueOf(in.i()), bx + 30 + 8 + 18, c, 12, 0xFF8EA0E0, false);
		step(k, bx + 82, c, 0xF067, () -> {
			in.set(in.get() + 1);
			ModulesPage.changed(m, in.getKey());
		});
		return y + h2;
	}

	private static void step(MenuKit k, float x, float cy, int cp, Runnable act) {
		k.box(x, cy - 15, 30, 30, 8, INSET, EDGE);
		k.icon(cp, x + 15, cy, 14, 0xFFFFFFFF);
		k.hit(x, cy - 15, 30, 30, act);
	}
}
