package net.glacier.ui.screen.pages;

import net.glacier.config.Config;
import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.module.ModuleGroup;
import net.glacier.module.ModuleManager;
import net.glacier.module.impl.ElementsModule;
import net.glacier.module.impl.PerformanceMods;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.ColorSetting;
import net.glacier.setting.Setting;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Modules tab (pages/modules.js, .mp rules in pages.css): search box, category chips and enabled counter over a
 * scrolling 3 column grid of cards, plus the settings sheet that slides over the whole panel from the gear.
 * All numbers are css px.
 */
public final class ModulesPage {
	private static final String[] PALETTE = {"#7289da", "#6b7fe0", "#43b581", "#faa61a", "#f04747", "#ffffff",
			"#99aab5", "#eb459e", "#00b0f4", "#f1c40f", "#2ecc71", "#9b59b6"};
	private static final String[] GROUP_ORDER = {"General", "Layout", "Opacity", "Elements", "Colors"};
	private static final int GROUP_MIN = 6;
	private static final int DEFAULT_ICON = 0xF085;
	private static final Map<String, Integer> ICONS = new HashMap<>();
	/** Collapsed sections, kept for the session: "grid/GROUP" for grid categories, "moduleid/Section" in a settings sheet. */
	private static final java.util.Set<String> COLLAPSED = new java.util.HashSet<>();

	private static void flip(String key) {
		if (!COLLAPSED.remove(key)) COLLAPSED.add(key);
	}

	static {
		Object[] t = {"fpscounter", 0xf625, "pingcounter", 0xf012, "coordinates", 0xf3c5, "coordinateshud", 0xf3c5,
				"directionhud", 0xf14e, "speedometer", 0xf625, "armorhud", 0xf132, "keystrokes", 0xf11c,
				"fullbright", 0xf185, "performancemods", 0xf2db, "clock", 0xf017, "daycounter", 0xf186,
				"serverdisplay", 0xf233, "memoryusage", 0xf538, "lowdurability", 0xf071, "walkdistance", 0xf554,
				"mainhandhud", 0xf256, "timerhud", 0xf2f2, "deathcoordinates", 0xf54c, "zoom", 0xf00e,
				"nohurtcam", 0xf06e, "particlelimiter", 0xf0d0, "menufpslimit", 0xf0e4, "glacierplayers", 0xf007,
				"elements", 0xf0c9, "editors", 0xf044, "cosmetics", 0xf553, "reachdisplay", 0xf545,
				"lightlevel", 0xf0eb, "falldistance", 0xf063, "blockinfo", 0xf1b2, "playersonline", 0xf0c0,
				"entitycount", 0xf1b0, "weather", 0xf0c2, "hitcombo", 0xf0e7, "verticalspeed", 0xf062,
				"airsupply", 0xf043, "attackcooldown", 0xf05b, "health", 0xf004, "fpsstats", 0xf080,
				"gamemode", 0xf11b, "potiontimers", 0xf0c3, "autorespawn", 0xf021};
		for (int i = 0; i < t.length; i += 2) ICONS.put((String) t[i], (Integer) t[i + 1]);
	}

	/** Selected group chip; null is "All". */
	private ModuleGroup group;
	private String query = "";
	private boolean typing;
	private float scroll;
	private String openId;
	private String selKey;
	private boolean capturing;
	private float sheetScroll;
	private String descFor;
	private List<String> descLines = List.of();
	private float lastGridContent, lastGridH, lastBodyContent, lastBodyH;

	public boolean sheetOpen() {
		return openId != null;
	}

	public void closeSheet() {
		openId = null;
		selKey = null;
		capturing = false;
		sheetScroll = 0;
	}

	public void blur() {
		typing = false;
		if (capturing) capturing = false;
	}

	public void enter() {
		typing = false;
	}

	// ------------------------------------------------------------------ data

	private static List<Module> mods() {
		List<Module> out = new ArrayList<>();
		for (Module m : ModuleManager.all()) {
			if (m.category() != Category.SYSTEM) out.add(m);
		}
		return out;
	}

	private static final Comparator<Module> BY_NAME = Comparator.comparing(m -> m.name().toLowerCase(Locale.ROOT));

	private static boolean matches(Module m, ModuleGroup g, String q) {
		if (m.name().toLowerCase(Locale.ROOT).contains(q)) return true;
		if (g.display().toLowerCase(Locale.ROOT).contains(q)) return true;
		String d = m.description();
		return q.length() >= 3 && d != null && d.toLowerCase(Locale.ROOT).contains(q);
	}

	private static int glyph(Module m) {
		Integer cp = ICONS.get(m.name().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""));
		return cp == null ? DEFAULT_ICON : cp;
	}

	static String fmt(double v) {
		double r = Math.round(v * 100.0) / 100.0;
		if (r == Math.rint(r)) return String.valueOf((long) r);
		return String.valueOf(r);
	}

	/** Module-side effects after a setting changed, then save (WebApi.afterChange). */
	static void changed(Module m, String key) {
		if (m instanceof PerformanceMods pm) pm.syncEnabled();
		if (m instanceof ElementsModule em && (key == null || "showsubtitles".equals(key))) em.applyOptions();
		Config.save();
	}

	static List<Setting<?>> visible(Module m) {
		List<Setting<?>> out = new ArrayList<>();
		if (m == null) return out;
		for (Setting<?> s : m.settings()) {
			if (s.isHidden()) continue;
			if (s instanceof ToggleSetting || s instanceof SliderSetting || s instanceof ChoiceSetting
					|| s instanceof ColorSetting) out.add(s);
		}
		return out;
	}

	// ------------------------------------------------------------------ page

	/** Draws the toolbar and grid in the (x, y, w, h) page box (the host content area). */
	public void draw(MenuKit k, float x, float y, float w, float h) {
		List<Module> all = mods();
		int enabled = 0;
		for (Module m : all) if (m.isEnabled()) enabled++;

		// toolbar: 32px row (search left, enabled counter right)
		float cy = y + 16;
		boolean focus = typing;
		k.box(x, y, 220, 32, 8, 0xFF1C1D20, focus ? 0xBF6B7FE0 : 0x0FFFFFFF);
		float ix = x + 1 + 10;
		k.icon(0xF002, ix + MenuKit.iconWidth(0xF002, 11) / 2f, cy, 11, 0xFF8B9097);
		float tx = ix + MenuKit.iconWidth(0xF002, 11) + 8;
		String shown = query.isEmpty() ? (typing ? "|" : "Search modules...") : query + (typing ? "|" : "");
		int tc = query.isEmpty() && !typing ? 0xFF8B9097 : 0xFFFFFFFF;
		if (query.isEmpty() && typing) tc = 0xFFFFFFFF;
		k.scissor(tx, y + 1, x + 220 - 11 - tx, 30);
		float tw = MenuKit.width(shown, 12, false);
		float maxW = x + 220 - 11 - tx;
		k.text(shown, tw > maxW ? tx - (tw - maxW) : tx, cy, 12, tc);
		k.endScissor();
		k.hit(x, y, 220, 32, () -> typing = true);
		k.textRight(enabled + " enabled", x + w, cy, 11, 0xFF8B9097, false);

		// group chips: All first, empty groups hidden, wrapping onto further rows
		EnumMap<ModuleGroup, List<Module>> byGroup = new EnumMap<>(ModuleGroup.class);
		for (Module m : all) byGroup.computeIfAbsent(ModuleGroup.of(m), g -> new ArrayList<>()).add(m);
		if (group != null && !byGroup.containsKey(group)) group = null;
		float chipX = x, chipY = y + 32 + 10;
		for (int ci = -1; ci < ModuleGroup.ordered().size(); ci++) {
			ModuleGroup g = ci < 0 ? null : ModuleGroup.ordered().get(ci);
			List<Module> members = g == null ? all : byGroup.get(g);
			if (members == null || members.isEmpty()) continue;
			String label = g == null ? "All" : g.display();
			String count = String.valueOf(members.size());
			float cw = MenuKit.width(label, 11, false) + 6 + MenuKit.width(count, 9, false) + 20;
			if (chipX > x && chipX + cw > x + w) {
				chipX = x;
				chipY += 26 + 6;
			}
			boolean on = g == group;
			boolean hov = k.hover(chipX, chipY, cw, 26);
			k.round(chipX, chipY, cw, 26, 6, on ? 0xFF6B7FE0 : hov ? 0x17FFFFFF : 0x0DFFFFFF);
			float lw = MenuKit.width(label, 11, false);
			k.text(label, chipX + 10, chipY + 13, 11, on ? 0xFFFFFFFF : 0xFFD6DAE0, false);
			k.text(count, chipX + 10 + lw + 6, chipY + 13, 9, on ? 0xCCFFFFFF : 0xFF8B9097, false);
			final ModuleGroup gg = g;
			k.hit(chipX, chipY, cw, 26, () -> {
				group = gg;
				scroll = 0;
			});
			chipX += cw + 6;
		}

		// scrolling grid
		float gy = chipY + 26 + 14;
		float gh = Math.max(40, h - (gy - y));
		float gw = w - 10;
		float colW = (gw - 24) / 3f;
		String q = query.toLowerCase(Locale.ROOT);
		boolean headers = group == null;
		List<ModuleGroup> sections = new ArrayList<>();
		List<List<Module>> lists = new ArrayList<>();
		int total = 0;
		for (ModuleGroup g : ModuleGroup.ordered()) {
			if (group != null && g != group) continue;
			List<Module> src = byGroup.get(g);
			if (src == null) continue;
			List<Module> list = new ArrayList<>();
			for (Module m : src) {
				if (q.isEmpty() || matches(m, g, q)) list.add(m);
			}
			if (list.isEmpty()) continue;
			list.sort(BY_NAME);
			sections.add(g);
			lists.add(list);
			total += list.size();
		}

		k.scissor(x, gy, w, gh);
		k.setClip(gy, gy + gh);
		float top = gy - scroll;
		float cur = top;
		if (total == 0) {
			k.textCenter("No modules match.", x + gw / 2f, cur + 24 + 7.5f, 12, 0xFF99AAB5, false);
			cur += 24 + 15 + 24;
		}
		for (int si = 0; si < sections.size(); si++) {
			List<Module> list = lists.get(si);
			if (headers) {
				if (si > 0) cur += 10;
				String ck = "grid/" + sections.get(si).name();
				boolean shut = q.isEmpty() && COLLAPSED.contains(ck);
				if (cur + 16 >= gy && cur <= gy + gh) {
					boolean hh = k.hover(x, cur, gw, 16);
					String title = sections.get(si).display().toUpperCase(Locale.ROOT);
					String count = String.valueOf(list.size());
					float hw = MenuKit.width(title, 10, true), nw = MenuKit.width(count, 10, false);
					k.text(title, x, cur + 8, 10, hh ? 0xFFD6DAE0 : 0xFF8B9097, true);
					k.text(count, x + hw + 8, cur + 8, 10, 0xFF5E636A, false);
					k.fill(x + hw + 8 + nw + 10, cur + 8, gw - (hw + 8 + nw + 10) - 18, 1, 0x12FFFFFF);
					k.icon(shut ? 0xF054 : 0xF078, x + gw - 6, cur + 8, 8, hh ? 0xFFD6DAE0 : 0xFF8B9097);
					if (q.isEmpty()) k.hit(x, cur, gw, 16, () -> flip(ck));
				}
				cur += 16 + 8;
				if (shut) {
					cur -= 4;
					continue;
				}
			}
			for (int i = 0; i < list.size(); i++) {
				float cx = x + (i % 3) * (colW + 12);
				float cyy = cur + (i / 3) * 122;
				if (cyy + 110 < gy || cyy > gy + gh) continue;
				card(k, list.get(i), cx, cyy, colW);
			}
			int rows = (list.size() + 2) / 3;
			cur += rows * 110 + Math.max(0, rows - 1) * 12;
		}
		float content = cur - top;
		lastGridContent = content;
		lastGridH = gh;
		scroll = Math.max(0, Math.min(scroll, Math.max(0, content - gh)));
		k.clearClip();
		k.endScissor();
		k.scrollbar(x + w, gy, gh, content, scroll);
	}

	private void card(MenuKit k, Module m, float x, float y, float w) {
		boolean on = m.isEnabled();
		boolean hov = k.hover(x, y, w, 110);
		k.box(x, y, w, 110, 12, hov ? 0xFF303237 : 0xFF2A2B2F, hov ? 0x1FFFFFFF : 0x12FFFFFF);
		k.round(x + 13, y + 13, 8, 8, 4, on ? 0xFF43B581 : 0xFF4A4D52);
		float cx = x + w / 2f;
		k.icon(glyph(m), cx, y + 25 + 12, 18, on ? 0xFF6B7FE0 : 0xFF8B9097);
		String name = MenuKit.fit(m.name(), w - 26, 12, false);
		k.textCenter(name, cx, y + 57 + 7.5f, 12, 0xFFFFFFFF, false);
		k.textCenter(on ? "ENABLED" : "DISABLED", cx, y + 75 + 5, 8, on ? 0xFF43B581 : 0xFF8B9097, true);
		final Module mm = m;
		k.hit(x, y, w, 110, () -> {
			mm.toggle();
			changed(mm, null);
		});
		// gear: 18x18 at right 8 top 8 (padding box), opacity .7
		float gx = x + w - 1 - 8 - 18, gy = y + 1 + 8;
		boolean gh = k.hover(gx, gy, 18, 18);
		if (gh) k.round(gx, gy, 18, 18, 5, 0x14FFFFFF);
		k.icon(0xF013, gx + 9, gy + 9, 11, gh ? 0xFFD6DAE0 : 0xB3D6DAE0);
		k.hit(gx, gy, 18, 18, () -> openSheet(mm.id()));
	}

	public void scroll(double dy, boolean overSheetBody) {
		if (overSheetBody) {
			sheetScroll = (float) Math.max(0, Math.min(sheetScroll - dy * 20, Math.max(0, lastBodyContent - lastBodyH)));
		} else {
			scroll = (float) Math.max(0, Math.min(scroll - dy * 20, Math.max(0, lastGridContent - lastGridH)));
		}
	}

	// ------------------------------------------------------------------ sheet

	private void openSheet(String id) {
		openId = id;
		selKey = null;
		capturing = false;
		sheetScroll = 0;
	}

	/** Draws the sheet over the inner panel area (ix, iy, iw, ih). */
	public void drawSheet(MenuKit k, float ix, float iy, float iw, float ih) {
		Module m = ModuleManager.byId(openId);
		if (m == null) {
			closeSheet();
			return;
		}
		// scrim (click closes) and the 360px right panel
		k.round(ix, iy, iw, ih, 16, 0x9E0C0D0F);
		k.block(ix, iy, iw, ih, this::closeSheet);
		float sx = ix + iw - 360;
		k.fill(sx, iy + 16, 360, ih - 32, 0xFF1E1F22);
		k.round(sx + 320, iy, 40, ih, 16, 0xFF1E1F22);
		k.fill(sx, iy, 340, 16, 0xFF1E1F22);
		k.fill(sx, iy + ih - 16, 340, 16, 0xFF1E1F22);
		k.fill(sx, iy, 2, ih, 0xFF6B7FE0);
		k.block(sx, iy, 360, ih, null);

		float cx = sx + 2 + 18;
		float cw = 360 - 2 - 36;
		// head: padding 22 0 14, 20px icon, gap 10, title, switch 40x22
		float hcy = iy + 22 + 11;
		k.icon(glyph(m), cx + 10, hcy, 15, 0xFF6B7FE0);
		boolean on = m.isEnabled();
		float swx = cx + cw - 40;
		k.toggle(swx, hcy - 11, 40, on, 0xFF6B7FE0);
		k.hit(swx, hcy - 11, 40, 22, () -> {
			m.toggle();
			changed(m, null);
		});
		k.text(MenuKit.fit(m.name(), swx - 10 - (cx + 30), 16, true), cx + 30, hcy, 16, 0xFFFFFFFF, true);
		float headBottom = iy + 22 + 22 + 14;
		k.fill(cx, headBottom, cw, 1, 0x12FFFFFF);

		// foot: border-top, padding 12 0 14, 24px controls
		float footTop = iy + ih - 51;
		k.fill(cx, footTop, cw, 1, 0x12FFFFFF);
		float fcy = footTop + 1 + 12 + 12;
		float rw = MenuKit.width("Reset", 10, false) + 20;
		float rx = cx + cw - rw;
		boolean rh = k.hover(rx, fcy - 12, rw, 24);
		k.box(rx, fcy - 12, rw, 24, 6, 0x00000000, 0x14FFFFFF);
		k.textCenter("Reset", rx + rw / 2f, fcy, 10, rh ? 0xFFFFFFFF : 0xFF8B9097, false);
		k.hit(rx, fcy - 12, rw, 24, () -> {
			for (Setting<?> s : m.settings()) if (!s.isHidden()) s.resetToDefault();
			selKey = null;
			changed(m, null);
		});
		String kl = capturing ? "Press a key..." : m.keyName();
		float kw = Math.max(28, MenuKit.width(kl, 11, true) + 18);
		float kx = rx - 8 - kw;
		k.box(kx, fcy - 12, kw, 24, 6, 0xFF26272B, capturing ? 0xFF6B7FE0 : 0x24FFFFFF);
		k.textCenter(kl, kx + kw / 2f, fcy, 11, capturing ? 0xFFC9D1F5 : 0xFFFFFFFF, true);
		final boolean wasCap = capturing;
		k.hit(kx, fcy - 12, kw, 24, () -> capturing = !wasCap);
		k.text("Keybind", cx, fcy, 11, 0xFFC9CDD3);

		// body: scrolls between head and foot, padding 8 18
		float by = headBottom + 1;
		float bh = footTop - by;
		lastBodyH = bh;
		k.scissor(sx + 2, by, 358, bh);
		k.setClip(by, by + bh);
		float y = by + 8 - sheetScroll;
		List<Setting<?>> list = visible(m);
		float start = y;
		String desc = m.description();
		if (desc != null && !desc.isBlank()) {
			if (!desc.equals(descFor)) {
				descFor = desc;
				descLines = MenuKit.wrap(desc, cw, 11, false);
			}
			y += 6;
			for (String line : descLines) {
				k.text(line, cx, y + 7, 11, 0xFF8B9097);
				y += 15;
			}
			y += 4;
		}
		if (list.isEmpty()) {
			k.textCenter("This module has no settings.", cx + cw / 2f, y + 24 + 7.5f, 12, 0xFF99AAB5, false);
			y += 24 + 15 + 24;
		} else if (list.size() < GROUP_MIN) {
			for (Setting<?> s : list) y = row(k, m, s, cx, y, cw);
		} else {
			for (String gname : GROUP_ORDER) {
				int n = 0;
				for (Setting<?> s : list) if (groupOf(s).equals(gname)) n++;
				if (n == 0) continue;
				String ck = m.id() + "/" + gname;
				boolean shut = COLLAPSED.contains(ck);
				boolean hh = k.hover(cx, y + 10, cw, 21);
				String title = gname.toUpperCase(Locale.ROOT);
				float hy = y + 14 + 6.25f;
				k.text(title, cx, hy, 10, hh ? 0xFFD6DAE0 : 0xFF8B9097, true);
				k.text(String.valueOf(n), cx + MenuKit.width(title, 10, true) + 6, hy, 10, 0xFF5E636A, false);
				k.icon(shut ? 0xF054 : 0xF078, cx + cw - 6, hy, 8, hh ? 0xFFD6DAE0 : 0xFF8B9097);
				k.hit(cx, y + 10, cw, 21, () -> {
					flip(ck);
					selKey = null;
				});
				y += 14 + 12.5f + 2;
				if (shut) continue;
				for (Setting<?> s : list) {
					if (groupOf(s).equals(gname)) y = row(k, m, s, cx, y, cw);
				}
			}
		}
		lastBodyContent = (y - start) + 16;
		sheetScroll = Math.max(0, Math.min(sheetScroll, Math.max(0, lastBodyContent - bh)));
		k.clearClip();
		k.endScissor();
		k.scrollbar(sx + 360, by, bh, lastBodyContent, sheetScroll);
	}

	public boolean overSheetPanel(double lx, float ix, float iw) {
		return lx >= ix + iw - 360;
	}

	private static String groupOf(Setting<?> s) {
		String l = s.getName().toLowerCase(Locale.ROOT);
		if (s instanceof ColorSetting) return "Colors";
		if (s instanceof SliderSetting && l.matches(".*(opacity|alpha|transparen).*")) return "Opacity";
		if (s instanceof ChoiceSetting && l.matches(".*(position|orientation|style|layout|align|anchor).*")) return "Layout";
		if (s instanceof ToggleSetting) return "Elements";
		return "General";
	}

	/** One setting row; returns the y below it. Heights follow .sr: padding 9, gap 8. */
	private float row(MenuKit k, Module m, Setting<?> s, float x, float y, float w) {
		final int label = 0xFFC9CDD3;
		if (s instanceof ToggleSetting t) {
			float cy = y + 20;
			k.text(MenuKit.fit(s.getName(), w - 52, 11, false), x, cy, 11, label);
			k.toggle(x + w - 40, y + 9, 40, t.on(), 0xFF6B7FE0);
			k.hit(x + w - 40, y + 9, 40, 22, () -> {
				t.flip();
				changed(m, s.getKey());
			});
			return y + 40;
		}
		float ly = y + 9 + 6.875f;
		if (s instanceof SliderSetting sl) {
			String v = fmt(sl.get());
			k.text(MenuKit.fit(s.getName(), w - 50, 11, false), x, ly, 11, label);
			k.textRight(v, x + w, ly, 11, 0xFF8EA0E0, false);
			float ry = y + 9 + 13.75f + 8;
			k.range(x, ry, w, 14, sl.fraction(), 0xFF6B7FE0);
			final float fx = x, fw = w;
			k.drag(x, ry - 4, w, 22, lx -> sl.setFraction(MenuKit.fracAt(lx, fx, fw)), () -> changed(m, s.getKey()));
			return y + 9 + 13.75f + 8 + 14 + 9;
		}
		if (s instanceof ChoiceSetting c) {
			k.text(MenuKit.fit(s.getName(), w, 11, false), x, ly, 11, label);
			float sy = y + 9 + 13.75f + 8;
			boolean open = s.getKey().equals(selKey);
			boolean hov = k.hover(x, sy, w, 34);
			k.box(x, sy, w, 34, 8, 0xFF26272B, hov || open ? 0x996B7FE0 : 0x14FFFFFF);
			k.text(MenuKit.fit(c.get(), w - 24 - 20, 12, false), x + 13, sy + 17, 12, 0xFFFFFFFF);
			k.icon(0xF078, x + w - 13 - MenuKit.iconWidth(0xF078, 10) / 2f, sy + 17, 10, 0xFFC9CDD3);
			final String key = s.getKey();
			k.hit(x, sy, w, 34, () -> selKey = open ? null : key);
			float end = sy + 34;
			if (open) {
				float ly2 = end + 8;
				int n = c.options().size();
				float lh = 10 + 26 * n;
				k.box(x, ly2, w, lh, 8, 0xFF26272B, 0x14FFFFFF);
				for (int i = 0; i < n; i++) {
					String opt = c.options().get(i);
					float oy = ly2 + 1 + 4 + i * 26;
					boolean cur = opt.equals(c.get());
					boolean oh = k.hover(x + 5, oy, w - 10, 26);
					if (cur) k.round(x + 5, oy, w - 10, 26, 6, 0x3D6B7FE0);
					else if (oh) k.round(x + 5, oy, w - 10, 26, 6, 0x0FFFFFFF);
					k.text(MenuKit.fit(opt, w - 10 - 16, 12, false), x + 5 + 8, oy + 13, 12, cur || oh ? 0xFFFFFFFF : label);
					k.hit(x + 5, oy, w - 10, 26, () -> {
						c.set(opt);
						selKey = null;
						changed(m, key);
					});
				}
				end = ly2 + lh;
			}
			return end + 9;
		}
		if (s instanceof ColorSetting col) {
			k.text(MenuKit.fit(s.getName(), w, 11, false), x, ly, 11, label);
			float sy = y + 9 + 13.75f + 8;
			int perRow = (int) ((w + 6) / 28);
			int cur = col.argb() & 0xFFFFFF;
			for (int i = 0; i < PALETTE.length; i++) {
				float px = x + (i % perRow) * 28, py = sy + (i / perRow) * 28;
				int rgb = MenuKit.rgb(PALETTE[i]);
				boolean on = (rgb & 0xFFFFFF) == cur;
				k.round(px, py, 22, 22, 7, rgb);
				k.ring(px, py, 22, 22, 7, on ? 0xFFFFFFFF : 0x1AFFFFFF, 2);
				k.hit(px, py, 22, 22, () -> {
					col.set(rgb);
					changed(m, s.getKey());
				});
			}
			int rows = (PALETTE.length + perRow - 1) / perRow;
			return sy + rows * 22 + (rows - 1) * 6 + 9;
		}
		return y;
	}

	// ------------------------------------------------------------------- keys

	/** Key down. Returns true when the page used the key (so Esc does not close the screen). */
	public boolean key(int code) {
		if (capturing && openId != null) {
			Module m = ModuleManager.byId(openId);
			if (m != null) {
				if (code == GLFW.GLFW_KEY_BACKSPACE || code == GLFW.GLFW_KEY_DELETE) m.setKeyCode(0);
				else if (code != GLFW.GLFW_KEY_ESCAPE) m.setKeyCode(code);
				Config.save();
			}
			capturing = false;
			return true;
		}
		if (openId != null) {
			if (code == GLFW.GLFW_KEY_ESCAPE) {
				if (selKey != null) selKey = null;
				else closeSheet();
				return true;
			}
			return false;
		}
		if (!typing) return false;
		if (code == GLFW.GLFW_KEY_BACKSPACE) {
			if (!query.isEmpty()) query = query.substring(0, query.length() - 1);
			scroll = 0;
			return true;
		}
		if (code == GLFW.GLFW_KEY_ESCAPE || code == GLFW.GLFW_KEY_ENTER) {
			typing = false;
			return true;
		}
		return false;
	}

	public boolean typed(String ch) {
		if (capturing) return true;
		if (!typing || openId != null) return false;
		query += ch;
		scroll = 0;
		return true;
	}
}
