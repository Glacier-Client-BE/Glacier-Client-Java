package net.glacier.cosmetics;

import net.minecraft.client.model.geom.ModelPart;

/**
 * The wing silhouettes. Each is a Bedrock geometry under assets/glacier/models/cosmetics/wings (the Glacier one is the pack's
 * own file, the rest come from tools/gen_wing_models.py) with right-hand bones rwing, rwing2, rwing3* and left-hand bones lwing,
 * lwing2, lwing3*, hung off the body pivot. {@link #pose} turns the player's motion into bone rotations: a flap that lags
 * further along each segment, a harder flap while falling and folded wings while gliding.
 */
enum Wings {
	GLACIER("glacier", 0, 0, 0, 1.0F),
	ANGEL("angel", 14, 4, 0, 0.85F),
	BAT("bat", 11, 3, 0, 0.9F),
	DRAGON("dragon", 9, 3, 0, 1.0F),
	BUTTERFLY("butterfly", 16, 5, 22, 0.9F),
	MECHA("mecha", 6, 2, 0, 0.85F);

	private final String id;
	private final float flap;
	private final float lag;
	private final float yaw;
	private final float size;
	private BedrockModel model;
	private boolean loaded;

	Wings(String id, float flap, float lag, float yaw, float size) {
		this.id = id;
		this.flap = flap;
		this.lag = lag;
		this.yaw = yaw;
		this.size = size;
	}

	static Wings of(String modelId) {
		for (Wings w : values()) {
			if (w.id.equals(modelId)) return w;
		}
		return ANGEL;
	}

	/** Baked model, loaded on first use; null if the file is missing. */
	BedrockModel model() {
		if (!loaded) {
			loaded = true;
			model = BedrockModel.load("/assets/glacier/models/cosmetics/wings/" + id + ".geo.json");
		}
		return model;
	}

	/**
	 * Poses the model and returns its root.
	 *
	 * @param phase flap phase in radians, advanced by the layer so speed changes never jump
	 * @param fall  0..1, how hard the player is dropping
	 * @param glide 0..1, elytra gliding
	 * @param chest true with a chestplate on: wings sit one pixel further back
	 */
	ModelPart pose(float phase, float fall, float glide, boolean chest) {
		BedrockModel m = model();
		if (m == null) return null;
		float push = chest ? 1.0F : 0.0F;
		if (this == GLACIER) {
			// pack animation.glacier.wings with the 350 deg/s flap slowed to the layer's phase
			float d = (float) Math.toDegrees(phase);
			float amp = (1.0F + 0.6F * fall) * (1.0F - 0.7F * glide);
			float sin = (float) Math.sin(phase);
			float cos = (float) Math.cos(phase);
			float c150 = (float) Math.cos(Math.toRadians(150.0 - d));
			for (int s = 0; s < 2; s++) {
				String p = s == 0 ? "l" : "r";
				float sign = s == 0 ? 1.0F : -1.0F;
				m.scale(p + "wing", 0.2F);
				m.offset(p + "wing", 0.3F * sign, 0.0F, push);
				m.rotate(p + "wing", -25.0F - 13.0F * amp * c150, sign * (23.0F + 20.0F * amp * sin), sign * (20.0F + 15.0F * amp * sin));
				m.rotate(p + "wing2", 0.0F, 0.0F, sign * (-35.0F - 38.0F * amp * cos));
			}
			return m.root;
		}
		float amp = flap * (1.0F + 0.8F * fall) * (1.0F - 0.75F * glide);
		float f0 = (float) Math.sin(phase);
		float f1 = (float) Math.sin(phase - 0.8F);
		float f2 = (float) Math.sin(phase - 1.6F);
		for (int s = 0; s < 2; s++) {
			String p = s == 0 ? "r" : "l";
			float sign = s == 0 ? 1.0F : -1.0F;
			float raise = 30.0F * fall;
			String root = p + "wing";
			m.scale(root, size);
			m.offset(root, 0.0F, 0.0F, push);
			m.rotate(root, 0.0F, sign * (yaw * f0 - 40.0F * glide), sign * (amp * f0 + raise - 8.0F * glide));
			m.rotate(root + "2", 0.0F, 0.0F, sign * (amp * 0.9F * f1 - 26.0F * glide));
			for (String bone : m.withPrefix(root + "3")) {
				m.rotate(bone, 0.0F, 0.0F, sign * (amp * 0.7F * f2 - 14.0F * glide + lag * f2));
			}
		}
		return m.root;
	}
}
