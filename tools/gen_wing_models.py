"""Builds the Bedrock-format geometry files for the five procedural wing silhouettes (angel, bat, dragon, butterfly, mecha)
plus a rest-pose preview sheet (tools/preview_wing_models.png).  The cubes reuse the 64x32 wing texture layout of
gen_cosmetics.py (bone box at 0,0 size 15x2x2; feather i at (8i, 5) size 3 x LEN[i] x 1; covert i at (8i, 22) size 3x6x1),
so every generated wing texture fits every silhouette.  The Glacier Wings silhouette is the pack's own geometry
(see port_pack_cosmetics.py).
  python tools/gen_wing_models.py
Coordinates are Bedrock model space (y up, +z = back of the player); bones hang off `body` whose pivot is (0, 24, 0).
Java loads them with BedrockModel, which converts to ModelPart poses."""
import json, math, os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/assets/glacier/models/cosmetics/wings")
LEN = [11, 14, 15, 13, 9]
SHOULDER = (2.0, 22.0, 2.6)          # right wing root; the left wing mirrors it


class Wing:
    def __init__(self):
        self.bones = []

    def bone(self, name, parent, pivot, rot=(0, 0, 0)):
        par = next((x for x in self.bones if x["name"] == parent), None)
        b = {"name": name, "parent": parent, "pivot": list(pivot), "rotation": list(rot), "cubes": [],
             "cum": rot[2] + (par["cum"] if par else 0)}
        self.bones.append(b)
        return b

    @staticmethod
    def spar(b, start, length, thick=2.0):
        """Bone bar running along +x from start (x, y, z)."""
        b["cubes"].append({"origin": [start[0], start[1] - thick / 2, start[2] - 0.5], "size": [length, thick, 2], "uv": [0, 0]})

    @staticmethod
    def feather(b, at, i, ang, covert=True):
        """Feather hanging from anchor `at`; ang is the rest angle from straight down in model space (positive swings the
        tip away from the body), the bone chain's own rotation is subtracted."""
        ang = ang - b["cum"]
        L = LEN[i]
        b["cubes"].append({"origin": [at[0] - 1.5, at[1] - L, at[2]], "size": [3, L, 1], "uv": [8 * i, 5],
                           "pivot": list(at), "rotation": [0, 0, ang]})
        if covert:
            b["cubes"].append({"origin": [at[0] - 1.5, at[1] + 0.5 - 6, at[2] + 1], "size": [3, 6, 1], "uv": [8 * i, 22],
                               "pivot": [at[0], at[1] + 0.5, at[2]], "rotation": [0, 0, ang]})


def mirror(bones):
    out = []
    for b in bones:
        nb = {k: v for k, v in b.items() if k != "cum"}
        nb["name"] = "l" + b["name"][1:]
        nb["parent"] = "body" if b["parent"] == "body" else "l" + b["parent"][1:]
        nb["pivot"] = [-b["pivot"][0], b["pivot"][1], b["pivot"][2]]
        nb["rotation"] = [b["rotation"][0], -b["rotation"][1], -b["rotation"][2]]
        cubes = []
        for c in b["cubes"]:
            nc = dict(c)
            nc["origin"] = [-(c["origin"][0] + c["size"][0]), c["origin"][1], c["origin"][2]]
            if "pivot" in c:
                nc["pivot"] = [-c["pivot"][0], c["pivot"][1], c["pivot"][2]]
                nc["rotation"] = [c["rotation"][0], -c["rotation"][1], -c["rotation"][2]]
            cubes.append(nc)
        nb["cubes"] = cubes
        out.append(nb)
    return out


def fan(w, b, anchor, a0, a1, n, types, covert=True):
    """n feathers from one anchor between angles a0..a1; the types list is spread across the fan."""
    for k in range(n):
        t = k / max(1, n - 1)
        w.feather(b, anchor, types[min(len(types) - 1, int(round(t * (len(types) - 1))))], a0 + (a1 - a0) * t, covert)


def angel():
    """Arched arm and forearm, a swept fan of long feathers underneath, short coverts on top."""
    w = Wing(); S = SHOULDER
    a = w.bone("rwing", "body", S, (0, -20, 50))
    Wing.spar(a, S, 8)
    p1 = (S[0] + 8, S[1], S[2])
    for k, (d, i) in enumerate([(1.5, 0), (3.5, 0), (5.5, 1), (7.5, 1)]):
        w.feather(a, (S[0] + d, S[1], S[2]), i, 4 + k * 3)
    b = w.bone("rwing2", "rwing", p1, (0, 0, -42))
    Wing.spar(b, p1, 11)
    for k, (d, i) in enumerate([(1.5, 1), (3.5, 2), (5.5, 2), (7.5, 3), (9.5, 3), (11, 4)]):
        w.feather(b, (p1[0] + d, p1[1], p1[2]), i, 14 + k * 8, covert=k < 5)
    return w.bones


def bat():
    """Arm and forearm with three finger spars; membrane panels fan out between the fingers."""
    w = Wing(); S = SHOULDER
    a = w.bone("rwing", "body", S, (0, -20, 55))
    Wing.spar(a, S, 9, thick=2)
    p1 = (S[0] + 9, S[1], S[2])
    for k, d in enumerate((2, 4.5, 7)):
        w.feather(a, (S[0] + d, S[1], S[2]), 4, 2 + k * 4, covert=False)
    b = w.bone("rwing2", "rwing", p1, (0, 0, -62))
    Wing.spar(b, p1, 8, thick=1.6)
    wr = (p1[0] + 8, p1[1], p1[2])
    for k, al in enumerate((-8, -38, -68)):
        f = w.bone("rwing3%s" % "abc"[k], "rwing2", wr, (0, 0, al - b["cum"]))
        Wing.spar(f, (wr[0], wr[1] - 0.5, wr[2]), 11 + (k == 1), thick=1.0)
    for k, ang in enumerate((10, 34, 58, 82)):
        w.feather(b, (wr[0], wr[1], wr[2] + (k % 2) * 0.5), (3, 2, 2, 3)[k], ang, covert=False)
    return w.bones


def dragon():
    """Heavier bat: higher arm, longer spars and fewer, spikier membrane panels."""
    w = Wing(); S = SHOULDER
    a = w.bone("rwing", "body", S, (0, -22, 66))
    Wing.spar(a, S, 10, thick=2.4)
    p1 = (S[0] + 10, S[1], S[2])
    for k, ang in enumerate((6, 12, 20)):
        w.feather(a, (S[0] + 2 + 3 * k, S[1], S[2]), 4, ang, covert=False)
    b = w.bone("rwing2", "rwing", p1, (0, 0, -66))
    Wing.spar(b, p1, 9, thick=2)
    wr = (p1[0] + 9, p1[1], p1[2])
    for k, al in enumerate((12, -22, -54, -86)):
        f = w.bone("rwing3%s" % "abcd"[k], "rwing2", wr, (0, 0, al))
        Wing.spar(f, (wr[0], wr[1] - 0.5, wr[2]), 15 - k * 1.5, thick=1.2)
    for ang, i in ((88, 3), (52, 2), (20, 2)):
        w.feather(b, (wr[0], wr[1], wr[2] + 0.4), i, ang, covert=False)
    return w.bones


def butterfly():
    """Two broad lobes: forewing sweeping up and out, hindwing hanging low."""
    w = Wing(); S = (2.5, 21.0, 2.6)
    a = w.bone("rwing", "body", S, (0, -18, 0))
    Wing.spar(a, S, 4, thick=1.6)
    up = w.bone("rwing2", "rwing", S, (0, 0, 0))
    fan(w, up, (S[0] + 1, S[1] + 0.5, S[2]), 100, 150, 6, [1, 2, 3, 2, 1], covert=True)
    low = w.bone("rwing3", "rwing", S, (0, 0, 0))
    fan(w, low, (S[0] + 1, S[1] - 0.5, S[2] + 0.1), 20, 75, 4, [0, 3, 4], covert=False)
    return w.bones


def mecha():
    """Rigid angular blades on stepped struts."""
    w = Wing(); S = SHOULDER
    a = w.bone("rwing", "body", S, (0, -14, 38))
    Wing.spar(a, S, 8, thick=2.4)
    p1 = (S[0] + 8, S[1], S[2])
    for k in range(3):
        w.feather(a, (S[0] + 1.5 + 3 * k, S[1], S[2]), (2, 3, 2)[k], 30, covert=False)
    b = w.bone("rwing2", "rwing", p1, (0, 0, -45))
    Wing.spar(b, p1, 11, thick=2.0)
    for k in range(4):
        w.feather(b, (p1[0] + 1.5 + 3 * k, p1[1], p1[2]), (1, 2, 1, 4)[k], 36 + 4 * k, covert=False)
    p2 = (p1[0] + 11, p1[1], p1[2])
    c = w.bone("rwing3", "rwing2", p2, (0, 0, -40))
    Wing.spar(c, p2, 6, thick=1.6)
    for k in range(2):
        w.feather(c, (p2[0] + 1.5 + 3 * k, p2[1], p2[2]), (3, 4)[k], 52 + 6 * k, covert=False)
    return w.bones


KINDS = {"angel": angel, "bat": bat, "dragon": dragon, "butterfly": butterfly, "mecha": mecha}


def geometry(name, right):
    bones = [{"name": "root", "pivot": [0, 0, 0]}, {"name": "waist", "parent": "root", "pivot": [0, 12, 0]},
             {"name": "body", "parent": "waist", "pivot": [0, 24, 0]}]
    for b in right + mirror(right):
        bb = {k: v for k, v in b.items() if k != "cum"}
        if not bb["cubes"]: del bb["cubes"]
        if bb["rotation"] == [0, 0, 0]: del bb["rotation"]
        bones.append(bb)
    return {"format_version": "1.12.0",
            "minecraft:geometry": [{"description": {"identifier": "geometry.glacier.wings_" + name, "texture_width": 64, "texture_height": 32},
                                    "bones": bones}]}


# ---------------------------------------------------------------- rest-pose preview (bedrock rotation order: Rx, then Ry, then Rz)
def rot(v, r):
    x, y, z = v
    a, b, c = (math.radians(t) for t in r)
    y, z = y * math.cos(a) - z * math.sin(a), y * math.sin(a) + z * math.cos(a)
    x, z = x * math.cos(b) + z * math.sin(b), -x * math.sin(b) + z * math.cos(b)
    x, y = x * math.cos(c) - y * math.sin(c), x * math.sin(c) + y * math.cos(c)
    return (x, y, z)


def about(p, piv, r):
    q = rot((p[0] - piv[0], p[1] - piv[1], p[2] - piv[2]), r)
    return (q[0] + piv[0], q[1] + piv[1], q[2] + piv[2])


def world(bones, b, p):
    while b and b["name"] not in ("root", "waist", "body"):
        p = about(p, b["pivot"], b.get("rotation", [0, 0, 0]))
        b = next((x for x in bones if x["name"] == b.get("parent")), None)
    return p


COL = [(120, 170, 255), (140, 190, 255), (180, 215, 255), (150, 200, 255), (110, 150, 230)]


def preview(name, geo):
    bones = geo["minecraft:geometry"][0]["bones"]
    S, W, H = 6, 420, 300
    im = Image.new("RGB", (W * 2, H), (36, 40, 52)); d = ImageDraw.Draw(im)
    def proj_back(p): return (W / 2 + p[0] * S, H - 30 - (p[1] - 6) * S)
    def proj_side(p): return (W + W / 2 + p[2] * S - 20, H - 30 - (p[1] - 6) * S)
    quads = []
    for b in bones:
        for c in b.get("cubes", []):
            o, s = c["origin"], c["size"]
            face = [(o[0], o[1], o[2] + s[2]), (o[0] + s[0], o[1], o[2] + s[2]), (o[0] + s[0], o[1] + s[1], o[2] + s[2]), (o[0], o[1] + s[1], o[2] + s[2])]
            if "pivot" in c: face = [about(p, c["pivot"], c["rotation"]) for p in face]
            face = [world(bones, b, p) for p in face]
            col = (170, 130, 90) if c["uv"][1] == 0 else COL[c["uv"][0] // 8 % 5]
            quads.append((sum(p[2] for p in face), face, col))
    quads.sort(key=lambda q: -q[0])
    d.rectangle((W / 2 - 4 * S, H - 30 - 18 * S, W / 2 + 4 * S, H - 30 - 6 * S), fill=(90, 100, 130))
    d.rectangle((W / 2 - 4 * S, H - 30 - 26 * S, W / 2 + 4 * S, H - 30 - 18 * S), fill=(120, 130, 160))
    d.rectangle((W + W / 2 - 2 * S - 20, H - 30 - 18 * S, W + W / 2 + 2 * S - 20, H - 30 - 6 * S), fill=(90, 100, 130))
    for _, face, col in quads:
        d.polygon([proj_back(p) for p in face], fill=col, outline=(20, 20, 30))
        d.polygon([proj_side(p) for p in face], fill=col, outline=(20, 20, 30))
    d.text((8, 6), name, fill=(240, 240, 250))
    return im


def main():
    os.makedirs(OUT, exist_ok=True)
    tiles = []
    for name, fn in KINDS.items():
        geo = geometry(name, fn())
        with open(os.path.join(OUT, name + ".geo.json"), "w") as f: json.dump(geo, f, separators=(",", ":"))
        tiles.append(preview(name, geo))
    sheet = Image.new("RGB", (tiles[0].width, tiles[0].height * len(tiles)))
    for i, t in enumerate(tiles): sheet.paste(t, (0, i * t.height))
    sheet.save(os.path.join(ROOT, "tools/preview_wing_models.png"))
    print("wrote", len(tiles), "wing geometries")


if __name__ == "__main__":
    main()
