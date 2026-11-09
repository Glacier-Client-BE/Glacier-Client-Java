package net.glacier.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A Bedrock {@code .geo.json} model (format 1.12, box UVs, optional per-cube pivot and rotation) baked into a
 * {@link ModelPart} tree, so cosmetics can use the pack's own geometry as authored. Bedrock space is y-up with absolute pivots;
 * Java parts are y-down with pivots relative to their parent, so positions flip y and X/Z rotations flip sign. The
 * skeleton bones root, waist, body and head are not built: the caller draws the model already translated to the player part
 * (pivot at bedrock y = 24), which is what bones parented to them are relative to.
 * <p>
 * Bone poses can be driven with {@link #rotate}, {@link #offset} and {@link #scale}, all in bedrock degrees / units added
 * to the pose the geometry file defines.
 */
final class BedrockModel {
	private static final float ORIGIN_Y = 24.0F;
	private static final Set<String> SKELETON = Set.of("root", "waist", "body", "head");

	private record Bone(ModelPart part, float x, float y, float z, float rx, float ry, float rz) {
	}

	/** Root of the baked model; draw this. */
	final ModelPart root;
	private final Map<String, Bone> bones = new HashMap<>();

	private BedrockModel(ModelPart root) {
		this.root = root;
	}

	/** Loads a model from the mod's resources, e.g. {@code /assets/glacier/models/cosmetics/wings/angel.geo.json}; null on failure. */
	static BedrockModel load(String resource) {
		try (InputStream in = BedrockModel.class.getResourceAsStream(resource)) {
			if (in == null) throw new IOException("missing resource " + resource);
			JsonObject geometry = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject()
					.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
			return build(geometry);
		} catch (Exception e) {
			System.err.println("[Glacier] cosmetic model " + resource + " failed to load: " + e);
			return null;
		}
	}

	private static BedrockModel build(JsonObject geometry) {
		JsonObject description = geometry.getAsJsonObject("description");
		int texW = description.get("texture_width").getAsInt();
		int texH = description.get("texture_height").getAsInt();

		Map<String, JsonObject> defs = new HashMap<>();
		List<JsonObject> order = new ArrayList<>();
		for (JsonElement e : geometry.getAsJsonArray("bones")) {
			JsonObject b = e.getAsJsonObject();
			defs.put(b.get("name").getAsString(), b);
			order.add(b);
		}

		MeshDefinition mesh = new MeshDefinition();
		PartDefinition holder = mesh.getRoot().addOrReplaceChild("accessory", CubeListBuilder.create(), PartPose.ZERO);
		Map<String, PartDefinition> built = new HashMap<>();
		Map<String, Bone> poses = new HashMap<>();
		// parents may be listed after their children, so keep sweeping until nothing is left to place
		List<JsonObject> pending = new ArrayList<>();
		for (JsonObject b : order) {
			if (!SKELETON.contains(b.get("name").getAsString())) pending.add(b);
		}
		boolean progress = true;
		while (!pending.isEmpty() && progress) {
			progress = false;
			for (int i = 0; i < pending.size(); i++) {
				JsonObject b = pending.get(i);
				String parentName = b.has("parent") ? b.get("parent").getAsString() : null;
				boolean rooted = parentName == null || SKELETON.contains(parentName) || !defs.containsKey(parentName);
				if (!rooted && !built.containsKey(parentName)) continue;
				float[] pivot = vec(b.get("pivot"), 0, 0, 0);
				float[] parentPivot = rooted ? new float[]{0, ORIGIN_Y, 0} : vec(defs.get(parentName).get("pivot"), 0, 0, 0);
				PartDefinition parent = rooted ? holder : built.get(parentName);
				float[] rot = vec(b.get("rotation"), 0, 0, 0);
				float rx = (float) Math.toRadians(-rot[0]);
				float ry = (float) Math.toRadians(rot[1]);
				float rz = (float) Math.toRadians(-rot[2]);
				float px = pivot[0] - parentPivot[0];
				float py = -(pivot[1] - parentPivot[1]);
				float pz = pivot[2] - parentPivot[2];
				PartDefinition part = parent.addOrReplaceChild(b.get("name").getAsString(), CubeListBuilder.create(),
						PartPose.offsetAndRotation(px, py, pz, rx, ry, rz));
				built.put(b.get("name").getAsString(), part);
				poses.put(b.get("name").getAsString(), new Bone(null, px, py, pz, rx, ry, rz));
				addCubes(part, b, pivot);
				pending.remove(i--);
				progress = true;
			}
		}

		ModelPart root = LayerDefinition.create(mesh, texW, texH).bakeRoot();
		BedrockModel model = new BedrockModel(root);
		java.util.function.Function<String, ModelPart> lookup = root.createPartLookup();
		for (Map.Entry<String, Bone> e : poses.entrySet()) {
			Bone p = e.getValue();
			model.bones.put(e.getKey(), new Bone(lookup.apply(e.getKey()), p.x, p.y, p.z, p.rx, p.ry, p.rz));
		}
		return model;
	}

	/** Cubes of a bone. Plain cubes go straight on the bone; cubes with their own rotation get a child part at their pivot. */
	private static void addCubes(PartDefinition bonePart, JsonObject bone, float[] pivot) {
		if (!bone.has("cubes")) return;
		boolean boneMirror = bone.has("mirror") && bone.get("mirror").getAsBoolean();
		CubeListBuilder plain = CubeListBuilder.create();
		int n = 0;
		for (JsonElement e : bone.getAsJsonArray("cubes")) {
			JsonObject c = e.getAsJsonObject();
			float[] o = vec(c.get("origin"), 0, 0, 0);
			float[] s = vec(c.get("size"), 0, 0, 0);
			JsonElement uvEl = c.get("uv");
			int u = 0, v = 0;
			if (uvEl != null && uvEl.isJsonArray()) {
				u = Math.round(uvEl.getAsJsonArray().get(0).getAsFloat());
				v = Math.round(uvEl.getAsJsonArray().get(1).getAsFloat());
			}
			CubeDeformation grow = c.has("inflate") ? new CubeDeformation(c.get("inflate").getAsFloat()) : CubeDeformation.NONE;
			boolean mirror = c.has("mirror") ? c.get("mirror").getAsBoolean() : boneMirror;
			if (c.has("rotation") && !isZero(vec(c.get("rotation"), 0, 0, 0))) {
				float[] q = vec(c.get("pivot"), pivot[0], pivot[1], pivot[2]);
				float[] r = vec(c.get("rotation"), 0, 0, 0);
				PartPose pose = PartPose.offsetAndRotation(q[0] - pivot[0], -(q[1] - pivot[1]), q[2] - pivot[2],
						(float) Math.toRadians(-r[0]), (float) Math.toRadians(r[1]), (float) Math.toRadians(-r[2]));
				CubeListBuilder cube = CubeListBuilder.create().texOffs(u, v).mirror(mirror)
						.addBox(o[0] - q[0], q[1] - o[1] - s[1], o[2] - q[2], s[0], s[1], s[2], grow);
				bonePart.addOrReplaceChild(bone.get("name").getAsString() + "_c" + (n++), cube, pose);
			} else {
				plain.texOffs(u, v).mirror(mirror)
						.addBox(o[0] - pivot[0], pivot[1] - o[1] - s[1], o[2] - pivot[2], s[0], s[1], s[2], grow);
			}
		}
		if (!plain.getCubes().isEmpty()) {
			bonePart.addOrReplaceChild(bone.get("name").getAsString() + "_cubes", plain, PartPose.ZERO);
		}
	}

	private static boolean isZero(float[] v) {
		return v[0] == 0 && v[1] == 0 && v[2] == 0;
	}

	private static float[] vec(JsonElement el, float dx, float dy, float dz) {
		if (el == null || !el.isJsonArray()) return new float[]{dx, dy, dz};
		JsonArray a = el.getAsJsonArray();
		return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
	}

	boolean has(String bone) {
		return bones.containsKey(bone);
	}

	/** Bone names starting with the prefix, for groups such as the finger bones rwing3a, rwing3b. */
	List<String> withPrefix(String prefix) {
		List<String> out = new ArrayList<>();
		for (String name : bones.keySet()) {
			if (name.startsWith(prefix)) out.add(name);
		}
		return out;
	}

	/** Sets the bone rotation to the file's rotation plus (dx, dy, dz) bedrock degrees. */
	void rotate(String bone, float dx, float dy, float dz) {
		Bone b = bones.get(bone);
		if (b == null) return;
		b.part.xRot = b.rx - (float) Math.toRadians(dx);
		b.part.yRot = b.ry + (float) Math.toRadians(dy);
		b.part.zRot = b.rz - (float) Math.toRadians(dz);
	}

	/** Sets the bone position to the file's position plus (dx, dy, dz) bedrock units (y up). */
	void offset(String bone, float dx, float dy, float dz) {
		Bone b = bones.get(bone);
		if (b == null) return;
		b.part.x = b.x + dx;
		b.part.y = b.y - dy;
		b.part.z = b.z + dz;
	}

	/** Uniform scale about the bone pivot, applied to the bone and its children. */
	void scale(String bone, float s) {
		Bone b = bones.get(bone);
		if (b == null) return;
		b.part.xScale = s;
		b.part.yScale = s;
		b.part.zScale = s;
	}
}
