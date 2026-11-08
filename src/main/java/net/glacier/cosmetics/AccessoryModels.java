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
