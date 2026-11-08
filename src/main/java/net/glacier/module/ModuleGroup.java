package net.glacier.module;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Finer grouping of modules for the Modules page. The order of the constants is the fixed display order of the
 * sections and filter chips. {@link #of(Module)} uses the explicit id table first, then name keywords and finally the
 * {@link Category} of the module so new modules always land somewhere sensible.
 */
public enum ModuleGroup {
	COMBAT("Combat"),
	INFO("Info & Stats"),
	INVENTORY("Inventory & Armor"),
	MOVEMENT("Movement"),
	WORLD("World"),
	VISUALS("Visuals"),
	SOCIAL("Chat & Social"),
	UTILITY("Utility"),
	PERFORMANCE("Performance");

	private static final List<ModuleGroup> ORDERED = List.of(values());
	private static final Map<String, ModuleGroup> BY_ID = new HashMap<>();

	/** Module id (Module.id()) to group. One row per group, readable at a glance. */
	private static final Object[][] TABLE = {
			{COMBAT, "cps", "hitcombo", "reachdisplay", "targethud", "attackcooldown", "cooldowns", "shieldstatus",
					"bowindicator", "hitbox", "mobindicator", "nearestplayer", "crosshaircount", "keystrokes", "killcounter"},
			{INFO, "fpscounter", "pingcounter", "tps", "coordinateshud", "directionhud", "clockcompass", "sessiontime",
					"memoryusage", "speedometer", "serverdisplay", "fpsstats", "playersonline", "entitycount", "chunk",
					"rotation", "blockinfo", "gamemode", "statushud", "xpcalculator", "portalcoordinates",
					"deathcoordinates", "resourcepacks", "biome", "timerhud", "chunkmap", "debughud", "hudpopup"},
			{INVENTORY, "armorhud", "mainhandhud", "offhandhud", "itemscounter", "itemcounter", "slotcounter",
					"potiontimers", "potionicons", "lowdurability", "foodstats", "health", "inventoryhud", "airsupply"},
			{MOVEMENT, "togglesprint", "freelook", "zoom", "elytrapitch", "falldistance", "walkdistance",
					"verticalspeed"},
			{WORLD, "timechanger", "weatherchanger", "nofog", "lightlevel", "weather", "tnttimer", "daycounter",
					"daysplayed", "dayevent", "orevisualizer"},
			{VISUALS, "fullbright", "fovchanger", "lowfire", "nohurtcam", "hidearmor", "particlelimiter",
					"blockoutline", "customcrosshair", "cosmetics", "hitcolor", "safezoneviewer",
					"projectiletrails", "spinningprojectiles"},
			{SOCIAL, "chatfilter", "glacierplayers"},
			{UTILITY, "autoreconnect", "confirmdisconnect", "windowtitle", "noadvancement", "autorespawn"},
			{PERFORMANCE, "performancemods", "menufpslimit"},
	};

	/** Keyword fallbacks for modules missing from the table, checked against the lowercase id in order. */
	private static final Object[][] KEYWORDS = {
			{COMBAT, "cps", "combo", "reach", "target", "shield", "hitbox", "attack", "bow", "combat", "pvp"},
			{INVENTORY, "armor", "item", "slot", "potion", "durab", "food", "health", "hunger", "inventory", "hotbar"},
			{MOVEMENT, "sprint", "sneak", "freelook", "zoom", "elytra", "fall", "walk", "speed", "jump", "swim"},
			{WORLD, "weather", "time", "fog", "light", "tnt", "sky", "day", "biome"},
			{SOCIAL, "chat", "friend", "social", "player", "party", "message"},
			{PERFORMANCE, "perf", "fpslimit", "optimi", "cull", "lag"},
			{VISUALS, "fov", "fire", "hurt", "particle", "fullbright", "bright", "outline", "crosshair", "cosmetic",
					"cape", "shader", "animation"},
			{INFO, "fps", "ping", "tps", "coord", "clock", "memory", "server", "stat", "counter", "info"},
			{UTILITY, "reconnect", "respawn", "window", "advancement", "disconnect", "auto", "confirm"},
	};

	static {
		for (Object[] row : TABLE) {
			ModuleGroup g = (ModuleGroup) row[0];
			for (int i = 1; i < row.length; i++) BY_ID.put((String) row[i], g);
		}
	}

	private final String display;

	ModuleGroup(String display) {
		this.display = display;
	}

	public String display() {
		return display;
	}

	/** All groups in display order. */
	public static List<ModuleGroup> ordered() {
		return ORDERED;
	}

	public static ModuleGroup of(Module m) {
		String id = m.id();
		ModuleGroup g = BY_ID.get(id);
		if (g != null) return g;
		String lower = id.toLowerCase(Locale.ROOT);
		for (Object[] row : KEYWORDS) {
			for (int i = 1; i < row.length; i++) {
				if (lower.contains((String) row[i])) return (ModuleGroup) row[0];
			}
		}
		return switch (m.category()) {
			case HUD -> INFO;
			case RENDER -> VISUALS;
			case MISC, SYSTEM -> UTILITY;
		};
	}
}
