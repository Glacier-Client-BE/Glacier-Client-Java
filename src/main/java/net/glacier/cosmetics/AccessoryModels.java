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

	/** Right wing, extends towards -X from its pivot. Pivot is on the body's back at the shoulder line. */
	static ModelPart wingRight() {
		return Parts.WING_RIGHT;
	}

	/** Left wing, mirrored UVs so the design reads the same way round on both sides. */
	static ModelPart wingLeft() {
		return Parts.WING_LEFT;
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

		static final ModelPart WING_RIGHT = wing(1);

		static final ModelPart WING_LEFT = wing(-1);

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

		/** Five-feather fan on a bone, with shorter coverts behind. dir is 1 for the right wing, -1 for the left. */
		private static ModelPart wing(int dir) {
			final int[] len = {11, 14, 15, 13, 9};
			return bake(64, 32, root -> {
				root.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(0, 0)
						.addBox(dir > 0 ? -15.0F : 0.0F, -2.0F, -0.5F, 15, 2, 2), PartPose.ZERO);
				for (int i = 0; i < 5; i++) {
					float x = dir * -(1.5F + 3.0F * i);
					float rot = dir * (0.03F + 0.06F * i);
					PartPose pose = PartPose.offsetAndRotation(x, -1.0F, 0.0F, 0.0F, 0.0F, rot);
					root.addOrReplaceChild("f" + i, CubeListBuilder.create().texOffs(i * 8, 5).addBox(-1.5F, 0.0F, 0.0F, 3, len[i], 1), pose);
					root.addOrReplaceChild("c" + i, CubeListBuilder.create().texOffs(i * 8, 22).addBox(-1.5F, 0.0F, 1.0F, 3, 6, 1),
							PartPose.offsetAndRotation(x, -0.5F, 0.0F, 0.0F, 0.0F, rot));
				}
			});
		}

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
