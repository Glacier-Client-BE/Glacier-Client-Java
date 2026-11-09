"""Ports the Glacier set of the Bedrock pack (cape, wings, halo, mask) to Java assets.  One-off; the outputs are committed.
  python tools/port_pack_cosmetics.py [pack_root]
Writes under src/main/resources/assets/glacier:
  textures/cosmetics/pack/wings_glacier.png  halo_glacier.png  mask_glacier.png   the pack PNGs, pixel for pixel
  textures/cosmetics/pack/glacier_set.png                                         the pack cape, byte-identical (640x670)
  models/cosmetics/wings/glacier.geo.json                                         the pack wings geometry, bones untouched
  models/cosmetics/halo_glacier.geo.json                                          the pack halo geometry, bones untouched
  models/cosmetics/mask_glacier.mesh.json                                         the pack poly_mesh mask, converted to head-local Java coordinates
The pack cape is a 16-segment strip on a 640x670 sheet; Java capes are one box on 64x32, so the back panel (and the plain
inner panel) are box-filtered into the vanilla cape UV layout."""
import json, os, shutil, sys
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src/main/resources/assets/glacier")
PACK = sys.argv[1] if len(sys.argv) > 1 else r"C:/Users/User/Desktop/Glacier v7/packs/Glacier Client v7 [Main]"


def tex(name): return Image.open(os.path.join(PACK, "textures/entity", name + ".png")).convert("RGBA")


def geo(name): return json.load(open(os.path.join(PACK, "models/entity", name + ".json")))


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f: json.dump(obj, f, separators=(",", ":"))


def write_cape_mesh(g):
    """Cape.geometry: per-face UV cubes under bone cape (pivot 0,24,2, rotation y 180). Baked at rest pose into one quad mesh in
    body-local Java coordinates (the layer swings it as a whole), UVs normalized by the pack's native 640x670 sheet."""
    geo0 = g["minecraft:geometry"][0]
    tw, th = geo0["description"]["texture_width"], geo0["description"]["texture_height"]
    pos, nrm, uv, quads = [], [], [], []
    def vi(p): pos.extend(p); return len(pos) // 3 - 1
    def conv(x, y, z): return [-x, 24.0 - y, 4.0 - z]             # Ry(180) about z=2, then y up -> y down
    for bone in geo0["bones"]:
        for c in bone.get("cubes", []):
            ox, oy, oz = c["origin"]; sx, sy, sz = c["size"]
            x0, x1, y0, y1, z0, z1 = ox, ox + sx, oy, oy + sy, oz, oz + sz
            faces = {  # corners in uv order: (u0,v0) (u1,v0) (u1,v1) (u0,v1), normal
                "north": ([(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)], (0, 0, -1)),
                "south": ([(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)], (0, 0, 1)),
                "east": ([(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)], (1, 0, 0)),
                "west": ([(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)], (-1, 0, 0)),
                "up": ([(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)], (0, 1, 0)),
                "down": ([(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)], (0, -1, 0)),
            }
            for name, f in c["uv"].items():
                corners, n = faces[name]
                u, v = f["uv"]; w, h = f["uv_size"]
                uvs = [(u, v), (u + w, v), (u + w, v + h), (u, v + h)]
                ni = len(nrm) // 3; nrm.extend([-n[0], -n[1], -n[2]])   # normal after Ry(180) and the y flip
                q = []
                for (cx, cy, cz), (uu, vv) in zip(corners, uvs):
                    ui = len(uv) // 2; uv.extend([round(uu / tw, 6), round(vv / th, 6)])
                    q.extend([vi([round(t, 4) for t in conv(cx, cy, cz)]), ni, ui])
                quads.extend(q)
    write_json(os.path.join(RES, "models/cosmetics/cape_glacier.mesh.json"), {"pos": pos, "nrm": nrm, "uv": uv, "quads": quads})


def main():
    out = os.path.join(RES, "textures/cosmetics/pack"); os.makedirs(out, exist_ok=True)
    # textures are copied byte for byte: no resampling, no palette change
    for src, dst in (("wings", "wings_glacier"), ("halo", "halo_glacier"), ("mask", "mask_glacier"), ("cape", "glacier_set")):
        shutil.copyfile(os.path.join(PACK, "textures/entity", src + ".png"), os.path.join(out, dst + ".png"))
    write_cape_mesh(geo("cape.geometry"))

    # geometries kept as authored in the pack (root/waist/body skeleton included, the loader skips it)
    for src, dst in (("wings.geo", "models/cosmetics/wings/glacier.geo.json"), ):
        write_json(os.path.join(RES, dst), geo(src))
    halo = [g for g in geo("model.geo")["minecraft:geometry"] if g["description"]["identifier"] == "geometry.glacier.halo"][0]
    write_json(os.path.join(RES, "models/cosmetics/halo_glacier.geo.json"), {"format_version": "1.12.0", "minecraft:geometry": [halo]})

    # mask poly_mesh -> head-local Java coordinates.  Bone mask: pivot (0,24,0), rotation (0,180,0) under head (0,24,0).
    g = geo("mask.geo")["minecraft:geometry"][0]
    b = [x for x in g["bones"] if x["name"] == "mask"][0]
    pm = b["poly_mesh"]
    assert pm["normalized_uvs"]
    pos = [[-x, 24.0 - y, -z] for x, y, z in pm["positions"]]            # Ry(180), then y up -> y down
    nrm = [[-x, -y, -z] for x, y, z in pm["normals"]]
    uv = [[u, 1.0 - v] for u, v in pm["uvs"]]                             # normalized bedrock v runs bottom-up
    quads = []
    for poly in pm["polys"]:
        quads.append([i for v in poly for i in v])                        # pos, normal, uv per vertex (a triangle repeats vertex 0)
    rnd = lambda arr: [[round(t, 4) for t in row] for row in arr]
    write_json(os.path.join(RES, "models/cosmetics/mask_glacier.mesh.json"),
               {"pos": [t for r in rnd(pos) for t in r], "nrm": [t for r in rnd(nrm) for t in r], "uv": [t for r in rnd(uv) for t in r],
                "quads": [i for q in quads for i in q]})
    print("ported: pack textures, wings/halo geo, mask mesh (%d quads)" % len(quads))


if __name__ == "__main__":
    main()
