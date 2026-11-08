package net.glacier.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Test-scope scenarios and a textual dump of every element box after CSS + Layout.
 * Usable from JUnit ({@link EngineGoldenTest}), from benchmarks, and from the command line:
 * {@code GoldenDump <outDir>} regenerates all golden files into that directory.
 */
public final class GoldenDump {
	/** Viewports (width, height) every scenario is laid out at. */
	public static final int[][] VIEWPORTS = {{480, 270}, {960, 540}, {1920, 1080}};

	/** Scenario names: real menu/quickbar/start pages plus the synthetic large DOM. */
	public static final String[] SCENARIOS = {"menu", "quickbar", "start", "large"};

	private static final String WEB = "/assets/glacier/web/";
	private static final int LARGE_CARDS = 120;

	private GoldenDump() {}

	/** Reads a classpath resource under assets/glacier/web (or any absolute resource path). */
	public static String readResource(String name) {
		String path = name.startsWith("/") ? name : WEB + name;
		try (InputStream in = GoldenDump.class.getResourceAsStream(path)) {
			if (in == null) throw new IllegalStateException("missing resource " + path);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("cannot read " + path, e);
		}
	}

	/** The document markup of a scenario. */
	public static String html(String scenario) {
		switch (scenario) {
			case "menu": return readResource("menu.html");
			case "quickbar": return readResource("quickbar.html");
			case "start": return readResource("start.html");
			case "large": return largeHtml();
			default: throw new IllegalArgumentException("unknown scenario " + scenario);
		}
	}

	/** The external stylesheets of a scenario, in cascade order. */
	public static List<String> cssNames(String scenario) {
		switch (scenario) {
			case "menu":
			case "large":
				return List.of("menu.css", "pages.css", "pages-extra.css");
			case "quickbar": return List.of("quickbar.css");
			case "start": return List.of("start.css");
			default: throw new IllegalArgumentException("unknown scenario " + scenario);
		}
	}

	/**
	 * Parsed stylesheets for a scenario: the external CSS, then the document's own {@code <style>} text,
	 * the same order WebView uses.
	 */
	public static List<Css.Stylesheet> sheets(String scenario, Node root) {
		List<Css.Stylesheet> out = new ArrayList<>();
		for (String c : cssNames(scenario)) out.add(Css.parseSheet(readResource(c)));
		out.add(Css.parseSheet(root.styleText));
		return out;
	}

	/** Parses, styles and lays out a scenario at the given viewport with the fake text metrics. */
	public static Node layoutScenario(String scenario, int width, int height) {
		Node root = HtmlParser.parse(html(scenario));
		Css.apply(root, sheets(scenario, root));
		Layout.layout(root, width, height, FakeTextMeasurer.INSTANCE);
		return root;
	}

	/**
	 * One line per element: {@code path tag#id.class.class | x y w h contentH [hidden]}.
	 * Path is the child-index chain from the root, counting text nodes, so it is stable across runs.
	 * Numbers are rounded to 0.01.
	 */
	public static String dump(Node root) {
		StringBuilder sb = new StringBuilder();
		walk(root, "0", sb);
		return sb.toString();
	}

	private static void walk(Node n, String path, StringBuilder sb) {
		if (!n.isText()) {
			sb.append(path).append(' ').append(n.tag);
			if (!n.id.isEmpty()) sb.append('#').append(n.id);
			for (String c : n.classes) sb.append('.').append(c);
			sb.append(" | ")
					.append(fmt(n.x)).append(' ').append(fmt(n.y)).append(' ')
					.append(fmt(n.w)).append(' ').append(fmt(n.h)).append(' ')
					.append(fmt(n.contentH));
			if (!n.visible) sb.append(" hidden");
			sb.append('\n');
		}
		for (int i = 0; i < n.children.size(); i++) walk(n.children.get(i), path + "." + i, sb);
	}

	/** Formats a float with two decimals, rounding half away from zero, and no negative zero. */
	static String fmt(float v) {
		if (Float.isNaN(v)) return "NaN";
		if (Float.isInfinite(v)) return v > 0 ? "Inf" : "-Inf";
		long r = Math.round((double) v * 100.0);
		if (r == 0) return "0.00";
		long a = Math.abs(r);
		return (r < 0 ? "-" : "") + (a / 100) + "." + String.format(Locale.ROOT, "%02d", a % 100);
	}

	/** Golden file name for a scenario at a viewport, e.g. {@code menu_960x540}. */
	public static String goldenName(String scenario, int width, int height) {
		return scenario + "_" + width + "x" + height;
	}

	/** Full golden text for a scenario at a viewport. */
	public static String golden(String scenario, int width, int height) {
		return dump(layoutScenario(scenario, width, height));
	}

	/**
	 * The menu shell with 120 module cards in a 3-column grid, a tab bar, and the settings sheet
	 * shown (not display:none) with several rows and a slider.
	 */
	public static String largeHtml() {
		String shell = readResource("menu.html");
		StringBuilder page = new StringBuilder();
		page.append("<div class=\"pg\"><div class=\"toolbar\"><div class=\"search\">")
				.append("<img class=\"ico\" src=\"glacier:x\"><input type=\"text\"></div>")
				.append("<div class=\"chips\">");
		for (int i = 0; i < 5; i++) page.append("<button class=\"chip\">Category ").append(i).append("</button>");
		page.append("</div></div><div class=\"grid3\">");
		for (int i = 0; i < LARGE_CARDS; i++) page.append(moduleCard(i));
		page.append("</div></div>");

		StringBuilder tabs = new StringBuilder();
		for (String t : new String[] {"Modules", "Elements", "Editors", "Music", "Cosmetics", "Performance"}) {
			tabs.append("<button class=\"tab\"><img class=\"ico\" src=\"glacier:x\"><span>")
					.append(t).append("</span></button>");
		}

		StringBuilder settings = new StringBuilder();
		settings.append("<div class=\"drawer\"><div class=\"drawer-head\"><span class=\"drawer-title\">Settings</span>")
				.append("<span class=\"grow\"></span><button class=\"btn small\">Reset to defaults</button></div>");
		for (int i = 0; i < 6; i++) settings.append(settingRow(i));
		settings.append("<div class=\"row\"><div class=\"row-text\"><div class=\"row-label\">Scale</div></div>")
				.append("<input class=\"range\" type=\"range\"><span class=\"slider-val\">1.5</span></div></div>");

		String html = replaceOnce(shell, "<div id=\"tabs\" class=\"tabs\"></div>",
				"<div id=\"tabs\" class=\"tabs\">" + tabs + "</div>");
		html = replaceOnce(html, "<div id=\"page\" class=\"page\"></div>",
				"<div id=\"page\" class=\"page\">" + page + "</div>");
		html = replaceOnce(html, "class=\"sheet\" style=\"display: none\"", "class=\"sheet\"");
		html = replaceOnce(html, "<div id=\"sp\" class=\"sp\"></div>",
				"<div id=\"sp\" class=\"sp\">" + settings + "</div>");
		return html;
	}

	private static String replaceOnce(String src, String from, String to) {
		int at = src.indexOf(from);
		if (at < 0 || src.indexOf(from, at + 1) >= 0) {
			throw new IllegalStateException("menu.html shell marker not found exactly once: " + from);
		}
		return src.substring(0, at) + to + src.substring(at + from.length());
	}

	private static String moduleCard(int i) {
		String state = i % 3 == 0 ? "DISABLED" : "ENABLED";
		return "<div class=\"mcard\"><div class=\"mcard-top\"><div class=\"tile\"><img src=\"glacier:x\"></div>"
				+ "<div class=\"mcard-text\"><div class=\"mcard-name\">Module " + i + "</div>"
				+ "<div class=\"mcard-state\">" + state + "</div></div><span class=\"dot\"></span>"
				+ "<button class=\"gear\"><img class=\"ico\" src=\"glacier:x\"></button></div>"
				+ "<div class=\"mcard-desc\">Synthetic module description number " + i + " for layout coverage.</div></div>";
	}

	private static String settingRow(int i) {
		return "<div class=\"row\"><div class=\"row-text\"><div class=\"row-label\">Setting " + i + "</div>"
				+ "<div class=\"row-sub\">Explanatory text for setting " + i + ".</div></div>"
				+ "<button class=\"switch on\"><span class=\"knob\"></span></button></div>";
	}

	/** Regenerates every golden file into {@code outDir} (created if missing). */
	public static void main(String[] args) throws IOException {
		Path dir = Paths.get(args.length > 0 ? args[0] : "src/test/resources/golden");
		Files.createDirectories(dir);
		for (String s : SCENARIOS) {
			for (int[] vp : VIEWPORTS) {
				String name = goldenName(s, vp[0], vp[1]);
				Files.writeString(dir.resolve(name + ".txt"), golden(s, vp[0], vp[1]), StandardCharsets.UTF_8);
				System.out.println("wrote " + name);
			}
		}
	}
}
