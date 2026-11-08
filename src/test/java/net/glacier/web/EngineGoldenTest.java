package net.glacier.web;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Golden-master tests for the HTML/CSS/Layout engine: every element box of each scenario at each
 * viewport must match {@code src/test/resources/golden/<scenario>_<w>x<h>.txt}.
 * Regenerate after an intentional layout change with {@code -Dglacier.golden.update=true}
 * (or run {@link GoldenDump} with an output directory).
 */
public class EngineGoldenTest {
	private static final String UPDATE_PROP = "glacier.golden.update";
	private static final Path GOLDEN_DIR = Paths.get("src", "test", "resources", "golden");

	@TestFactory
	public Stream<DynamicTest> goldenLayouts() {
		List<DynamicTest> tests = new ArrayList<>();
		for (String scenario : GoldenDump.SCENARIOS) {
			for (int[] vp : GoldenDump.VIEWPORTS) {
				String name = GoldenDump.goldenName(scenario, vp[0], vp[1]);
				tests.add(DynamicTest.dynamicTest(name, () -> check(name, scenario, vp[0], vp[1])));
			}
		}
		return tests.stream();
	}

	private static void check(String name, String scenario, int width, int height) throws IOException {
		String actual = GoldenDump.golden(scenario, width, height);
		if (Boolean.getBoolean(UPDATE_PROP)) {
			Files.createDirectories(GOLDEN_DIR);
			Files.writeString(GOLDEN_DIR.resolve(name + ".txt"), actual, StandardCharsets.UTF_8);
			return;
		}
		String expected;
		try (InputStream in = EngineGoldenTest.class.getResourceAsStream("/golden/" + name + ".txt")) {
			assertNotNull(in, "missing golden file " + name + ".txt; regenerate with -D" + UPDATE_PROP + "=true");
			expected = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		if (!expected.equals(actual)) fail(firstDifference(name, expected, actual));
	}

	/** Describes the first differing line so a failure points at the element that moved. */
	static String firstDifference(String name, String expected, String actual) {
		String[] e = expected.split("\n", -1);
		String[] a = actual.split("\n", -1);
		int n = Math.min(e.length, a.length);
		for (int i = 0; i < n; i++) {
			if (!e[i].equals(a[i])) {
				return name + ": line " + (i + 1) + "\n  expected: " + e[i] + "\n  actual:   " + a[i];
			}
		}
		return name + ": line count differs, expected " + e.length + " actual " + a.length;
	}
}
