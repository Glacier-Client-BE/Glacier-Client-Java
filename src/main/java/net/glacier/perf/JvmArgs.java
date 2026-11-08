package net.glacier.perf;

/**
 * Client JVM arguments for Java 21 (G1). {@link #TEXT} is shown and copied on the Performance page, so the launcher
 * profile and the Loom run config cannot drift apart. The same list is mirrored in {@code build.gradle} under
 * {@code loom { runs { client { ... } } }}; change both together.
 */
public final class JvmArgs {
	/** One line, space separated, ready to paste into the launcher's JVM Arguments field. */
	public static final String TEXT = "-Xms1G -Xmx3G"
		+ " -XX:+UseG1GC"
		+ " -XX:MaxGCPauseMillis=200"
		+ " -XX:+ParallelRefProcEnabled"
		+ " -XX:+UnlockExperimentalVMOptions"
		+ " -XX:+DisableExplicitGC"
		+ " -XX:G1NewSizePercent=30"
		+ " -XX:G1MaxNewSizePercent=40"
		+ " -XX:G1ReservePercent=20"
		+ " -XX:SurvivorRatio=32"
		+ " -XX:MaxTenuringThreshold=1"
		+ " -XX:+PerfDisableSharedMem";

	private JvmArgs() {
	}
}
