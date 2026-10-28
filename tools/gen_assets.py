"""Generates all image assets for the Seconds Clock widget app (stdlib + Pillow only)."""
import os
import math
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
DOCS = os.path.join(ROOT, "docs")
NODEPI = os.path.join(RES, "drawable-nodpi")
DRAWABLE = os.path.join(RES, "drawable")
FONTS = r"C:\Windows\Fonts"

def F(name, size):
    p = os.path.join(FONTS, name)
    if os.path.exists(p):
        try:
            return ImageFont.truetype(p, size)
        except Exception:
            pass
    return ImageFont.load_default(size)

A = (17, 23, 31, 250)          # dial fill
BG_T = (10, 14, 20, 232)       # widget background (slightly translucent)
EDGE = (44, 54, 68, 255)
WHITE = (240, 244, 250, 255)
GRAY = (120, 130, 146, 255)
DATE = (158, 168, 180, 255)
RED = (255, 74, 74, 255)

for p in (NODEPI, DRAWABLE, DOCS):
    os.makedirs(p, exist_ok=True)

def rot_paste(base, src, cx, cy, side, deg):
    s = src.resize((side, side), Image.LANCZOS).rotate(-deg, resample=Image.BICUBIC, center=(side / 2, side / 2))
    base.alpha_composite(s, (int(cx - side / 2), int(cy - side / 2)))

# ---------------- 9-patch widget background ----------------
def bg_patch():
    side = 512
    img = Image.new("RGBA", (side + 2, side + 2), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([1, 1, side, side], radius=84, fill=BG_T)
    blk = (0, 0, 0, 255)
    a, b = 176, 336
    d.line([(a, 0), (b, 0)], fill=blk)              # top stretch
    d.line([(0, a), (0, b)], fill=blk)              # left stretch
    d.line([(10, side + 1), (side - 10, side + 1)], fill=blk)  # bottom: small padding
    d.line([(side + 1, 10), (side + 1, side - 10)], fill=blk)  # right: small padding
    img.save(os.path.join(NODEPI, "bg_clock.9.png"))

# ---------------- dial ----------------
def make_dial(size=1024):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = size / 2
    R = size * 0.49
    d.ellipse([c - R, c - R, c + R, c + R], fill=A, outline=EDGE, width=max(2, size // 320))
    tf = F("segoeuib.ttf", int(size * 0.082))
    for i in range(60):
        major = (i % 5 == 0)
        rad = math.radians(i * 6 - 90)
        ln = size * 0.048 if major else size * 0.016
        w = max(2, int(size * 0.008)) if major else max(2, int(size * 0.0035))
        col = WHITE if major else GRAY
        x1 = c + math.sin(rad) * (R * 0.90)
        y1 = c - math.cos(rad) * (R * 0.90)
        x2 = c + math.sin(rad) * (R * 0.90 - ln)
        y2 = c - math.cos(rad) * (R * 0.90 - ln)
        d.line([x1, y1, x2, y2], fill=col, width=w)
    for txt, deg in (("12", 0), ("3", 90), ("6", 180), ("9", 270)):
        rad = math.radians(deg - 90)
        x = c + math.sin(rad) * R * 0.665
        y = c - math.cos(rad) * R * 0.665
        d.text((x, y), txt, font=tf, fill=WHITE, anchor="mm")
    r = size * 0.018
    d.ellipse([c - r, c - r, c + r, c + r], fill=WHITE)
    return img

# ---------------- hands ----------------
def make_hand(kind):
    size = 1024
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if kind == "hour":
        w, length, color, tail = 34, 300, WHITE, 0
    elif kind == "minute":
        w, length, color, tail = 22, 440, WHITE, 0
    else:
        w, length, color, tail = 10, 470, RED, 122
    d.line([512, 512 + tail, 512, 512 - length], fill=color, width=w)
    r = w // 2
    d.ellipse([512 - r, 512 - length - r, 512 + r, 512 - length + r], fill=color)
    if tail:
        d.ellipse([512 - r, 512 + tail - r, 512 + r, 512 + tail + r], fill=color)
        rr = 15
        d.ellipse([512 - rr, 512 - rr, 512 + rr, 512 + rr], fill=color)
    img.save(os.path.join(DRAWABLE, "hand_%s.png" % kind))

# ---------------- previews ----------------
def analog_square(w, h, strip, fname):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([2, 2, w - 2, h - 2], radius=max(14, w // 16), fill=BG_T, outline=EDGE, width=2)
    sh = int(h * 0.24) if strip else int(h * 0.10)
    dsz = min(w - int(w * 0.22), h - sh - int(h * 0.10))
    cx, cy = w // 2, int((h - sh) / 2)
    rot_paste(img, DIAL, cx, cy, dsz, 0)
    rot_paste(img, HANDS["hour"], cx, cy, dsz, 305)
    rot_paste(img, HANDS["minute"], cx, cy, dsz, 48)
    rot_paste(img, HANDS["second"], cx, cy, dsz, 144)
    if strip:
        tf = F("segoeuisb.ttf", int(h * 0.078))
        df = F("segoeui.ttf", int(h * 0.056))
        d.text((w / 2, h - sh * 0.62), "12:45:08", font=tf, fill=WHITE, anchor="mm")
        d.text((w / 2, h - sh * 0.24), "Wed, Oct 28", font=df, fill=DATE, anchor="mm")
    img.save(fname)

def analog_wide(w, h, fname):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([2, 2, w - 2, h - 2], radius=max(14, h // 5), fill=BG_T, outline=EDGE, width=2)
    dsz = int(h * 0.78)
    cx = int(dsz / 2 + w * 0.06)
    cy = h // 2
    rot_paste(img, DIAL, cx, cy, dsz, 0)
    rot_paste(img, HANDS["hour"], cx, cy, dsz, 305)
    rot_paste(img, HANDS["minute"], cx, cy, dsz, 48)
    rot_paste(img, HANDS["second"], cx, cy, dsz, 144)
    tx = cx + dsz / 2 + w * 0.06
    tf = F("segoeuisb.ttf", int(h * 0.27))
    df = F("segoeui.ttf", int(h * 0.13))
    d.text((tx, cy - h * 0.10), "12:45:08", font=tf, fill=WHITE, anchor="lm")
    d.text((tx, cy + h * 0.16), "Wed, Oct 28", font=df, fill=DATE, anchor="lm")
    img.save(fname)

def digital_card(w, h, parts, fname, date_scale=0.12):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([2, 2, w - 2, h - 2], radius=max(14, w // 16), fill=BG_T, outline=EDGE, width=2)
    fonts = [F("segoeuisb.ttf", int(h * s)) for _, _, s in parts]
    text = "".join(t for t, _, _ in parts)
    total = sum(d.textlength(t, font=f) for (t, _, _), f in zip(parts, fonts))
    x = (w - total) / 2
    cy = h * (0.46 if h > w * 1.2 else 0.44)
    for (t, col, _), f in zip(parts, fonts):
        d.text((x, cy), t, font=f, fill=col, anchor="lm")
        x += d.textlength(t, font=f)
    df = F("segoeui.ttf", int(h * date_scale))
    d.text((w / 2, cy + max(fonts, key=lambda f: f.size).size * 0.62), "Wed, Oct 28", font=df, fill=DATE, anchor="mm")
    img.save(fname)

# ---------------- icons ----------------
def icon_foreground():
    size = 432
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = size / 2
    R = 150
    d.ellipse([c - R, c - R, c + R, c + R], outline=(236, 242, 248, 255), width=14)
    for i in range(12):
        rad = math.radians(i * 30)
        ln = 24
        x1 = c + math.sin(rad) * (R * 0.86)
        y1 = c - math.cos(rad) * (R * 0.86)
        x2 = c + math.sin(rad) * (R * 0.86 - ln)
        y2 = c - math.cos(rad) * (R * 0.86 - ln)
        d.line([x1, y1, x2, y2], fill=(236, 242, 248, 255), width=6)
    for deg, ln, w, col in ((305, 82, 18, (240, 244, 250, 255)), (48, 112, 12, (240, 244, 250, 255)), (144, 118, 6, RED)):
        rad = math.radians(deg)
        d.line([c, c, c + math.sin(rad) * ln, c - math.cos(rad) * ln], fill=col, width=w)
    d.ellipse([c - 7, c - 7, c + 7, c + 7], fill=WHITE)
    img.save(os.path.join(DRAWABLE, "ic_launcher_foreground.png"))

def status_icon():
    size = 96
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = size / 2
    R = 40
    d.ellipse([c - R, c - R, c + R, c + R], outline=(255, 255, 255, 255), width=8)
    for deg, ln, w in ((305, 20, 8), (48, 27, 6)):
        rad = math.radians(deg)
        d.line([c, c, c + math.sin(rad) * ln, c - math.cos(rad) * ln], fill=(255, 255, 255, 255), width=w)
    img.save(os.path.join(DRAWABLE, "ic_stat_clock.png"))

# ---------------- run ----------------
HANDS = {}
for k in ("hour", "minute", "second"):
    make_hand(k)
    HANDS[k] = Image.open(os.path.join(DRAWABLE, "hand_%s.png" % k))
DIAL = make_dial()
DIAL.save(os.path.join(DRAWABLE, "dial.png"))

bg_patch()
icon_foreground()
status_icon()

# Launcher preview images (what the picker shows)
analog_square(200, 200, False, os.path.join(NODEPI, "preview_1x1.png"))
analog_wide(360, 176, os.path.join(NODEPI, "preview_1x2.png"))
analog_square(320, 320, True, os.path.join(NODEPI, "preview_2x2.png"))
analog_square(420, 420, True, os.path.join(NODEPI, "preview_3x3.png"))

# README gallery
analog_square(240, 240, False, os.path.join(DOCS, "preview_1x1_analog.png"))
digital_card(240, 240, [("12:45:08", WHITE, 0.20)], os.path.join(DOCS, "preview_1x1_digital.png"))
analog_wide(440, 216, os.path.join(DOCS, "preview_1x2_analog.png"))
digital_card(440, 216, [("12:45:08", WHITE, 0.30)], os.path.join(DOCS, "preview_1x2_digital.png"), date_scale=0.09)
analog_square(400, 400, True, os.path.join(DOCS, "preview_2x2_analog.png"))
digital_card(400, 400, [("12:45:08", WHITE, 0.20)], os.path.join(DOCS, "preview_2x2_digital.png"))
analog_square(480, 480, True, os.path.join(DOCS, "preview_3x3_analog.png"))
digital_card(480, 480, [("12:45", WHITE, 0.24), (":08", RED, 0.24)], os.path.join(DOCS, "preview_3x3_digital.png"))

print("assets generated:", sorted(os.listdir(NODEPI)), sorted(os.listdir(DRAWABLE)))
