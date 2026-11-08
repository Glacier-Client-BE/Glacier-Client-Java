"""Generates the accessory textures under src/main/resources/assets/glacier/textures/cosmetics.

UV layouts must match AccessoryModels.java (box unfold: top/bottom above, then right/front/left/back).
Run: python tools/gen_accessories.py
"""
import os
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "glacier", "textures", "cosmetics")


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(4))


def shade(c, f):
    """f>0 lightens toward white, f<0 darkens toward black."""
    if f >= 0:
        return lerp(c, (255, 255, 255, c[3]), f)
    return lerp(c, (0, 0, 0, c[3]), -f)


def faces(ox, oy, w, h, d):
    return {
        "top": (ox + d, oy, w, d), "bottom": (ox + d + w, oy, w, d),
        "right": (ox, oy + d, d, h), "front": (ox + d, oy + d, w, h),
        "left": (ox + d + w, oy + d, d, h), "back": (ox + 2 * d + w, oy + d, w, h),
    }


def paint(img, rect, fn):
    x0, y0, w, h = rect
    for v in range(h):
        for u in range(w):
            c = fn(u, v, w, h)
            if c is not None:
                img.putpixel((x0 + u, y0 + v), c)


def paint_box(img, ox, oy, w, h, d, front, side=None, top=None):
    f = faces(ox, oy, w, h, d)
    side = side or front
    top = top or side
    paint(img, f["front"], front)
    paint(img, f["back"], front)
    for k in ("left", "right"):
        paint(img, f[k], side)
    for k in ("top", "bottom"):
        paint(img, f[k], top)


def save(img, folder, name):
    d = os.path.join(OUT, folder)
    os.makedirs(d, exist_ok=True)
    img.save(os.path.join(d, name.lower().replace(" ", "_") + ".png"))


# ---------------------------------------------------------------- wings
FEATHERS = [11, 14, 15, 13, 9]
COVERTS = 6
WING_STYLES = {
    "Angel": dict(root="#FFFFFF", tip="#CFDAFF", edge="#8FA3D8", shaft="#FFFFFF", bone="#E3C467"),
    "Bat": dict(root="#2A1240", tip="#6B2C91", edge="#12061D", shaft="#9A55CC", bone="#1B0C2B"),
    "Dragon": dict(root="#7A1410", tip="#FF7A1F", edge="#350604", shaft="#FFC25A", bone="#4A1A10"),
    "Ice": dict(root="#9FE3FF", tip="#FFFFFF", edge="#3E9BCB", shaft="#EAFBFF", bone="#5FB9E6"),
    "Butterfly": dict(root="#FF6EC0", tip="#7FA8FF", edge="#4E1860", shaft="#FFE4F4", bone="#34163F"),
}


def wing(name, s):
    root, tip, edge, shaft, bone = (hexc(s[k]) for k in ("root", "tip", "edge", "shaft", "bone"))
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def bone_px(u, v, w, h):
        c = shade(bone, 0.18 if v == 0 else (-0.25 if v == h - 1 else 0.0))
        return shade(c, 0.1) if (u % 4 == 0 and 0 < v < h - 1) else c

    paint_box(img, 0, 0, 15, 2, 2, bone_px, lambda u, v, w, h: shade(bone, -0.3))

    for i, L in enumerate(FEATHERS):
        def feather(u, v, w, h, L=L):
            if v == h - 1 and u != 1:
                return None  # pointed tip
            t = v / max(1, h - 1)
            c = lerp(root, tip, t ** 0.9)
            if u == 1:
                c = lerp(c, shaft, 0.55)
            elif (u + v) % 3 == 0:
                c = shade(c, -0.07)
            if u != 1 and (v == h - 1 or v == h - 2):
                c = lerp(c, edge, 0.45)
            if u in (0, w - 1):
                c = lerp(c, edge, 0.35)
            return c
        paint_box(img, i * 8, 5, 3, L, 1, feather, lambda u, v, w, h: edge)

    for i in range(5):
        def covert(u, v, w, h):
            if v == h - 1 and u != 1:
                return None
            t = v / max(1, h - 1)
            c = lerp(shade(root, 0.04), shade(tip, 0.1), t)
            if u == 1:
                c = shade(c, 0.12)
            if v >= h - 2 or u in (0, w - 1):
                c = lerp(c, edge, 0.3)
            return c
        paint_box(img, i * 8, 22, 3, COVERTS, 1, covert, lambda u, v, w, h: edge)
    save(img, "wings", name)


# ---------------------------------------------------------------- halo
HALO_STYLES = {
    "Gold Halo": ("#FFF3B0", "#F2B824", "#A86A0C"),
    "Ice Halo": ("#FFFFFF", "#9EE6FF", "#3E9BCB"),
    "Neon Halo": ("#B9FFF6", "#2DF0E0", "#A23DFF"),
}


def halo(name, cols):
    core, mid, rim = (hexc(c) for c in cols)
    img = Image.new("RGBA", (16, 8), (0, 0, 0, 0))

    def top(u, v, w, h):
        if v == 0 or v == h - 1:
            return rim if u in (0, w - 1) else mid
        return core if 0 < u < w - 1 else mid

    def side(u, v, w, h):
        return mid

    paint_box(img, 0, 0, 5, 1, 2, side, side, top)
    save(img, "halo", name)


# ---------------------------------------------------------------- ears
EAR_STYLES = {
    "Cat": dict(fur="#4A4A58", inner="#F29FB8", tip="#4A4A58"),
    "Fox": dict(fur="#E8731A", inner="#FFF1DC", tip="#2B1A12"),
    "Bear": dict(fur="#8B5A2B", inner="#D9AE7C", tip="#8B5A2B"),
}


def ears(name, s):
    fur, inner, tip = (hexc(s[k]) for k in ("fur", "inner", "tip"))
    img = Image.new("RGBA", (16, 8), (0, 0, 0, 0))

    def mk(has_inner, is_tip):
        def px(u, v, w, h):
            base = tip if is_tip else fur
            c = shade(base, 0.08 if v == 0 else -0.08 * (v / max(1, h - 1)))
            if has_inner and 0 < u < w - 1 and v >= 0:
                c = shade(inner, -0.1 * v)
            return c
        return px

    def side(u, v, w, h):
        return shade(fur, -0.25)

    paint_box(img, 0, 0, 3, 2, 1, mk(True, False), side)
    paint_box(img, 0, 4, 2, 1, 1, mk(False, False), side)
    paint_box(img, 8, 4, 1, 1, 1, mk(False, True), side)
    save(img, "ears", name)


# ---------------------------------------------------------------- glasses
def glasses(name, frame, glass, glass_a, glint, rows=None):
    frame, glass, glint = hexc(frame), hexc(glass, glass_a), hexc(glint, 200)
    img = Image.new("RGBA", (24, 10), (0, 0, 0, 0))

    def lens(u, v, w, h):
        if rows:
            ch = rows[v][u]
            return {"F": frame, "G": glass, ".": None, "H": glint}[ch]
        if v == 0 or u in (0, w - 1):
            return frame
        if v == 1 and u == 1:
            return glint
        return glass

    paint_box(img, 0, 0, 4, 3, 1, lens, lambda u, v, w, h: shade(frame, -0.15))
    paint_box(img, 10, 0, 1, 1, 1, lambda u, v, w, h: shade(frame, 0.1), lambda u, v, w, h: frame)
    paint_box(img, 0, 5, 1, 1, 4, lambda u, v, w, h: frame, lambda u, v, w, h: shade(frame, -0.2))
    save(img, "glasses", name)


# ---------------------------------------------------------------- bandana
def bandana(name, painter):
    img = Image.new("RGBA", (36, 11), (0, 0, 0, 0))
    f = faces(0, 0, 9, 2, 9)
    for k in ("front", "back", "left", "right"):
        rect = f[k]
        x0, y0, w, h = rect
        for v in range(h):
            for u in range(w):
                img.putpixel((x0 + u, y0 + v), painter(k, u, v, w, h))
    for k in ("top", "bottom"):
        x0, y0, w, h = f[k]
        for v in range(h):
            for u in range(w):
                img.putpixel((x0 + u, y0 + v), painter("top", u, v, w, h))
    save(img, "bandana", name)


def band_base(c1, c2):
    c1, c2 = hexc(c1), hexc(c2)

    def base(v, h, u):
        c = lerp(c1, c2, v / max(1, h - 1))
        if v == 0 and u % 2 == 0:
            c = shade(c, 0.18)  # stitching
        return c
    return base


def bandana_red(k, u, v, w, h):
    c = band_base("#D02B33", "#8E1219")(v, h, u)
    if k in ("front", "left", "right") and (u + v * 3) % 5 == 2 and v == 1:
        c = hexc("#FFE9E9")  # paisley dots
    if k == "back" and 3 <= u <= 5:
        c = shade(c, -0.25 if v else 0.1)  # knot
    return c


def bandana_blue(k, u, v, w, h):
    c = band_base("#8CC8F0", "#3F6FD0")(v, h, u)
    if k != "top" and (u * 2 + v) % 6 == 1 and v == 1:
        c = hexc("#FFFFFF")
    if k == "back" and 3 <= u <= 5:
        c = shade(c, -0.25 if v else 0.15)
    return c


def bandana_skull(k, u, v, w, h):
    c = band_base("#26262E", "#0E0E12")(v, h, u)
    if k == "front" and u in (3, 5) and v == 0:
        c = hexc("#F4F4F4")
    if k == "front" and u == 4 and v == 1:
        c = hexc("#F4F4F4")
    if k == "back" and 3 <= u <= 5:
        c = shade(c, 0.3 if v == 0 else 0.15)
    return c


def bandana_navy(k, u, v, w, h):
    c = band_base("#27406E", "#14234A")(v, h, u)
    if k != "top" and (u + v) % 4 == 0:
        c = hexc("#E8EEFF")
    if k == "back" and 3 <= u <= 5:
        c = shade(c, -0.3 if v else 0.1)
    return c


# ---------------------------------------------------------------- bracelet
def bracelet(name, painter):
    img = Image.new("RGBA", (20, 7), (0, 0, 0, 0))
    f = faces(0, 0, 5, 2, 5)
    for k, (x0, y0, w, h) in f.items():
        for v in range(h):
            for u in range(w):
                img.putpixel((x0 + u, y0 + v), painter(k, u, v, w, h))
    save(img, "bracelet", name)


def brace_gold(k, u, v, w, h):
    if k in ("top", "bottom"):
        return hexc("#C48A1C")
    c = lerp(hexc("#FFE48A"), hexc("#B87A12"), v / max(1, h - 1))
    if k == "front" and u == 2:
        c = hexc("#FFF6C8") if v == 0 else hexc("#8E5B08")
    return c


def brace_silver(k, u, v, w, h):
    if k in ("top", "bottom"):
        return hexc("#8C94A3")
    c = lerp(hexc("#F2F5FA"), hexc("#99A2B2"), v / max(1, h - 1))
    if k == "front" and u == 2:
        return hexc("#6FD4FF") if v == 0 else hexc("#2B86C4")
    return c


def brace_rainbow(k, u, v, w, h):
    cols = ["#FF4D4D", "#FFA94D", "#FFE94D", "#5DDB6A", "#4DA6FF", "#A24DFF"]
    c = hexc(cols[(u + (0 if k in ("front", "back") else 2)) % 6])
    return shade(c, -0.18 if v == 1 else 0.05)


def brace_leather(k, u, v, w, h):
    c = lerp(hexc("#8A5A34"), hexc("#5B3820"), v / max(1, h - 1))
    if k in ("top", "bottom"):
        return hexc("#4A2D18")
    if v == 0 and u % 2 == 0:
        c = hexc("#E2C79B")  # stitching
    if k == "front" and u == 2 and v == 1:
        c = hexc("#C9B26A")  # rivet
    return c


def brace_neon(k, u, v, w, h):
    if k in ("top", "bottom"):
        return hexc("#101828")
    if v == 0:
        return hexc("#38F6FF")
    return lerp(hexc("#162036"), hexc("#0A1020"), 0.5)


def main():
    for n, s in WING_STYLES.items():
        wing(n, s)
    for n, c in HALO_STYLES.items():
        halo(n, c)
    for n, s in EAR_STYLES.items():
        ears(n, s)
    glasses("Classic", "#1B1B22", "#BFE6FF", 150, "#FFFFFF")
    glasses("Shades", "#0C0C10", "#14141C", 238, "#4A4A66")
    glasses("Gold Round", "#E2B33A", "#CFEAFF", 120, "#FFFFFF",
            rows=[".FF.", "FGHF", ".FF."])
    glasses("Hearts", "#E0365E", "#FF8FB4", 170, "#FFFFFF",
            rows=["FFFF", "FGHF", "FGGF"])
    glasses("Pixel", "#000000", "#000000", 255, "#FFFFFF",
            rows=["FFFF", "FHFF", "FFFF"])
    bandana("Red Classic", bandana_red)
    bandana("Glacier Blue", bandana_blue)
    bandana("Black Skull", bandana_skull)
    bandana("Navy Stripes", bandana_navy)
    bracelet("Gold Cuff", brace_gold)
    bracelet("Silver Gem", brace_silver)
    bracelet("Rainbow", brace_rainbow)
    bracelet("Leather", brace_leather)
    bracelet("Neon", brace_neon)


if __name__ == "__main__":
    main()
