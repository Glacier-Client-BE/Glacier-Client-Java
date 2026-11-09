package net.glacier.cosmetics;

import java.util.ArrayList;
import java.util.List;

import net.glacier.cosmetics.AccessoryModels.Box;

/** Per-design box lists for masks, glasses, tails, pets, scarves, backpacks, bracelets and halos (model pixels). */
final class AccessoryShapes {
	private AccessoryShapes() {
	}

	private static void a(List<Box> b, String n, int tx, int ty, float x, float y, float z, int w, int h, int d) {
		b.add(Box.of(n, tx, ty, x, y, z, w, h, d));
	}

	private static void r(List<Box> b, String n, int tx, int ty, float x, float y, float z, int w, int h, int d,
						  float px, float py, float pz, float rx, float ry, float rz) {
		b.add(Box.of(n, tx, ty, x, y, z, w, h, d).rot(px, py, pz, rx, ry, rz));
	}

	/** Bar on a circle of radius {@code rad} at height {@code y}, facing outward, centred on its own axis. */
	private static void bar(List<Box> b, String n, int tx, int ty, int w, int h, int d, float rad, float y, float deg, float yoff, float tilt) {
		double ang = Math.toRadians(deg);
		r(b, n, tx, ty, -w / 2f, yoff, -d / 2f, w, h, d, (float) (Math.cos(ang) * rad), y, (float) (Math.sin(ang) * rad), tilt, deg + 90, 0);
	}

	// ------------------------------------------------------------------ masks (head coordinates)

	static List<Box> mask(String d) {
		List<Box> b = new ArrayList<>();
		switch (d) {
			case "Oni" -> {
				a(b, "plate", 0, 0, -4, -6, -5, 8, 6, 1);
				a(b, "brow", 0, 0, -4, -6.5F, -5.5F, 8, 1, 1);
				a(b, "band", 0, 7, -4.5F, -7, -4.5F, 9, 2, 9);
				r(b, "hornR", 0, 7, -3.5F, -3, -0.5F, 1, 3, 1, 0, -8, -4, 0, 0, 14);
				r(b, "hornL", 0, 7, 2.5F, -3, -0.5F, 1, 3, 1, 0, -8, -4, 0, 0, -14);
				a(b, "tipR", 0, 0, -4.2F, -12, -4.5F, 1, 2, 1);
				a(b, "tipL", 0, 0, 3.2F, -12, -4.5F, 1, 2, 1);
				a(b, "fangR", 0, 0, -2.5F, -1, -5.5F, 1, 2, 1);
				a(b, "fangL", 0, 0, 1.5F, -1, -5.5F, 1, 2, 1);
			}
			case "Kitsune" -> {
				a(b, "plate", 0, 0, -4, -6, -5, 8, 5, 1);
				a(b, "snout", 0, 0, -1.5F, -3, -8, 3, 2, 3);
				a(b, "nose", 0, 7, -0.5F, -3.5F, -8.5F, 1, 1, 1);
				a(b, "earR", 0, 7, -4, -11, -3.5F, 2, 3, 1);
				a(b, "earL", 0, 7, 2, -11, -3.5F, 2, 3, 1);
				a(b, "whiskR", 0, 7, -5, -4, -4.5F, 1, 1, 2);
				a(b, "whiskL", 0, 7, 4, -4, -4.5F, 1, 1, 2);
				a(b, "band", 0, 7, -4.5F, -7, -4.5F, 9, 2, 9);
			}
			case "Surgical" -> {
				a(b, "plate", 0, 0, -4, -3.5F, -5, 8, 4, 1);
				a(b, "pleat", 0, 7, -4, -2, -5.5F, 8, 1, 1);
				a(b, "loopR", 0, 7, -4.5F, -3, -4, 1, 1, 6);
				a(b, "loopL", 0, 7, 3.5F, -3, -4, 1, 1, 6);
			}
			case "Ninja" -> {
				a(b, "wrap", 0, 0, -4.5F, -3.5F, -4.5F, 9, 4, 9);
				a(b, "band", 0, 7, -4.5F, -7, -4.5F, 9, 2, 9);
				a(b, "knot", 0, 7, -1, -7, 4.5F, 2, 2, 2);
				r(b, "tailR", 0, 7, -2, -6, 6.5F, 1, 4, 1, 0, -6, 6.5F, 0, 0, 15);
				r(b, "tailL", 0, 7, 1, -6, 6.5F, 1, 5, 1, 0, -6, 6.5F, 0, 0, -20);
			}
			case "Domino" -> {
				a(b, "band", 0, 0, -4.5F, -5.5F, -4.5F, 9, 2, 1);
				a(b, "bridge", 0, 7, -1, -6, -5, 2, 1, 1);
				a(b, "wingR", 0, 7, -5, -6, -4.5F, 1, 2, 4);
				a(b, "wingL", 0, 7, 4, -6, -4.5F, 1, 2, 4);
				a(b, "tipR", 0, 7, -5.5F, -7, -4.5F, 1, 1, 2);
				a(b, "tipL", 0, 7, 4.5F, -7, -4.5F, 1, 1, 2);
			}
			case "Hockey" -> {
				a(b, "top", 0, 0, -4.5F, -7, -4.5F, 9, 2, 9);
				for (int i = 0; i < 5; i++) a(b, "v" + i, 0, 7, -3.5F + i * 1.75F, -5, -5.5F, 1, 6, 1);
				a(b, "h1", 0, 7, -4, -4, -5.5F, 8, 1, 1);
				a(b, "h2", 0, 7, -4, -1.5F, -5.5F, 8, 1, 1);
				a(b, "sideR", 0, 0, -5, -5, -4, 1, 5, 4);
				a(b, "sideL", 0, 0, 4, -5, -4, 1, 5, 4);
				a(b, "chin", 0, 0, -2, 0, -5.5F, 4, 1, 1);
			}
			case "Skull Mask" -> {
				a(b, "plate", 0, 0, -4, -6, -5, 8, 5, 1);
				a(b, "brow", 0, 7, -4, -6.5F, -5.5F, 8, 1, 1);
				a(b, "eyeR", 0, 7, -3, -5, -5.5F, 2, 2, 1);
				a(b, "eyeL", 0, 7, 1, -5, -5.5F, 2, 2, 1);
				a(b, "nose", 0, 7, -0.5F, -3, -5.5F, 1, 1, 1);
				a(b, "jaw", 0, 0, -3, -1, -5, 6, 2, 1);
				for (int i = 0; i < 3; i++) a(b, "tooth" + i, 0, 7, -2.5F + i * 2, 1, -5.3F, 1, 1, 1);
			}
			case "Venetian" -> {
				a(b, "plate", 0, 0, -4, -6, -5, 8, 3, 1);
				a(b, "cheekR", 0, 0, -4, -3, -5, 3, 1, 1);
				a(b, "cheekL", 0, 0, 1, -3, -5, 3, 1, 1);
				r(b, "nose", 0, 7, -1, -4.5F, -9, 2, 2, 4, 0, -4.5F, -5, 25, 0, 0);
				a(b, "band", 0, 7, -4.5F, -7, -4.5F, 9, 1, 9);
				r(b, "plume", 0, 7, 3.5F, -13, -0.5F, 1, 6, 1, 4, -7, 0, 0, 0, 25);
			}
			case "Wolf Mask" -> {
				a(b, "plate", 0, 0, -4, -6, -5, 8, 5, 1);
				a(b, "snout", 0, 0, -2, -4, -8, 4, 3, 3);
				a(b, "nose", 0, 7, -1, -4.5F, -8.5F, 2, 1, 1);
				a(b, "earR", 0, 7, -4, -10, -3, 2, 3, 1);
				a(b, "earL", 0, 7, 2, -10, -3, 2, 3, 1);
				a(b, "band", 0, 7, -4.5F, -7, -4.5F, 9, 2, 9);
			}
			case "Robo Plate" -> {
				a(b, "plate", 0, 0, -4, -6, -5, 8, 6, 1);
				a(b, "visor", 0, 7, -3.5F, -5, -5.5F, 7, 1, 1);
				a(b, "vent1", 0, 7, -2, -2.5F, -5.5F, 4, 1, 1);
				a(b, "vent2", 0, 7, -2, -1, -5.5F, 4, 1, 1);
				a(b, "boltR", 0, 7, -5.5F, -4, -2.5F, 2, 2, 2);
				a(b, "boltL", 0, 7, 3.5F, -4, -2.5F, 2, 2, 2);
				a(b, "ant", 0, 7, 2.5F, -11, -1, 1, 3, 1);
				a(b, "band", 0, 7, -4.5F, -7, -4.5F, 9, 1, 9);
			}
			default -> {
				a(b, "plate", 0, 0, -4, -6, -5, 8, 6, 1);
				a(b, "strap", 0, 7, -4.5F, -6, -4.5F, 9, 2, 9);
			}
		}
		return b;
	}

	// ------------------------------------------------------------------ glasses (head coordinates, front at z = -5)

	private static void temples(List<Box> b, float y) {
		a(b, "templeR", 0, 5, -5, y, -4, 1, 1, 4);
		a(b, "templeL", 0, 5, 4, y, -4, 1, 1, 4);
	}

	private static void strap(List<Box> b) {
		a(b, "strapR", 0, 5, -5, -4.5F, -4, 1, 1, 8);
		a(b, "strapL", 0, 5, 4, -4.5F, -4, 1, 1, 8);
		a(b, "strapB", 0, 5, -4.5F, -4.5F, 4, 9, 1, 1);
	}

	private static void pair(List<Box> b, String n, int w, int h, float y, float gap) {
		a(b, n + "R", 0, 0, -gap - w, y, -5, w, h, 1);
		a(b, n + "L", 0, 0, gap, y, -5, w, h, 1);
	}

	static List<Box> glasses(String d) {
		List<Box> b = new ArrayList<>();
		switch (d) {
			case "Shades" -> {
				pair(b, "lens", 4, 2, -5, 0.5F);
				a(b, "bar", 10, 0, -4.5F, -6, -5, 9, 1, 1);
				a(b, "bridge", 10, 0, -0.5F, -5, -5, 1, 1, 1);
				temples(b, -5);
			}
			case "Gold Round" -> {
				for (int s = -1; s <= 1; s += 2) {
					float cx = s * 2.5F;
					a(b, "c" + s, 0, 0, cx - 1, -5, -5, 2, 3, 1);
					a(b, "m" + s, 0, 0, cx - 2, -4, -5, 4, 1, 1);
				}
				a(b, "bridge", 10, 0, -0.5F, -4.5F, -5, 1, 1, 1);
				temples(b, -4.5F);
			}
			case "Hearts" -> {
				for (int s = -1; s <= 1; s += 2) {
					float cx = s * 2.5F;
					a(b, "h1" + s, 0, 0, cx - 2, -5, -5, 1, 1, 1);
					a(b, "h2" + s, 0, 0, cx + 1, -5, -5, 1, 1, 1);
					a(b, "h3" + s, 0, 0, cx - 2, -4, -5, 4, 1, 1);
					a(b, "h4" + s, 0, 0, cx - 1, -3, -5, 2, 1, 1);
				}
				a(b, "bridge", 10, 0, -0.5F, -4.5F, -5, 1, 1, 1);
				temples(b, -4.5F);
			}
			case "Pixel" -> {
				pair(b, "lens", 4, 2, -4.5F, 0.5F);
				a(b, "bar", 10, 0, -5, -5.5F, -5, 10, 1, 1);
				a(b, "bridge", 10, 0, -0.5F, -4.5F, -5, 1, 1, 1);
				temples(b, -5);
			}
			case "Aviator" -> {
				for (int s = -1; s <= 1; s += 2) {
					float cx = s * 2.5F;
					a(b, "t" + s, 0, 0, cx - 2, -5, -5, 4, 1, 1);
					a(b, "m" + s, 0, 0, cx - 1.5F, -4, -5, 3, 1, 1);
					a(b, "u" + s, 0, 0, cx - 1, -3, -5, 2, 1, 1);
				}
				a(b, "bridge", 10, 0, -0.5F, -5, -5, 1, 1, 1);
				a(b, "bridge2", 10, 0, -0.5F, -4, -5, 1, 1, 1);
				a(b, "templeR", 0, 5, -5, -4.5F, -4, 1, 1, 4);
				a(b, "templeL", 0, 5, 4, -4.5F, -4, 1, 1, 4);
			}
			case "3D Glasses" -> {
				pair(b, "lens", 4, 3, -5, 0.5F);
				a(b, "top", 10, 0, -5, -6, -5, 10, 1, 1);
				a(b, "outR", 10, 0, -5, -5, -5, 1, 3, 1);
				a(b, "outL", 10, 0, 4, -5, -5, 1, 3, 1);
				a(b, "bot", 10, 0, -5, -2, -5, 10, 1, 1);
				temples(b, -5);
			}
			case "Cyber Visor" -> {
				a(b, "visor", 0, 0, -4.5F, -5, -5, 9, 2, 1);
				a(b, "wrapR", 0, 5, -5, -5, -4, 1, 2, 5);
				a(b, "wrapL", 0, 5, 4, -5, -4, 1, 2, 5);
				a(b, "led", 10, 0, -2, -6, -5, 4, 1, 1);
				a(b, "ant", 10, 0, 4.5F, -8, -3, 1, 3, 1);
			}
			case "Star Shades" -> {
				for (int s = -1; s <= 1; s += 2) {
					float cx = s * 2.5F;
					a(b, "v" + s, 0, 0, cx - 0.5F, -6, -5, 1, 4, 1);
					a(b, "h" + s, 0, 0, cx - 2.5F, -5, -5, 5, 1, 1);
					a(b, "l" + s, 0, 0, cx - 1.5F, -3, -5, 1, 1, 1);
					a(b, "rr" + s, 0, 0, cx + 0.5F, -3, -5, 1, 1, 1);
					a(b, "x" + s, 0, 0, cx - 1.5F, -4, -5, 3, 1, 1);
				}
				a(b, "bridge", 10, 0, -0.5F, -4.5F, -5, 1, 1, 1);
				temples(b, -4.5F);
			}
			case "Round Neon" -> {
				for (int s = -1; s <= 1; s += 2) {
					float cx = s * 2.5F;
					a(b, "t" + s, 0, 0, cx - 1.5F, -6, -5, 3, 1, 1);
					a(b, "b" + s, 0, 0, cx - 1.5F, -2, -5, 3, 1, 1);
					a(b, "l" + s, 0, 0, cx - 2.5F, -5, -5, 1, 3, 1);
					a(b, "r" + s, 0, 0, cx + 1.5F, -5, -5, 1, 3, 1);
				}
				a(b, "bridge", 10, 0, -0.5F, -4.5F, -5, 1, 1, 1);
				temples(b, -4.5F);
			}
			case "Welding" -> {
				for (int s = -1; s <= 1; s += 2) {
					a(b, "barrel" + s, 0, 0, s * 2.5F - 1.5F, -5.5F, -7, 3, 3, 2);
					a(b, "rim" + s, 10, 0, s * 2.5F - 2, -6, -5.5F, 4, 4, 1);
				}
				a(b, "bridge", 10, 0, -0.5F, -4.5F, -5, 1, 1, 1);
				strap(b);
			}
			case "Sunset Tint" -> {
				pair(b, "lens", 4, 4, -6, 0.5F);
				a(b, "bridge", 10, 0, -0.5F, -5, -5, 1, 1, 1);
				temples(b, -5);
			}
			case "Ski Goggles" -> {
				a(b, "lens", 0, 0, -4.5F, -6, -5.5F, 9, 4, 1);
				a(b, "frame", 10, 0, -4.5F, -6.5F, -5, 9, 1, 1);
				a(b, "foam", 10, 0, -4.5F, -2, -5, 9, 1, 1);
				strap(b);
			}
			case "Wayfarer" -> {
				pair(b, "lens", 4, 2, -5, 0.5F);
				a(b, "rimR", 10, 0, -5.5F, -6, -5, 5, 1, 1);
				a(b, "rimL", 10, 0, 0.5F, -6, -5, 5, 1, 1);
				a(b, "bridge", 10, 0, -0.5F, -5.5F, -5, 1, 1, 1);
				temples(b, -5.5F);
			}
			case "Matrix" -> {
				a(b, "slitR", 0, 0, -4.5F, -4.5F, -5, 4, 1, 1);
				a(b, "slitL", 0, 0, 0.5F, -4.5F, -5, 4, 1, 1);
				a(b, "bridge", 10, 0, -0.5F, -4.5F, -5, 1, 1, 1);
				temples(b, -4.5F);
			}
			case "Steampunk" -> {
				a(b, "barrelR", 0, 0, -4, -5.5F, -7, 3, 3, 2);
				a(b, "ringR", 10, 0, -4.5F, -6, -5.5F, 4, 4, 1);
				a(b, "lensL", 0, 0, 1, -5, -5, 3, 3, 1);
				a(b, "tower", 10, 0, 1.5F, -6.5F, -6.5F, 2, 2, 2);
				a(b, "tube", 10, 0, 3, -8, -5, 1, 2, 1);
				a(b, "bridge", 10, 0, -0.5F, -4.5F, -5, 1, 1, 1);
				temples(b, -4.5F);
			}
			case "Nerd" -> {
				pair(b, "lens", 4, 4, -6, 0.5F);
				a(b, "tape", 10, 0, -0.5F, -4.5F, -5.5F, 1, 1, 1);
				a(b, "tape2", 10, 0, -0.5F, -3.5F, -5, 1, 1, 1);
				temples(b, -5);
			}
			case "Rainbow Lens" -> {
				for (int i = 0; i < 3; i++) {
					float z = i == 1 ? -5 : -5.7F;
					a(b, "sR" + i, 0, 0, -4.5F, -5 + i, z, 4, 1, 1);
					a(b, "sL" + i, 0, 0, 0.5F, -5 + i, z, 4, 1, 1);
				}
				a(b, "bridge", 10, 0, -0.5F, -4.5F, -5, 1, 1, 1);
				temples(b, -4.5F);
			}
			case "Ice Lens" -> {
				pair(b, "lens", 4, 3, -5, 0.5F);
				r(b, "shardR", 10, 0, -0.5F, -3, -0.5F, 1, 3, 1, -5, -6, -5, 0, 0, 20);
				r(b, "shardL", 10, 0, -0.5F, -3, -0.5F, 1, 3, 1, 5, -6, -5, 0, 0, -20);
				a(b, "bridge", 10, 0, -0.5F, -4.5F, -5, 1, 1, 1);
				temples(b, -4.5F);
			}
			case "Toxic Goggles" -> {
				for (int s = -1; s <= 1; s += 2) {
					a(b, "barrel" + s, 0, 0, s * 2.5F - 1.5F, -5.5F, -7, 3, 3, 2);
					a(b, "can" + s, 10, 0, s * 5.5F - 1, -5, -3, 2, 3, 2);
				}
				a(b, "hazard", 10, 0, -2, -6.5F, -5.5F, 4, 1, 1);
				a(b, "bridge", 10, 0, -0.5F, -4.5F, -5, 1, 1, 1);
				strap(b);
			}
			case "Emerald" -> {
				for (int s = -1; s <= 1; s += 2) {
					float cx = s * 2.5F;
					a(b, "t" + s, 0, 0, cx - 1, -6, -5, 2, 1, 1);
					a(b, "m" + s, 0, 0, cx - 2, -5, -5, 4, 2, 1);
					a(b, "u" + s, 0, 0, cx - 1, -3, -5, 2, 1, 1);
				}
				a(b, "bridge", 10, 0, -0.5F, -4.5F, -5, 1, 1, 1);
				temples(b, -4.5F);
			}
			default -> {
				pair(b, "lens", 4, 3, -5, 0.5F);
				a(b, "bridge", 10, 0, -0.5F, -4.5F, -5, 1, 1, 1);
				temples(b, -4.5F);
			}
		}
		return b;
	}

	// ------------------------------------------------------------------ tails (segments along +Z, centred on x/y)

	private static final int[][] TAIL_TEX = {{0, 0}, {0, 7}, {14, 0}, {26, 0}};

	/** Step length of segment i (distance to the next segment's origin). */
	static float tailStep(String d, int i) {
		return switch (d) {
			case "Fox" -> new float[]{3, 4, 4, 3}[i];
			case "Tabby" -> 4;
			case "Wolf" -> new float[]{4, 5, 4, 3}[i];
			case "Devil" -> new float[]{5, 5, 5, 1}[i];
			case "Bunny Puff" -> new float[]{1, 5, 0, 0}[i];
			case "Raccoon" -> 3;
			case "Mermaid" -> new float[]{4, 4, 3, 2}[i];
			case "Neon Pulse" -> 4;
			case "Dragon" -> new float[]{4, 4, 4, 2}[i];
			default -> new float[]{4, 4, 4, 3}[i];
		};
	}

	static List<Box> tail(String d, int i) {
		List<Box> b = new ArrayList<>();
		int tx = TAIL_TEX[i][0], ty = TAIL_TEX[i][1];
		int[] w, h;
		int len = (int) tailStep(d, i);
		switch (d) {
			case "Fox" -> {
				w = new int[]{3, 4, 4, 2};
				h = w;
			}
			case "Tabby" -> {
				w = new int[]{2, 2, 2, 2};
				h = w;
			}
			case "Wolf" -> {
				w = new int[]{3, 3, 3, 3};
				h = new int[]{4, 4, 4, 3};
			}
			case "Dragon" -> {
				w = new int[]{3, 3, 2, 2};
				h = w;
			}
			case "Devil" -> {
				w = new int[]{1, 1, 1, 1};
				h = w;
			}
			case "Bunny Puff" -> {
				w = new int[]{1, 5, 1, 1};
				h = w;
				if (i >= 2) len = 1;
			}
			case "Raccoon" -> {
				w = new int[]{4, 4, 4, 3};
				h = w;
			}
			case "Mermaid" -> {
				w = new int[]{3, 2, 2, 1};
				h = new int[]{3, 3, 2, 1};
			}
			case "Skunk" -> {
				w = new int[]{3, 4, 4, 3};
				h = new int[]{4, 6, 6, 5};
			}
			case "Neon Pulse" -> {
				w = new int[]{2, 2, 2, 3};
				h = w;
				len = i == 3 ? 3 : 3;
			}
			default -> {
				w = new int[]{3, 3, 2, 3};
				h = w;
			}
		}
		int bl = d.equals("Bunny Puff") ? (i == 1 ? 5 : 1) : len;
		int yShift = d.equals("Skunk") ? -1 : 0;
		a(b, "seg", tx, ty, -w[i] / 2f, -h[i] / 2f + yShift, 0, w[i], h[i], bl);
		switch (d) {
			case "Dragon" -> {
				if (i < 3) a(b, "spike", 14, 0, -0.5F, -h[i] / 2f - 2, 1, 1, 2, 2);
				else {
					a(b, "spade", 26, 0, -2.5F, -0.5F, 2, 5, 1, 3);
					a(b, "spade2", 26, 0, -1.5F, -0.5F, 5, 3, 1, 1);
				}
			}
			case "Devil" -> {
				if (i == 3) {
					a(b, "spade", 26, 0, -2, -0.5F, 1, 4, 1, 3);
					a(b, "spade2", 26, 0, -1, -0.5F, 4, 2, 1, 1);
				}
			}
			case "Mermaid" -> {
				if (i == 3) {
					r(b, "flukeR", 26, 0, -4, -0.5F, 0, 4, 1, 3, 0, 0, 0, 0, 25, 0);
					r(b, "flukeL", 26, 0, 0, -0.5F, 0, 4, 1, 3, 0, 0, 0, 0, -25, 0);
				} else if (i == 1) {
					a(b, "finU", 0, 7, -0.5F, -3.5F, 1, 1, 2, 2);
				}
			}
			case "Skunk" -> {
				if (i < 3) a(b, "stripe", 26, 0, -0.5F, -h[i] / 2f - 1.5F + yShift, 0, 1, 1, bl);
			}
			case "Neon Pulse" -> {
				if (i < 3) a(b, "node", 26, 0, -1.5F, -0.5F, 3, 3, 1, 1);
			}
			case "Wolf" -> {
				if (i == 3) a(b, "tip", 26, 0, -1, -1.5F, 3, 2, 3, 1);
			}
			default -> {
			}
		}
		return b;
	}

	// ------------------------------------------------------------------ pets

	static List<Box> petBody(String d) {
		List<Box> b = new ArrayList<>();
		switch (d) {
			case "Tabby Cat" -> {
				a(b, "body", 0, 0, -1.5F, -3, -2.5F, 3, 3, 5);
				legs(b, 1.5F, 2);
			}
			case "Red Fox" -> {
				a(b, "body", 0, 0, -1.5F, -3, -3, 3, 3, 6);
				legs(b, 1.5F, 2.5F);
				a(b, "chest", 0, 0, -1.5F, -2, -3.5F, 3, 2, 1);
			}
			case "Snowy Owl" -> {
				a(b, "body", 0, 0, -2, -5, -1.5F, 4, 5, 3);
				a(b, "wingR", 0, 0, -3, -4, -1, 1, 3, 3);
				a(b, "wingL", 0, 0, 2, -4, -1, 1, 3, 3);
				a(b, "foot", 0, 0, -1, 0, -2, 2, 1, 2);
			}
			case "Penguin" -> {
				a(b, "body", 0, 0, -2, -5, -1.5F, 4, 5, 3);
				a(b, "belly", 18, 0, -1, -4, -2, 2, 3, 1);
				r(b, "flipR", 0, 0, -0.5F, 0, -1, 1, 3, 2, -2, -4.5F, 0, 0, 0, 20);
				r(b, "flipL", 0, 0, -0.5F, 0, -1, 1, 3, 2, 2, -4.5F, 0, 0, 0, -20);
				a(b, "feetR", 0, 0, -2, 0, -2.5F, 2, 1, 2);
				a(b, "feetL", 0, 0, 0, 0, -2.5F, 2, 1, 2);
			}
			case "Bunny" -> {
				a(b, "body", 0, 0, -1.5F, -3, -2, 3, 3, 4);
				a(b, "haunch", 0, 0, -2, -3, 0.5F, 4, 2, 2);
				a(b, "foot", 0, 0, -1.5F, 0, -2, 3, 1, 1);
			}
			case "Frog" -> {
				a(b, "body", 0, 0, -2.5F, -2, -2, 5, 2, 4);
				a(b, "legR", 0, 0, -3.5F, -1.5F, 0.5F, 1, 1, 2);
				a(b, "legL", 0, 0, 2.5F, -1.5F, 0.5F, 1, 1, 2);
				a(b, "footR", 0, 0, -3.5F, 0, -1, 1, 1, 2);
				a(b, "footL", 0, 0, 2.5F, 0, -1, 1, 1, 2);
			}
			case "Panda" -> {
				a(b, "body", 0, 0, -2.5F, -3.5F, -2, 5, 3, 4);
				a(b, "belt", 18, 0, -2.5F, -3.5F, -0.5F, 5, 3, 1);
				legs(b, 2, 1.5F);
			}
			case "Parrot" -> {
				a(b, "body", 0, 0, -1.5F, -5, -1.5F, 3, 5, 3);
				a(b, "wingR", 0, 0, -2.5F, -4.5F, -1, 1, 3, 3);
				a(b, "wingL", 0, 0, 1.5F, -4.5F, -1, 1, 3, 3);
				a(b, "foot", 0, 0, -1, 0, -1, 2, 1, 1);
			}
			case "Hatchling" -> {
				a(b, "body", 0, 0, -1.5F, -3, -2.5F, 3, 3, 5);
				r(b, "wingR", 0, 0, -3, -0.5F, -1, 3, 1, 3, -1.5F, -3, 0, 0, 0, -30);
				r(b, "wingL", 0, 0, 0, -0.5F, -1, 3, 1, 3, 1.5F, -3, 0, 0, 0, 30);
				legs(b, 1.5F, 2);
			}
			case "Raven" -> {
				a(b, "body", 0, 0, -1.5F, -3, -2, 3, 3, 4);
				a(b, "wingR", 0, 0, -2.5F, -3, -1.5F, 1, 2, 4);
				a(b, "wingL", 0, 0, 1.5F, -3, -1.5F, 1, 2, 4);
				a(b, "foot", 0, 0, -1, 0, -1, 2, 1, 1);
			}
			default -> {
				a(b, "body", 0, 0, -2, -3, -2.5F, 4, 3, 5);
				legs(b, 2, 2);
			}
		}
		return b;
	}

	private static void legs(List<Box> b, float x, float z) {
		a(b, "legFR", 0, 0, -x - 0.5F, 0, -z - 0.5F, 1, 1, 1);
		a(b, "legFL", 0, 0, x - 0.5F, 0, -z - 0.5F, 1, 1, 1);
		a(b, "legBR", 0, 0, -x - 0.5F, 0, z - 0.5F, 1, 1, 1);
		a(b, "legBL", 0, 0, x - 0.5F, 0, z - 0.5F, 1, 1, 1);
	}

	/** Head relative to the neck pivot; the skull is x -1.5..1.5, y -3..0, z -2..1. */
	static List<Box> petHead(String d) {
		List<Box> b = new ArrayList<>();
		switch (d) {
			case "Tabby Cat" -> {
				a(b, "skull", 18, 0, -1.5F, -3, -2, 3, 3, 3);
				a(b, "earR", 30, 0, -1.5F, -5, -1, 1, 2, 1);
				a(b, "earL", 30, 0, 0.5F, -5, -1, 1, 2, 1);
			}
			case "Red Fox" -> {
				a(b, "skull", 18, 0, -1.5F, -3, -2, 3, 3, 3);
				a(b, "snout", 30, 0, -1, -1.5F, -4, 2, 1, 2);
				a(b, "earR", 30, 0, -1.5F, -5, -1, 1, 2, 1);
				a(b, "earL", 30, 0, 0.5F, -5, -1, 1, 2, 1);
			}
			case "Snowy Owl" -> {
				a(b, "skull", 18, 0, -2, -3, -2, 4, 3, 3);
				a(b, "beak", 30, 0, -0.5F, -1.5F, -3, 1, 1, 1);
				a(b, "eyeR", 30, 0, -1.5F, -2.5F, -2.5F, 1, 1, 1);
				a(b, "eyeL", 30, 0, 0.5F, -2.5F, -2.5F, 1, 1, 1);
				a(b, "tuftR", 30, 0, -2, -4, -1, 1, 1, 1);
				a(b, "tuftL", 30, 0, 1, -4, -1, 1, 1, 1);
			}
			case "Penguin" -> {
				a(b, "skull", 18, 0, -1.5F, -3, -2, 3, 3, 3);
				a(b, "beak", 30, 0, -0.5F, -1.5F, -4, 1, 1, 2);
				a(b, "cap", 30, 0, -1.5F, -3.5F, -1.5F, 3, 1, 2);
			}
			case "Bunny" -> {
				a(b, "skull", 18, 0, -1.5F, -3, -2, 3, 3, 3);
				a(b, "earR", 30, 0, -1.5F, -7, -0.5F, 1, 4, 1);
				a(b, "earL", 30, 0, 0.5F, -7, -0.5F, 1, 4, 1);
				a(b, "nose", 30, 0, -0.5F, -1.5F, -3, 1, 1, 1);
			}
			case "Frog" -> {
				a(b, "skull", 18, 0, -2, -2, -2, 4, 2, 3);
				a(b, "eyeR", 30, 0, -2, -3, -1.5F, 1, 1, 1);
				a(b, "eyeL", 30, 0, 1, -3, -1.5F, 1, 1, 1);
				a(b, "mouth", 30, 0, -1.5F, -0.5F, -2.5F, 3, 1, 1);
			}
			case "Panda" -> {
				a(b, "skull", 18, 0, -1.5F, -3, -2, 3, 3, 3);
				a(b, "earR", 30, 0, -2, -4, -1, 1, 1, 1);
				a(b, "earL", 30, 0, 1, -4, -1, 1, 1, 1);
				a(b, "patchR", 30, 0, -1.5F, -2.5F, -2.3F, 1, 1, 1);
				a(b, "patchL", 30, 0, 0.5F, -2.5F, -2.3F, 1, 1, 1);
				a(b, "snout", 30, 0, -0.5F, -1.5F, -2.6F, 1, 1, 1);
			}
			case "Parrot" -> {
				a(b, "skull", 18, 0, -1.5F, -3, -2, 3, 3, 3);
				a(b, "beak", 30, 0, -0.5F, -2, -3.5F, 1, 1, 2);
				a(b, "hook", 30, 0, -0.5F, -1, -3.5F, 1, 1, 1);
				a(b, "crest", 30, 0, -0.5F, -5, -1, 1, 2, 1);
			}
			case "Hatchling" -> {
				a(b, "skull", 18, 0, -1.5F, -3, -2, 3, 3, 3);
				a(b, "snout", 30, 0, -1, -1.5F, -4, 2, 1, 2);
				r(b, "hornR", 30, 0, -0.5F, -2, 0, 1, 2, 1, -1, -3, -1, 30, 0, 0);
				r(b, "hornL", 30, 0, -0.5F, -2, 0, 1, 2, 1, 1, -3, -1, 30, 0, 0);
			}
			case "Raven" -> {
				a(b, "skull", 18, 0, -1.5F, -3, -2, 3, 3, 3);
				a(b, "beak", 30, 0, -0.5F, -1.5F, -4, 1, 1, 2);
				a(b, "crest", 30, 0, -0.5F, -4, -0.5F, 1, 1, 2);
			}
			default -> {
				a(b, "skull", 18, 0, -1.5F, -3, -2, 3, 3, 3);
				a(b, "earR", 30, 0, -2, -4, -1, 1, 1, 1);
				a(b, "earL", 30, 0, 1, -4, -1, 1, 1, 1);
				a(b, "snout", 30, 0, -0.5F, -1.5F, -3, 1, 1, 1);
			}
		}
		return b;
	}

	/** Tail relative to its root, pitched up 40 degrees. */
	static List<Box> petTail(String d) {
		List<Box> b = new ArrayList<>();
		switch (d) {
			case "Tabby Cat" -> r(b, "tail", 34, 0, -0.5F, -0.5F, 0, 1, 1, 4, 0, 0, 0, -40, 0, 0);
			case "Red Fox" -> r(b, "tail", 34, 0, -1, -1, 0, 2, 2, 4, 0, 0, 0, -30, 0, 0);
			case "Snowy Owl" -> r(b, "tail", 34, 0, -1.5F, -0.5F, 0, 3, 1, 2, 0, 0, 0, -20, 0, 0);
			case "Penguin" -> r(b, "tail", 34, 0, -1, -0.5F, 0, 2, 1, 1, 0, 0, 0, -10, 0, 0);
			case "Bunny" -> r(b, "tail", 34, 0, -1, -1, 0, 2, 2, 2, 0, 0, 0, -10, 0, 0);
			case "Frog" -> r(b, "tail", 34, 0, -0.5F, -0.5F, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0);
			case "Parrot" -> {
				r(b, "tail", 34, 0, -1, -0.5F, 0, 2, 1, 5, 0, 0, 0, -20, 0, 0);
				r(b, "tail2", 34, 0, -0.5F, -1, 1, 1, 1, 4, 0, 0, 0, -30, 0, 0);
			}
			case "Hatchling" -> {
				r(b, "tail", 34, 0, -0.5F, -0.5F, 0, 1, 1, 4, 0, 0, 0, -30, 0, 0);
				r(b, "spA", 34, 0, -0.5F, -1.5F, 1, 1, 1, 1, 0, 0, 0, -30, 0, 0);
				r(b, "spB", 34, 0, -0.5F, -1.5F, 3, 1, 1, 1, 0, 0, 0, -30, 0, 0);
			}
			case "Raven" -> r(b, "tail", 34, 0, -1.5F, -0.5F, 0, 3, 1, 3, 0, 0, 0, -25, 0, 0);
			default -> r(b, "tail", 34, 0, -0.5F, -0.5F, 0, 1, 1, 3, 0, 0, 0, -40, 0, 0);
		}
		return b;
	}

	// ------------------------------------------------------------------ scarves

	static List<Box> scarfCollar(String d) {
		List<Box> b = new ArrayList<>();
		switch (d) {
			case "Red Wool" -> {
				a(b, "collar", 0, 0, -4.5F, -1.5F, -2.5F, 9, 4, 5);
			}
			case "Candy Stripe" -> {
				a(b, "collar", 0, 0, -4.5F, -1, -2.5F, 9, 3, 5);
				a(b, "twist", 0, 0, -1.5F, -1, -3.5F, 3, 3, 1);
			}
			case "Rainbow Knit" -> {
				a(b, "collar", 0, 0, -4.5F, -1, -2.5F, 9, 3, 5);
				a(b, "ridge", 0, 0, -4, -2, -2, 8, 1, 4);
			}
			case "Cozy Plaid" -> {
				a(b, "collar", 0, 0, -4.5F, -1, -2.5F, 9, 3, 5);
				a(b, "knot", 0, 0, 1, 0, -3.5F, 3, 3, 1);
			}
			case "Midnight Silk" -> {
				a(b, "collar", 0, 0, -4.5F, -0.5F, -2.5F, 9, 2, 5);
			}
			case "Autumn Wrap" -> {
				a(b, "collar", 0, 0, -4.5F, -1, -2.5F, 9, 3, 5);
				a(b, "wrap2", 0, 0, -5, 1, -3, 10, 2, 6);
			}
			case "Starry Night" -> {
				a(b, "collar", 0, 0, -4.5F, -1, -2.5F, 9, 3, 5);
				a(b, "star1", 0, 0, -3, -1.5F, -3, 1, 1, 1);
				a(b, "star2", 0, 0, 2, 0, -3, 1, 1, 1);
			}
			case "Sakura Wrap" -> {
				a(b, "collar", 0, 0, -4.5F, -1, -2.5F, 9, 3, 5);
				a(b, "knot", 0, 0, -1, 0, -3.5F, 2, 2, 1);
				r(b, "bowR", 0, 0, -3.5F, -0.5F, -0.5F, 3, 2, 1, -1, 1, -3.5F, 0, 0, 20);
				r(b, "bowL", 0, 0, 0.5F, -0.5F, -0.5F, 3, 2, 1, 1, 1, -3.5F, 0, 0, -20);
			}
			case "Frost Fluff" -> {
				a(b, "collar", 0, 0, -4.5F, -1, -2.5F, 9, 3, 5);
				a(b, "puffR", 0, 0, -5, -1.5F, -1, 2, 2, 3);
				a(b, "puffL", 0, 0, 3, -1.5F, -1, 2, 2, 3);
				a(b, "puffF", 0, 0, -2, -2, -3, 4, 2, 1);
			}
			case "Ocean Blue" -> {
				a(b, "collar", 0, 0, -4.5F, -1, -2.5F, 9, 3, 5);
				a(b, "wave", 0, 0, -4.5F, 2, -2.5F, 9, 1, 5);
			}
			default -> a(b, "collar", 0, 0, -4.5F, -1, -2.5F, 9, 3, 5);
		}
		return b;
	}

	/** A hanging end: {@code long} picks the long end. Top at y = 0, centred on x. */
	static List<Box> scarfFlap(String d, boolean tall) {
		List<Box> b = new ArrayList<>();
		int tx = tall ? 0 : 8;
		int len = tall ? 8 : 5;
		switch (d) {
			case "Red Wool", "Cozy Plaid" -> {
				a(b, "flap", tx, 8, -1.5F, 0, -0.5F, 3, len, 1);
				a(b, "f1", tx, 8, -1.5F, len, -0.5F, 1, 1, 1);
				a(b, "f2", tx, 8, 0.5F, len, -0.5F, 1, 1, 1);
			}
			case "Candy Stripe" -> {
				a(b, "flapA", tx, 8, -1.5F, 0, -0.5F, 3, len / 2, 1);
				a(b, "flapB", tx, 8, -0.5F, len / 2f, -0.5F, 3, len - len / 2, 1);
			}
			case "Rainbow Knit" -> {
				a(b, "flap", tx, 8, -2, 0, -0.5F, 4, len + 1, 1);
			}
			case "Midnight Silk" -> {
				a(b, "flap", tx, 8, -1, 0, -0.5F, 2, len + 3, 1);
			}
			case "Autumn Wrap" -> {
				a(b, "flap", tx, 8, -1.5F, 0, -0.5F, 3, len - 1, 2);
			}
			case "Starry Night" -> {
				a(b, "flap", tx, 8, -1.5F, 0, -0.5F, 3, len, 1);
				a(b, "starH", tx, 8, -1.5F, len, -0.5F, 3, 1, 1);
				a(b, "starV", tx, 8, -0.5F, len - 1, -0.5F, 1, 3, 1);
			}
			case "Sakura Wrap" -> {
				a(b, "flap", tx, 8, -1, 0, -0.5F, 2, len - 1, 1);
				a(b, "petalR", tx, 8, -2, len - 1, -0.5F, 2, 2, 1);
				a(b, "petalL", tx, 8, 0, len - 1, -0.5F, 2, 2, 1);
			}
			case "Frost Fluff" -> {
				a(b, "flap", tx, 8, -1.5F, 0, -0.5F, 3, len, 1);
				a(b, "puff", tx, 8, -2, len - 1, -1, 4, 2, 2);
			}
			case "Ocean Blue" -> {
				int seg = len / 3 + 1;
				a(b, "w1", tx, 8, -1.5F, 0, -0.5F, 3, seg, 1);
				a(b, "w2", tx, 8, -0.5F, seg, -0.5F, 3, seg, 1);
				a(b, "w3", tx, 8, -1.5F, seg * 2, -0.5F, 3, seg, 1);
			}
			default -> a(b, "flap", tx, 8, -1.5F, 0, -0.5F, 3, len, 1);
		}
		return b;
	}

	// ------------------------------------------------------------------ backpacks (body coordinates, back at +Z)

	static List<Box> backpack(String d) {
		List<Box> b = new ArrayList<>();
		switch (d) {
			case "Hiker Green" -> {
				a(b, "main", 0, 0, -3.5F, 1.5F, 2, 7, 10, 3);
				a(b, "lid", 0, 12, -4, 0.5F, 1.9F, 8, 3, 4);
				a(b, "roll", 0, 12, -4, 11.5F, 2.5F, 8, 2, 2);
				a(b, "sideR", 0, 19, -5, 5, 2.5F, 1, 4, 2);
				a(b, "sideL", 0, 19, 4, 5, 2.5F, 1, 4, 2);
			}
			case "Leather Brown" -> {
				a(b, "main", 0, 0, -4, 3, 2, 8, 7, 2);
				a(b, "flap", 0, 12, -4, 2.5F, 3.9F, 8, 4, 1);
				a(b, "buckle", 0, 19, -1, 5.5F, 5, 2, 2, 1);
			}
			case "School Red" -> {
				a(b, "main", 0, 0, -4, 1.5F, 2, 8, 9, 3);
				a(b, "pocket", 0, 19, -3, 6, 5, 6, 4, 2);
				a(b, "zip", 0, 12, -3, 5.5F, 5, 6, 1, 1);
				a(b, "handle", 0, 19, -1, 0, 3, 2, 1, 1);
			}
			case "Camo Pack" -> {
				a(b, "main", 0, 0, -3.5F, 2.5F, 2, 7, 8, 3);
				a(b, "roll", 0, 12, -4, 0.5F, 2, 8, 2, 3);
				a(b, "pouchR", 0, 19, -3, 6, 5, 2, 3, 1);
				a(b, "pouchL", 0, 19, 1, 6, 5, 2, 3, 1);
				a(b, "clip", 0, 19, -1, 3, 5, 2, 1, 1);
			}
			case "Cyber Pack" -> {
				a(b, "main", 0, 0, -3, 1.5F, 2, 6, 9, 3);
				a(b, "ventR", 0, 19, -4, 3, 2.5F, 1, 5, 2);
				a(b, "ventL", 0, 19, 3, 3, 2.5F, 1, 5, 2);
				a(b, "core", 0, 12, -1.5F, 4, 5, 3, 3, 1);
				a(b, "ant", 0, 19, 2, -3, 3, 1, 4, 1);
				a(b, "cap", 0, 12, -3, 0.5F, 2, 6, 1, 3);
			}
			case "Rainbow Pack" -> {
				a(b, "main", 0, 0, -3, 3, 2, 6, 6, 3);
				a(b, "s1", 0, 19, -3.5F, 4, 5, 7, 1, 1);
				a(b, "s2", 0, 19, -3.5F, 6, 5, 7, 1, 1);
				a(b, "s3", 0, 19, -3.5F, 8, 5, 7, 1, 1);
			}
			case "Star Pack" -> {
				a(b, "main", 0, 0, -2.5F, 3, 2, 5, 5, 2);
				a(b, "up", 0, 12, -0.5F, 1, 2, 1, 2, 2);
				a(b, "left", 0, 12, -4.5F, 4, 2, 2, 1, 2);
				a(b, "right", 0, 12, 2.5F, 4, 2, 2, 1, 2);
				a(b, "legL", 0, 12, -3, 8, 2, 1, 2, 2);
				a(b, "legR", 0, 12, 2, 8, 2, 1, 2, 2);
			}
			case "Galaxy Pack" -> {
				a(b, "main", 0, 0, -3.5F, 1.5F, 2, 7, 9, 3);
				a(b, "lid", 0, 12, -4, 0.5F, 1.9F, 8, 3, 4);
				r(b, "ring", 0, 19, -5, -0.5F, -0.5F, 10, 1, 1, 0, 6, 6, 0, 0, 25);
				a(b, "moon", 0, 19, 4, 0, 5.5F, 2, 2, 2);
			}
			case "Candy Pack" -> {
				a(b, "main", 0, 0, -4, 3.5F, 2, 8, 6, 4);
				a(b, "swirl", 0, 19, -2, 4.5F, 6, 4, 4, 1);
				a(b, "cane", 0, 12, 2, -1, 3, 1, 4, 1);
				a(b, "hook", 0, 12, 0, -2, 3, 3, 1, 1);
			}
			case "Treasure" -> {
				a(b, "main", 0, 0, -4, 4.5F, 2, 8, 6, 4);
				a(b, "lid", 0, 12, -4, 2.5F, 2, 8, 2, 4);
				a(b, "arch", 0, 12, -3, 1.5F, 2.5F, 6, 1, 3);
				a(b, "lock", 0, 19, -1, 4, 6, 2, 3, 1);
				a(b, "bandR", 0, 19, -4.5F, 2.5F, 2, 1, 8, 4);
				a(b, "bandL", 0, 19, 3.5F, 2.5F, 2, 1, 8, 4);
			}
			case "Glacier Bear" -> {
				a(b, "main", 0, 0, -3.5F, 1.5F, 2, 7, 9, 3);
				a(b, "lid", 0, 12, -4, 0.5F, 1.9F, 8, 3, 4);
				a(b, "pocket", 0, 19, -2.5F, 6, 5, 5, 3, 1);
				a(b, "earR", 0, 19, -4, -1.5F, 3, 2, 2, 1);
				a(b, "earL", 0, 19, 2, -1.5F, 3, 2, 2, 1);
			}
			default -> {
				a(b, "main", 0, 0, -3.5F, 1.5F, 2, 7, 9, 3);
				a(b, "lid", 0, 12, -4, 0.5F, 1.9F, 8, 3, 4);
				a(b, "pocket", 0, 19, -2.5F, 6, 5, 5, 3, 1);
			}
		}
		a(b, "strapR", 24, 0, -3, 0, -2.5F, 1, 8, 1);
		a(b, "strapL", 24, 0, 2, 0, -2.5F, 1, 8, 1);
		a(b, "bridgeR", 28, 0, -3, -0.5F, -2.5F, 1, 1, 5);
		a(b, "bridgeL", 28, 0, 2, -0.5F, -2.5F, 1, 1, 5);
		return b;
	}

	// ------------------------------------------------------------------ bracelets (arm coordinates, centred on cx)

	static List<Box> bracelet(String d, float cx) {
		List<Box> b = new ArrayList<>();
		float x0 = cx - 2.5F;
		switch (d) {
			case "Gold Cuff" -> a(b, "ring", 0, 0, x0, 7.5F, -2.5F, 5, 3, 5);
			case "Silver Gem" -> {
				a(b, "ring", 0, 0, x0, 8, -2.5F, 5, 2, 5);
				a(b, "gem", 0, 0, cx - 1, 8, -3.5F, 2, 2, 1);
			}
			case "Rainbow" -> {
				a(b, "r1", 0, 0, x0, 7, -2.5F, 5, 1, 5);
				a(b, "r2", 0, 0, x0 - 0.3F, 8, -2.8F, 5, 1, 5);
				a(b, "r3", 0, 0, x0, 9, -2.5F, 5, 1, 5);
				a(b, "r4", 0, 0, x0 - 0.3F, 10, -2.8F, 5, 1, 5);
			}
			case "Leather" -> {
				a(b, "ring", 0, 0, x0, 7, -2.5F, 5, 4, 5);
				a(b, "buckle", 0, 0, cx - 1, 8, -3.3F, 2, 2, 1);
			}
			case "Neon" -> {
				a(b, "r1", 0, 0, x0, 7, -2.5F, 5, 1, 5);
				a(b, "r2", 0, 0, x0, 10, -2.5F, 5, 1, 5);
				a(b, "core", 0, 0, cx - 1, 8.5F, -2, 2, 1, 4);
			}
			case "Gold Chain" -> {
				for (int i = 0; i < 4; i++) {
					a(b, "lA" + i, 0, 0, x0 + i * 1.4F, 8, -2.5F, 1, 1, 1);
					a(b, "lB" + i, 0, 0, x0 + i * 1.4F, 9, 1.5F, 1, 1, 1);
				}
				a(b, "sideA", 0, 0, x0, 8.5F, -1.5F, 1, 1, 3);
				a(b, "sideB", 0, 0, x0 + 4, 8.5F, -1.5F, 1, 1, 3);
			}
			case "Spiked" -> {
				a(b, "ring", 0, 0, x0, 8, -2.5F, 5, 2, 5);
				a(b, "s1", 0, 0, x0 - 0.5F, 8.5F, -3.5F, 1, 1, 1);
				a(b, "s2", 0, 0, x0 + 4.5F, 8.5F, -3.5F, 1, 1, 1);
				a(b, "s3", 0, 0, x0 - 0.5F, 8.5F, 2.5F, 1, 1, 1);
				a(b, "s4", 0, 0, x0 + 4.5F, 8.5F, 2.5F, 1, 1, 1);
				a(b, "s5", 0, 0, cx - 0.5F, 7, -3.5F, 1, 1, 1);
			}
			case "Friendship" -> {
				a(b, "ring", 0, 0, x0, 8, -2.5F, 5, 1, 5);
				a(b, "t1", 0, 0, x0 + 0.5F, 9, -2.5F, 1, 3, 1);
				a(b, "t2", 0, 0, cx - 0.5F, 9, -2.5F, 1, 4, 1);
				a(b, "t3", 0, 0, x0 + 3.5F, 9, -2.5F, 1, 2, 1);
			}
			case "Tech Watch" -> {
				a(b, "ring", 0, 0, x0, 8, -2.5F, 5, 2, 5);
				a(b, "face", 0, 0, cx - 1.5F, 7.5F, -3.5F, 3, 3, 1);
				a(b, "screen", 0, 0, cx - 1, 8, -4, 2, 2, 1);
			}
			case "Ruby Band" -> {
				a(b, "ring", 0, 0, x0, 8, -2.5F, 5, 2, 5);
				a(b, "gem", 0, 0, cx - 1.5F, 7.5F, -4, 3, 3, 2);
			}
			case "Diamond" -> {
				a(b, "ring", 0, 0, x0, 8, -2.5F, 5, 2, 5);
				r(b, "gem", 0, 0, -1, -1, -1, 2, 2, 2, cx, 9, -3.5F, 0, 0, 45);
			}
			case "Obsidian" -> {
				a(b, "ring", 0, 0, x0 - 0.5F, 7.5F, -3, 6, 3, 6);
				r(b, "shard", 0, 0, -0.5F, -3, -0.5F, 1, 3, 1, cx, 8, -3, 0, 0, 20);
			}
			case "Jade" -> {
				a(b, "ring", 0, 0, x0, 8, -2.5F, 5, 2, 5);
				a(b, "disc", 0, 0, cx - 1.5F, 7.5F, -3.5F, 3, 3, 1);
				a(b, "hole", 0, 0, cx - 0.5F, 8.5F, -4, 1, 1, 1);
			}
			case "Cyber Cuff" -> {
				a(b, "ring", 0, 0, x0, 8, -2.5F, 5, 2, 5);
				a(b, "led", 0, 0, x0, 7, -1, 5, 1, 2);
				a(b, "finR", 0, 0, x0 - 1, 8.5F, -0.5F, 1, 3, 1);
				a(b, "finL", 0, 0, x0 + 5, 8.5F, -0.5F, 1, 3, 1);
			}
			case "Pearl Strand" -> {
				for (int i = 0; i < 4; i++) {
					a(b, "pF" + i, 0, 0, x0 + i * 1.4F, 8, -3, 1, 2, 1);
					a(b, "pB" + i, 0, 0, x0 + i * 1.4F, 8, 2, 1, 2, 1);
				}
				a(b, "pR", 0, 0, x0 - 0.5F, 8, -1, 1, 2, 2);
				a(b, "pL", 0, 0, x0 + 4.5F, 8, -1, 1, 2, 2);
			}
			case "Bead Pop" -> {
				for (int i = 0; i < 2; i++) {
					a(b, "bF" + i, 0, 0, x0 + i * 3, 7.5F, -3, 2, 3, 2);
					a(b, "bB" + i, 0, 0, x0 + i * 3, 7.5F, 1, 2, 3, 2);
				}
				a(b, "bR", 0, 0, x0 - 0.5F, 8, -1, 2, 2, 2);
				a(b, "bL", 0, 0, x0 + 3.5F, 8, -1, 2, 2, 2);
			}
			case "Runic" -> {
				a(b, "ring", 0, 0, x0, 8, -2.5F, 5, 2, 5);
				a(b, "r1", 0, 0, x0 + 0.5F, 7, -3, 1, 1, 1);
				a(b, "r2", 0, 0, cx - 0.5F, 10, -3, 1, 1, 1);
				a(b, "r3", 0, 0, x0 + 3.5F, 7, -3, 1, 1, 1);
				a(b, "r4", 0, 0, cx - 0.5F, 8, -3.5F, 1, 2, 1);
			}
			default -> a(b, "ring", 0, 0, x0, 8, -2.5F, 5, 2, 5);
		}
		return b;
	}

	// ------------------------------------------------------------------ halos (head coordinates, spins about the head axis)

	static List<Box> halo(String d) {
		List<Box> b = new ArrayList<>();
		switch (d) {
			case "Gold Halo" -> {
				for (int i = 0; i < 8; i++) bar(b, "o" + i, 0, 0, 5, 2, 2, 4.8F, -11, i * 45, 0, 0);
			}
			case "Ice Halo" -> {
				for (int i = 0; i < 8; i++) {
					bar(b, "o" + i, 0, 0, 5, 1, 2, 4.8F, -11, i * 45, 0, 0);
					bar(b, "i" + i, 0, 0, 1, 2, 1, 4.8F, -11, i * 45, 1, 0);
				}
			}
			case "Neon Halo" -> {
				for (int i = 0; i < 12; i++) bar(b, "o" + i, 0, 0, 3, 1, 1, 5.2F, -11, i * 30, 0, 0);
			}
			case "Thorn" -> {
				for (int i = 0; i < 8; i++) {
					bar(b, "o" + i, 0, 0, 5, 1, 2, 4.8F, -11, i * 45, 0, 0);
					if ((i & 1) == 0) bar(b, "t" + i, 0, 0, 1, 2, 1, 4.8F, -11, i * 45, -2, 0);
				}
			}
			case "Laurel" -> {
				for (int i = 0; i < 12; i++) {
					double deg = 60 + i * 20;
					if (deg > 300) break;
					bar(b, "l" + i, 0, 0, 2, 1, 3, 5F, -11, (float) deg + 0, 0, (i & 1) == 0 ? 30 : -30);
				}
			}
			case "Rainbow" -> {
				for (int i = 0; i < 8; i++) {
					bar(b, "a" + i, 0, 0, 4, 1, 1, 4F, -11, i * 45, 0, 0);
					bar(b, "b" + i, 0, 0, 5, 1, 1, 5.6F, -11, i * 45 + 22, 0, 0);
				}
			}
			case "Neon Lime" -> {
				a(b, "f", 0, 0, -5, -11, -5, 10, 1, 1);
				a(b, "bk", 0, 0, -5, -11, 4, 10, 1, 1);
				a(b, "l", 0, 0, -5, -11, -4, 1, 1, 8);
				a(b, "r", 0, 0, 4, -11, -4, 1, 1, 8);
			}
			case "Eclipse" -> {
				for (int i = 0; i < 8; i++) bar(b, "o" + i, 0, 0, 5, 1, 2, 4.8F, -11, i * 45, 0, 0);
				a(b, "disc", 0, 0, -3, -10, -3, 6, 1, 6);
			}
			case "Storm" -> {
				for (int i = 0; i < 8; i++) bar(b, "o" + i, 0, 0, 5, 1, 2, 4.8F, -11, i * 45, 0, 0);
				for (int i = 0; i < 4; i++) {
					bar(b, "z" + i, 0, 0, 1, 3, 1, 4.8F, -11, i * 90 + 22, 1, 0);
					bar(b, "y" + i, 0, 0, 1, 2, 1, 5.5F, -11, i * 90 + 22, 3, 0);
				}
			}
			case "Flame" -> {
				for (int i = 0; i < 8; i++) {
					bar(b, "o" + i, 0, 0, 5, 1, 2, 4.8F, -11, i * 45, 0, 0);
					bar(b, "f" + i, 0, 0, 2, 2 + (i % 3), 1, 4.8F, -11, i * 45, -2 - (i % 3), 0);
				}
			}
			case "Crystal" -> {
				for (int i = 0; i < 8; i++) {
					bar(b, "o" + i, 0, 0, 4, 1, 1, 4.8F, -11, i * 45, 0, 0);
					bar(b, "c" + i, 0, 0, 1, 3, 1, 4.8F, -11, i * 45 + 22, -3, i % 2 == 0 ? 25 : -25);
				}
			}
			case "Void" -> {
				for (int i = 0; i < 12; i++) bar(b, "o" + i, 0, 0, 3, 1, 1, 6.5F, -11, i * 30, 0, 0);
				for (int i = 0; i < 4; i++) bar(b, "i" + i, 0, 0, 3, 1, 1, 3F, -11, i * 90, 0, 0);
			}
			case "Candy Cane" -> {
				for (int i = 0; i < 8; i++) bar(b, "o" + i, 0, 0, 5, 1, 2, 4.8F, -11, i * 45, i % 2 == 0 ? -1 : 0, 0);
				a(b, "bow", 0, 0, -2, -13, -6, 4, 2, 1);
				a(b, "knot", 0, 0, -0.5F, -12.5F, -6, 1, 1, 1);
			}
			case "Sakura" -> {
				for (int i = 0; i < 4; i++) {
					bar(b, "o" + i, 0, 0, 5, 1, 2, 4.8F, -11, i * 90 + 45, 0, 0);
					bar(b, "p" + i, 0, 0, 2, 1, 2, 4.8F, -11, i * 90, -1, 0);
					bar(b, "q" + i, 0, 0, 1, 1, 1, 4.8F, -11, i * 90, -2, 0);
					bar(b, "r" + i, 0, 0, 3, 1, 1, 4.8F, -11, i * 90, 0, 0);
				}
			}
			case "Aurora" -> {
				for (int i = 0; i < 12; i++) bar(b, "o" + i, 0, 0, 3, 1, 1, 5F, -11, i * 30, i % 2 == 0 ? -1 : 1, 0);
			}
			case "Starlight" -> {
				for (int i = 0; i < 8; i++) bar(b, "o" + i, 0, 0, 3, 1, 1, 4.8F, -11, i * 45, 0, 0);
				for (int i = 0; i < 4; i++) {
					bar(b, "v" + i, 0, 0, 1, 3, 1, 4.8F, -11, i * 90 + 22, -1, 0);
					bar(b, "h" + i, 0, 0, 3, 1, 1, 4.8F, -11, i * 90 + 22, 0, 0);
				}
			}
			case "Holy Light" -> {
				for (int i = 0; i < 6; i++) bar(b, "o" + i, 0, 0, 3, 1, 1, 3F, -11, i * 60, 0, 0);
				for (int i = 0; i < 8; i++) bar(b, "ray" + i, 0, 0, 1, 1, 3, 6.5F, -11, i * 45, 0, 0);
			}
			default -> {
				for (int i = 0; i < 8; i++) bar(b, "bar" + i, 0, 0, 5, 1, 2, 4.8F, -11, i * 45, 0, 0);
			}
		}
		return b;
	}
}
