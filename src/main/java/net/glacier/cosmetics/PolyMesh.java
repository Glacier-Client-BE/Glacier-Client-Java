package net.glacier.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Free-form quad mesh for pack models that are not boxes (the Glacier mask is a Blockbench poly mesh). The file is produced by
 * tools/port_pack_cosmetics.py: positions in head-local model pixels (y down), normals, normalized UVs (v already top-down) and
 * quads as groups of four (position, normal, uv) index triples; a triangle repeats its first vertex.
 */
final class PolyMesh {
	private static final float PX = 1.0F / 16.0F;

	private final float[] pos;
	private final float[] nrm;
	private final float[] uv;
	private final int[] quads;

	private PolyMesh(float[] pos, float[] nrm, float[] uv, int[] quads) {
		this.pos = pos;
		this.nrm = nrm;
		this.uv = uv;
		this.quads = quads;
	}

	/** Loads a mesh from the mod's resources; null on failure. */
	static PolyMesh load(String resource) {
		try (InputStream in = PolyMesh.class.getResourceAsStream(resource)) {
			if (in == null) throw new IOException("missing resource " + resource);
			JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			int[] q = new int[o.getAsJsonArray("quads").size()];
			JsonArray qa = o.getAsJsonArray("quads");
			for (int i = 0; i < q.length; i++) q[i] = qa.get(i).getAsInt();
			return new PolyMesh(floats(o.getAsJsonArray("pos")), floats(o.getAsJsonArray("nrm")), floats(o.getAsJsonArray("uv")), q);
		} catch (Exception e) {
			System.err.println("[Glacier] cosmetic mesh " + resource + " failed to load: " + e);
			return null;
		}
	}

	private static float[] floats(JsonArray a) {
		float[] out = new float[a.size()];
		for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsFloat();
		return out;
	}

	/** Writes every quad with the current pose of the stack. */
	void render(PoseStack.Pose pose, VertexConsumer vc, int light, int overlay) {
		for (int i = 0; i + 11 < quads.length; i += 12) {
			for (int k = 0; k < 4; k++) {
				int p = quads[i + k * 3] * 3;
				int n = quads[i + k * 3 + 1] * 3;
				int t = quads[i + k * 3 + 2] * 2;
				vc.addVertex(pose, pos[p] * PX, pos[p + 1] * PX, pos[p + 2] * PX)
						.setColor(-1)
						.setUv(uv[t], uv[t + 1])
						.setOverlay(overlay)
						.setLight(light)
						.setNormal(pose, nrm[n], nrm[n + 1], nrm[n + 2]);
			}
		}
	}
}
