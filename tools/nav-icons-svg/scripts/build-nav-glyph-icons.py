#!/usr/bin/env python3
"""Raster nav icons: transparent PNG, warm-gray glyph only (no milk disc)."""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
PROJECT = ROOT.parents[1]
ASSETS = ROOT / "assets"
GLYPH = (92, 87, 79, 255)

# Procedural sources (viewBox 0 0 180 180), same purple as user assets for outline pipeline.

# Gear cog — v0 NeuNavSettings glyphPath
SETTINGS_GEAR_PATH = [
    (90, 50), (100, 54), (110, 52), (114, 62), (124, 66), (124, 76), (132, 82), (128, 92),
    (132, 102), (124, 108), (124, 118), (114, 122), (110, 132), (100, 130), (90, 134),
    (80, 130), (70, 132), (66, 122), (56, 118), (56, 108), (48, 102), (52, 92), (48, 82),
    (56, 76), (56, 66), (66, 62), (70, 52), (80, 54),
]

SIZES = [
    ("drawable-mdpi", 43),
    ("drawable-hdpi", 64),
    ("drawable-xhdpi", 85),
    ("drawable-xxhdpi", 128),
    ("drawable-xxxhdpi", 171),
]

ICONS = [
    # mode: fill = silhouette; outline = contour. glyph_scale: fraction of canvas (no disc).
    ("nav_direction", "direction-map-source.png", 0.84, 0.5, "fill"),
    ("nav_prayer", "prayer-rug-source.png", 0.84, 0.5, "fill"),
    ("nav_dua", "dua-pray-source.png", 0.84, 0.5, "fill"),
    ("nav_settings", "settings-gear-source.png", 0.84, 0.5, "outline"),
]


def is_background(r: int, g: int, b: int) -> bool:
    lum = 0.299 * r + 0.587 * g + 0.114 * b
    if lum < 32:
        return True
    if r > 235 and g > 235 and b > 235:
        return True
    return False


def is_foreground(r: int, g: int, b: int) -> bool:
    return not is_background(r, g, b)


def glyph_from_source_fill(src: Path) -> Image.Image:
    img = Image.open(src).convert("RGBA")
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
    if not bbox:
        raise SystemExit(f"No glyph pixels in {src}")
    return img.crop(bbox)


def glyph_from_source_outline(src: Path) -> Image.Image:
    """Filled art -> contour: keep fg pixels that touch bg (outer edge + holes)."""
    src_img = Image.open(src).convert("RGBA")
    px = src_img.load()
    w, h = src_img.size
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    op = out.load()

    for y in range(h):
        for x in range(w):
            r, g, b, _ = px[x, y]
            if not is_foreground(r, g, b):
                continue
            on_edge = False
            for dy in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    if dx == 0 and dy == 0:
                        continue
                    nx, ny = x + dx, y + dy
                    if nx < 0 or ny < 0 or nx >= w or ny >= h:
                        on_edge = True
                        break
                    nr, ng, nb, _ = px[nx, ny]
                    if is_background(nr, ng, nb):
                        on_edge = True
                        break
                if on_edge:
                    break
            if on_edge:
                op[x, y] = GLYPH

    # Slight thicken so contour survives 45dp nav cell
    r, g, b, a = out.split()
    a = a.filter(ImageFilter.MaxFilter(3))
    out = Image.merge("RGBA", (r, g, b, a))

    bbox = out.getbbox()
    if not bbox:
        raise SystemExit(f"No outline pixels in {src}")
    return out.crop(bbox)


def glyph_from_source(src: Path, mode: str) -> Image.Image:
    if mode == "outline":
        return glyph_from_source_outline(src)
    return glyph_from_source_fill(src)


def fit_glyph(glyph: Image.Image, max_side: int) -> Image.Image:
    g = glyph.copy()
    g.thumbnail((max_side, max_side), Image.Resampling.LANCZOS)
    px = g.load()
    for y in range(g.height):
        for x in range(g.width):
            if px[x, y][3] < 24:
                px[x, y] = (0, 0, 0, 0)
    box = g.getbbox()
    return g.crop(box) if box else g


SOURCE_FILL = (110, 55, 170)
SOURCE_SIZE = 180


def write_polygon_source(filename: str, points: list[tuple[int, int]], *, overwrite: bool = False) -> Path:
    path = ASSETS / filename
    if path.exists() and not overwrite:
        return path
    ASSETS.mkdir(parents=True, exist_ok=True)
    img = Image.new("RGB", (SOURCE_SIZE, SOURCE_SIZE), (255, 255, 255))
    ImageDraw.Draw(img).polygon(points, fill=SOURCE_FILL)
    img.save(path)
    print(f"generated {path.name}")
    return path


def write_bell_source() -> Path:
    """Prayer times / reminders — notification bell (replaces mihrab)."""
    path = ASSETS / "prayer-bell-source.png"
    ASSETS.mkdir(parents=True, exist_ok=True)
    img = Image.new("RGB", (SOURCE_SIZE, SOURCE_SIZE), (255, 255, 255))
    d = ImageDraw.Draw(img)
    f = SOURCE_FILL
    d.ellipse((84, 30, 96, 44), fill=f)
    d.polygon(
        [
            (90, 46), (112, 58), (120, 88), (118, 112),
            (90, 118), (62, 112), (60, 88), (68, 58),
        ],
        fill=f,
    )
    d.ellipse((85, 122, 95, 132), fill=f)
    img.save(path)
    print(f"generated {path.name}")
    return path


def ensure_procedural_assets() -> None:
    write_bell_source()
    write_polygon_source("settings-gear-source.png", SETTINGS_GEAR_PATH)


def composite(glyph: Image.Image, size: int, glyph_scale: float, glyph_y: float) -> Image.Image:
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    g = fit_glyph(glyph, int(size * glyph_scale))
    x = (size - g.width) // 2
    y = int(size * glyph_y) - g.height // 2
    out.alpha_composite(g, (x, y))
    return out


def build_icon(
    name: str,
    asset: str,
    glyph_scale: float,
    glyph_y: float,
    mode: str = "fill",
) -> None:
    src = ASSETS / asset
    glyph = glyph_from_source(src, mode)
    res = PROJECT / "app" / "src" / "main" / "res"
    export = ROOT / "v0" / "export"
    export.mkdir(parents=True, exist_ok=True)
    print(f"{name} ({mode}) glyph {glyph.width}x{glyph.height} from {asset}")
    for folder, px in SIZES:
        icon = composite(glyph, px, glyph_scale, glyph_y)
        for base in (res / folder, export):
            base.mkdir(parents=True, exist_ok=True)
            icon.save(base / f"{name}.png")
        print(f"  {folder}/{name}.png {px}px")


def main() -> None:
    ensure_procedural_assets()
    for item in ICONS:
        build_icon(*item)


if __name__ == "__main__":
    main()
