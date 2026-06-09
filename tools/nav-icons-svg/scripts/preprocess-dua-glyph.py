#!/usr/bin/env python3
"""Purple hands on white/black -> dark glyph, background transparent."""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "assets" / "dua-hands-source.png"
OUT = ROOT / "v0" / "01" / "public" / "nav-dua-glyph.png"
GLYPH = (92, 87, 79, 255)


def is_background(r: int, g: int, b: int) -> bool:
    lum = 0.299 * r + 0.587 * g + 0.114 * b
    if lum < 32:
        return True
    if r > 235 and g > 235 and b > 235:
        return True
    return False


def main() -> None:
    img = Image.open(SRC).convert("RGBA")
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, _ = px[x, y]
            if is_background(r, g, b):
                px[x, y] = (0, 0, 0, 0)
            else:
                px[x, y] = GLYPH
    bbox = img.getbbox()
    if bbox:
        img = img.crop(bbox)
    img.save(OUT)
    opaque = sum(1 for y in range(img.height) for x in range(img.width) if px[x, y][3] > 0)
    print(f"Wrote {OUT} ({img.width}x{img.height}), opaque~{opaque}")


if __name__ == "__main__":
    main()
