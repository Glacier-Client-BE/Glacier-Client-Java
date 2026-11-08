"""Procedural generator for Glacier cosmetic accessory textures.
  python tools/gen_cosmetics.py          regenerate all accessory PNGs + tools/preview_<type>.png + tools/cosmetics_preview.png, then check
  python tools/gen_cosmetics.py --check  verify Accessories.java design lists, PNG presence and sizes (no writes)
  python tools/gen_cosmetics.py --capes  also rewrite the cape PNGs (off by default)
Design lists live in DESIGNS (registry order must equal Accessories.java)."""
import math, os
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/assets/glacier/textures/cosmetics")

def hx(s): s = s.lstrip('#'); return tuple(int(s[i:i+2], 16) for i in range(0, len(s), 2))
def clamp(v): return max(0, min(255, int(round(v))))
def mul(c, f): return tuple(clamp(a * f) for a in c[:3]) + tuple(c[3:])
def mix(a, b, t):
    r = tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))
    if len(a) > 3 or len(b) > 3:
        aa = a[3] if len(a) > 3 else 255; ba = b[3] if len(b) > 3 else 255
        r += (clamp(aa + (ba - aa) * t),)
    return r
def rnd(x, y, s=0):
    n = (x * 374761393 + y * 668265263 + s * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0
def vnoise(x, y, s=0, sc=3.0):
    fx, fy = x / sc, y / sc; x0, y0 = math.floor(fx), math.floor(fy); tx, ty = fx - x0, fy - y0
    tx = tx * tx * (3 - 2 * tx); ty = ty * ty * (3 - 2 * ty)
    a = rnd(x0, y0, s); b = rnd(x0 + 1, y0, s); c = rnd(x0, y0 + 1, s); d = rnd(x0 + 1, y0 + 1, s)
    return (a + (b - a) * tx) * (1 - ty) + (c + (d - c) * tx) * ty
BAY = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]
def ramp(cols, t, x, y):
    """Pick from palette with ordered dithering between adjacent steps; t in 0..1."""
    t = max(0.0, min(1.0, t)) * (len(cols) - 1)
    i = min(int(t), len(cols) - 2); f = t - i
    return cols[i + 1] if f > (BAY[y % 4][x % 4] + 0.5) / 16 else cols[i]

class Img:
    def __init__(self, w, h): self.im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    def put(self, x, y, c):
        if 0 <= x < self.im.width and 0 <= y < self.im.height and c is not None:
            a = c[3] if len(c) > 3 else 255
            if a == 0: return
            if max(c[:3]) < 24 and a > 200: c = mix(c, (30, 26, 44), 0.55)  # never opaque black
            self.im.putpixel((x, y), (c[0], c[1], c[2], a))
    def get(self, x, y): return self.im.getpixel((x, y))[:3]
    def face(self, u, v, w, h, fn):
        for y in range(h):
            for x in range(w): self.put(u + x, v + y, fn(x, y))
    def box(self, u, v, w, h, d, top, bot, front, right, left, back):
        self.face(u + d, v, w, d, top); self.face(u + d + w, v, w, d, bot)
        self.face(u, v + d, d, h, right); self.face(u + d, v + d, w, h, front)
        self.face(u + d + w, v + d, d, h, left); self.face(u + 2 * d + w, v + d, w, h, back)

# ---------------------------------------------------------------- capes (10x16 face)
W, H = 10, 16

def c_glacier(x, y):
    P = [hx(s) for s in ("2a3470", "4558a8", "7289da", "9fb2f0", "d4defc")]
    t = 0.4 + 0.5 * vnoise(x, y, 1, 3) - 0.25 * (y / 15)
    if (x + y * 0.6) % 7 < 0.7: t += 0.25
    if abs(((x * 0.6 - y) % 9) - 4.5) < 0.5: t -= 0.2
    for (cx, cy) in ((2, 3), (7, 9), (3, 13)):
        if (x, y) == (cx, cy): return P[4]
        if abs(x - cx) + abs(y - cy) == 1: return P[3]
    return ramp(P, t, x, y)

def c_midnight(x, y):
    P = [hx(s) for s in ("0a0e22", "141a3d", "1f2a5c", "2c3b7a")]
    t = 0.15 + 0.3 * (1 - y / 15) + 0.3 * vnoise(x, y, 2, 4)
    if (x, y) in {(6, 3), (7, 3), (5, 4), (5, 5), (6, 6)}: return hx("f4f1c8")
    if (x, y) in {(6, 4), (6, 5), (7, 6)}: return hx("c9c58f")
    for sx, sy, b in ((1, 1, 1), (8, 1, 0), (2, 6, 0), (8, 8, 1), (3, 10, 0), (7, 13, 1), (1, 14, 0), (4, 1, 0)):
        if (x, y) == (sx, sy): return hx("ffffff") if b else hx("aab6ff")
    return ramp(P, t, x, y)

def c_ember(x, y):
    P = [hx(s) for s in ("1b0603", "5a120a", "a8260c", "e2561a", "ff9a2e", "ffd36a")]
    flame = 0.5 + 0.5 * math.sin(x * 1.9 + rnd(x, 0, 3) * 4)
    t = max(0, (y / 15) ** 1.4 * 0.95 + (flame - 0.5) * 0.3 * (y / 15)) - 0.05
    if (x, y) in {(2, 4), (7, 2), (5, 8), (8, 11), (1, 9)}: return P[5]
    return ramp(P, t, x, y)

def c_moss(x, y):
    P = [hx(s) for s in ("15301a", "234a26", "2f5a2c", "4a7a3e", "6fa65e", "9bcf7e")]
    n = vnoise(x, y, 4, 2.5) * 0.7 + vnoise(x, y, 5, 1.2) * 0.3
    t = 0.2 + n * 0.7 - (y / 15) * 0.12
    if rnd(x, y, 9) > 0.93: return P[5]
    if rnd(x, y, 8) > 0.9: return P[0]
    return ramp(P, t, x, y)

def c_frost(x, y):
    P = [hx(s) for s in ("6fb6da", "9ccfe8", "cfe9f5", "e8f6fc", "ffffff")]
    t = 0.7 - (y / 15) * 0.35 + 0.2 * vnoise(x, y, 6, 3)
    if (x in (4, 5) and 3 <= y <= 12) or (y in (7, 8) and 2 <= x <= 7): return P[0] if (x + y) % 2 else P[1]
    if (x, y) in {(3, 5), (6, 5), (3, 10), (6, 10), (2, 7), (7, 7)}: return P[1]
    if (x, y) in {(4, 3), (5, 12), (2, 14), (8, 2), (1, 2)}: return P[4]
    return ramp(P[1:], t, x, y)

def c_violet(x, y):
    P = [hx(s) for s in ("2a1250", "3d1d70", "5b2f92", "7d4fb8", "a77fe0", "d0b4ff")]
    d = abs(((x + y) % 6) - 3) + abs(((x - y) % 6) - 3)
    t = 0.25 + 0.12 * d + (0.5 - y / 15) * 0.25
    if (x + y) % 6 == 0 and (x - y) % 6 == 0: return P[5]
    return ramp(P, t, x, y)

def c_sunset(x, y):
    P = [hx(s) for s in ("2a1245", "4b1f78", "6b2c91", "b3447e", "ff7a59", "ffb26b", "ffe08a")]
    col = ramp(P[1:6], y / 15, x, y)
    dx, dy = x - 4.5, y - 11
    if dx * dx + dy * dy <= 6: return mix(P[6], P[5], dy / 4 + 0.3)
    if y in (7, 9, 10) and rnd(x, y, 3) > 0.3: return mix(col, P[1], 0.45)
    return col

def c_mono(x, y):
    P = [hx(s) for s in ("0c0c0e", "1a1a1d", "26262a", "3a3a40")]
    t = 0.45 - y / 15 * 0.2 + 0.15 * vnoise(x, y, 7, 3)
    if x in (4, 5) and y not in (0, 15): return hx("e6e6e6") if x == 4 else hx("b4b4b8")
    if y in (3, 12) and 1 <= x <= 8: return hx("8a8a90")
    return ramp(P, t, x, y)

def c_aurora(x, y):
    P = [hx(s) for s in ("050d1c", "0a1b30", "0d2c3e", "114a4a")]
    base = ramp(P, 0.2 + 0.35 * (y / 15), x, y)
    if rnd(x, y, 11) > 0.965 and y > 8: return hx("e8f4ff")
    for i, (cols, off, amp) in enumerate((("2dffb4", 4, 2.2), ("33d6ff", 8, 2.0), ("b05cff", 11.5, 1.6))):
        cy = off + amp * math.sin(x * 0.7 + i * 1.7)
        dd = abs(y - cy)
        if dd < 2.6:
            c = hx(cols); a = 1 - dd / 2.6
            if a > 0.75: return mix(c, hx("ffffff"), 0.35)
            if a > 0.4: return c
            if a > 0.15: return mix(base, c, 0.45) if (x + y) % 2 == 0 else mix(base, c, 0.25)
    return base

def c_frostbite(x, y):
    P = [hx(s) for s in ("5fa4c6", "86c2de", "b4dcee", "d6eef8", "f2fafd")]
    t = 0.6 - y / 15 * 0.3 + 0.35 * vnoise(x, y, 12, 2.5)
    cr = {(5, 2), (4, 3), (4, 4), (5, 5), (6, 6), (6, 7), (5, 8), (4, 9), (4, 10), (3, 11), (3, 12), (2, 13), (5, 4), (7, 7), (8, 8), (6, 9)}
    if (x, y) in cr: return P[0]
    if (x - 1, y) in cr: return P[4]
    if rnd(x, y, 13) > 0.95: return P[4]
    return ramp(P, t, x, y)

def c_magma(x, y):
    P = [hx(s) for s in ("0c0605", "1c0e0a", "2e1610", "40201a")]
    L = [hx(s) for s in ("ff5a10", "ff8a1a", "ffc23a")]
    n = vnoise(x, y, 14, 3.0); n2 = vnoise(x, y, 15, 3.0)
    if abs(n - 0.5) < 0.07 or abs(n2 - 0.55) < 0.05:
        return L[2] if abs(n - 0.5) < 0.025 else (L[1] if abs(n - 0.5) < 0.05 else L[0])
    if abs(n - 0.5) < 0.13: return mix(P[3], L[0], 0.3)
    return ramp(P, 0.1 + n * 0.8, x, y)

def c_cyber(x, y):
    P = [hx(s) for s in ("0b0f1f", "180f33", "3a1255", "7a1a82", "b4208f")]
    t = 0.2 + y / 15 * 0.5 + (0.1 if (x + y) % 2 == 0 else 0)
    C = hx("37f2ff"); C2 = hx("ff4fd8")
    if x == 2 and y not in (6, 7, 8) or (x == 7 and 3 <= y <= 12 and y not in (6, 7, 8)): return mix(C, hx("0a2a4a"), 0.35 if y % 4 else 0)
    if (y == 5 and 2 <= x <= 7) or (y == 10 and 5 <= x <= 9): return C2 if x % 3 else mix(C2, P[0], 0.5)
    if (x, y) in {(2, 6), (7, 4), (7, 11), (5, 10), (9, 10), (2, 14)}: return hx("ffffff")
    return ramp(P, t, x, y)

def c_sakura(x, y):
    P = [hx(s) for s in ("d9708f", "ec93ac", "f6b3c6", "ffd0dd", "fff0f5")]
    t = 0.45 + 0.25 * vnoise(x, y, 16, 3) - y / 15 * 0.1
    for px, py in ((2, 2), (7, 4), (4, 8), (8, 11), (1, 12), (6, 14)):
        r = (x - px, y - py)
        if r in {(0, 0), (1, 0), (0, 1)}: return P[4] if r != (1, 0) else P[3]
        if r == (1, 1): return hx("b95a78")
    return ramp(P[:4], t, x, y)

def c_ocean(x, y):
    P = [hx(s) for s in ("031a33", "062d52", "0a4575", "0c5f90", "1a86b8", "4fb3d9")]
    t = 1 - y / 15 * 0.95
    wv = y + 1.2 * math.sin(x * 1.1)
    if abs(wv - 3) < 0.6 or abs(wv - 7.5) < 0.5: return P[5]
    if abs(wv - 3) < 1.4: return P[4]
    if (x, y) in {(2, 10), (7, 12), (5, 14), (8, 9)}: return hx("a8e4ff")
    return ramp(P, t * 0.85 + 0.1 * vnoise(x, y, 17, 3), x, y)

def c_royal(x, y):
    P = [hx(s) for s in ("1f0d3d", "2e1458", "3a1a6b", "50288c")]
    G = [hx(s) for s in ("8a6a1a", "c79a2c", "e8c14a", "fff0a0")]
    t = 0.3 + 0.35 * (1 - y / 15) + (0.12 if (x + y) % 4 == 0 else 0)
    if (x in (1, 8) and 1 <= y <= 14) or (y in (1, 14) and 1 <= x <= 8): return G[2] if (x + y) % 3 else G[1]
    cr = {(4, 4), (5, 4), (3, 5), (4, 5), (5, 5), (6, 5), (2, 6), (4, 6), (5, 6), (7, 6), (3, 7), (4, 7), (5, 7), (6, 7), (3, 8), (4, 8), (5, 8), (6, 8)}
    if (x, y) in cr: return G[3] if y == 6 and x in (2, 7) else (G[2] if y < 8 else G[1])
    if y == 9 and 2 <= x <= 7: return G[0]
    return ramp(P, t, x, y)

def c_galaxy(x, y):
    P = [hx(s) for s in ("05030f", "0f0a2a", "1c1052", "3a1a80", "6a2cc0", "b24be0")]
    n = vnoise(x + y * 0.5, y, 18, 3.5) * 0.7 + vnoise(x, y, 19, 1.6) * 0.3
    t = 0.05 + max(0, n - 0.35) * 1.3
    if rnd(x, y, 20) > 0.92: return hx("ffffff") if rnd(x, y, 21) > 0.5 else hx("b6c8ff")
    if rnd(x, y, 22) > 0.95: return hx("ff9ad8")
    return ramp(P, t, x, y)

def c_glacier2(x, y):
    P = [hx(s) for s in ("a9b6e8", "c7d1f4", "e2e8fb", "ffffff")]
    B = hx("7289da"); Bd = hx("4a5fb8")
    t = 0.7 - y / 15 * 0.35 + 0.15 * vnoise(x, y, 23, 3)
    ax, ay = abs(x - 4.5), abs(y - 7.5)
    if ax < 1 and ay < 1: return B
    if (ax < 1 and ay <= 5) or (ay < 1 and ax <= 4.5): return B if ay < 4 or ax < 4 else Bd
    if abs(ax - ay) < 0.9 and ax <= 4: return Bd if (int(ax) % 2) else B
    return ramp(P, t, x, y)

def c_checker(x, y):
    A = [hx(s) for s in ("0a0d1c", "101426", "1a2040")]; Lc = [hx(s) for s in ("9fb4e8", "cfe0ff", "e8f0ff")]
    c = ((x // 2) + (y // 2)) % 2
    inner = (x % 2 == 0 and y % 2 == 0)
    if c: col = Lc[2] if inner else Lc[1]
    else: col = A[2] if inner else A[1]
    if c and (x % 2 == 1) and (y % 2 == 1): col = Lc[0]
    return mul(col, 1 - y / 15 * 0.25)

def c_sunburst(x, y):
    P = [hx(s) for s in ("d9701a", "ff9a2e", "ffb347", "ffd35a", "fff0a0")]
    dx, dy = x - 4.5, y - 7.5
    ang = math.atan2(dy, dx); r = math.hypot(dx, dy)
    t = 0.3 + 0.45 * (math.cos(ang * 6) * 0.5 + 0.5) - r * 0.05
    if r < 1.9: return P[4] if r < 1.2 else P[3]
    if r < 2.6: return P[0]
    return ramp(P, t + 0.1, x, y)

HEART = ["..XX..XX..", ".XXXXXXXX.", ".XXXXXXXX.", ".XXXXXXXX.", "..XXXXXX..", "...XXXX...", "....XX...."]
def c_heart(x, y):
    P = [hx(s) for s in ("a3162a", "d92640", "ff3b4e", "ff6b7a")]
    t = 0.35 + 0.25 * vnoise(x, y, 24, 3) - y / 15 * 0.1
    hy = y - 4
    if 0 <= hy < len(HEART) and HEART[hy][x] == "X":
        if (x, hy) in {(2, 1), (3, 1)}: return hx("ffd0d6")
        if hy >= 4: return hx("a3162a")
        return hx("ff2a40") if hy < 3 else hx("e01e38")
    if (x, y) in {(1, 1), (8, 2), (1, 12), (8, 13), (2, 13), (7, 1)}: return P[3]
    return ramp(P[:3], t, x, y)

CAPES = {"glacier": c_glacier, "midnight": c_midnight, "ember": c_ember, "moss": c_moss, "frost": c_frost,
         "violet": c_violet, "sunset": c_sunset, "mono": c_mono, "aurora": c_aurora, "frostbite": c_frostbite,
         "magma": c_magma, "cyber": c_cyber, "sakura": c_sakura, "ocean": c_ocean, "royal": c_royal,
         "galaxy": c_galaxy, "glacier2": c_glacier2, "checker": c_checker, "sunburst": c_sunburst, "heart": c_heart}

def make_cape(fn):
    img = Img(64, 32)
    raw = [[fn(x, y) for x in range(W)] for y in range(H)]
    def front(x, y):
        c = raw[y][x]
        c = mul(c, 1.06 - 0.16 * (y / 15) + (rnd(x, y, 99) - 0.5) * 0.05)
        if x == 1 and 1 <= y <= 14: c = mix(c, (255, 255, 255), 0.07)
        if x in (0, 9) or y == 15: c = mul(c, 0.5)
        elif y == 0: c = mul(c, 0.72)
        elif y == 14: c = mul(c, 0.82)
        return c
    img.face(1, 1, W, H, front)
    img.face(12, 1, W, H, lambda x, y: mul(img.get(1 + (9 - x), 1 + y), 0.72))
    img.face(0, 1, 1, H, lambda x, y: mul(img.get(1, 1 + y), 0.62))
    img.face(11, 1, 1, H, lambda x, y: mul(img.get(10, 1 + y), 0.62))
    img.face(1, 0, W, 1, lambda x, y: mul(img.get(1 + x, 1), 0.6))
    img.face(11, 0, W, 1, lambda x, y: mul(img.get(1 + x, 16), 0.5))
    return img


# ================================================================ accessories
# Face layouts are fixed by AccessoryModels.java; every design only paints pixels inside those faces.
from types import SimpleNamespace as NS

def rp(cols, t, x, y, k=2.3):
    """Palette ramp with a sharpened ordered dither: bands stay readable, only the seams between steps are dithered."""
    t = max(0.0, min(1.0, t)) * (len(cols) - 1)
    i = min(int(t), len(cols) - 2); f = max(0.0, min(1.0, .5 + (t - i - .5) * k))
    return cols[i + 1] if f > (BAY[y % 4][x % 4] + 0.5) / 16 else cols[i]

def P(s): return [hx(t) for t in s.split()]
def lit(c, f): return mix(c, (255, 255, 255), f)
def dim(c, f): return mix(c, (14, 8, 30), f)
def opq(c): return tuple(c[:3])
def alpha(c, a): return tuple(c[:3]) + (a,)

def grid_pat(rows, pal):
    """Rows of palette-index characters ('.' = transparent) -> 2D list of colours."""
    return [[None if ch == '.' else pal[int(ch, 16)] for ch in row] for row in rows]

# ---------------------------------------------------------------- wings
LEN = [11, 14, 15, 13, 9]          # feather lengths fixed by the model
def wctx(c, r):
    i, x = divmod(c, 3); L = LEN[i]
    return NS(c=c, r=r, i=i, x=x, L=L, g=r / (L - 1), u=c / 14.0)
def sc(w, st=0): return (w.r + st * w.i - (1 if w.x == 1 else 0)) % 4 == 3   # scalloped overlapping-feather line
def nz(w, s=1, k=3.0): return vnoise(w.c, w.r, s, k)
def tipcut(w, k=2): return w.L - (0 if w.x == 1 else k)

def ol_default(c): return mix(mul(c, 0.5), (40, 14, 70), 0.18)
def ol_soft(c): return mix(mul(c, 0.68), (70, 40, 120), 0.12)

def wing_grid(fn, ol):
    g = [[None] * 15 for _ in range(15)]
    for c in range(15):
        for r in range(LEN[c // 3]): g[c][r] = fn(wctx(c, r))
    def inside(c, r):
        if c < 0 or r < 0: return True
        if c > 14 or r > 14: return False
        return g[c][r] is not None
    out = [[None] * 15 for _ in range(15)]
    for c in range(15):
        for r in range(15):
            col = g[c][r]
            if col is None: continue
            if ol:
                lo, ro, bo, to = (not inside(c - 1, r)), (not inside(c + 1, r)), (not inside(c, r + 1)), (not inside(c, r - 1))
                if bo or to or lo != ro: col = ol(col)       # thin 1px spars keep their colour, only their tips get outlined
            out[c][r] = col
    return out

def make_wing(spec):
    fn, ol, bone = spec
    cv = wing_grid(fn, ol)
    bhi, bmid, blo = bone
    img = Img(64, 32)
    def bf(x, y):
        c = bhi if y == 0 else bmid
        if x in (0, 14): return blo
        if x % 5 == 0: return mix(c, blo, 0.35)
        return c
    img.box(0, 0, 15, 2, 2, lambda x, y: bmid, lambda x, y: blo, lambda x, y: mul(bf(x, y), 0.8),
            lambda x, y: blo, lambda x, y: blo, lambda x, y: bf(x, y))
    def shade(c, f): return None if c is None else mul(c, f)
    for i in range(5):
        L = LEN[i]
        def cell(x, y, i=i): return cv[3 * i + x][y] if 0 <= y < 15 else None
        back = lambda x, y, i=i: cell(x, y)
        front = lambda x, y, i=i: shade(cell(2 - x, y), 0.72)
        img.box(i * 8, 5, 3, L, 1, lambda x, y, i=i: shade(cell(x, 0), 0.55), lambda x, y, i=i, L=L: shade(cell(x, L - 1), 0.6),
                front, lambda x, y, i=i: shade(cell(0, y), 0.6), lambda x, y, i=i: shade(cell(2, y), 0.6), back)
        img.box(i * 8, 22, 3, 6, 1, lambda x, y, i=i: shade(cell(x, 0), 0.55), lambda x, y, i=i: shade(cell(x, 5), 0.6),
                front, lambda x, y, i=i: shade(cell(0, y), 0.6), lambda x, y, i=i: shade(cell(2, y), 0.6), back)
    return img

# --- palettes + painters (return a colour, or None for transparent)
AN = P("7d8bb8 a9b6dc d2dbf0 eef2fc ffffff ffd878")
def w_angel(w):
    if w.r >= tipcut(w): return None
    if w.x == 1 and w.r >= w.L - 2: return AN[5]
    t = .8 - .4 * w.g + (.08 if w.x == 1 else 0) - (.36 if sc(w) else 0) + (.1 if (w.r - (1 if w.x == 1 else 0)) % 4 == 0 else 0) + .04 * (1 - w.u)
    if w.r < 1: t -= .2
    return rp(AN[:5], t, w.c, w.r)

BA = P("1a0e26 2e1a42 4a2866 6c3c8c 9a5cb4 caa0e4")
def w_bat(w):
    bone = w.x == 1
    if w.r >= (w.L if bone else w.L - 3 - (1 if w.x == 2 else 0)): return None
    if bone: return rp(BA[3:], .55 - .4 * w.g + (.3 if w.r < 2 else 0), w.c, w.r)
    if w.r < 2: return BA[3] if w.r else BA[4]
    t = .3 + .45 * (1 - w.g) + .1 * (nz(w, 3, 2) - .5) - (.13 if w.r % 5 == 4 else 0)
    return rp(BA[:5], t, w.c, w.r)

DR = P("123416 1f5a1a 359428 62cc3c b2ee68 e8f8b0")
DRR = P("4a1008 8c241a d4503a f08a5a")
BONE = P("8a7a58 c8b88a f2e6c0")
def w_dragon(w):
    fingers = w.x == 1
    lim = w.L if fingers else w.L - 3 - (1 if w.x == 0 else 0)
    if w.r >= lim: return None
    if fingers: return BONE[2] if w.r >= w.L - 2 else rp(BONE, .6 - .35 * w.g, w.c, w.r)
    if w.r < 1: return BONE[1]
    t = .66 - .42 * w.g + .1 * (nz(w, 4, 2) - .5) + (.07 if (w.c + w.r) % 2 == 0 else 0) + (.1 if w.x != 1 and w.r % 4 == 2 else 0)
    base = rp(DR[:5], t, w.c, w.r)
    if w.r >= lim - 2: base = mix(base, DRR[2], .5 + .2 * (w.r - (lim - 2)))
    return base

IC = P("24609a 3e96dc 7ad2ff c0f0ff ffffff")
def w_ice(w):
    if w.r >= w.L - (0 if w.x == 1 else 2) - ((w.i + w.x) % 2): return None
    t = .62 - .4 * w.g + (.15 if w.x == 1 else 0)
    if (w.c * 2 + w.r) % 7 == 0: t += .3                        # diagonal facet lines
    if (w.c + w.r) % 6 == 3: t -= .22
    if w.x == 1 and w.r >= w.L - 3: return IC[4]
    if rnd(w.c, w.r, 12) > .94: return IC[4]
    return rp(IC, t, w.c, w.r)

BU = [P("5a1a6a b83a8a ff5fa8 ff9ac8 ffd0e4"), P("7a3a0a d86a14 ffa22e ffcb6a ffe9b0"), P("1a4a7a 2a8ac8 4ad0ff 98ecff d8faff")]
def w_butter(w):
    # rounded wing with 3 colour zones, dark veins, white-dotted border and an eye spot
    d = math.hypot((w.c - 7.0) / 7.8, (w.r - 6.0) / 9.2)
    if d > 1.0 or w.r >= w.L: return None
    zone = BU[0] if w.g < .38 else (BU[1] if w.g < .72 else BU[2])
    t = .3 + .6 * (1 - d) + .1 * (nz(w, 4, 2) - .5)
    if abs(((math.atan2(w.r, w.c + .5) * 7) % 1) - .5) < .13 and d < .85: return zone[0]    # veins
    if d > .84: return BU[0][0] if (w.c + w.r) % 2 or d > .94 else BU[0][4]
    e = math.hypot(w.c - 7, w.r - 7)
    if e < 2.7: return (255, 255, 255) if e < .9 else ((40, 20, 70) if e > 1.9 else (255, 200, 90))
    return rp(zone, t, w.c, w.r)

def bay(x, y): return (BAY[y % 4][x % 4] + 0.5) / 16

PH = P("38080a 8a1408 c9300c f2641a ffa42a ffd860 fff4b8")
def w_phoenix(w):
    edge = w.L if w.x == 1 else w.L - 2 - ((w.i + w.x) % 2) - (1 if w.i == 4 else 0)
    if w.r >= edge: return None
    if w.x == 1 and w.r >= edge - 2: return PH[6] if w.r == edge - 1 else PH[5]
    t = .1 + .78 * w.g ** 1.1 + .1 * (nz(w, 1, 2) - .5) + (.1 if w.x == 1 else 0) - (.12 if sc(w) else 0)
    return rp(PH, t, w.c, w.r)

CR = P("2a1458 4c2a98 7a4ee0 a984ff d2baff f4ecff")
def w_crystal(w):
    if w.r >= tipcut(w, 3): return None
    a, b = (w.c + w.r) // 3, (w.c - w.r) // 3
    if (w.c + w.r) % 3 == 0 and (w.c - w.r) % 3 == 0: return CR[5]
    if (w.c + w.r) % 3 == 0 or (w.c - w.r) % 3 == 0: return CR[4] if rnd(a, b, 4) > .4 else CR[3]
    return rp(CR[:5], .22 + .6 * rnd(a, b, 4) + .12 * (1 - w.g), w.c, w.r)

ME = P("1c2230 36404f 566275 8392a8 b4c3d8 e4eefc")
CY = P("0a5a78 18b4e0 7cf0ff e8ffff")
def w_mecha(w):
    if w.r >= w.L - (2 - w.x): return None
    if w.r == 0: return ME[4]
    if w.x == 1 and 2 <= w.r < w.L - 3: return CY[1 + (w.r % 2)] if w.r % 4 else CY[3]
    if w.r % 5 == 4: return ME[0]
    if w.x != 1 and w.r % 5 == 1: return ME[5]                               # rivets
    t = .3 + .4 * (1 - w.g) + (.12 if w.x == 2 else 0) - (.1 if w.x == 0 else 0)
    return rp(ME, t, w.c, w.r)

VO = P("120a22 241042 3c1a68 5a2ca0 8248d8 c490ff")
def w_void(w):
    if w.g > .5 and rnd(w.c, w.r, 7) > 1 - (w.g - .5) * 1.6 + .25: return None
    if w.r >= tipcut(w, 2): return None
    if abs(nz(w, 3, 3.5) - .5) < .04: return VO[5]
    if rnd(w.c, w.r, 8) > .955: return (236, 222, 255)
    return rp(VO[:5], .08 + .3 * nz(w, 9, 3) + .3 * (1 - w.g), w.c, w.r)

FA = P("a07ad8 c9a6f0 e6ccff ffe6ff ffffff 7adcd8")
def w_fairy(w):
    d = math.hypot((w.c - 7.5) / 7.8, (w.r - 6.5) / 8.0)
    lower = math.hypot((w.c - 5.0) / 4.4, (w.r - 12.0) / 3.2) < 1
    if (d > 1 and not lower) or w.r >= w.L: return None
    if rnd(w.c, w.r, 5) > .955: return (255, 255, 255)
    if abs(((math.atan2(w.r + 1, w.c + 1.5) * 6) % 1) - .5) < .12: return alpha(FA[3], 235)      # veins
    base = rp([FA[1], FA[2], FA[3]], .3 + .5 * (1 - d) + .1 * (nz(w, 2) - .5), w.c, w.r)
    return alpha(mix(base, FA[5], .25 * w.g), 150 if bay(w.c, w.r) > .3 else 190)

MO = P("3a2410 664426 9a7040 c9a466 e8d09a f7ecc8")
def w_moth(w):
    if w.r >= w.L - 1 - (2 if rnd(w.c, 2, 3) > .5 else 0) - (2 if w.x != 1 else 0) * (rnd(w.c, 5, 3) > .6): return None
    e = math.hypot(w.c - 8, w.r - 7.5)
    if e < 3.1: return (250, 244, 220) if e < .9 else ((222, 138, 40) if e < 1.9 else MO[0])
    band = abs(w.r - (10 + 1.4 * math.sin(w.c * .8)))
    if band < .9: return MO[1] if band < .5 else MO[2]
    t = .5 + .3 * (nz(w, 6, 2) - .5) - .25 * w.g + (.18 if w.r < 4 else 0)
    if w.r < 5 and rnd(w.c, w.r, 6) > .55: t += .2                              # fuzzy shoulder
    return rp(MO[1:], t, w.c, w.r)

RV = P("12141e 1c2132 2a3250 3c4868 5a6a96 94a6d4")
def w_raven(w):
    if w.r >= w.L - (0 if w.x == 1 else 3 - (w.i % 2)): return None
    t = .42 + .3 * (1 - w.g) + (.22 if w.x == 1 else 0) - (.3 if sc(w) else 0) + (.1 if w.x == 0 else 0) + (.1 if (w.r - (1 if w.x == 1 else 0)) % 4 == 0 else 0)
    if rnd(w.c, w.r, 3) > .965: return RV[5]
    return rp(RV[:5], t, w.c, w.r)

SE = P("8a5a14 c98a22 f0c040 ffe58a fff6cc ffffff")
CRM = P("b8a47c e0d2ae f6eed8 ffffff")
def w_seraph(w):
    edge = tipcut(w, 2)
    if w.r >= edge: return None
    if w.r < 2: return SE[3] if w.r == 0 else SE[1]
    if w.x == 1 and w.r == 4: return (222, 40, 66)
    if w.r >= edge - 3: return rp(SE[:5], .35 + .35 * (w.r - (edge - 3)) / 2 + (.1 if w.x == 1 else 0), w.c, w.r)
    t = .8 - .5 * w.g + (.12 if w.x == 1 else 0) - (.16 if sc(w) else 0)
    return rp(CRM, t, w.c, w.r)

CG = P("0e1236 182260 24348a 3448b4")
def w_cyber(w):
    if w.r >= w.L - (2 - w.x) - (1 if w.x == 0 else 0) * 0: return None
    vr, hr = w.c % 3 == 0, w.r % 4 == 1
    if vr and hr: return (255, 255, 255)
    if vr or hr: return (40, 232, 255) if (w.r % 8 < 4 or hr) else (30, 150, 220)
    if w.x == 1 and w.r == w.L - 3: return (255, 70, 215)
    return rp(CG, .15 + .5 * (1 - w.g) + (.12 if (w.c + w.r) % 2 == 0 else 0), w.c, w.r)

LVR = P("22120c 3a2014 56321e 784832")
MG = P("c82a08 ff5a10 ff9a22 ffd54a")
def w_lava(w):
    if w.r >= w.L - (0 if w.x == 1 else 2) - int(2.4 * rnd(w.c, 1, 3)): return None
    n = vnoise(w.c, w.r, 5, 3.2)
    d = abs(n - .5)
    if d < .035: return MG[3] if d < .015 else MG[2]
    if d < .07: return MG[1]
    if d < .11: return mix(rp(LVR, .6, w.c, w.r), MG[0], .6)
    if w.r >= w.L - 2 and rnd(w.c, w.r, 4) > .4: return MG[2]
    return rp(LVR, .12 + .75 * vnoise(w.c, w.r, 6, 2.2) - .15 * w.g, w.c, w.r)

ST = P("1e2438 34405e 52628a 7a8cb0 acbcd8 dce6f4")
BOLT = {(8, 1), (7, 2), (7, 3), (6, 4), (7, 5), (6, 6), (6, 7), (5, 8), (6, 9), (5, 10), (5, 11), (4, 12)}
def w_storm(w):
    bump = int(1 + 1.6 * abs(math.sin(w.c * 1.15 + .6)))
    if w.r >= w.L - bump - (0 if w.g < 2 else 0): return None
    if (w.c, w.r) in BOLT: return (255, 252, 200) if w.r % 2 else (255, 226, 70)
    if any((w.c + dx, w.r + dy) in BOLT for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))): return mix(ST[4], (255, 230, 90), .55)
    return rp(ST, .12 + .55 * nz(w, 6, 2.6) + .3 * (1 - w.g) - .08 * (w.r >= w.L - 2), w.c, w.r)

CD = P("c03a78 ff6fa8 ff9cc4 ffd0e4 ffffff")
MI = P("5ec8a8 96f0d4 d2fff0")
def w_candy(w):
    if w.r >= w.L - (0 if w.x == 1 else 1): return None
    s = ((w.c + w.r) // 2) % 3
    t = .75 - .3 * w.g + (.12 if w.x == 0 and w.r % 5 == 1 else 0)
    if rnd(w.c, w.r, 8) > .95: return (255, 232, 90) if w.c % 2 else (110, 190, 255)
    if s == 0: return rp(CD[:4], t, w.c, w.r)
    if s == 1: return rp([CD[3], CD[4]], t + .2, w.c, w.r)
    return rp(MI, t, w.c, w.r)

RBW = [hx(s) for s in ("ff3b3b", "ff8a2a", "ffd92a", "3ed860", "2a9bff", "9a4aff")]
def w_rainbow(w):
    if w.r >= w.L - (0 if w.x == 1 else 1) - (1 if w.i == 4 else 0): return None
    band = min(5, w.r * 6 // 14); pos = w.r * 6 / 14 - band
    c = RBW[band]
    if pos < .17: c = lit(c, .35)
    elif pos > .72: c = mul(c, .76)
    if rnd(w.c, w.r, 9) > .965: return (255, 255, 255)
    return c

SK = P("5a4a38 8a7a60 b8aa88 ddd2b4 f4ecd2 fffbe8")
def w_skeleton(w):
    bonecols = {2: 0, 3: 1, 5: 0, 6: 1, 8: 0, 9: 1, 11: 0, 12: 1}
    if w.c in bonecols:
        if w.r >= w.L - (0 if bonecols[w.c] else 1): return None
        t = .9 if bonecols[w.c] == 0 else .55
        if w.r % 5 == 0 and w.r: t -= .25                                  # joints
        return rp(SK[1:], t - .12 * w.g, w.c, w.r)
    if w.r < 2: return SK[3] if w.r == 0 else SK[2]
    if w.r in (6, 10) and w.c < 13 and w.r < w.L - 1: return SK[2] if w.r == 6 else SK[1]
    if w.r < w.L - 3 and rnd(w.c, w.r, 3) > .82: return alpha(SK[1], 200)
    return None

SP = P("2a1808 5a3a14 8a5a1e b88434 e0b458 ffe6a0")
TL = P("143e3c 2a8078 5ac8b8")
def w_steam(w):
    if w.r >= w.L - (1 if w.x != 1 else 0): return None
    dx, dy = w.c - 7, w.r - 7; d = math.hypot(dx, dy)
    if w.i in (2,) or (w.i == 1 and w.x == 2) or (w.i == 3 and w.x == 0):                   # gear
        if d < 1.2: return SP[0]
        if d < 3.6:
            tooth = d > 2.7 and (int(round(math.atan2(dy, dx) / (math.pi / 4))) % 2 == 0)
            if 2.0 < d < 2.8 or (d > 2.8 and tooth): return TL[2] if dx + dy < 0 else TL[1]
            if d < 2.0 and (abs(dx) < .6 or abs(dy) < .6): return TL[1]
    if w.r % 5 == 4: return SP[0]
    if w.x != 1 and w.r % 5 == 1: return SP[5]
    return rp(SP, .3 + .45 * (1 - w.g) + (.1 if w.x == 1 else 0) + (.06 if w.r % 5 == 0 else 0), w.c, w.r)

GL = P("2a3470 4558a8 7289da 9fb2f0 d4defc ffffff")
def w_glacier(w):
    if w.r >= w.L - (0 if w.x == 1 else 1) - (1 if (w.i + w.x) % 2 else 0): return None
    dx, dy = w.c - 7, w.r - 6
    if (dx == 0 and abs(dy) <= 2) or (dy == 0 and abs(dx) <= 2): return GL[5]
    if abs(dx) == abs(dy) and 1 <= abs(dx) <= 2: return GL[4]
    if w.r >= w.L - 2: return GL[4] if w.x != 1 else GL[5]
    t = .72 - .38 * w.g + (.1 if w.x == 1 else 0) - (.15 if sc(w) else 0) + .08 * (nz(w, 3, 2) - .5)
    if rnd(w.c, w.r, 13) > .95: return GL[5]
    return rp(GL[:5], t, w.c, w.r)

GX = P("0a0620 160c3c 261468 42209a 6e32cc aa52e8")
def w_galaxy(w):
    if w.r >= w.L - (0 if w.x == 1 else 1): return None
    n = vnoise(w.c + w.r * .5, w.r, 9, 3.4) * .65 + vnoise(w.c, w.r, 10, 1.7) * .35
    if rnd(w.c, w.r, 20) > .92: return (255, 255, 255) if rnd(w.c, w.r, 21) > .5 else (170, 196, 255)
    if rnd(w.c, w.r, 22) > .955: return (255, 154, 216)
    if (w.c, w.r) in ((4, 4), (10, 8)): return (255, 255, 255)
    if (abs(w.c - 4) + abs(w.r - 4) == 1) or (abs(w.c - 10) + abs(w.r - 8) == 1): return GX[5]
    return rp(GX, .02 + max(0, n - .3) * 1.7, w.c, w.r)

AUP = [P("4a0a10 8a1420 c8283a ff6a6a"), P("6a2a08 b8541a f08a2a ffb860"), P("6a4a08 c89418 ffd040 fff08a"),
       P("5a1a08 a8401a e06a2a ffa060"), P("3a2008 6a3e1a a0622c d09050")]
def w_autumn(w):
    if w.r >= w.L - (0 if w.x == 1 else 3): return None
    p = AUP[w.i]
    if w.x == 1: return lit(p[3], .2) if w.r > 1 else p[2]                           # midrib
    if (w.x == 0 and w.r % 3 == 0) or (w.x == 2 and w.r % 3 == 1): return p[3]       # herringbone veins
    return rp(p[:3], .75 - .5 * w.g + .08 * (nz(w, 5, 2) - .5) + (.12 if w.x == 0 else 0), w.c, w.r)

SA = P("b84e70 e07a9c f4a6bf ffcfdc ffe8ef ffffff")
def w_sakura(w):
    edge = w.L - (3 if w.x == 1 else (1 if w.x == 0 else 0)) - (1 if w.i == 4 else 0)
    if w.r >= edge: return None
    if w.r < 2 and w.x == 1: return (255, 220, 120)
    t = .98 - .72 * w.g + .08 * (nz(w, 7, 2) - .5) - (.12 if w.x == 0 and w.i else 0)
    if w.x == 1 and w.r % 3 == 0: t -= .2                                             # petal veins
    if rnd(w.c, w.r, 3) > .96: return SA[5]
    return rp(SA[:5], t, w.c, w.r)

TX = P("0e2410 18421a 2c7a1e 54c42a 9aee3a d8ff6a")
BUB = {(4, 5), (10, 4), (8, 9), (3, 11)}
def w_toxic(w):
    if w.r >= w.L - 1 - int(5 * rnd(w.c // 2, 1, 3)) * (0 if w.x == 1 else 1): return None
    for bx, by in BUB:
        d = abs(w.c - bx) + abs(w.r - by)
        if d == 0: return TX[5]
        if d == 1 and (w.c, w.r) != (bx + 1, by) : return TX[4]
    n = nz(w, 5, 3.0)
    if abs(n - .5) < .06: return TX[4]
    return rp(TX[:5], .06 + .42 * n + .32 * w.g, w.c, w.r)

GP = P("5a3a08 8c5c10 c48a1e e8b830 ffd850 fff0a0 ffffff")
def w_gold(w):
    if w.r >= w.L - (1 if w.x != 1 else 0): return None
    if w.x == 1 and w.r >= w.L - 2: return GP[5]
    t = .3 + .45 * (1 - w.g) - (.2 if sc(w) else 0)
    if (w.c - w.r) % 6 == 0: t += .38                                                  # specular band
    if (w.c - w.r) % 6 == 1: t += .15
    if w.x == 1 and w.r % 4 == 0: t -= .2
    return rp(GP, t + .08, w.c, w.r)

SH = P("1a1c2c 2e3652 46547c 6a7ca8 9aaad4")
def w_shadow(w):
    if w.r >= w.L - 1 - int(3.2 * rnd(w.c, 2, 3)) - (1 if w.x != 1 else 0): return None
    streak = abs(((w.c * 1.6 + w.r * .5 + nz(w, 3, 3) * 3) % 4) - 2) < .4
    c = rp(SH, .22 + .3 * (1 - w.g) + (.3 if streak else 0) + .12 * (nz(w, 4, 2) - .5), w.c, w.r)
    if w.g > .5 and bay(w.c, w.r) > 1.25 - w.g: return alpha(c, 140)
    if (w.c, w.r) in ((6, 5), (8, 5)): return (150, 235, 255)
    return c

IS = P("1f5f9a 3a96d0 7cd4f4 b8f0ff e8fcff ffffff")
def w_shard(w):
    keep = w.L if w.x == 1 else max(3, int(w.L * .45) - (w.i % 2) - 1)
    if w.r >= keep: return None
    if w.r >= keep - 2 and w.x == 1: return IS[5]
    t = [.55, .8, .3][w.x] + .25 * (1 - w.g) + (.12 if (w.r % 5 == 2) else 0)
    if w.x == 1 and w.r % 4 == 3: t -= .25
    return rp(IS[:5], t, w.c, w.r)

DF = P("2a6a78 4aa8b8 8ae0e8 c8faff ffffff")
def w_dragonfly(w):
    if w.r >= w.L - (1 if w.i in (0, 4) else 0): return None
    if w.c in (13, 14) and w.r in (1, 2, 3): return (60, 40, 70)                       # pterostigma
    vein = w.c % 3 == 1 or w.r % 4 == 2 or w.r == 0
    if vein: return alpha(DF[1], 245) if w.r % 4 == 2 or w.c % 3 == 1 and w.r % 2 else alpha(DF[2], 235)
    return alpha(rp(DF[2:], .35 + .5 * (1 - w.g) * nz(w, 2, 2.5), w.c, w.r), 130 + int(60 * w.g))

# ---------------------------------------------------------------- halo (16x8: top 5x2, sides, 5x1 front/back)
def D(c, f): return None if c is None else dim(c, f)

HALOS = {
 "Gold Halo":   ("a06a0c d8a020 ffd84a fff2a0 ffffe0 70440a", "23432", "12121", "13431"),
 "Ice Halo":    ("3a86b8 6cc4ee b4eaff f4feff ffffff 2a5e90", "23532", "12321", "13531"),
 "Neon Halo":   ("a00e8a ff3fd0 ff9cf0 ffffff ff3fd070 6a0a60", "12321", "04140", "13531"),
 "Thorn":       ("2a4a1a 4a7a2a 7ab048 5a2a1a e8e0c0 c0302a", "12421", "01210", "14241"),
 "Laurel":      ("2a5a22 4a8a32 7cc050 c8e878 e8c040 fff0a0", "23132", "12021", "32423"),
 "Rainbow":     ("ff4a4a ff9a2a ffe040 4adc6a 4a9aff a82a2a b8681a b89a1a 2a9a40 2a62b8", "01234", "56789", "01234"),
 "Neon Lime":   ("2a9a10 5aee22 a8ff70 f0ffd8 5aee2270 1a6a0c", "12321", "04140", "13531"),
 "Eclipse":     ("20102c 4a2060 ff9a2a ffd870 fff6d0 8a3a10", "23432", "01110", "23132"),
 "Storm":       ("2c3652 4e5c80 8394b8 c4d0e8 ffe84a fffbb0", "12321", "01210", "12421"),
 "Flame":       ("6a0a08 c8280c f2641a ffa42a ffe060 fff4b0", "12342", "01231", "23432"),
 "Crystal":     ("4a2a9a 7a52e0 b090ff e0d0ff ffffff 40e0e0", "13531", "02320", "24542"),
 "Void":        ("1a0a2c 3a1a5a 6a2cb0 a65cff c890ff 241040", "12321", "01210", "13431"),
 "Candy Cane":  ("c82030 ff4a5a ffffff d8d8e8 880c1a ffd0d8", "11330", "01132", "11332"),
 "Sakura":      ("d0608a f090b0 ffc0d4 fff0f5 8a4a2a ffe888", "12321", "0.3.0", "13231"),
 "Aurora":      ("108a6a 2ee0a8 6af0ff b070ff ff7ae0 0a5a8a", "01234", "12340", "12340"),
 "Starlight":   ("5a7ae0 a0c0ff d8e8ff ffffff ffe890 3a50b0", "0.4.0", "12321", "10301"),
 "Holy Light":  ("e8b830 fff0a0 ffffe0 ffffff fff8c8aa d8a020", "12321", "04140", "23432"),
}
def make_halo(name):
    pal, t0, t1, f = HALOS[name]
    pal = P(pal)
    g = lambda s: [None if ch == '.' else pal[int(ch, 16)] for ch in s]
    T = [g(t0), g(t1)]; F = g(f)
    img = Img(16, 8)
    img.box(0, 0, 5, 1, 2, lambda x, y: T[y][x], lambda x, y: D(T[y][x], .5), lambda x, y: F[x],
            lambda x, y: D(F[2], .35), lambda x, y: D(F[2], .35), lambda x, y: D(F[x], .3))
    return img

# ---------------------------------------------------------------- ears (16x8: A 3x2, B 2x1, C 1x1 per ear)
# spec: palette, front rows of A, B, C (palette digits, '.' transparent). 4/5 are inner-ear tones (flattened on the back).
EARS = {
 "Cat":      ("3a3e4c 5a6070 8a90a4 b8bece ffb8cc e88aa8 22242c", ["242", "151"], "23", "6"),
 "Fox":      ("2a1a14 b84a10 f0782a ffa860 fff0e0 e8d0b8 22140e", ["242", "242"], "66", "6"),
 "Bear":     ("3a2412 5a3a22 8a5a34 b88a58 d9b48a e8c8a0 2a180c", ["232", "242"], "22", "."),
 "Wolf":     ("2e2a2a 5a5250 8a8078 b8aea4 e8dcd0 a89080 1c1a1a", ["242", "232"], "22", "1"),
 "Panda":    ("26262e 3c3c46 585862 8a8a96 d8d8e0 a8a8b4 5a5a64", ["232", "121"], "23", "."),
 "Bunny":    ("c8c0c8 e8e0e8 f8f4f8 ffffff ffb0c8 e888a8 b0a8b4", ["242", "252"], "44", "2"),
 "Deer":     ("3a2412 7a4a28 b07a44 d8a870 f0dcb8 8a5a30 fff4e0", ["262", "242"], "22", "1"),
 "Devil":    ("3a0a10 8a1420 d02a34 ff5a5a ffa0a0 5a0a20 2a0408", ["232", "252"], "22", "6"),
 "Elf":      ("8a5a3a c88a62 f0b890 ffd8b8 ffa8a0 e07a80 ffd84a", ["232", "242"], "23", "6"),
 "Robot":    ("202630 3e4656 6a788c 9aaac0 d0def0 18c8e8 ff4060 18c8e8", ["323", "373"], "22", "6"),
 "Mouse":    ("8a7a80 b0a4ac d8ccd4 f4ecf2 ffb0c8 f08aa8 6a5a60", ["232", "444"], "33", "."),
 "Tiger":    ("2a1408 d8641a f48a30 ffb860 fff4e0 ffd8a0 18100a", ["262", "242"], "22", "."),
 "Frog":     ("1a4a1a 2e8a2a 5ac048 98f070 d8ffa0 e8e040 0e180a e8e040", ["272", "232"], "..", "."),
 "Husky":    ("2a2c34 4a4e5c 7a8090 d0d4dc f4f6fa 9ab8d8 18181e", ["343", "353"], "22", "1"),
 "Raccoon":  ("1c1c20 3c3c44 6a6a74 a8a8b4 e8e8ee d8a8a0 101014", ["434", "414"], "33", "."),
 "Snow Leopard": ("5a5a68 8a8a98 b8b8c6 e0e0ea f8f8fc 9a9aa8 4a4a58", ["232", "262"], "33", "."),
 "Neon Cat":  ("10162c 1c2850 2a3c78 3a54a8 7af0ff 3a54a8 ff5ad8", ["464", "424"], "42", "4"),
 "Pug":       ("2a2018 4a3a28 7a6448 b89a6c e8d8b8 f0a8a8 1c140e", ["121", "141"], "11", "."),
 "Dragon":    ("1a3a1a 2e7a2e 4ab048 8ae060 f0e4b0 d8a030 6a1a10 f0e4b0 ffd860", ["232", "232"], "77", "8"),
}
def make_ears(name):
    pal, fa, fb, fc = EARS[name]; pal = P(pal)
    flat = {'4': '2', '5': '1'}
    def gr(rows, back=False):
        return [[None if ch == '.' else pal[int((flat.get(ch, ch) if back else ch), 16)] for ch in row] for row in rows]
    FA, FB, FC = gr(fa), gr([fb]), gr([fc]); KA, KB, KC = gr(fa, True), gr([fb], True), gr([fc], True)
    hi = pal[3]; dk = pal[0]; sd = pal[1]
    tip = FC[0][0] or (FB[0][0] if FB[0][0] else hi)
    def side(rows): return lambda x, y: mix(rows[y][1] or sd, dk, .45)
    img = Img(16, 8)
    img.box(0, 0, 3, 2, 1, lambda x, y: lit(FA[0][x] or hi, .12), lambda x, y: D(dk, 0), lambda x, y: FA[y][x],
            side(FA), side(FA), lambda x, y: KA[y][x])
    img.box(0, 4, 2, 1, 1, lambda x, y: lit(FB[0][x] or hi, .12), lambda x, y: dk, lambda x, y: FB[0][x],
            lambda x, y: mix(FB[0][0] or sd, dk, .45), lambda x, y: mix(FB[0][1] or sd, dk, .45), lambda x, y: KB[0][x])
    img.box(8, 4, 1, 1, 1, lambda x, y: lit(FC[0][0] or hi, .15), lambda x, y: dk, lambda x, y: FC[0][0],
            lambda x, y: mix(FC[0][0] or sd, dk, .45), lambda x, y: mix(FC[0][0] or sd, dk, .45), lambda x, y: KC[0][0])
    return img

# ---------------------------------------------------------------- bandana (36x11: 9x2 side strips around the head)
def cloth(c, U, y):
    c = mul(c, 1 + math.sin(U * 0.9 + y * 0.6) * 0.045 + (rnd(U, y, 77) - .5) * 0.05)
    return lit(c, .09) if y == 0 else mul(c, .87)
def vn(U, y, s=1, k=2.0): return vnoise(U, y * 2.2, s, k)

RC = P("8f1414 c4221f e03a30 f27a6a f6e6dc")
def b_red(U, y):
    if (U % 6 in (1, 2) and y == 0) or (U % 6 == 4 and y == 1) or (U % 6 == 5 and y == 0 and U % 12 > 5): return RC[4]
    if U % 6 == 3 and y == 0: return RC[3]
    return rp(RC[:4], .42 + .3 * (vn(U, y, 2) - .5) + (.18 if y == 0 else 0), U, y)
GB = P("2d3f8f 4a62c4 7289da a8bcf4 e4ecff")
def b_glacier(U, y):
    if (U % 7 == 3 and y == 0) or (U % 7 in (2, 4) and y == 1 and U % 14 < 7) or (U % 7 == 3 and y == 1 and U % 14 >= 7): return GB[4]
    if (U + y * 2) % 7 == 0: return GB[3]
    return rp(GB[:4], .5 + .3 * (vn(U, y, 3) - .5) + (.15 if y == 0 else 0), U, y)
BS = P("16161e 262632 3a3a4a 5a5a6c f0f0f0 b8b8c8")
def b_skull(U, y):
    sk = {(12, 0): 4, (13, 0): 4, (14, 0): 4, (12, 1): 4, (13, 1): 0, (14, 1): 4, (10, 1): 5, (16, 1): 5, (10, 0): 5, (16, 0): 5}
    if (U, y) in sk:
        k = sk[(U, y)]
        return BS[k] if k else BS[0]
    if U % 6 == 3 and y == 0 and not 9 <= U <= 17: return BS[3]
    return rp(BS[:4], .3 + .35 * (vn(U, y, 4) - .5) + (.1 if y == 0 else 0), U, y)
NS_ = P("0e1c3e 1a3470 2e54a8 f4f6ff c8d4ee")
def b_navy(U, y):
    if (U + y) % 4 == 0: return NS_[3] if y == 0 else NS_[4]
    return rp(NS_[:3], .4 + .3 * (vn(U, y, 5) - .5) + (.2 if y == 0 else 0), U, y)

CAM = P("26361a 46562a 6a7a3a 8e9e54 a8845a 5a4028")
def b_camo(U, y):
    n = vnoise(U, y * 2.5 + 3, 11, 2.4) * .7 + vnoise(U, y * 3, 12, 1.2) * .3
    return CAM[0] if n < .3 else CAM[1] if n < .45 else CAM[2] if n < .58 else CAM[4] if n < .7 else CAM[5] if n < .8 else CAM[3]
FL = P("2a0606 7a1410 c8300c f2641a ffa42a ffe070")
def b_flame(U, y):
    h = (0, 1, 2, 1, 0, 1, 0)[U % 7]; j = (U // 7) % 2
    if y == 0: return rp(FL[2:], .1 + .42 * (h + j * .3) + .4 * (vn(U, y, 3) - .5), U, y)
    return rp(FL[1:5], .25 + .3 * h / 2 + .2 * vn(U, y, 4), U, y)
TD = [hx(s) for s in ("ff3fa8", "ff8a2a", "ffe14a", "5ae86a", "2ad8e8", "4a6aff", "a84aff")]
def b_tiedye(U, y):
    t = ((U * .16 + y * .5 + 1.6 * vnoise(U, y * 2, 3, 4)) % 1.0) * 7
    i = int(t) % 7; f = t - int(t)
    return mix(TD[i], TD[(i + 1) % 7], f) if f > .55 + (bay(U, y) - .5) * .4 else TD[i]
BD = P("2a2420 4a403a 6a5e52 8a7c6c d8c08a a83020 ffd84a")
def b_bandit(U, y):
    if U % 3 == 0 and y == 0: return BD[4]                                        # stitching
    if U in (12, 14) and y == 1 or (U == 13 and y == 0): return BD[6]               # sheriff star
    if U == 13 and y == 1: return lit(BD[6], .4)
    if U % 9 in (3, 5) and y == 0 or U % 9 == 4 and y == 1: return BD[5]           # red crosses
    return rp(BD[:4], .3 + .3 * (vn(U, y, 6) - .5) + (.1 if y == 0 else 0), U, y)
ST_ = P("0c1a4a 142a78 1e3c9c e8f0ff ffd84a ffffff")
def b_stars(U, y):
    k = (U + (4 if (U // 8) % 2 else 0)) % 8
    if y == 0 and k == 2: return ST_[4] if (U // 8) % 2 == 0 else ST_[5]
    if y == 1 and k in (1, 2, 3): return ST_[4] if (U // 8) % 2 == 0 else ST_[5]
    return rp(ST_[:3], .35 + .35 * (vn(U, y, 7) - .5) + (.12 if y == 0 else 0), U, y)
PL = P("5a0e12 a01c24 d8343c 2a5a34 4a8a52 f0e0a0 18281c")
def b_plaid(U, y):
    base = rp(PL[:3], .55 + .25 * (vn(U, y, 8) - .5), U, y)
    k = U % 12
    if y == 1: base = mul(base, .78)
    if k in (3, 4, 5): base = mix(base, PL[3] if y else PL[4], .72)
    if k == 9: base = mix(base, PL[5], .8 if y == 0 else .55)
    if k == 0 and y == 0: base = PL[6]
    return base
FR = P("5a9ac4 8ac8e8 b8e4f6 e0f6fc ffffff")
def b_frost(U, y):
    if (U % 9 == 4 and y == 0) or (U % 9 in (3, 4, 5) and y == 1): return FR[4]
    if (U + y * 2) % 7 == 0: return FR[0]
    return rp(FR[:4], .5 + .35 * (vn(U, y, 9) - .5) + (.2 if y == 0 else 0), U, y)
PA = P("10304a 1a4a6a 2a7a8a 58b0b8 f4e8c8 e0a848")
def b_paisley(U, y):
    k = U % 8
    if (y == 0 and k in (1, 2)) or (y == 1 and k in (2, 3)): return PA[4] if (y, k) != (1, 3) else PA[5]
    if y == 1 and k == 6: return PA[5]
    if y == 0 and k == 5: return PA[3]
    return rp(PA[:4], .22 + .32 * (vn(U, y, 10) - .5) + (.14 if y == 0 else 0), U, y)
SK_ = P("c05a7c e486a4 f6b4c8 ffd8e4 fff0f5 8a3a58")
def b_sakura(U, y):
    k = U % 7
    if (y == 0 and k in (2, 3)) or (y == 1 and k == 3): return SK_[4] if k == 3 or y == 1 else SK_[3]
    if y == 1 and k in (0, 5): return SK_[5]
    return rp(SK_[:4], .4 + .3 * (vn(U, y, 11) - .5) + (.14 if y == 0 else 0), U, y)
def b_rainbow(U, y):
    c = RBW[((U + y) // 2) % 6]
    return lit(c, .22) if (U + y) % 2 == 0 and y == 0 else c
CB = P("0a0e24 141c48 202c70 2e3e98")
def b_cyber(U, y):
    k = U % 8
    if y == 0 and k == 0: return (255, 255, 255)
    if y == 0 and k in (1, 2, 3): return (40, 232, 255) if k < 3 else (30, 150, 215)
    if y == 1 and k == 4: return (255, 255, 255)
    if y == 1 and k in (5, 6, 7): return (255, 70, 215) if k < 7 else (170, 40, 150)
    return rp(CB, .25 + .2 * ((U + y) % 2) + .2 * y, U, y)
HZY = P("a07a08 d8a812 ffd22a ffe97a"); HZK = P("2a2826 3e3a36 54504a 6a645c")
def b_hazard(U, y):
    s = ((U + y) // 3) % 2; e = (U + y) % 3
    if s == 0: return rp(HZY, .55 + (.3 if y == 0 else 0) - (.3 if e == 2 else 0) + .1 * (vn(U, y, 12) - .5), U, y)
    return rp(HZK, .35 + (.2 if y == 0 else 0) + (.2 if e == 0 else 0), U, y)
def b_checker(U, y):
    sets = ((hx("ff4fa8"), hx("fff4fa")), (hx("28d8c8"), hx("fff4fa")), (hx("ffe14a"), hx("ff4fa8")))
    a, b = sets[(U // 8) % 3]
    return a if (U // 2 + y) % 2 == 0 else b
GXB = P("0a0620 160c3c 261468 42209a 6e32cc aa52e8")
def b_galaxy(U, y):
    if rnd(U, y, 20) > .86: return (255, 255, 255) if rnd(U, y, 21) > .5 else (170, 196, 255)
    if rnd(U, y, 22) > .93: return (255, 154, 216)
    n = vnoise(U * .8, y * 2, 9, 3.0) * .7 + vnoise(U, y * 2, 10, 1.4) * .3
    return rp(GXB, .05 + max(0, n - .25) * 1.5, U, y)
def b_pirate(U, y):
    if U % 9 == 4 and y == 1: return hx("ffd84a")
    if U % 9 == 5 and y == 0: return hx("c8981a")
    s = (U // 2) % 2
    return rp(P("5a0a10 a01c24 d8343c"), .4 + .2 * (vn(U, y, 13) - .5), U, y) if s == 0 else rp(P("c8b890 e8dcc0 f8f0dc"), .55 + .2 * (vn(U, y, 14) - .5), U, y)
def b_toxic(U, y):
    if U % 5 in (1, 2) and y == 1: return TX[1] if U % 5 == 1 else TX[2]
    if rnd(U, y, 23) > .9: return TX[5]
    return rp(TX[2:5], .3 + .5 * vn(U, y, 15) + (.15 if y == 0 else -.1), U, y)
HK = P("c8c8d8 e8e8f0 ffffff b4b4c8 b8101c e0283a 6a0a12")
def b_hachi(U, y):
    if 12 <= U <= 14: return HK[5] if (U == 13 and y == 0) else HK[4]
    if U in (10, 16) and y == 0: return HK[6]
    if U in (10, 16) and y == 1 and U == 10: return HK[6]
    if U % 5 == 2 and y == 1 and not 9 <= U <= 17: return HK[3]
    return rp(HK[:3], .5 + .35 * (vn(U, y, 16) - .5) + (.15 if y == 0 else 0), U, y)
RG = P("2a1058 3e1c80 5a2ca8 7a48d0 8a5a10 c8981c ffd84a fff0a0")
def b_royal(U, y):
    k = U % 4
    if (y == 0 and k in (0, 1, 2)) or (y == 1 and k == 3): return RG[6] if k != 1 or y else RG[7]
    if y == 1 and U % 12 == 6: return (255, 60, 90)
    if y == 0 and k == 3 and U % 12 == 3: return (255, 255, 255)
    return rp(RG[:4], .3 + .3 * (vn(U, y, 17) - .5) + .12 * y, U, y)

def make_bandana(fn):
    S = [[cloth(fn(U, y), U, y) for U in range(36)] for y in range(2)]
    img = Img(36, 11)
    mid = mul(S[0][13], .8)
    face = lambda off: (lambda x, y: S[y][off + x])
    img.box(0, 0, 9, 2, 9, lambda x, y: mid, lambda x, y: mul(mid, .6), face(9), face(0), face(18), face(27))
    return img

# ---------------------------------------------------------------- glasses (24x10)
# spec: palette, 3 rows of 4 lens digits, top-edge digits, side digits (3), bridge digit, temple digits hinge->tip
GLS = {
 "Classic":      ("16161c 38384a 5a5a70 2a5a90 5aa0e0 a8dcff ffffff", ["1221", "0350", "0440"], "2222", "120", 1, "2110"),
 "Shades":       ("0e0e14 20202c 3a3a4c 12121a 22222e 5a6a8a 8a9ac0", ["1221", "1351", "0440"], "2222", "120", 1, "2110"),
 "Gold Round":   ("8a5a10 d8a820 ffe070 5ab0d0 a8e4f4 ffffff", [".21.", "1451", ".11."], "2222", "100", 1, "2110"),
 "Hearts":       ("c2185b ff5c9a ff8fb8 ffe0ec ff3a7a 8a0c40", ["3..1", "2411", ".15."], "2222", "210", 1, "1000"),
 "Pixel":        ("1c1c34 2a2a46 3a3a5c f4f4ff 8a8aa8 0e0e1a", ["0302", "1212", "1212"], "1111", "101", 0, "1010"),
 "Aviator":      ("7a5a14 d8a828 ffe488 5a3a18 a87038 e8b070 fff0d0", ["1221", "1461", ".34."], "2222", "210", 1, "2110"),
 "3D Glasses":   ("f4f0e8 c8c0b0 6a6458 d82a2a ff7a7a 2ab8d8 7ae8ff", ["1001", "1351", "1461"], "0000", "010", 1, "0110"),
 "Cyber Visor":  ("0a1030 18286a 3a50b0 18d8f0 8af4ff ffffff ff4ad8", ["1221", "3453", "1661"], "2222", "120", 1, "1221"),
 "Star Shades":  ("4a1a6a 7a3aa0 a860d8 2a1248 3e1c68 ffe04a fff4a0", ["1221", "3563", "3453"], "2222", "110", 1, "1210"),
 "Round Neon":   ("ff3fd0 ff9cf0 ffffff 3a1a7a90 7af0ff 20104a60", [".02.", "0350", ".01."], "0000", "010", 1, "0110"),
 "Welding":      ("3a3e46 5a606a 8a929e 0e3a18 1a7a30 8aff6a e8ffd8", ["2222", "1461", "1331"], "2222", "100", 1, "2110"),
 "Sunset Tint":  ("2a1a3a 4a2a5a 6a3a7a ff6a3a ff9a4a ffc86a d85a8a", ["1111", "1451", "0630"], "1111", "120", 1, "1210"),
 "Ski Goggles":  ("e8560a ff8a3a ffc080 1a6ad8 5aa8ff d0eaff ffffff", ["1221", "1451", "1331"], "2222", "110", 1, "1100"),
 "Wayfarer":     ("12121a 2e2e3c 4a4a5e c88a3a e8b060 fff0c8 8a5a20", ["0110", "0460", "0330"], "1111", "010", 1, "1110"),
 "Matrix":       ("2a2e34 5a6068 8a929c 0a1c0e 14602a 40ff70 b8ffc8", ["2222", "3453", "3043"], "2222", "120", 1, "2110"),
 "Steampunk":    ("5a3a14 b88434 ffe08a 3a7a70 70d0c0 d8fff0 8a5a1e", [".11.", "1451", ".11."], "2222", "210", 1, "2110"),
 "Nerd":         ("12121a 2a2a3a 4a4a5e c8e8ff e8f8ff ffffff d8d8d8", ["0000", "0450", "0330"], "0000", "010", 5, "0110"),
 "Rainbow Lens": ("f4f4ff c8c8e8 8a8ab8 ff4a4a ffd92a 3ed860 2a9bff", ["0000", "1356", "0000"], "1111", "120", 1, "1210"),
 "Ice Lens":     ("8ab8d8 c8ecff ffffff 9ad8f0a0 d8f6ffc0 5a9ac8a0", ["0110", "1341", "0550"], "1111", "120", 1, "1210"),
 "Toxic Goggles": ("1a2a14 3a4a2a 8a9a4a 1a6a10 5aee22 d8ff8a ffe14a", [".22.", "2452", ".66."], "2222", "210", 6, "2110"),
 "Emerald":      ("0e5a30 1ea560 5ee0a0 b8ffd8 ffffff 2a7a50 d8c060", ["1221", "1341", ".11."], "2222", "210", 6, "2110"),
}
def make_glasses(name):
    pal, rows, top, side, br, tp = GLS[name]; pal = P(pal)
    pc = lambda ch: None if ch == '.' else pal[int(ch, 16)]
    L = [[pc(ch) for ch in r] for r in rows]; T = [pc(ch) for ch in top]; S = [pc(ch) for ch in side]; TP = [pc(ch) for ch in tp]
    bridge = pal[br]
    img = Img(24, 10)
    img.box(0, 0, 4, 3, 1, lambda x, y: T[x], lambda x, y: D(T[x], .45), lambda x, y: L[y][x],
            lambda x, y: S[y], lambda x, y: S[y], lambda x, y: D(L[y][3 - x], .35))
    img.box(10, 0, 1, 1, 1, lambda x, y: lit(bridge, .15), lambda x, y: D(bridge, .4), lambda x, y: bridge, lambda x, y: D(bridge, .3),
            lambda x, y: D(bridge, .3), lambda x, y: D(bridge, .2))
    img.box(0, 5, 1, 1, 4, lambda x, y: TP[3 - y], lambda x, y: D(TP[y], .4), lambda x, y: TP[0], lambda x, y: TP[3 - x], lambda x, y: TP[x],
            lambda x, y: TP[3])
    return img

# ---------------------------------------------------------------- bracelet (20x7: 5x2 side faces right/front/left/back)
GC = P("6a4a0c a87a18 d8a828 ffd84a fff4a8")
def br_cuff(U, y):
    x = U % 5
    if y == 1: return GC[1] if x in (0, 4) or (U + 1) % 4 == 0 else GC[2]
    return GC[4] if x == 1 else GC[3] if x != 4 else GC[2]
SV = P("5a6270 8a94a4 bcc6d4 e8eef6 ffffff"); RB_ = P("6a0a1e c4183a ff4a68 ffb0c0")
def br_gem(U, y):
    x = U % 5
    if 1 <= x <= 3:
        if y == 0: return RB_[3] if x == 1 else RB_[2]
        return RB_[1] if x != 1 else RB_[2]
    return SV[3] if y == 0 else SV[1]
def br_rainbow(U, y):
    c = RBW[(U % 5)]
    return lit(c, .3) if y == 0 else mul(c, .72)
LE = P("3a2010 5a3820 7a5030 9a6a40 e8dcc0 c0c4cc")
def br_leather(U, y):
    x = U % 5
    if x == 2 and U // 5 in (0, 2): return LE[5] if y == 0 else mix(LE[5], (60, 60, 70), .5)    # studs
    if y == 0: return LE[4] if U % 2 == 0 else LE[3]                                             # stitching
    return rp(LE[:4], .25 + .3 * (vnoise(U, y, 3, 2) - .5) + .25, U, y)
NE = P("0a0e22 141e3a 1e3a58 38f2ff d8ffff 0a8aa8")
def br_neon(U, y):
    if y == 0: return NE[4] if U % 5 == 1 else NE[3]
    return NE[5] if U % 2 == 0 else NE[2]
def br_chain(U, y):
    link = (U // 5) % 2; x = U % 5
    top = (1 <= x <= 3) if link == 0 else (x in (0, 4))
    bot = (x in (0, 4)) if link == 0 else (1 <= x <= 3)
    on = top if y == 0 else bot
    if not on: return None
    return GC[4] if (y == 0 and x == 2) or (y == 1 and x == 1) else (GC[3] if x < 3 else GC[1])
SPK = P("2a2024 3e3238 54464e 6a5a64")
def br_spiked(U, y):
    x = U % 5
    if x in (1, 3): return SV[4] if y == 0 else SV[1]
    if x == 2: return SV[2] if y == 0 else SV[0]
    return rp(SPK, .35 + .15 * y + .15 * (vnoise(U, y, 4, 2) - .5) - (.1 if y else 0), U, y)
FRI = [hx(s) for s in ("e83a4a", "ffd23a", "3a8aff", "f4f4ff", "ff7ac8")]
def br_friend(U, y):
    c = FRI[((U + y) // 2) % 5]
    return lit(c, .22) if y == 0 else mul(c, .78)
TW = P("1c222e 2e3648 46526a 6a7a96 a8b8d0 e0ecff")
def br_watch(U, y):
    f, x = U // 5, U % 5
    if f == 1:
        if x in (0, 4): return TW[4] if y == 0 else TW[3]
        scr = [[(60, 240, 255), (10, 58, 80), (60, 240, 255)], [(20, 110, 140), (60, 240, 255), (20, 110, 140)]]
        return scr[y][x - 1]
    if f == 3: return TW[5] if (x in (1, 2, 3) and y == 0) else (TW[4] if 1 <= x <= 3 else TW[2])
    return TW[3] if (U % 2 == 0 and y == 0) else (TW[2] if y == 0 else TW[1])
def br_ruby(U, y):
    f, x = U // 5, U % 5
    if f in (1, 3) and 1 <= x <= 3:
        return (RB_[3] if x == 1 else RB_[2]) if y == 0 else (RB_[1] if x != 1 else RB_[2])
    if y == 1: return GC[1] if x in (0, 4) else GC[2]
    return GC[4] if x == 2 else GC[3]
PT = P("6a7a90 a8b8d0 dce6f4 ffffff")
def br_diamond(U, y):
    f, x = U // 5, U % 5
    if f == 1 and 1 <= x <= 3: return [[(200, 244, 255), (255, 255, 255), (200, 244, 255)], [(122, 204, 232), (200, 244, 255), (122, 204, 232)]][y][x - 1]
    if f in (0, 2, 3) and x == 2: return (255, 255, 255) if y == 0 else (160, 224, 244)
    if y == 1: return PT[0] if x % 2 else PT[1]
    return PT[3] if x == 1 else PT[2]
OB = P("241a34 3a2a54 5a4682 8a74b8 c8b8f0")
def br_obsidian(U, y):
    b = U % 4
    return [OB[1], OB[4], OB[3], OB[1]][b] if y == 0 else [OB[0], OB[2], OB[1], OB[0]][b]
JD = P("0e3a22 1a6a3a 2ea65a 74e098 d0ffe0 d8a828")
def br_jade(U, y):
    b = U % 4
    return [JD[5], JD[4], JD[3], JD[2]][b] if y == 0 else [JD[5], JD[2], JD[1], JD[0]][b]
CC = P("0c1024 161e44 22306a 303e94")
def br_cyber(U, y):
    k = U % 10
    if y == 1 and k < 7: return (40, 232, 255) if k not in (3,) else (255, 255, 255)
    if y == 0 and U % 5 == 2: return (255, 70, 215)
    return rp(CC, .3 + .2 * ((U + y) % 2) + .25 * (1 - y), U, y)
def br_pearl(U, y):
    b = U % 4; pink = (U // 4) % 2
    p = [(255, 255, 255), (244, 232, 236), (216, 196, 204)] if not pink else [(255, 240, 246), (255, 214, 226), (232, 178, 196)]
    q = [(200, 160, 64), (232, 216, 220), (192, 168, 180), (160, 136, 152)] if not pink else [(200, 160, 64), (250, 196, 212), (216, 160, 180), (176, 128, 148)]
    if y == 0: return None if b == 0 else p[b - 1]
    return q[b]
BP = [hx(s) for s in ("ff4a9a", "ffd92a", "2ad8f0", "5ae84a", "ff8a2a")]
def br_beads(U, y):
    b = U % 4; c = BP[(U // 4) % 5]
    if b == 0: return (90, 70, 80) if y else None
    return [lit(c, .5), c, mul(c, .8)][b - 1] if y == 0 else [c, mul(c, .8), mul(c, .6)][b - 1]
RN = P("3a3e4a 5a606e 7a8294 9aa4b8 6ae0ff e8ffff")
RUNES = [("X.X", ".X."), (".X.", "XXX"), ("XX.", ".XX"), ("X.X", "XXX")]
def br_runic(U, y):
    f, x = U // 5, U % 5
    if 1 <= x <= 3 and RUNES[f][y][x - 1] == 'X': return RN[5] if y == 0 else RN[4]
    if 1 <= x <= 3 and any(RUNES[f][yy][xx] == 'X' for yy in range(2) for xx in range(3) if abs(yy - y) + abs(xx - (x - 1)) == 1): return mix(RN[1], RN[4], .35)
    return rp(RN[:4], .35 + .3 * (vnoise(U, y, 6, 2) - .5) + (.15 if y == 0 else 0) + (.1 if x == 0 else 0), U, y)

def make_bracelet(spec):
    fn, hi, lo = spec
    S = [[fn(U, y) for U in range(20)] for y in range(2)]
    face = lambda off: (lambda x, y: S[y][off + x])
    rim = lambda x, y: hi if (x in (0, 4) or y in (0, 4)) else mix(hi, lo, .45)
    img = Img(20, 7)
    img.box(0, 0, 5, 2, 5, rim, lambda x, y: lo, face(5), face(0), face(10), face(15))
    return img

# ---------------------------------------------------------------- registry (order = order in Accessories.java)
olo = lambda c: ol_default(opq(c))
OL_NONE = None
WINGS = [  # name, painter, outline fn (None = off, 'd' = default), bone colours (highlight, mid, dark)
 ("Angel", w_angel, ol_soft, (AN[4], AN[2], AN[0])), ("Bat", w_bat, 'd', (BA[4], BA[3], BA[0])),
 ("Dragon", w_dragon, 'd', (BONE[2], BONE[1], BONE[0])), ("Ice", w_ice, ol_soft, (IC[4], IC[2], IC[0])),
 ("Butterfly", w_butter, ol_soft, (BU[0][3], BU[0][2], BU[0][0])),
 ("Phoenix", w_phoenix, 'd', (PH[5], PH[3], PH[0])), ("Crystal", w_crystal, 'd', (CR[4], CR[2], CR[0])),
 ("Mecha", w_mecha, 'd', (ME[4], ME[2], ME[0])), ("Void", w_void, lambda c: VO[4], (VO[4], VO[3], VO[0])),
 ("Fairy", w_fairy, lambda c: FA[0], (FA[3], FA[1], FA[0])), ("Moth", w_moth, ol_soft, (MO[3], MO[2], MO[0])),
 ("Raven", w_raven, 'd', (RV[4], RV[3], RV[1])), ("Seraph", w_seraph, ol_soft, (SE[3], SE[2], SE[0])),
 ("Cyber Grid", w_cyber, lambda c: (255, 70, 215), ((40, 232, 255), CG[3], CG[0])),
 ("Lava", w_lava, 'd', (MG[2], LVR[3], LVR[0])), ("Storm", w_storm, 'd', (ST[4], ST[3], ST[1])),
 ("Candy", w_candy, ol_soft, (CD[3], CD[1], CD[0])), ("Pixel Rainbow", w_rainbow, 'd', ((255, 255, 255), (200, 200, 230), (90, 90, 140))),
 ("Skeleton", w_skeleton, None, (SK[4], SK[2], SK[0])), ("Steampunk", w_steam, 'd', (SP[4], SP[3], SP[0])),
 ("Glacier", w_glacier, ol_soft, (GL[4], GL[2], GL[0])), ("Galaxy", w_galaxy, 'd', (GX[5], GX[4], GX[1])),
 ("Autumn", w_autumn, 'd', (AUP[2][3], AUP[4][2], AUP[4][0])), ("Sakura", w_sakura, ol_soft, ((200, 150, 110), (120, 80, 50), (60, 36, 24))),
 ("Toxic", w_toxic, 'd', (TX[4], TX[3], TX[0])), ("Gold Plated", w_gold, 'd', (GP[5], GP[3], GP[0])),
 ("Shadow", w_shadow, 'd', (SH[4], SH[2], SH[0])), ("Ice Shard", w_shard, 'd', (IS[4], IS[2], IS[0])),
 ("Dragonfly", w_dragonfly, lambda c: (74, 168, 184), (DF[3], DF[1], DF[0])),
]
BANDANAS = [("Red Classic", b_red), ("Glacier Blue", b_glacier), ("Black Skull", b_skull), ("Navy Stripes", b_navy),
            ("Camo", b_camo), ("Flame", b_flame), ("Tie Dye", b_tiedye), ("Bandit", b_bandit), ("Stars", b_stars),
            ("Plaid", b_plaid), ("Frost", b_frost), ("Paisley", b_paisley), ("Sakura", b_sakura), ("Rainbow", b_rainbow),
            ("Cyber Grid", b_cyber), ("Hazard", b_hazard), ("Checker Pop", b_checker), ("Galaxy", b_galaxy),
            ("Pirate", b_pirate), ("Toxic", b_toxic), ("Hachimaki", b_hachi), ("Royal Gold", b_royal)]
BRACELETS = [("Gold Cuff", (br_cuff, GC[3], GC[1])), ("Silver Gem", (br_gem, SV[3], SV[1])),
             ("Rainbow", (br_rainbow, lit(RBW[0], .3), mul(RBW[0], .6))), ("Leather", (br_leather, LE[3], LE[1])),
             ("Neon", (br_neon, NE[3], NE[1])), ("Gold Chain", (br_chain, GC[2], GC[0])), ("Spiked", (br_spiked, SPK[3], SPK[0])),
             ("Friendship", (br_friend, FRI[1], mul(FRI[0], .6))), ("Tech Watch", (br_watch, TW[3], TW[0])),
             ("Ruby Band", (br_ruby, GC[3], GC[1])), ("Diamond", (br_diamond, PT[2], PT[0])), ("Obsidian", (br_obsidian, OB[2], OB[0])),
             ("Jade", (br_jade, JD[2], JD[0])), ("Cyber Cuff", (br_cyber, CC[3], CC[0])),
             ("Pearl Strand", (br_pearl, (232, 216, 220), (160, 136, 152))), ("Bead Pop", (br_beads, BP[0], mul(BP[0], .6))),
             ("Runic", (br_runic, RN[2], RN[0]))]

# ================================================================ glacier brand art
# Palette sampled from the Glacier v7 panorama / pause screen backgrounds (navy-teal night) and the logo blue (5b87ea).
NAVY = P("0c1620 122539 1a2a3e 24384f 2e5c8a")
BLU = P("24477e 3a5cb8 5b87ea 8fb4ff c8e0ff e8f4ff")
_LOGO = None
def logo_mask(w, h, thr=40, dil=5):
    """The bear-head logo (assets/glacier/textures/ui/logo.png) thickened and box-downscaled to a w x h boolean mask."""
    global _LOGO
    from PIL import ImageFilter
    if _LOGO is None:
        a = Image.open(os.path.join(ROOT, "src/main/resources/assets/glacier/textures/ui/logo.png")).convert("RGBA").split()[3]
        _LOGO = a.crop(a.getbbox())
    m = _LOGO.filter(ImageFilter.MaxFilter(dil)).resize((w, h), Image.BOX)
    return [[m.getpixel((x, y)) > thr for x in range(w)] for y in range(h)]
def emblem(mask, x, y, ox=0, oy=0):
    mx, my = x - ox, y - oy
    return 0 <= my < len(mask) and 0 <= mx < len(mask[0]) and mask[my][mx]
_EMB_CAPE = logo_mask(10, 9)
_EMB_BP = logo_mask(7, 6)
_EMB_HAT = logo_mask(7, 5)

def c_glacier_set(x, y):
    if emblem(_EMB_CAPE, x, y, 0, 2): return BLU[3] if y < 6 else BLU[2]
    ridge = 12 + (1 if math.sin(x * 1.3 + 0.6) > 0 else 0) - (1 if x in (3, 4) else 0)
    if y >= ridge: return rp(BLU[3:], .9 - (y - ridge) * .22, x, y)
    if rnd(x, y, 31) > .93 and y < 12: return BLU[5]
    return rp(NAVY, .12 + .5 * (y / 15) ** 1.4 + .12 * vnoise(x, y, 5, 3), x, y)

# ================================================================ generic cuboid painter (new accessory types)
FACES = ("top", "bot", "front", "right", "left", "back")
SHADE = {"top": 1.14, "bot": .62, "front": 1.0, "back": .94, "right": .8, "left": .8}
def paint_box(img, u, v, w, h, d, fn, edge=.8):
    """fn(face, x, y, fw, fh) -> colour or None. Adds per-face shading and darker edge pixels (outline)."""
    dims = {"top": (w, d), "bot": (w, d), "front": (w, h), "back": (w, h), "right": (d, h), "left": (d, h)}
    def mk(face):
        fw, fh = dims[face]
        def f(x, y):
            c = fn(face, x, y, fw, fh)
            if c is None: return None
            c = mul(c, SHADE[face])
            if fw > 2 and fh > 2 and (x in (0, fw - 1) or y in (0, fh - 1)): c = mul(c, edge)
            return c
        return f
    img.box(u, v, w, h, d, *[mk(f) for f in FACES])

def pat(kind, x, y, W, H, s=1):
    """Pattern -> (slot, tone): slot 0 = base palette, 1 = accent palette, 2 = rainbow index."""
    n = vnoise(x, y, s, 2.4)
    t = .5 + (n - .5) * .4 - (y / max(1, H - 1) - .5) * .18
    a = 0
    if kind == "hstripe": a = (y // 2) % 2
    elif kind == "vstripe": a = (x // 2) % 2
    elif kind == "diag": a = ((x + y) // 2) % 2
    elif kind == "checker": a = ((x // 2) + (y // 2)) % 2
    elif kind == "dots": a = int(x % 4 == 1 and y % 3 == 1)
    elif kind == "plaid":
        v_, h_ = x % 6 in (2, 3), y % 6 in (2, 3)
        a = int(v_ or h_); t -= .22 if (v_ and h_) else 0
    elif kind == "camo":
        m = vnoise(x, y, s + 7, 3.0)
        if m > .6: a = 1
        elif m < .34: t -= .32
    elif kind == "stars":
        t = .22 + (n - .5) * .3
        if rnd(x, y, s + 3) > .93: a, t = 1, .9
        elif any(rnd(x + dx, y + dy, s + 3) > .975 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))): a, t = 1, .35
    elif kind == "speck":
        t = .12 + .55 * max(0, vnoise(x + y * .5, y, s + 5, 3.5) - .35)
        if rnd(x, y, s + 6) > .9: a, t = 1, .5 + rnd(x, y, s + 8) * .5
    elif kind == "grid": a = int(x % 4 == 0 or y % 4 == 0)
    elif kind == "knit": t += (.14 if (x + y) % 2 == 0 else -.1) + (.1 if y % 3 == 0 else 0)
    elif kind == "rainbow": return 2, min(5.0, y * 6.0 / max(1, H))
    elif kind == "rainbowv": return 2, float(x % 6)
    if a and kind not in ("stars", "speck"): t = .5 + (n - .5) * .4 - (y / max(1, H - 1) - .5) * .12
    return a, t
def pcol(pal, acc, kind, x, y, W, H, s=1):
    a, t = pat(kind, x, y, W, H, s)
    if a == 2: return RBW[int(t)]
    return rp(acc if a else pal, t, x, y)

# ---------------------------------------------------------------- backpack (64x32)
# parts: main 7x9x3 (0,0) | lid 8x3x4 (0,12) | pocket 5x3x1 (0,19) | strap 1x8x1 (24,0) | bridge 1x1x5 (28,0)
def bp_glacier_deco(part, face, x, y, W, H):
    if part == "main" and face == "back" and emblem(_EMB_BP, x, y, 0, 1): return BLU[3] if y < 3 else BLU[2]
    if part == "lid" and face == "back" and y == 2: return BLU[4]
    if part == "pocket" and face == "back" and y == 1 and x in (1, 3): return BLU[4]
    return None
BACKPACKS = [  # name, base, accent, pattern, deco
 ("Glacier Bear", P("0c1620 122539 1a3452 24477e 3a5cb8"), P("24477e 3a5cb8 5b87ea 8fb4ff e8f4ff"), "solid", bp_glacier_deco),
 ("Hiker Green", P("1c2a14 2e4420 466430 648a44 88b45e"), P("5a4020 826030 a88448 d0ac68 f0d898"), "solid", None),
 ("Leather Brown", P("2a1608 4a2a14 6a4224 8a5c34 b0824a"), P("8a6a1a c8981c e8c040 ffe480 fff4b8"), "solid", None),
 ("School Red", P("5a0e12 8a1a1e c02a2c e04848 f08a7a"), P("a07a08 d8a812 ffd22a ffe97a fff4b0"), "solid", None),
 ("Camo Pack", P("1c2412 36441e 566a30 7c9040 a6b45e"), P("3a2c18 5a4426 7c5e36 a08450 c4a874"), "camo", None),
 ("Cyber Pack", P("0a0e22 141e3a 1e3058 2a4478 38629a"), P("7a1a82 b4208f ff4fd8 ff9cf0 ffe0fa"), "grid", None),
 ("Rainbow Pack", P("2a2a44 3a3a5c 4a4a74 5a5a8c 6a6aa4"), P("5a5a8c 8a8ab8 b8b8dc d8d8f0 ffffff"), "rainbow", None),
 ("Star Pack", P("0a1030 141c48 202c70 2e3e98 4a5cc0"), P("b88a1a e8b830 ffd84a fff0a0 ffffff"), "stars", None),
 ("Galaxy Pack", P("0a0620 160c3c 261468 42209a 6e32cc"), P("6e32cc aa52e8 d08aff ffb0ff ffffff"), "speck", None),
 ("Candy Pack", P("c03a78 ff6fa8 ff9cc4 ffd0e4 ffffff"), P("3a9a80 5ec8a8 96f0d4 d2fff0 ffffff"), "diag", None),
 ("Treasure", P("3a2410 5a3a1a 7a5028 9a6a38 c08a4c"), P("8a6a1a c8981c e8c040 ffe480 fff4b8"), "hstripe", None),
]
def make_backpack(spec):
    _, pal, acc, kind, deco = spec
    img = Img(64, 32)
    def part(name):
        def fn(face, x, y, W, H):
            if deco:
                c = deco(name, face, x, y, W, H)
                if c is not None: return c
            if name in ("strap", "bridge"):
                return rp(acc, .42 + (.22 if (x + y) % 3 == 0 else 0) + (.1 if face == "top" else 0), x, y)
            if name == "lid":
                if face == "back" and x in (3, 4) and y in (1, 2): return acc[4] if y == 1 else acc[3]
                return rp(acc, .5 + (.2 if y == 0 else 0) - (.18 if y == H - 1 else 0) + .1 * (vnoise(x, y, 3, 2) - .5), x, y)
            if name == "pocket":
                if face == "back":
                    if y == 0: return acc[3] if x % 2 == 0 else acc[1]
                    if x == 2 and y == 1: return acc[4]
                return pcol(pal, acc, kind, x, y, W, H, 4) if kind in ("solid", "camo") else rp(pal, .4, x, y)
            c = pcol(pal, acc, kind, x, y, W, H, 2)
            if face == "back" and (x in (1, W - 2) or y in (1, H - 2)) and (x + y) % 2 == 0: c = lit(c, .22)   # stitching
            return c
        return fn
    paint_box(img, 0, 0, 7, 9, 3, part("main")); paint_box(img, 0, 12, 8, 3, 4, part("lid"))
    paint_box(img, 0, 19, 5, 3, 1, part("pocket")); paint_box(img, 24, 0, 1, 8, 1, part("strap"))
    paint_box(img, 28, 0, 1, 1, 5, part("bridge"))
    return img

# ---------------------------------------------------------------- hat (64x32)
# parts: crown 9x5x9 (0,0) | brim 12x1x12 (0,14) | orn 3x2x3 (36,0) | bill 9x1x4 (36,5)
STAR3 = ("010", "111", "010")
GOLD3 = P("c8981c ffd84a fff0a0")
def hat_crown(spec, face, x, y, W, H):
    name, cp, ca, kind, brim, bill, orn, style = spec
    side = face in ("front", "back", "left", "right")
    if style == "crown":
        if face in ("top", "bot"): return None
        if y == 0 and x % 2 == 1: return None
        if y == 3 and x in (2, 6): return ca[2]                                   # ruby
        if y == 3 and x == 4: return (90, 168, 255)                               # sapphire
        return rp(cp, .72 - .1 * y / 4 + (.25 if (x + y) % 3 == 0 else 0) - (.3 if y == 4 else 0), x, y)
    if style == "glacier":
        if face == "bot": return NAVY[1]
        if y == 4 and side: return rp(BLU[3:], .6 + .2 * (x % 2), x, y)           # ice cuff
        if face == "front" and emblem(_EMB_HAT, x, y, 1, 0): return BLU[3] if y < 2 else BLU[2]
        return pcol(cp, ca, "knit", x, y, W, H, 3)
    if style == "beanie":
        if side and y >= 3: return rp(ca, .45 + (.25 if x % 2 == 0 else 0), x, y)
        return pcol(cp, ca, "knit", x, y, W, H, 3)
    if style == "santa":
        if side and y >= 4: return rp(ca, .72 + .25 * (vnoise(x, y, 8, 1.4) - .5), x, y)
        return rp(cp, .5 + .35 * (vnoise(x, y, 9, 2) - .5) + (.1 if face == "top" else 0), x, y)
    if style == "chef":
        if y == 4 and side: return rp(ca, .45, x, y)
        return rp(cp, .75 - (.28 if x % 3 == 0 else 0) + .2 * (vnoise(x, y, 4, 2) - .5), x, y)
    if style == "cap":
        if face == "front" and 3 <= x <= 5 and 1 <= y <= 3 and STAR3[y - 1][x - 3] == "1": return ca[1]
        return rp(cp, .52 + .3 * (vnoise(x, y, 5, 2) - .5) + (.14 if face == "top" else 0) - (.1 if x % 3 == 0 and side else 0), x, y)
    if style == "captain":
        if y == 4 and side: return rp(ca, .45, x, y)
        if face == "front" and 3 <= x <= 5 and 1 <= y <= 3 and STAR3[y - 1][x - 3] == "1": return GOLD3[1]
        return rp(cp, .7 - (.2 if face == "top" and (x + y) % 2 else 0) + .1 * (vnoise(x, y, 4, 2) - .5), x, y)
    if style == "wizard":
        if y == 4 and side: return GOLD3[1] if x % 2 == 0 else GOLD3[0]
        return pcol(cp, ca, "stars", x, y, W, H, 6)
    if style == "cowboy":
        if y == 4 and side:
            return GOLD3[1] if face == "front" and x in (2, 6) else rp(ca, .42 + (.2 if x % 2 == 0 else 0), x, y)
        if face == "top" and x == 4: return mul(cp[1], .8)                           # crease
        return rp(cp, .55 + .25 * (vnoise(x, y, 5, 2) - .5) + (.1 if face == "top" else 0) - (.14 if y == 3 else 0), x, y)
    if style == "party":
        if rnd(x, y, 14) > .93: return ((255, 225, 74), (90, 232, 106), (255, 255, 255))[int(rnd(x, y, 15) * 3)]
        return pcol(cp, ca, "diag", x, y, W, H, 7)
    if style == "top":
        if y == 4 and side: return rp(ca, .5 + (.3 if face == "front" and x == 4 else 0), x, y)
        return rp(cp, .5 + .3 * (vnoise(x, y, 5, 2.5) - .5) + (.12 if x in (1, 2) and side else 0) + (.1 if face == "top" else 0), x, y)
    return pcol(cp, ca, kind, x, y, W, H, 2)

def make_hat(spec):
    name, cp, ca, kind, brim, bill, orn, style = spec
    img = Img(64, 32)
    paint_box(img, 0, 0, 9, 5, 9, lambda f, x, y, W, H: hat_crown(spec, f, x, y, W, H))
    if brim:
        def bf(f, x, y, W, H):
            if f in ("top", "bot") and min(x, y, W - 1 - x, H - 1 - y) == 1 and style in ("wizard", "cowboy", "top"):
                return rp(ca, .55, x, y)
            return rp(brim, .55 + .3 * (vnoise(x, y, 5, 2.2) - .5) + (.08 if f == "top" else 0), x, y)
        paint_box(img, 0, 14, 12, 1, 12, bf)
    if bill:
        paint_box(img, 36, 5, 9, 1, 4, lambda f, x, y, W, H: rp(bill, .5 + (.14 if f == "top" and x in (2, 4, 6) else 0) + (.06 if f == "top" else 0), x, y))
    if orn:
        paint_box(img, 36, 0, 3, 2, 3, lambda f, x, y, W, H: rp(orn, .7 + .3 * (rnd(x, y, 4) - .5) - (.15 if y == H - 1 and f not in ("top", "bot") else 0), x, y))
    return img

HATS = [  # name, crown pal, accent, kind, brim pal, bill pal, orn pal, style
 ("Glacier Beanie", P("0c1620 122539 1a3452 24477e 3a5cb8"), P("8fb4ff c8e0ff e8f4ff"), "knit", None, None, P("c8e0ff e8f4ff ffffff"), "glacier"),
 ("Top Hat", P("14101c 221c30 30283f 463c5a"), P("6a0a1e a8182e d8304a"), "solid", P("14101c 221c30 30283f 463c5a"), None, None, "top"),
 ("Baseball Cap", P("5a0e12 8a1a1e c02a2c e04848"), P("f4f4ff ffffff"), "solid", None, P("5a0e12 8a1a1e c02a2c e04848"), None, "cap"),
 ("Royal Crown", P("8a5a10 c8981c e8c040 ffe480 fff4b8"), P("6a0a1e a8182e d8304a ff7a8a"), "solid", None, None, None, "crown"),
 ("Cozy Beanie", P("2a3a6a 3e5aa0 5a82d8 8ab0f8"), P("e8e8f4 f8f8ff"), "knit", None, None, P("c8d8f8 f0f4ff ffffff"), "beanie"),
 ("Wizard", P("1a1048 2a1a70 3e2a9a 5a42c4"), P("ffd84a"), "stars", P("1a1048 2a1a70 3e2a9a 5a42c4"), None, P("c8981c ffd84a fff0a0"), "wizard"),
 ("Cowboy", P("6a4a28 8a6a3a b08c52 d0aa70"), P("3a2410 5a3a1a 7a5028 9a6a38"), "solid", P("5a3e20 7a5a30 9a7a44 bc9a60"), None, None, "cowboy"),
 ("Santa", P("7a0a10 b01820 d83030 f06a5a"), P("d8dce8 f4f6fc ffffff"), "solid", None, None, P("d8dce8 f4f6fc ffffff"), "santa"),
 ("Chef", P("c8c8d8 e4e4f0 f4f4fa ffffff"), P("a8a8bc 8a8aa0"), "solid", None, None, P("d8d8e8 f4f4fa ffffff"), "chef"),
 ("Captain", P("c8ccd8 e8ecf4 f8faff ffffff"), P("14284a 1e3c6a 2c5290"), "solid", None, P("14284a 1e3c6a 2c5290 3c6ab8"), None, "captain"),
 ("Party Hat", P("ff3fa8 ff6fc0 ff9ad4 ffc8e8"), P("2ad8e8 5aeaf4 98f6fc d8ffff"), "diag", None, None, P("ffd23a ffe46a fff4a8"), "party"),
]

# ---------------------------------------------------------------- tail (64x32, four segments along +Z)
# seg0 3x3x4 (0,0) | seg1 3x3x4 (0,7) | seg2 2x2x4 (14,0) | seg3 3x3x3 (26,0)
TAILSEG = [(0, 0, 3, 3, 4), (0, 7, 3, 3, 4), (14, 0, 2, 2, 4), (26, 0, 3, 3, 3)]
def tail_ab(i, face, x, y, W, H):
    d = TAILSEG[i][4]; base = (0, 4, 8, 12)[i]
    if face == "top": return base + (d - 1 - y), x, W
    if face == "bot": return base + y, x, W
    if face == "right": return base + (d - 1 - x), y, H
    if face == "left": return base + x, y, H
    return base + (0 if face == "front" else d), x, W
def tail_color(spec, i, face, x, y, W, H):
    name, pal, acc, kind, tip = spec
    a, c, cw = tail_ab(i, face, x, y, W, H)
    n = vnoise(a, c + i * 3, 11, 2.2)
    if i == 3 and tip:
        return rp(tip, .55 + .35 * (n - .5) + (.2 if c in (0, cw - 1) else 0) - (.1 if a > 13 else 0), a, c)
    t = .5 + (n - .5) * .4 + (.12 if face == "top" else 0)
    if kind == "fade": return rp(pal, .15 + .8 * min(1, a / 15) + (n - .5) * .12, a, c)
    if kind == "rings" and (a // 2) % 2 == 1: return rp(acc, .5 + (n - .5) * .3, a, c)
    if kind == "stripes" and a % 3 == 1 and face != "bot" and c != 1: return rp(acc, .45, a, c)
    if kind == "spots" and rnd(a, c, 17 + i) > .78: return rp(acc, .5 + (n - .5) * .3, a, c)
    if kind == "scales":
        hit = (a + (c % 2) * 2) % 4 == 0
        return rp(acc if hit else pal, t + (.1 if hit else 0), a, c)
    if kind == "bolt" and ((a + c * 2) % 5 == 0): return rp(acc, .8, a, c)
    return rp(pal, t, a, c)
def make_tail(spec):
    img = Img(64, 32)
    for i, (u, v, w, h, d) in enumerate(TAILSEG):
        paint_box(img, u, v, w, h, d, lambda f, x, y, W, H, i=i: tail_color(spec, i, f, x, y, W, H))
    return img
TAILS = [  # name, base, accent, pattern, tip palette
 ("Glacier Frost", P("122539 1a3e6a 24477e 3a5cb8 5b87ea 8fb4ff c8e0ff"), None, "fade", P("8fb4ff c8e0ff e8f4ff ffffff")),
 ("Fox", P("7a2a08 b84a10 e0661c f0782a ffa860"), None, "solid", P("c8c4c0 e8e4dc f8f4ec ffffff")),
 ("Tabby", P("4a4a58 6a6a7a 8a8a9a b0b0be"), P("2a2a34 3a3a48 4a4a5a"), "stripes", P("2a2a34 3a3a48 4a4a5a")),
 ("Wolf", P("3a3a44 5a5a66 7a7a88 a0a0ae c8c8d4"), None, "solid", P("2a2a32 3c3c48 52525e")),
 ("Dragon", P("123416 1f5a1a 359428 62cc3c b2ee68"), P("8c241a d4503a f08a5a"), "scales", P("8c241a d4503a f08a5a ffb890")),
 ("Devil", P("5a0a14 8a1420 c8283a ff4a5a"), None, "solid", P("f4c030 ff6a2a ff3a30 ffe070")),
 ("Bunny Puff", P("c8c0c8 e8e0e8 f8f4f8 ffffff"), None, "solid", P("e8e0e8 f8f4f8 ffffff ffffff")),
 ("Raccoon", P("6a6a76 8a8a98 a8a8b6 c8c8d4"), P("1c1c24 2c2c38 3c3c4a"), "rings", P("1c1c24 2c2c38 3c3c4a")),
 ("Mermaid", P("0a5a78 1a8ab0 2ad0d8 7af0e8"), P("a84ad8 d07aff e8b0ff"), "scales", P("a84ad8 d07aff e8b0ff ffd8ff")),
 ("Skunk", P("1c1c24 2c2c38 3c3c4a 54545e"), P("e8e8f0 f8f8ff ffffff"), "stripes", P("f4f4fa ffffff ffffff")),
 ("Neon Pulse", P("0a0e22 141e3a 1e3a58 2a5a80"), P("38f2ff 98f8ff d8ffff"), "bolt", P("ff4fd8 ff9cf0 ffe0fa ffffff")),
]

# ---------------------------------------------------------------- shoulder pet (64x32)
# body 4x3x5 (0,0) | head 3x3x3 (18,0) | ear 1x1x1 (30,0) | tail 1x1x3 (34,0)
def make_pet(spec):
    name, pal, face, kind, ear, tail, tip, vest = spec
    pal = P(pal); img = Img(64, 32)
    def body(f, x, y, W, H):
        al = x if f in ("left", "right") else y          # along the body for top / sides
        if kind == "belly" and (f == "bot" or (f != "top" and y == H - 1)): return pal[4]
        if kind == "stripes" and f in ("top", "left", "right") and al % 2 == 1: return pal[1]
        if kind == "spots" and f != "bot" and rnd(x, y, 21 + len(name)) > .78: return pal[1]
        if kind == "vest" and f in ("top", "left", "right") and 1 <= al <= 3: return pal[vest]
        return rp(pal, .55 + .3 * (vnoise(x, y, 6, 1.8) - .5) + (.12 if f == "top" else 0), x, y)
    def head(f, x, y, W, H):
        if f == "front":
            ch = face[y][x]
            return pal[2] if ch == "." else pal[int(ch, 16)]
        if f == "top": return rp(pal, .78 + .1 * (rnd(x, y, 3) - .5), x, y)
        if f == "back": return pal[1] if y == 2 else rp(pal, .45, x, y)
        return pal[1] if f == "bot" else (pal[2] if y < 2 else pal[1])
    paint_box(img, 0, 0, 4, 3, 5, body); paint_box(img, 18, 0, 3, 3, 3, head)
    if ear is not None: paint_box(img, 30, 0, 1, 1, 1, lambda f, x, y, W, H: pal[ear])
    if tail is not None:
        paint_box(img, 34, 0, 1, 1, 3, lambda f, x, y, W, H: pal[tip] if (tip is not None and (y if f in ("top", "bot") else x) == 2) else pal[tail])
    return img
PETS = [  # name, palette, face rows (3x3), body kind, ear colour idx, tail idx, tail tip idx, vest idx
 ("Glacier Bear", "122539 a8b8d8 e8f0fa ffffff 5b87ea 1a2a3e", ["222", "020", "303"], "vest", 2, 2, None, 4),
 ("Tabby Cat", "2a1408 8a4a14 d8782a f8b868 ffe8c8 5ae070", ["222", "525", "404"], "stripes", 1, 2, 1, 1),
 ("Red Fox", "2a1008 b84a10 f0782a ffa860 fff0e0 3a2a28", ["222", "020", "404"], "belly", 5, 2, 4, 4),
 ("Snowy Owl", "2a2a34 8a8a9a d8dce8 ffffff f8c030 5a4a3a", ["222", "424", "252"], "spots", None, 1, None, 1),
 ("Penguin", "0e1220 1e2a44 2e3e66 f4f8ff e8eef8 f0a030", ["222", "323", "252"], "belly", None, 1, None, 1),
 ("Bunny", "6a5a60 d8ccd4 f8f4f8 ffffff ffb0c8 e04a60", ["222", "020", "242"], "belly", 3, 3, None, 1),
 ("Frog", "0e2a10 2e7a2a 5ac048 98f070 d8ffa0 f8f8f0", ["222", "222", "010"], "spots", 5, None, None, 1),
 ("Panda", "16161c 3a3a44 f4f4f8 ffffff 8a8a96 5a5a64", ["222", "020", "202"], "vest", 0, 3, None, 0),
 ("Parrot", "7a0a10 d82020 ff5050 ffc030 3aa0f0 f8f8f0", ["222", "505", "232"], "vest", 3, 4, 3, 4),
 ("Hatchling", "123416 1f5a1a 359428 62cc3c e8f8b0 f08a2a", ["222", "020", "242"], "belly", 5, 2, 5, 4),
 ("Raven", "10121c 222840 3c466a 6a78a8 e8c038 8a90c0", ["222", "040", "242"], "plain", None, 1, 0, 1),
]

# ---------------------------------------------------------------- scarf (64x32)
# collar 9x3x5 (0,0) | flapA 3x8x1 (0,8) | flapB 3x5x1 (8,8)
def snowflake(part, face, x, y, W, H):
    if part == "flapA" and face == "front" and 2 <= y <= 4 and STAR3[y - 2][x] == "1": return BLU[5]
    return None
SCARVES = [  # name, base, accent, pattern, fringe, deco
 ("Glacier Knit", P("122539 1a3e6a 24477e 3a5cb8"), P("8fb4ff c8e0ff e8f4ff"), "stripe", True, snowflake),
 ("Red Wool", P("5a0e12 8a1a1e c02a2c e04848"), P("e8e0d0 f8f0e4 ffffff"), "stripe", True, None),
 ("Candy Stripe", P("c82030 e8384a ff5a6a ff8a96"), P("e8e8f0 f8f8ff ffffff"), "diag", True, None),
 ("Rainbow Knit", P("2a2a44 3a3a5c 4a4a74"), P("8a8ab8 b8b8dc"), "rainbow", True, None),
 ("Cozy Plaid", P("1a3a22 2a5a34 3e7a4a 5aa066"), P("8a1a1e c02a2c e04848"), "plaid", True, None),
 ("Midnight Silk", P("1a1030 2a1a4a 3e2a6a 5a42a0"), P("c8981c ffd84a"), "solid", True, None),
 ("Autumn Wrap", P("7a3a0a b8541a e0782a ffa04a"), P("5a2a08 8a4a14 b86a2a"), "stripe", True, None),
 ("Starry Night", P("0a1030 141c48 202c70 2e3e98"), P("b88a1a e8b830 ffd84a fff0a0 ffffff"), "stars", False, None),
 ("Sakura Wrap", P("e8a0b8 f4b8cc f8cede ffe0ea"), P("c8587c e07a9c f090b4"), "dots", True, None),
 ("Frost Fluff", P("8ab8d8 b4d8ee d0e8f6 f0f8ff"), P("e8f4ff f8fcff ffffff"), "knit", True, None),
 ("Ocean Blue", P("0a4575 0c5f90 1a86b8 4fb3d9"), P("e8f4ff c8e0ff"), "stripe", True, None),
]
def make_scarf(spec):
    name, pal, acc, kind, fringe, deco = spec
    ck, fk = ("vstripe", "hstripe") if kind == "stripe" else ("rainbowv", "rainbow") if kind == "rainbow" else (kind, kind)
    img = Img(64, 32)
    def part(pname, k):
        def fn(f, x, y, W, H):
            if deco:
                c = deco(pname, f, x, y, W, H)
                if c is not None: return c
            if fringe and pname != "collar" and y == H - 1: return None if x % 2 == 1 else pal[1]
            c = pcol(pal, acc, k, x, y, W, H, 5)
            if pname == "collar" and f in ("front", "back") and y == 0: c = lit(c, .1)
            return c
        return fn
    paint_box(img, 0, 0, 9, 3, 5, part("collar", ck))
    paint_box(img, 0, 8, 3, 8, 1, part("flapA", fk)); paint_box(img, 8, 8, 3, 5, 1, part("flapB", fk))
    return img

# ---------------------------------------------------------------- mask (64x32)
# plate 8x6x1 (0,0) | strap 8x2x8 (0,7)
MASKS = {  # name -> (palette, 6 rows of 8 chars, strap palette index or None); '.' transparent
 "Glacier Visor": ("122539 24477e 5b87ea 8fb4ff e8f4ff c8e0ff60 ffffff",
                   ["........", ".122221.", "15655551", "15555561", ".133331.", "........"], 1),
 "Oni": ("5a0a10 a01820 d8303c ff6a5a f8e8c8 1c1c24 e8b830",
         ["62222226", "20322302", "2..22..2", "2..22..2", "22222222", "24455442"], 0),
 "Kitsune": ("8a1a1a d83a3a f4f0e8 d8d0c4 ffffff 2a2a34 e8a8a8",
             ["22222222", "21222212", "2..22..2", "2..22..2", "12255221", "22222222"], 1),
 "Surgical": ("5a7aa8 88a8d4 b4cce8 d8e8f8 ffffff",
              ["........", "........", "........", "...22...", ".233332.", "24111142"], None),
 "Ninja": ("10141c 1e2430 2e3648 44506a 6a7a98 c82030",
           ["32323232", "55555555", "2......2", "3......3", "23232323", "32323232"], 2),
 "Domino": ("2a1a4a 4a2a7a 7a4ab0 ffd84a fff0a0",
            ["........", ".222222.", "2..00..2", ".2....2.", "........", "........"], 1),
 "Hockey": ("6a6a78 b8b8c8 f0f0f4 ffffff c82030 2a2a34",
            ["42222224", "23333332", "2..33..2", "2..33..2", "22525222", "25252522"], 1),
 "Skull Mask": ("6a6458 a89c84 d8ceb4 f4eede ffffff 2a2630",
                ["23333332", "23333332", "2..33..2", "2..33..2", "22255222", "23535352"], 1),
 "Venetian": ("8a6a1a c8981c ffd84a fff0a0 ffffff 7a2ad8 2ad8c8",
              ["..2332..", "22355322", "2..22..2", ".2....2.", "........", "........"], 1),
 "Wolf Mask": ("2a2a30 4a4a56 7a7a88 aaaab8 e8e8f0 f0a0a0",
               ["21222212", "22333322", "2..33..2", "2..33..2", "23300332", "22422422"], 1),
 "Robo Plate": ("202630 3e4656 6a788c 9aaac0 d0def0 18c8e8 ff4060",
                ["........", "........", "........", "...22...", ".233332.", "23555532"], 1),
}
def make_mask(name):
    pal, rows, strap = MASKS[name]; pal = P(pal)
    G = [[None if ch == '.' else pal[int(ch, 16)] for ch in r] for r in rows]
    img = Img(64, 32)
    def plate(f, x, y, W, H):
        if f == "front": return G[y][x]
        if f == "back":
            c = G[y][W - 1 - x]
            return mul(c, .6) if c else None
        if f == "top": return G[0][x]
        if f == "bot": return G[5][x]
        return G[y][0 if f == "right" else 7]
    paint_box(img, 0, 0, 8, 6, 1, plate, edge=1.0)
    if strap is not None:
        sc_ = pal[strap]
        paint_box(img, 0, 7, 8, 2, 8, lambda f, x, y, W, H: None if f == "front" else rp([mul(sc_, .7), sc_, lit(sc_, .15)], .5 + (.2 if y == 0 else 0) + .1 * (vnoise(x, y, 7, 2) - .5), x, y), edge=1.0)
    return img

# ================================================================ Glacier set: one branded design per older type
GAU = P("122539 1a3e6a 24477e 3a5cb8 5b87ea 8fb4ff c8e0ff e8f4ff")
_EMB_WING = logo_mask(10, 9)
def w_gaurora(w):
    if w.r >= w.L - (0 if w.x == 1 else 1) - (1 if (w.i + w.x) % 2 else 0): return None
    if emblem(_EMB_WING, w.c, w.r, 2, 2): return GAU[7] if w.r < 6 else GAU[6]
    if w.r >= w.L - 2: return GAU[7] if w.x != 1 else GAU[6]                      # frosted tips
    t = .12 + .62 * w.g + (.1 if w.x == 1 else 0) - (.12 if sc(w) else 0) + .08 * (nz(w, 3, 2) - .5)
    return rp(GAU, t, w.c, w.r)
WINGS.insert(0, ("Glacier Aurora", w_gaurora, ol_soft, (GAU[7], GAU[5], GAU[1])))

GBN = P("122539 24477e 5b87ea 8fb4ff e8f4ff")
def b_gfrost(U, y):
    if (U % 6 == 2 and y == 0) or (U % 6 == 5 and y == 1): return GBN[4]
    if (U % 6 in (1, 3) and y == 0) or (U % 6 in (4, 0) and y == 1): return GBN[3]
    return rp(GBN[:4], .45 + .3 * (vn(U, y, 3) - .5) + (.15 if y == 0 else 0), U, y)
BANDANAS.insert(0, ("Glacier Frost", b_gfrost))

def br_gcuff(U, y):
    x = U % 5
    if y == 1: return GBN[1] if x in (0, 4) else GBN[2]
    return GBN[4] if x == 2 else GBN[3] if x != 4 else GBN[2]
BRACELETS.insert(0, ("Glacier Cuff", (br_gcuff, GBN[3], GBN[1])))
HALOS = {"Glacier Ring": ("24477e 5b87ea 8fb4ff e8f4ff ffffff 122539", "13431", "01210", "23532"), **HALOS}
GLS = {"Glacier Lens": ("122539 24477e 5b87ea 8fb4ff c8e0ff ffffff", ["1221", "2354", "1221"], "2222", "120", 2, "2110"), **GLS}
EARS = {"Glacier Bear": ("5a6c8c 8c9eba c8d6ea f0f6ff 5b87ea 3a5cb8 122539", ["232", "242"], "22", "."), **EARS}


def _wing(spec):
    name, fn, ol, bone = spec
    return make_wing((fn, ol_default if ol == 'd' else ol, bone))

DESIGNS = {  # folder -> [(display name, builder)] ; sizes in SIZES
    "bandana": [(n, (lambda f=f: make_bandana(f))) for n, f in BANDANAS],
    "glasses": [(n, (lambda n=n: make_glasses(n))) for n in GLS],
    "wings": [(s[0], (lambda s=s: _wing(s))) for s in WINGS],
    "bracelet": [(n, (lambda s=s: make_bracelet(s))) for n, s in BRACELETS],
    "halo": [(n, (lambda n=n: make_halo(n))) for n in HALOS],
    "ears": [(n, (lambda n=n: make_ears(n))) for n in EARS],
    "backpack": [(s[0], (lambda s=s: make_backpack(s))) for s in BACKPACKS],
    "hat": [(s[0], (lambda s=s: make_hat(s))) for s in HATS],
    "tail": [(s[0], (lambda s=s: make_tail(s))) for s in TAILS],
    "pet": [(s[0], (lambda s=s: make_pet(s))) for s in PETS],
    "scarf": [(s[0], (lambda s=s: make_scarf(s))) for s in SCARVES],
    "mask": [(n, (lambda n=n: make_mask(n))) for n in MASKS],
}
SIZES = {"bandana": (36, 11), "glasses": (24, 10), "wings": (64, 32), "bracelet": (20, 7), "halo": (16, 8), "ears": (16, 8),
         "backpack": (64, 32), "hat": (64, 32), "tail": (64, 32), "pet": (64, 32), "scarf": (64, 32), "mask": (64, 32)}
TYPE_ORDER = ["bandana", "glasses", "wings", "bracelet", "halo", "ears", "backpack", "hat", "tail", "pet", "scarf", "mask"]   # enum order in Accessories.java
def slug(name): return name.lower().replace(" ", "_")

# ---------------------------------------------------------------- previews
from PIL import ImageDraw, ImageFont
def _font(sz):
    try: return ImageFont.load_default(size=sz)
    except TypeError: return ImageFont.load_default()

def _px(im, x, y): return im.getpixel((x, y))
def view_wings(img):
    im = img.im; out = Image.new("RGBA", (34, 17), (0, 0, 0, 0))
    for y in range(17):
        for x in range(15, 19): out.putpixel((x, y), (46, 50, 66, 255))
    for u in range(15):
        for y in range(2):
            col = _px(im, 19 + u, 2 + y); out.putpixel((u, y), col); out.putpixel((19 + u, y), col)
    for i in range(5):
        for x in range(3):
            for r in range(LEN[i]):
                col = _px(im, i * 8 + 5 + x, 6 + r)
                out.putpixel((19 + 3 * i + x, 2 + r), col); out.putpixel(((4 - i) * 3 + x, 2 + r), col)
    return out
def view_strip(img, box):
    return img.im.crop(box)
def view_halo(img):
    im = img.im; out = Image.new("RGBA", (40, 3), (0, 0, 0, 0))
    for k in range(8):
        for x in range(5):
            for y in range(2): out.putpixel((k * 5 + x, y), _px(im, 2 + x, y))
            out.putpixel((k * 5 + x, 2), _px(im, 2 + x, 2))
    return out
def view_ears(img):
    im = img.im; out = Image.new("RGBA", (50, 4), (0, 0, 0, 0))
    def ear(ox, fx, fy_a, fx_b, fx_c):
        for x in range(3):
            for y in range(2):
                for dx in (0, 1): out.putpixel((ox + 2 * x + dx, 2 + y), _px(im, fx + x, fy_a + y))
        for x in range(2):
            for dx in (0, 1): out.putpixel((ox + 1 + 2 * x + dx, 1), _px(im, fx_b + x, 5))
        for dx in (0, 1): out.putpixel((ox + 2 + dx, 0), _px(im, fx_c, 5))
    for k, ox in enumerate((0, 14)): ear(ox, 1, 1, 1, 9)
    for k, ox in enumerate((30, 44)): ear(ox, 5, 1, 4, 11)
    return out
def view_glasses(img):
    im = img.im; out = Image.new("RGBA", (11, 5), (214, 170, 130, 255))
    for k in range(2):
        for x in range(4):
            for y in range(3):
                c = _px(im, 1 + x, 1 + y)
                if c[3]: out.alpha_composite(Image.new("RGBA", (1, 1), c), (1 + x + 5 * k, 1 + y))
    out.alpha_composite(Image.new("RGBA", (1, 1), _px(im, 11, 1)), (5, 2))
    return out

def contact(title, tiles, S, cols):
    f = _font(12); ft = _font(16)
    cw = max(t.width for _, t in tiles) * S + 14; ch = max(t.height for _, t in tiles) * S + 30
    rows = -(-len(tiles) // cols)
    sh = Image.new("RGBA", (cols * cw + 8, rows * ch + 40), (28, 30, 38, 255)); d = ImageDraw.Draw(sh)
    d.text((8, 8), title, fill=(220, 226, 240), font=ft)
    for k, (name, t) in enumerate(tiles):
        x0 = 8 + (k % cols) * cw - 4 + 4; y0 = 34 + (k // cols) * ch
        d.rectangle((x0, y0, x0 + cw - 6, y0 + ch - 4), fill=(38, 41, 52))
        big = t.resize((t.width * S, t.height * S), Image.NEAREST)
        sh.alpha_composite(big, (x0 + (cw - 6 - big.width) // 2, y0 + 4))
        d.text((x0 + 4, y0 + ch - 20), name, fill=(170, 180, 205), font=f)
    return sh

PREVIEW = {  # folder -> (view function, scale, columns)
    "wings": (view_wings, 6, 5), "bandana": (lambda i: view_strip(i, (0, 9, 36, 11)), 9, 3),
    "glasses": (view_glasses, 14, 5), "bracelet": (lambda i: view_strip(i, (0, 5, 20, 7)), 12, 4),
    "halo": (view_halo, 8, 3), "ears": (view_ears, 10, 3),
    "backpack": (lambda i: i.im.crop((0, 0, 40, 23)), 5, 4), "hat": (lambda i: i.im.crop((0, 0, 62, 27)), 4, 3),
    "tail": (lambda i: i.im.crop((0, 0, 38, 14)), 6, 3), "pet": (lambda i: i.im.crop((0, 0, 42, 8)), 6, 3),
    "scarf": (lambda i: i.im.crop((0, 0, 28, 17)), 6, 3), "mask": (lambda i: i.im.crop((0, 0, 32, 17)), 6, 3),
}

# ---------------------------------------------------------------- build / check
def accessories_java():
    return os.path.join(ROOT, "src/main/java/net/glacier/cosmetics/Accessories.java")

def parse_java():
    """Enum constants of Accessories.Type -> {folder: [design names]}."""
    import re
    src = open(accessories_java(), encoding="utf-8").read()
    out = {}
    for m in re.finditer(r'[A-Z]+\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,((?:\s*"[^"]*"\s*,?)+)\s*\)', src):
        out[m.group(2)] = re.findall(r'"([^"]*)"', m.group(3))
    return out

def build():
    n = 0; imgs = {}
    for folder in TYPE_ORDER:
        os.makedirs(os.path.join(OUT, folder), exist_ok=True)
        tiles = []
        for name, mk in DESIGNS[folder]:
            img = mk(); assert img.im.size == SIZES[folder], (folder, name, img.im.size)
            img.im.save(os.path.join(OUT, folder, slug(name) + ".png"), optimize=True, compress_level=9)
            fn, S, cols = PREVIEW[folder]; tiles.append((name, fn(img))); n += 1
        sh = contact(f"{folder.title()} ({len(tiles)})", tiles, S, cols)
        sh.save(os.path.join(ROOT, f"tools/preview_{folder}.png"), optimize=True); imgs[folder] = sh
    W_ = max(s.width for s in imgs.values()); H_ = sum(s.height for s in imgs.values())
    big = Image.new("RGBA", (W_, H_), (28, 30, 38, 255)); y = 0
    for folder in TYPE_ORDER: big.alpha_composite(imgs[folder], (0, y)); y += imgs[folder].height
    big.save(os.path.join(ROOT, "tools/cosmetics_preview.png"), optimize=True)
    return n

def check():
    errs = []; java = parse_java(); total = 0
    for folder in TYPE_ORDER:
        mine = [n for n, _ in DESIGNS[folder]]
        if folder not in java: errs.append(f"{folder}: missing in Accessories.java"); continue
        jn = java[folder]
        if jn != mine: errs.append(f"{folder}: Java list != generator list (java-only {sorted(set(jn) - set(mine))}, gen-only {sorted(set(mine) - set(jn))})")
        if len(set(jn)) != len(jn): errs.append(f"{folder}: duplicate names")
        slugs = [slug(n) for n in jn]
        if len(set(slugs)) != len(slugs): errs.append(f"{folder}: duplicate slugs")
        d = os.path.join(OUT, folder)
        files = sorted(f for f in os.listdir(d) if f.endswith(".png")) if os.path.isdir(d) else []
        for n in jn:
            if not n.isascii() or len(n) > 18: errs.append(f"{folder}/{n}: name must be ASCII and <= 18 chars")
            p = os.path.join(d, slug(n) + ".png")
            if not os.path.exists(p): errs.append(f"{folder}/{n}: missing {slug(n)}.png"); continue
            im = Image.open(p).convert("RGBA"); total += 1
            if im.size != SIZES[folder]: errs.append(f"{folder}/{n}: size {im.size} != {SIZES[folder]}")
            px = [im.getpixel((x, y)) for y in range(im.height) for x in range(im.width)]
            vis = [p_ for p_ in px if p_[3] > 0]
            if not vis: errs.append(f"{folder}/{n}: empty"); continue
            if any(p_[3] > 200 and max(p_[:3]) < 8 for p_ in px): errs.append(f"{folder}/{n}: opaque black pixel")
            if len({p_[:3] for p_ in vis}) < 4: errs.append(f"{folder}/{n}: fewer than 4 colours")
            if any(p_[3] == 0 and p_[:3] != (0, 0, 0) for p_ in px): errs.append(f"{folder}/{n}: transparent pixel with colour data")
        for f in files:
            if f not in {slug(n) + ".png" for n in jn}: errs.append(f"{folder}: stray file {f} not in Accessories.java")
    for e in errs: print("CHECK FAIL:", e)
    print(f"check: {total} designs, " + ("OK" if not errs else f"{len(errs)} problem(s)"))
    return not errs

def save(img, rel):
    p = os.path.join(OUT, rel); assert os.path.exists(p), p
    assert Image.open(p).size == img.im.size, rel
    img.im.save(p, optimize=True)

def main():
    import sys
    if "--check" in sys.argv: sys.exit(0 if check() else 1)
    if "--capes" in sys.argv:
        for n, fn in CAPES.items(): save(make_cape(fn), n + ".png")
    make_cape(c_glacier_set).im.save(os.path.join(OUT, "glacier_set.png"), optimize=True, compress_level=9)
    n = build()
    print(n, "accessory textures written;", {f: len(DESIGNS[f]) for f in TYPE_ORDER})
    if "--no-check" not in sys.argv: sys.exit(0 if check() else 1)

if __name__ == "__main__":
    main()
