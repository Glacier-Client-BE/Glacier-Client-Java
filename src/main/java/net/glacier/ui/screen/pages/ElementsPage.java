package net.glacier.ui.screen.pages;

import net.glacier.module.Module;
import net.glacier.module.ModuleManager;
import net.glacier.module.impl.ElementsModule;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.glacier.setting.Setting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Elements tab (pages/elements.js): a 2 column grid of cards, each a titled list of switch and slider rows. */
public final class ElementsPage {
	private record Group(String title, String... labels) {
	}

	private static final Group[] GROUPS = {
			new Group("HUD Factories Elements", "Hide Item Name Text", "Hide Jukebox Text", "Hide Tip Text", "Hide Actionbar Text"),
			new Group("Bossbar Elements", "Better Boss Bar", "Boss Bar Style", "Boss Bar Position", "Bar Width", "Boss Bar Scale", "Smooth Fill", "Color By Boss Color", "Show Overlay Notches", "Hide Boss Bar", "Hide Boss Name", "Show Boss Percentage", "Boss Bar Vertical Padding"),
			new Group("Scoreboard", "Hide Scoreboard", "Hide Scores"),
			new Group("HUD Elements", "Hide Vignette", "Hide Effects Icon", "Use Base BG", "Show Sound Subtitles")};
	private static final Map<String, String> SUB = new HashMap<>();

	static {
		String[] t = {
				"Hide Item Name Text", "Name of the held item above your hotbar.",
				"Hide Jukebox Text", "Now-playing text from jukeboxes.",
				"Hide Tip Text", "Tips and hints shown on the HUD.",
				"Hide Actionbar Text", "Action bar messages above the hotbar.",
				"Hide Boss Bar", "The boss health bar.",
				"Hide Boss Name", "The boss name above the bar.",
				"Show Boss Percentage", "Health percentage on the boss bar.",
				"Boss Bar Vertical Padding", "Space between stacked boss bars.",
				"Hide Scoreboard", "The sidebar scoreboard.",
				"Hide Scores", "The red score numbers on the scoreboard.",
				"Hide Vignette", "The dark edge overlay effect.",
				"Hide Effects Icon", "Status effect icons at the top right.",
				"Use Base BG", "Backgrounds behind HUD modules.",
				"Show Sound Subtitles", "Sound subtitles at the bottom right."};
		for (int i = 0; i < t.length; i += 2) SUB.put(t[i], t[i + 1]);
	}

	private static final float ROW_H = 47.75f;

	public void enter() {
		ElementsModule em = ModuleManager.get(ElementsModule.class);
		if (em != null) em.syncFromOptions();
	}

	/** Draws at (x, y) in a w-wide column; returns the y below the content. */
	public float draw(MenuKit k, float x, float y, float w) {
		Module m = ModuleManager.get(ElementsModule.class);
		if (m == null) return y;
		long sig = signature(m);
		if (titles == null || sig != cardsSig) {
			cardsSig = sig;
			rebuild(m);
		}
		List<String> titles = this.titles;
		List<List<Setting<?>>> cards = this.cards;

		float colW = (w - 12) / 2f;
		float rowY = y;
		for (int i = 0; i < cards.size(); i += 2) {
			float h0 = cardH(cards.get(i).size());
			float h1 = i + 1 < cards.size() ? cardH(cards.get(i + 1).size()) : 0;
			card(k, m, titles.get(i), cards.get(i), x, rowY, colW);
			if (i + 1 < cards.size()) card(k, m, titles.get(i + 1), cards.get(i + 1), x + colW + 12, rowY, colW);
			rowY += Math.max(h0, h1) + 12;
		}
		return rowY - 12;
	}

	/** Card layout, rebuilt only when the set of visible settings changes. */
	private List<String> titles;
	private List<List<Setting<?>>> cards;
	private long cardsSig;

	/** Changes whenever a setting is added, shown or hidden; allocation free. */
	private static long signature(Module m) {
		long sig = 1;
		for (Setting<?> s : m.settings()) sig = sig * 31 + (s.isHidden() ? 1 : 2);
		return sig;
	}

	private void rebuild(Module m) {
		List<Setting<?>> list = ModulesPage.visible(m);
		Map<String, Setting<?>> byLabel = new HashMap<>();
		for (Setting<?> s : list) byLabel.put(s.getName(), s);
		titles = new ArrayList<>();
		cards = new ArrayList<>();
		Set<String> used = new HashSet<>();
		for (Group g : GROUPS) {
			List<Setting<?>> rows = new ArrayList<>();
			for (String l : g.labels()) {
				Setting<?> s = byLabel.get(l);
				if (s == null) continue;
				used.add(l);
				rows.add(s);
			}
			if (!rows.isEmpty()) {
				titles.add(g.title());
				cards.add(rows);
			}
		}
		List<Setting<?>> other = new ArrayList<>();
		for (Setting<?> s : list) if (!used.contains(s.getName())) other.add(s);
		if (!other.isEmpty()) {
			titles.add("Other");
			cards.add(other);
		}
	}

	private static float cardH(int n) {
		return 49.5f + 49.75f * n;
	}

	private void card(MenuKit k, Module m, String title, List<Setting<?>> rows, float x, float y, float w) {
		k.box(x, y, w, cardH(rows.size()), 18, 0xFF2C2F33, 0xFF393C41);
		float ix = x + 1 + 14, iw = w - 2 - 28;
		k.text(title, ix, y + 13 + 8.75f, 14, 0xFFFFFFFF, true);
		float ry = y + 38.5f;
		for (Setting<?> s : rows) {
			row(k, m, s, ix, ry, iw);
			ry += ROW_H + 2;
		}
	}

	private void row(MenuKit k, Module m, Setting<?> s, float x, float y, float w) {
		k.fill(x, y, w, 1, 0x0DFFFFFF);
		float cy = y + 1 + 8 + 15.375f;
		String sub = SUB.get(s.getName());
		float right = x + w;
		float textW;
		if (s instanceof SliderSetting sl) {
			String v = ModulesPage.fmt(sl.get()) + (s.getName().equals("Boss Bar Scale") ? " %" : " px");
			k.textRight(v, right, cy, 11, 0xFF8EA0E0, false);
			float rx = right - 36 - 12 - 140;
			k.range(rx, cy - 7, 140, 14, sl.fraction(), 0xFF7289DA);
			k.drag(rx, cy - 11, 140, 22, lx -> sl.setFraction(MenuKit.fracAt(lx, rx, 140)),
					() -> ModulesPage.changed(m, s.getKey()));
			textW = rx - 12 - x;
		} else if (s instanceof ToggleSetting t) {
			k.toggle(right - 38, cy - 11, 38, t.on(), 0xFF7289DA);
			k.hit(right - 38, cy - 11, 38, 22, () -> {
				t.flip();
				ModulesPage.changed(m, s.getKey());
			});
			textW = right - 38 - 12 - x;
		} else if (s instanceof ChoiceSetting c) {
			float bw = 112;
			float bx = right - bw;
			boolean hov = k.hover(bx, cy - 13, bw, 26);
			k.box(bx, cy - 13, bw, 26, 8, 0xFF26272B, hov ? 0x996B7FE0 : 0x14FFFFFF);
			k.textCenter(MenuKit.fit(c.get(), bw - 12, 11, false), bx + bw / 2, cy, 11, 0xFFFFFFFF, false);
			k.hit(bx, cy - 13, bw, 26, () -> {
				c.next();
				ModulesPage.changed(m, s.getKey());
			});
			textW = bx - 12 - x;
		} else {
			textW = w;
		}
		k.text(MenuKit.fit(s.getName(), textW, 12, false), x, y + 1 + 8 + 7.5f, 12, 0xFFFFFFFF);
		if (sub != null) k.text(MenuKit.fit(sub, textW, 11, false), x, y + 1 + 8 + 15 + 2 + 6.875f, 11, 0xFF99AAB5);
	}
}
