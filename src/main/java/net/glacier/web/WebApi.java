package net.glacier.web;

import net.glacier.GlacierClient;
import net.glacier.config.Config;
import net.glacier.cosmetics.Accessories;
import net.glacier.cosmetics.Capes;
import net.glacier.hud.HudModule;
import net.glacier.module.Category;
import net.glacier.module.Module;
import net.glacier.module.ModuleManager;
import net.glacier.module.impl.CosmeticsModule;
import net.glacier.module.impl.EditorsModule;
import net.glacier.module.impl.ElementsModule;
import net.glacier.module.impl.PerformanceMods;
import net.glacier.music.MusicPlayer;
import net.glacier.perf.JvmArgs;
import net.glacier.setting.ChoiceSetting;
import net.glacier.setting.ColorSetting;
import net.glacier.setting.Setting;
import net.glacier.setting.SliderSetting;
import net.glacier.setting.ToggleSetting;
import net.glacier.ui.screen.HudEditorScreen;
import net.glacier.ui.screen.WebScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.sounds.SoundSource;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The {@code glacier} global for HTML menus. Settings, modules, cosmetics and music are read and written directly
 * on the live objects; every change is followed by {@link Config#save()}.
 *
 * <p>Value formats: colours are CSS hex strings {@code "#RRGGBB"} or {@code "#RRGGBBAA"} (set also accepts a number
 * as ARGB). Module categories are their display names ("HUD", "Render", "Misc", "System").
 */
public final class WebApi {
	/** Grid overlay state for the quick bar, shared across opens. */
	private static boolean gridOn;

	private final Screen screen;
	private JsBridge bridge;

	/** {@code screen} is the page's host screen: close() closes it, and the HUD editor returns to it. */
	public WebApi(Screen screen) {
		this.screen = screen;
	}

	/** Creates the {@code glacier} global in the bridge's scope. */
	public void install(JsBridge bridge) {
		this.bridge = bridge;
		Scriptable g = bridge.newObject();
		fn(g, "modules", a -> modules());
		fn(g, "toggle", a -> toggle(JsBridge.argString(a, 0)));
		fn(g, "settings", a -> settingsOf(ModuleManager.byId(JsBridge.argString(a, 0))));
		fn(g, "set", a -> set(JsBridge.argString(a, 0), JsBridge.argString(a, 1), JsBridge.arg(a, 2)));
		fn(g, "reset", a -> reset(JsBridge.argString(a, 0)));
		fn(g, "editorSettings", a -> settingsOf(ModuleManager.get(EditorsModule.class)));
		fn(g, "elementsSettings", a -> {
			ElementsModule em = ModuleManager.get(ElementsModule.class);
			if (em != null) em.syncFromOptions();
			return settingsOf(em);
		});
		fn(g, "performanceSettings", a -> settingsOf(ModuleManager.get(PerformanceMods.class)));
		fn(g, "jvmArgs", a -> JvmArgs.TEXT);
		fn(g, "copyJvmArgs", a -> copyToClipboard(JvmArgs.TEXT));
		fn(g, "cosmetics", a -> cosmetics());
		fn(g, "selectCape", a -> selectCape(JsBridge.argString(a, 0)));
		fn(g, "accessories", a -> accessories());
		fn(g, "setAccessory", a -> setAccessory(JsBridge.argString(a, 0), JsBridge.argString(a, 1)));
		def(g, "music", musicObject());
		fn(g, "fps", a -> (double) Minecraft.getInstance().getFps());
		fn(g, "openHudEditor", a -> {
			open(new HudEditorScreen(screen));
			return Undefined.instance;
		});
		fn(g, "openMenu", a -> {
			open(net.glacier.ui.screen.ScreenFactory.menu(null));
			return Undefined.instance;
		});
		fn(g, "openQuickBar", a -> {
			open(net.glacier.ui.screen.ScreenFactory.quickBar());
			return Undefined.instance;
		});
		fn(g, "close", a -> {
			if (screen != null) screen.onClose();
			return Undefined.instance;
		});
		fn(g, "gridOn", a -> gridOn);
		fn(g, "toggleGrid", a -> {
			gridOn = !gridOn;
			return gridOn;
		});
		fn(g, "viewport", a -> viewport());
		fn(g, "keyName", a -> {
			Module m = ModuleManager.byId(JsBridge.argString(a, 0));
			return m == null ? "None" : m.keyName();
		});
		fn(g, "setKey", a -> setKey(JsBridge.argString(a, 0), (int) Context.toNumber(JsBridge.arg(a, 1))));
		fn(g, "clearKey", a -> setKey(JsBridge.argString(a, 0), 0));
		fn(g, "enabledCount", a -> {
			int n = 0;
			for (Module m : ModuleManager.all()) if (m.isEnabled()) n++;
			return (double) n;
		});
		fn(g, "keyHandled", a -> {
			if (screen instanceof WebScreen ws) ws.markKeyHandled();
			return Undefined.instance;
		});
		bridge.scope().put("glacier", bridge.scope(), g);
	}

	private void fn(Scriptable o, String name, JsBridge.Fn f) {
		o.put(name, o, bridge.fn(name, f));
	}

	private void def(Scriptable o, String name, Object value) {
		o.put(name, o, value);
	}

	/* ------------------------------------------------------------ modules */

	/*
	 * The module list is built once per module set: the JS objects and their array are kept, and each call only
	 * refreshes "enabled" (the one field that changes at runtime). The page reads this several times a second.
	 */
	private List<Module> moduleSnapshot;
	private Scriptable[] moduleObjects;
	/** The "enabled" value last written to each module object; a property is only rewritten when it changes. */
	private boolean[] moduleEnabled;
	private Scriptable moduleArray;

	private Scriptable modules() {
		// SYSTEM modules back the menu pages (Elements, Editors, ...) and are not shown in the Modules grid.
		List<Module> all = new ArrayList<>();
		for (Module m : ModuleManager.all()) {
			if (m.category() != Category.SYSTEM) all.add(m);
		}
		if (moduleArray == null || moduleSnapshot == null || moduleSnapshot.size() != all.size()) {
			moduleSnapshot = List.copyOf(all);
			moduleObjects = new Scriptable[moduleSnapshot.size()];
			moduleEnabled = new boolean[moduleSnapshot.size()];
			List<Object> out = new ArrayList<>(moduleSnapshot.size());
			for (int i = 0; i < moduleSnapshot.size(); i++) {
				Module m = moduleSnapshot.get(i);
				Scriptable o = bridge.newObject();
				def(o, "id", m.id());
				def(o, "name", m.name());
				def(o, "desc", m.description());
				def(o, "category", m.category().display);
				def(o, "hud", m instanceof HudModule);
				def(o, "icon", iconFor(m));
				moduleEnabled[i] = m.isEnabled();
				def(o, "enabled", moduleEnabled[i]);
				moduleObjects[i] = o;
				out.add(o);
			}
			moduleArray = bridge.newArray(out);
		}
		for (int i = 0; i < moduleObjects.length; i++) {
			boolean on = moduleSnapshot.get(i).isEnabled();
			if (on != moduleEnabled[i]) {
				moduleEnabled[i] = on;
				def(moduleObjects[i], "enabled", on);
			}
		}
		return moduleArray;
	}

	private boolean toggle(String id) {
		Module m = ModuleManager.byId(id);
		if (m == null) return false;
		m.toggle();
		afterChange(m, null);
		return m.isEnabled();
	}

	/** Binds a module to a GLFW key code (0 clears) and returns the new key name. */
	private String setKey(String id, int code) {
		Module m = ModuleManager.byId(id);
		if (m == null) return "None";
		m.setKeyCode(Math.max(0, code));
		Config.save();
		return m.keyName();
	}

	private boolean reset(String id) {
		Module m = ModuleManager.byId(id);
		if (m == null) return false;
		// Restore defaults on the live instance: a fresh one would re-run constructors that register static state.
		for (Setting<?> s : m.settings()) {
			if (!s.isHidden()) s.resetToDefault();
		}
		afterChange(m, null);
		return true;
	}

	private boolean set(String id, String key, Object value) {
		Module m = ModuleManager.byId(id);
		Setting<?> s = findSetting(m, key);
		if (s == null || !applyValue(s, value)) return false;
		afterChange(m, key);
		return true;
	}

	private static Setting<?> findSetting(Module m, String key) {
		if (m == null) return null;
		for (Setting<?> s : m.settings()) {
			if (!s.isHidden() && s.getKey().equals(key)) return s;
		}
		return null;
	}

	/** Module-side effects after a setting changed: sync hooks, then save the config. */
	private static void afterChange(Module m, String key) {
		if (m instanceof PerformanceMods pm) pm.syncEnabled();
		if (m instanceof ElementsModule em && (key == null || "showsubtitles".equals(key))) em.applyOptions();
		Config.save();
	}

	/** Coerces a JS value to the setting's type and stores it. Returns false when the value is unusable. */
	private static boolean applyValue(Setting<?> s, Object v) {
		if (s instanceof ToggleSetting t) {
			t.set(v instanceof Boolean b ? b : Context.toBoolean(v));
			return true;
		}
		if (s instanceof SliderSetting sl) {
			if (JsBridge.isMissing(v)) return false;
			double d = Context.toNumber(v);
			if (Double.isNaN(d)) return false;
			sl.set(d);
			return true;
		}
		if (s instanceof ChoiceSetting ch) {
			String str = Context.toString(v);
			if (!ch.options().contains(str)) return false;
			ch.set(str);
			return true;
		}
		if (s instanceof ColorSetting co) {
			Integer argb = parseColor(v);
			if (argb == null) return false;
			co.set(argb);
			return true;
		}
		return false;
	}

	private Scriptable settingsOf(Module m) {
		List<Object> out = new ArrayList<>();
		if (m != null) {
			for (Setting<?> s : m.settings()) {
				if (s.isHidden()) continue;
				Scriptable o = settingObject(s);
				if (o != null) out.add(o);
			}
		}
		return bridge.newArray(out);
	}

	private Scriptable settingObject(Setting<?> s) {
		Scriptable o = bridge.newObject();
		def(o, "key", s.getKey());
		def(o, "label", s.getName());
		if (s instanceof ToggleSetting t) {
			def(o, "type", "toggle");
			def(o, "value", t.on());
		} else if (s instanceof SliderSetting sl) {
			def(o, "type", "slider");
			def(o, "value", sl.get());
			def(o, "min", sl.min());
			def(o, "max", sl.max());
			def(o, "step", sl.step());
		} else if (s instanceof ChoiceSetting ch) {
			def(o, "type", "choice");
			def(o, "value", ch.get());
			def(o, "options", bridge.newArray(ch.options()));
		} else if (s instanceof ColorSetting co) {
			def(o, "type", "color");
			def(o, "value", cssHex(co.argb()));
		} else {
			return null;
		}
		return o;
	}

	/* ------------------------------------------------------------ cosmetics */

	/** Every cape, with None first; each entry carries its swatch colours and whether it is selected. */
	private Scriptable cosmetics() {
		CosmeticsModule cm = ModuleManager.get(CosmeticsModule.class);
		String selected = cm == null ? Capes.NONE : cm.cape.get();
		List<Object> out = new ArrayList<>();
		Scriptable none = bridge.newObject();
		def(none, "name", Capes.NONE);
		def(none, "base", "");
		def(none, "accent", "");
		def(none, "selected", Capes.NONE.equals(selected));
		out.add(none);
		for (Capes.Cape c : Capes.ALL) {
			Scriptable o = bridge.newObject();
			def(o, "name", c.name());
			def(o, "file", c.fileName());
			def(o, "base", cssHex(c.base()));
			def(o, "accent", cssHex(c.accent()));
			def(o, "selected", c.name().equals(selected));
			out.add(o);
		}
		return bridge.newArray(out);
	}

	private boolean selectCape(String name) {
		CosmeticsModule cm = ModuleManager.get(CosmeticsModule.class);
		if (cm == null || !cm.cape.options().contains(name)) return false;
		cm.cape.set(name);
		afterChange(cm, "cape");
		return true;
	}

	/**
	 * One entry per accessory type: {type, designs:[{name, slug, file}], selected}. {@code file} is relative to
	 * textures/cosmetics, so the menu can build glacier:textures/cosmetics/&lt;file&gt;.
	 */
	private Scriptable accessories() {
		CosmeticsModule cm = ModuleManager.get(CosmeticsModule.class);
		List<Object> out = new ArrayList<>();
		for (Accessories.Type t : Accessories.Type.values()) {
			String selected = cm == null ? Accessories.NONE : cm.accessory(t.displayName);
			List<Object> designs = new ArrayList<>();
			for (String design : t.DESIGNS) {
				String slug = design.toLowerCase(Locale.ROOT).replace(' ', '_');
				Scriptable d = bridge.newObject();
				def(d, "name", design);
				def(d, "slug", slug);
				def(d, "file", t.folder + "/" + slug + ".png");
				designs.add(d);
			}
			Scriptable o = bridge.newObject();
			def(o, "type", t.displayName);
			def(o, "designs", bridge.newArray(designs));
			def(o, "selected", selected);
			out.add(o);
		}
		return bridge.newArray(out);
	}

	/** Sets the design for an accessory type ("Bandana", "Glasses", ...). "None" clears it. */
	private boolean setAccessory(String type, String name) {
		CosmeticsModule cm = ModuleManager.get(CosmeticsModule.class);
		if (cm == null) return false;
		for (Accessories.Type t : Accessories.Type.values()) {
			if (!t.displayName.equals(type)) continue;
			ChoiceSetting s = cm.accessorySetting(t);
			if (s == null || !s.options().contains(name)) return false;
			s.set(name);
			afterChange(cm, s.getKey());
			return true;
		}
		return false;
	}

	/* ------------------------------------------------------------ music */

	private Scriptable musicObject() {
		Scriptable m = bridge.newObject();
		fn(m, "state", a -> musicState());
		fn(m, "play", a -> {
			MusicPlayer.play((int) JsBridge.argNumber(a, 0));
			return Undefined.instance;
		});
		fn(m, "stop", a -> {
			MusicPlayer.stop();
			return Undefined.instance;
		});
		fn(m, "next", a -> {
			int n = MusicPlayer.count();
			int cur = MusicPlayer.current();
			MusicPlayer.play(cur < 0 ? 0 : (cur + 1) % n);
			return Undefined.instance;
		});
		fn(m, "prev", a -> {
			int n = MusicPlayer.count();
			int cur = MusicPlayer.current();
			MusicPlayer.play(cur <= 0 ? n - 1 : cur - 1);
			return Undefined.instance;
		});
		fn(m, "volume", a -> {
			if (!JsBridge.isMissing(JsBridge.arg(a, 0))) setRecordsVolume(JsBridge.argNumber(a, 0));
			return recordsVolume();
		});
		return m;
	}

	/*
	 * The page polls this several times a second. One state object and one tracks array are kept; the tracks
	 * array is rebuilt only when the track count changes (the list is fixed at class load).
	 */
	private Scriptable musicObj, musicTracks;
	private int musicTrackCount = -1;

	private Scriptable musicState() {
		int cur = MusicPlayer.current();
		if (musicObj == null) musicObj = bridge.newObject();
		int count = MusicPlayer.count();
		if (musicTracks == null || musicTrackCount != count) {
			List<Object> tracks = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				Scriptable t = bridge.newObject();
				def(t, "index", i);
				def(t, "title", MusicPlayer.title(i));
				def(t, "seconds", MusicPlayer.lengthSeconds(i));
				tracks.add(t);
			}
			musicTracks = bridge.newArray(tracks);
			musicTrackCount = count;
			def(musicObj, "tracks", musicTracks);
		}
		def(musicObj, "playing", cur >= 0);
		def(musicObj, "index", cur);
		def(musicObj, "title", MusicPlayer.title(cur));
		def(musicObj, "length", MusicPlayer.lengthSeconds(cur));
		def(musicObj, "progress", (double) MusicPlayer.progressSeconds());
		def(musicObj, "volume", recordsVolume());
		return musicObj;
	}

	/** Music uses the vanilla Records volume, so this is the same slider as the sound options. 0..1. */
	private static double recordsVolume() {
		Options o = Minecraft.getInstance().options;
		return o == null ? 0.0 : o.getSoundSourceVolume(SoundSource.RECORDS);
	}

	private static void setRecordsVolume(double v) {
		Options o = Minecraft.getInstance().options;
		if (o == null) return;
		o.getSoundSourceOptionInstance(SoundSource.RECORDS).set(Math.max(0.0, Math.min(1.0, v)));
	}

	/* ------------------------------------------------------------ performance */

	/** Puts text on the system clipboard through Minecraft's keyboard handler. Returns false when it fails. */
	private static boolean copyToClipboard(String text) {
		try {
			Minecraft.getInstance().keyboardHandler.setClipboard(text);
			return true;
		} catch (RuntimeException e) {
			GlacierClient.LOGGER.warn("[ui] clipboard copy failed", e);
			return false;
		}
	}

	/* ------------------------------------------------------------ helpers */

	/** The page's logical viewport in css px as {w, h}; both 0 before the first frame. */
	private Scriptable viewportObj;
	private double vpW = Double.NaN, vpH = Double.NaN;

	private Scriptable viewport() {
		double w = 0, h = 0;
		if (screen instanceof WebScreen ws) {
			w = ws.viewportWidth();
			h = ws.viewportHeight();
		}
		if (viewportObj == null) viewportObj = bridge.newObject();
		if (w != vpW) {
			vpW = w;
			def(viewportObj, "w", w);
		}
		if (h != vpH) {
			vpH = h;
			def(viewportObj, "h", h);
		}
		return viewportObj;
	}

	private static void open(Screen s) {
		Minecraft.getInstance().gui.setScreen(s);
	}

	private static String iconFor(Module m) {
		return switch (m.id()) {
			case "fpscounter" -> "f625";
			case "pingcounter" -> "f012";
			case "coordinateshud" -> "f3c5";
			case "directionhud" -> "f14e";
			case "speedometer" -> "f624";
			case "armorhud" -> "f132";
			case "keystrokes" -> "f11c";
			case "fullbright" -> "f185";
			case "performancemods" -> "f0e7";
			default -> "f1b2";
		};
	}

	/** "#RRGGBBAA", the CSS 8-digit form of the stored ARGB value. */
	private static String cssHex(int argb) {
		return String.format(Locale.ROOT, "#%06X%02X", argb & 0xFFFFFF, argb >>> 24);
	}

	/** Accepts "#RRGGBB", "#RRGGBBAA" (CSS order) or a number holding ARGB. Returns null when unusable. */
	private static Integer parseColor(Object v) {
		if (v instanceof Number n) return (int) n.longValue();
		if (JsBridge.isMissing(v)) return null;
		String s = Context.toString(v).trim();
		if (s.startsWith("#")) s = s.substring(1);
		try {
			if (s.length() == 6) return (int) (0xFF000000L | Long.parseLong(s, 16));
			if (s.length() == 8) {
				long rgba = Long.parseLong(s, 16);
				long alpha = rgba & 0xFF;
				long rgb = rgba >>> 8;
				return (int) ((alpha << 24) | rgb);
			}
		} catch (NumberFormatException e) {
			return null;
		}
		return null;
	}
}
