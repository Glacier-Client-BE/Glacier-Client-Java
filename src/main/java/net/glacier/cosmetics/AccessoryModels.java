package net.glacier.cosmetics;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Accessory geometry, baked once on first use and reused every frame. Each returned root has a single child part
 * holding the cubes. Coordinates are in model pixels and relative to the player part the accessory is drawn on:
 * head pivot for bandana, glasses, halo and ears; body pivot for wings; arm pivots for bracelets.
 */
final class AccessoryModels {
	private AccessoryModels() {
	}

	static ModelPart bandana() {
		return Parts.BANDANA;
	}

	static ModelPart glasses() {
		return Parts.GLASSES;
	}

	static ModelPart braceletRight() {
		return Parts.BRACELET_RIGHT;
	}

	static ModelPart braceletLeft() {
		return Parts.BRACELET_LEFT;
	}

	static ModelPart halo() {
		return Parts.HALO;
	}

	private static BedrockModel glacierHalo;
	private static boolean glacierHaloLoaded;
	private static PolyMesh glacierMask;
	private static boolean glacierMaskLoaded;
	private static PolyMesh glacierCape;
	private static boolean glacierCapeLoaded;

	/** The pack halo (square ring bone "halo" under "glacier_halo_base"), or null if its file is missing. */
	static BedrockModel glacierHalo() {
		if (!glacierHaloLoaded) {
			glacierHaloLoaded = true;
			glacierHalo = BedrockModel.load("/assets/glacier/models/cosmetics/halo_glacier.geo.json");
		}
		return glacierHalo;
	}

	/** The pack mask poly mesh in head coordinates, or null if its file is missing. */
	static PolyMesh glacierMask() {
		if (!glacierMaskLoaded) {
			glacierMaskLoaded = true;
			glacierMask = PolyMesh.load("/assets/glacier/models/cosmetics/mask_glacier.mesh.json");
		}
		return glacierMask;
	}

	/** The pack cape (16 stacked segments, per-face UVs on the 640x670 sheet) in body coordinates, hanging from y = 0, z = 2. */
	static PolyMesh glacierCape() {
		if (!glacierCapeLoaded) {
			glacierCapeLoaded = true;
			glacierCape = PolyMesh.load("/assets/glacier/models/cosmetics/cape_glacier.mesh.json");
		}
		return glacierCape;
	}

	static ModelPart ears() {
		return Parts.EARS;
	}

	/** Backpack in body coordinates: sits on the back (+Z), straps over the shoulders and down the chest. */
	static ModelPart backpack() {
		return Parts.BACKPACK;
	}

	/** Hat in head coordinates: crown, flat brim, top ornament and a cap bill. Designs hide the pieces they do not use. */
	static ModelPart hat() {
		return Parts.HAT;
	}

	/** Tail segment 0..3, each extending along +Z from its own origin; the layer chains them with a pose per segment. */
	static ModelPart tailSegment(int i) {
		return Parts.TAIL[i];
	}

	/** Length in model pixels of tail segment i, to step the pose to the next segment. */
	static float tailLength(int i) {
		return TAIL_LEN[i];
	}

	/** Pet body, centred on the pet origin (x = 0 is the shoulder centre, y = 0 the top of the arm). */
	static ModelPart petBody() {
		return Parts.PET_BODY;
	}

	/** Pet head with ears, relative to its neck pivot (0, -3, -2.5 from the pet origin). */
	static ModelPart petHead() {
		return Parts.PET_HEAD;
	}

	/** Pet tail relative to its root (0, -1.5, 2.5 from the pet origin), already pitched up. */
	static ModelPart petTail() {
		return Parts.PET_TAIL;
	}

	/** Neck collar in body coordinates. */
	static ModelPart scarfCollar() {
		return Parts.SCARF_COLLAR;
	}

	/** Long hanging scarf end, relative to its top pivot. */
	static ModelPart scarfFlapA() {
		return Parts.SCARF_A;
	}

	/** Short hanging scarf end, relative to its top pivot. */
	static ModelPart scarfFlapB() {
		return Parts.SCARF_B;
	}

	/** Face plate plus head band in head coordinates. */
	static ModelPart mask() {
		return Parts.MASK;
	}


	/** One box of a variant model: texture offset, origin, size, pivot and rotation in degrees (x, y, z). */
	private record Box(String name, int tx, int ty, float x, float y, float z, int w, int h, int d,
					   float px, float py, float pz, float rx, float ry, float rz) {
		static Box of(String name, int tx, int ty, float x, float y, float z, int w, int h, int d) {
			return new Box(name, tx, ty, x, y, z, w, h, d, 0, 0, 0, 0, 0, 0);
		}

		Box rot(float px, float py, float pz, float rx, float ry, float rz) {
			return new Box(name, tx, ty, x, y, z, w, h, d, px, py, pz, rx, ry, rz);
		}
	}

	private static ModelPart bakeBoxes(int texW, int texH, java.util.List<Box> boxes) {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition holder = mesh.getRoot().addOrReplaceChild("accessory", CubeListBuilder.create(), PartPose.ZERO);
		float r = (float) (Math.PI / 180.0);
		for (Box b : boxes) {
			holder.addOrReplaceChild(b.name(), CubeListBuilder.create().texOffs(b.tx(), b.ty()).addBox(b.x(), b.y(), b.z(), b.w(), b.h(), b.d()),
					PartPose.offsetAndRotation(b.px(), b.py(), b.pz(), b.rx() * r, b.ry() * r, b.rz() * r));
		}
		return LayerDefinition.create(mesh, texW, texH).bakeRoot();
	}

	private static final java.util.Map<String, ModelPart> VARIANTS = new java.util.HashMap<>();

	/** Hat geometry of one design (head coordinates, the head top is y = -8); every hat has its own silhouette. */
	static ModelPart hat(String design) {
		return VARIANTS.computeIfAbsent("hat/" + design, k -> bakeBoxes(64, 32, hatBoxes(design)));
	}

	private static java.util.List<Box> hatBoxes(String d) {
		java.util.List<Box> b = new java.util.ArrayList<>();
		switch (d) {
			case "Top Hat" -> {
				b.add(Box.of("brim", 0, 14, -6, -9, -6, 12, 1, 12));
				b.add(Box.of("crown", 0, 0, -4, -17, -4, 8, 8, 8));
				b.add(Box.of("band", 0, 0, -4.2F, -11, -4.2F, 8, 2, 8));
			}
			case "Baseball Cap" -> {
				b.add(Box.of("crown", 0, 0, -4.5F, -12, -4.5F, 9, 4, 9));
				b.add(Box.of("top", 0, 0, -3.5F, -13, -3.5F, 7, 1, 7));
				b.add(Box.of("bill", 36, 5, -4.5F, -9, -9, 9, 1, 5).rot(0, 0, 0, 12, 0, 0));
			}
			case "Royal Crown" -> {
				b.add(Box.of("ring", 0, 0, -4.5F, -11, -4.5F, 9, 3, 9));
				for (int i = 0; i < 4; i++) {
					float cx = (i & 1) == 0 ? -4.5F : 2.5F, cz = (i & 2) == 0 ? -4.5F : 2.5F;
					b.add(Box.of("spike" + i, 36, 0, cx, -14, cz, 2, 3, 2));
				}
				b.add(Box.of("mid", 36, 0, -1, -14, -5, 2, 3, 2));
				b.add(Box.of("gem", 36, 0, -1, -13, -5.5F, 2, 2, 1));
			}
			case "Cozy Beanie", "Glacier Beanie" -> {
				b.add(Box.of("cuff", 0, 0, -4.8F, -10, -4.8F, 10, 2, 10));
				b.add(Box.of("crown", 0, 0, -4.5F, -13, -4.5F, 9, 4, 9));
				b.add(Box.of("top", 0, 0, -3, -14, -3, 6, 1, 6));
				if (d.startsWith("Cozy")) b.add(Box.of("pom", 36, 0, -2, -17, -2, 4, 3, 4));
			}
			case "Wizard" -> {
				b.add(Box.of("brim", 0, 14, -7, -9, -7, 14, 1, 14));
				b.add(Box.of("t1", 0, 0, -4.5F, -13, -4.5F, 9, 4, 9));
				b.add(Box.of("t2", 0, 0, -3, -17, -3, 6, 4, 6));
				b.add(Box.of("t3", 0, 0, -2, -20, -1, 4, 3, 4).rot(0, -17, 0, 14, 0, 0));
				b.add(Box.of("tip", 36, 0, -1, -23, 0.5F, 2, 3, 2).rot(0, -17, 0, 28, 0, 0));
			}
			case "Cowboy" -> {
				b.add(Box.of("brim", 0, 14, -8, -9, -7, 16, 1, 14));
				b.add(Box.of("crown", 0, 0, -4, -14, -4.5F, 8, 5, 9));
				b.add(Box.of("band", 0, 0, -4.2F, -10.5F, -4.7F, 8, 1, 9));
				b.add(Box.of("dent", 36, 0, -1, -15, -4, 2, 1, 8));
			}
			case "Santa" -> {
				b.add(Box.of("cuff", 36, 5, -5, -10, -5, 10, 2, 10));
				b.add(Box.of("crown", 0, 0, -4.5F, -13, -4.5F, 9, 3, 9));
				b.add(Box.of("mid", 0, 0, -3.5F, -15, -3, 7, 2, 7).rot(0, -13, 0, 8, 0, 0));
				b.add(Box.of("drop", 0, 0, -2.5F, -15.5F, 0, 5, 3, 6).rot(0, -13, 0, 70, 0, 0));
				b.add(Box.of("pom", 36, 5, -2, -12, 7, 4, 4, 4));
			}
			case "Chef" -> {
				b.add(Box.of("band", 0, 0, -4.5F, -11, -4.5F, 9, 3, 9));
				b.add(Box.of("puff", 0, 0, -5, -19, -5, 10, 8, 10));
				b.add(Box.of("top", 0, 0, -4, -20, -4, 8, 1, 8));
			}
			case "Captain" -> {
				b.add(Box.of("crown", 0, 0, -4.5F, -12, -4.5F, 9, 4, 9));
				b.add(Box.of("top", 0, 14, -5.5F, -14, -5.5F, 11, 2, 11));
				b.add(Box.of("badge", 36, 0, -1.5F, -12, -5.5F, 3, 2, 1));
				b.add(Box.of("bill", 36, 5, -4.5F, -9, -8.5F, 9, 1, 4));
			}
			case "Party Hat" -> {
				b.add(Box.of("c1", 0, 0, -4, -12, -4, 8, 3, 8));
				b.add(Box.of("c2", 0, 0, -3, -15, -3, 6, 3, 6));
				b.add(Box.of("c3", 0, 0, -2, -18, -2, 4, 3, 4));
				b.add(Box.of("c4", 36, 0, -1, -20, -1, 2, 2, 2));
				b.add(Box.of("pom", 36, 5, -1.5F, -22, -1.5F, 3, 2, 3));
			}
			default -> {
				b.add(Box.of("crown", 0, 0, -4.5F, -13, -4.5F, 9, 5, 9));
				b.add(Box.of("brim", 0, 14, -6, -9, -6, 12, 1, 12));
				b.add(Box.of("orn", 36, 0, -1.5F, -15, -1.5F, 3, 2, 3));
			}
		}
		return b;
	}

	/** Ear / horn geometry of one design (head coordinates), mirrored left and right. */
	static ModelPart ears(String design) {
		return VARIANTS.computeIfAbsent("ears/" + design, k -> bakeBoxes(16, 8, earBoxes(design)));
	}

	private static String earStyle(String d) {
		return switch (d) {
			case "Glacier Bear", "Bear", "Panda", "Mouse", "Raccoon" -> "round";
			case "Fox", "Wolf", "Husky" -> "tall";
			case "Bunny" -> "bunny";
			case "Devil", "Dragon" -> "horn";
			case "Elf" -> "elf";
			case "Robot" -> "antenna";
			case "Frog" -> "eyes";
			case "Pug" -> "drop";
			case "Deer" -> "antler";
			default -> "cat";
		};
	}

	private static java.util.List<Box> earBoxes(String d) {
		java.util.List<Box> b = new java.util.ArrayList<>();
		String style = earStyle(d);
		for (int side = -1; side <= 1; side += 2) {
			String n = side < 0 ? "R" : "L";
			float s = side;
			switch (style) {
				case "round" -> b.add(Box.of("a" + n, 0, 0, s * 3.6F - 1.5F, -10, -0.5F, 3, 3, 1));
				case "tall" -> {
					b.add(Box.of("a" + n, 0, 0, s * 3 - 1.5F, -10, -0.5F, 3, 3, 1));
					b.add(Box.of("b" + n, 0, 4, s * 3 - 1, -12, -0.5F, 2, 2, 1));
					b.add(Box.of("c" + n, 8, 4, s * 3 - 0.5F, -13, -0.5F, 1, 1, 1));
				}
				case "bunny" -> b.add(Box.of("a" + n, 0, 0, s * 2.2F - 1, -17, -0.5F, 2, 9, 1).rot(s * 2.2F, -8, 0, 0, 0, s * 8));
				case "horn" -> {
					b.add(Box.of("a" + n, 0, 0, s * 3 - 1, -10, -1, 2, 2, 2));
					b.add(Box.of("b" + n, 0, 4, s * 3.4F - 0.75F, -11.5F, -0.75F, 1, 2, 1).rot(s * 3.4F, -10, 0, 0, 0, s * 18));
					b.add(Box.of("c" + n, 8, 4, s * 4.2F - 0.5F, -13, -0.5F, 1, 1, 1));
				}
				case "elf" -> {
					b.add(Box.of("a" + n, 0, 0, s * 4 - 0.5F, -5, -0.5F, 1, 2, 1));
					b.add(Box.of("b" + n, 0, 4, s * 5.5F - 0.5F, -6, -0.5F, 2, 1, 1).rot(s * 4.5F, -4.5F, 0, 0, 0, s * -25));
					b.add(Box.of("c" + n, 8, 4, s * 7.2F - 0.5F, -7.6F, -0.5F, 2, 1, 1).rot(s * 4.5F, -4.5F, 0, 0, 0, s * -25));
				}
				case "antenna" -> {
					if (side > 0) {
						b.add(Box.of("a", 0, 0, -0.5F, -12, -0.5F, 1, 4, 1));
						b.add(Box.of("ball", 8, 4, -1, -13.5F, -1, 2, 2, 2));
					}
				}
				case "eyes" -> {
					b.add(Box.of("a" + n, 0, 0, s * 2.5F - 1.5F, -10, -2.5F, 3, 2, 3));
					b.add(Box.of("b" + n, 8, 4, s * 2.5F - 0.5F, -10.8F, -1.5F, 1, 1, 1));
				}
				case "drop" -> b.add(Box.of("a" + n, 0, 0, s * 4.4F - 0.5F, -8, -1.5F, 1, 3, 3).rot(s * 4.4F, -8, 0, 0, 0, s * 12));
				case "antler" -> {
					b.add(Box.of("a" + n, 0, 0, s * 2.6F - 0.5F, -12, -0.5F, 1, 5, 1).rot(s * 2.6F, -8, 0, 0, 0, s * 14));
					b.add(Box.of("b" + n, 0, 4, s * 4.2F - 0.5F, -14, -0.5F, 1, 3, 1));
					b.add(Box.of("c" + n, 8, 4, s * 2.4F - 0.5F, -14, -0.5F, 1, 3, 1));
					b.add(Box.of("e" + n, 0, 0, s * 3.4F - 1, -10, -0.5F, 2, 2, 1));
				}
				default -> {
					b.add(Box.of("a" + n, 0, 0, s * 3 - 1.5F, -10, -0.5F, 3, 2, 1).rot(s * 2.6F, -8, 0, 0, 0, s * 9));
					b.add(Box.of("b" + n, 0, 4, s * 3 - 1, -11, -0.5F, 2, 1, 1).rot(s * 2.6F, -8, 0, 0, 0, s * 9));
					b.add(Box.of("c" + n, 8, 4, s * 3 - 0.5F, -12, -0.5F, 1, 1, 1).rot(s * 2.6F, -8, 0, 0, 0, s * 9));
				}
			}
		}
		return b;
	}

	private static final float[] TAIL_LEN = {4.0F, 4.0F, 4.0F, 3.0F};

	/** Built on first access to any part; the holder keeps the work out of class loading of the layer. */
	private static final class Parts {
		static final ModelPart BANDANA = bake(36, 11,
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -7.0F, -4.5F, 9, 2, 9,
							new net.minecraft.client.model.geom.builders.CubeDeformation(0.15F)));

		static final ModelPart GLASSES = bake(24, 10, root -> {
			root.addOrReplaceChild("lensR", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -5.0F, -5.0F, 4, 3, 1), PartPose.ZERO);
			root.addOrReplaceChild("lensL", CubeListBuilder.create().texOffs(0, 0).addBox(0.5F, -5.0F, -5.0F, 4, 3, 1), PartPose.ZERO);
			root.addOrReplaceChild("bridge", CubeListBuilder.create().texOffs(10, 0).addBox(-0.5F, -4.5F, -5.0F, 1, 1, 1), PartPose.ZERO);
			root.addOrReplaceChild("templeR", CubeListBuilder.create().texOffs(0, 5).addBox(-5.0F, -4.5F, -4.0F, 1, 1, 4), PartPose.ZERO);
			root.addOrReplaceChild("templeL", CubeListBuilder.create().texOffs(0, 5).addBox(4.0F, -4.5F, -4.0F, 1, 1, 4), PartPose.ZERO);
		});

		static final ModelPart BRACELET_RIGHT = bake(20, 7,
				CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, 8.0F, -2.5F, 5, 2, 5));

		static final ModelPart BRACELET_LEFT = bake(20, 7,
				CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, 8.0F, -2.5F, 5, 2, 5));

		static final ModelPart HALO = bake(16, 8, root -> {
			for (int i = 0; i < 8; i++) {
				double a = i * Math.PI / 4.0;
				root.addOrReplaceChild("bar" + i, CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, 0.0F, -1.0F, 5, 1, 2),
						PartPose.offsetAndRotation((float) (Math.cos(a) * 4.8), -11.0F, (float) (Math.sin(a) * 4.8), 0.0F, (float) (a + Math.PI / 2), 0.0F));
			}
		});

		static final ModelPart EARS = bake(16, 8, root -> {
			for (int side = -1; side <= 1; side += 2) {
				float lean = side * 0.16F;
				PartPose pose = PartPose.offsetAndRotation(side * 2.6F, -8.0F, 0.0F, 0.0F, 0.0F, lean);
				PartDefinition ear = root.addOrReplaceChild(side < 0 ? "earR" : "earL", CubeListBuilder.create(), pose);
				ear.addOrReplaceChild("a", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -2.0F, -0.5F, 3, 2, 1), PartPose.ZERO);
				ear.addOrReplaceChild("b", CubeListBuilder.create().texOffs(0, 4).addBox(-1.0F, -3.0F, -0.5F, 2, 1, 1), PartPose.ZERO);
				ear.addOrReplaceChild("c", CubeListBuilder.create().texOffs(8, 4).addBox(-0.5F, -4.0F, -0.5F, 1, 1, 1), PartPose.ZERO);
			}
		});

		static final ModelPart BACKPACK = bake(64, 32, root -> {
			root.addOrReplaceChild("main", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, 1.5F, 2.0F, 7, 9, 3), PartPose.ZERO);
			root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 12).addBox(-4.0F, 0.5F, 1.9F, 8, 3, 4), PartPose.ZERO);
			root.addOrReplaceChild("pocket", CubeListBuilder.create().texOffs(0, 19).addBox(-2.5F, 6.0F, 5.0F, 5, 3, 1), PartPose.ZERO);
			root.addOrReplaceChild("strapR", CubeListBuilder.create().texOffs(24, 0).addBox(-3.0F, 0.0F, -2.5F, 1, 8, 1), PartPose.ZERO);
			root.addOrReplaceChild("strapL", CubeListBuilder.create().texOffs(24, 0).addBox(2.0F, 0.0F, -2.5F, 1, 8, 1), PartPose.ZERO);
			root.addOrReplaceChild("bridgeR", CubeListBuilder.create().texOffs(28, 0).addBox(-3.0F, -0.5F, -2.5F, 1, 1, 5), PartPose.ZERO);
			root.addOrReplaceChild("bridgeL", CubeListBuilder.create().texOffs(28, 0).addBox(2.0F, -0.5F, -2.5F, 1, 1, 5), PartPose.ZERO);
		});

		static final ModelPart HAT = bake(64, 32, root -> {
			root.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -13.0F, -4.5F, 9, 5, 9), PartPose.ZERO);
			root.addOrReplaceChild("brim", CubeListBuilder.create().texOffs(0, 14).addBox(-6.0F, -9.0F, -6.0F, 12, 1, 12), PartPose.ZERO);
			root.addOrReplaceChild("orn", CubeListBuilder.create().texOffs(36, 0).addBox(-1.5F, -15.0F, -1.5F, 3, 2, 3), PartPose.ZERO);
			root.addOrReplaceChild("bill", CubeListBuilder.create().texOffs(36, 5).addBox(-4.5F, -9.0F, -8.5F, 9, 1, 4), PartPose.ZERO);
		});

		static final ModelPart[] TAIL = {
				bake(64, 32, CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -1.5F, 0.0F, 3, 3, 4)),
				bake(64, 32, CubeListBuilder.create().texOffs(0, 7).addBox(-1.5F, -1.5F, 0.0F, 3, 3, 4)),
				bake(64, 32, CubeListBuilder.create().texOffs(14, 0).addBox(-1.0F, -1.0F, 0.0F, 2, 2, 4)),
				bake(64, 32, CubeListBuilder.create().texOffs(26, 0).addBox(-1.5F, -1.5F, 0.0F, 3, 3, 3))};

		static final ModelPart PET_BODY = bake(64, 32,
				CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -3.0F, -2.5F, 4, 3, 5));

		static final ModelPart PET_HEAD = bake(64, 32, root -> {
			root.addOrReplaceChild("skull", CubeListBuilder.create().texOffs(18, 0).addBox(-1.5F, -3.0F, -2.0F, 3, 3, 3), PartPose.ZERO);
			root.addOrReplaceChild("earR", CubeListBuilder.create().texOffs(30, 0).addBox(-1.5F, -4.0F, -1.0F, 1, 1, 1), PartPose.ZERO);
			root.addOrReplaceChild("earL", CubeListBuilder.create().texOffs(30, 0).addBox(0.5F, -4.0F, -1.0F, 1, 1, 1), PartPose.ZERO);
		});

		static final ModelPart PET_TAIL = bake(64, 32, root ->
				root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(34, 0).addBox(-0.5F, -0.5F, 0.0F, 1, 1, 3),
						PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7F, 0.0F, 0.0F)));

		static final ModelPart SCARF_COLLAR = bake(64, 32,
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -1.0F, -2.5F, 9, 3, 5));

		static final ModelPart SCARF_A = bake(64, 32,
				CubeListBuilder.create().texOffs(0, 8).addBox(-1.5F, 0.0F, -0.5F, 3, 8, 1));

		static final ModelPart SCARF_B = bake(64, 32,
				CubeListBuilder.create().texOffs(8, 8).addBox(-1.5F, 0.0F, -0.5F, 3, 5, 1));

		static final ModelPart MASK = bake(64, 32, root -> {
			root.addOrReplaceChild("plate", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -6.0F, -5.0F, 8, 6, 1), PartPose.ZERO);
			root.addOrReplaceChild("strap", CubeListBuilder.create().texOffs(0, 7).addBox(-4.0F, -6.0F, -4.0F, 8, 2, 8,
					new net.minecraft.client.model.geom.builders.CubeDeformation(0.6F)), PartPose.ZERO);
		});

		private static ModelPart bake(int texW, int texH, CubeListBuilder cubes) {
			return bake(texW, texH, root -> root.addOrReplaceChild("accessory", cubes, PartPose.ZERO));
		}

		private static ModelPart bake(int texW, int texH, java.util.function.Consumer<PartDefinition> build) {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition holder = mesh.getRoot().addOrReplaceChild("accessory", CubeListBuilder.create(), PartPose.ZERO);
			build.accept(holder);
			return LayerDefinition.create(mesh, texW, texH).bakeRoot();
		}
	}
}
