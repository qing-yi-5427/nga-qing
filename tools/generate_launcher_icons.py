"""Generate V8 launcher vectors, legacy rasters, and a mask review preview.

Run from the project root: python tools/generate_launcher_icons.py
The geometry below is shared by VectorDrawable paths and the Pillow rasterizer.
"""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
BACKGROUND = "#F8F9FB"
SHADOW = "#D4E8FF"
BLUE = "#087EFF"  # V8 light theme primary color.
WHITE = "#FFFFFF"
LEGACY_SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

# The mark, including its tail, stays inside the center 66 dp diameter circle.
BUBBLE_PATH = (
    "M42,30 H66 Q78,30 78,42 V66 Q78,78 66,78 "
    "H53 L43,84 L41,78 Q30,77 30,66 V42 Q30,30 42,30 Z"
)
N_PATH = "M39,40 H46 L63,59 V40 H70 V68 H63 L46,49 V68 H39 Z"
N_POINTS = [(39, 40), (46, 40), (63, 59), (63, 40), (70, 40),
            (70, 68), (63, 68), (46, 49), (46, 68), (39, 68)]


def bubble_points(dy: float = 0.0) -> list[tuple[float, float]]:
    points: list[tuple[float, float]] = []

    def line(x: float, y: float) -> None:
        points.append((x, y + dy))

    def quad(start: tuple[float, float], control: tuple[float, float], end: tuple[float, float]) -> None:
        for step in range(1, 17):
            t = step / 16
            one = 1 - t
            line(one * one * start[0] + 2 * one * t * control[0] + t * t * end[0],
                 one * one * start[1] + 2 * one * t * control[1] + t * t * end[1])

    line(42, 30)
    line(66, 30)
    quad((66, 30), (78, 30), (78, 42))
    line(78, 66)
    quad((78, 66), (78, 78), (66, 78))
    line(53, 78)
    line(43, 84)
    line(41, 78)
    quad((41, 78), (30, 77), (30, 66))
    line(30, 42)
    quad((30, 42), (30, 30), (42, 30))
    return points


def draw_mark(canvas: Image.Image, *, monochrome: bool = False) -> None:
    scale = canvas.width / 108
    draw = ImageDraw.Draw(canvas)

    def scaled(points: list[tuple[float, float]]) -> list[tuple[int, int]]:
        return [(round(x * scale), round(y * scale)) for x, y in points]

    if not monochrome:
        draw.polygon(scaled(bubble_points(1)), fill=SHADOW)
    draw.polygon(scaled(bubble_points()), fill=WHITE if monochrome else BLUE)
    draw.polygon(scaled(N_POINTS), fill=(0, 0, 0, 0) if monochrome else WHITE)


def render_icon(size: int, *, round_icon: bool = False) -> Image.Image:
    # Android displays only the middle 72 dp of a 108 dp adaptive layer.
    # Raster previews and legacy icons use that exact visible window.
    master = Image.new("RGBA", (864, 864), BACKGROUND)
    draw_mark(master)
    master = master.crop((144, 144, 720, 720))
    if round_icon:
        mask = Image.new("L", master.size, 0)
        ImageDraw.Draw(mask).ellipse((0, 0, 575, 575), fill=255)
        master.putalpha(mask)
    return master.resize((size, size), Image.Resampling.LANCZOS)


def write_vectors() -> None:
    drawable = RES / "drawable"
    drawable.mkdir(parents=True, exist_ok=True)
    # Old density-specific drawables would outrank the new unqualified vectors.
    for old in (
        RES / "drawable-xxxhdpi" / "ic_launcher_foreground.png",
        RES / "drawable-xxxhdpi" / "ic_launcher_monochrome.png",
        drawable / "ic_launcher.xml",
    ):
        old.unlink(missing_ok=True)
    foreground = f'''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <group android:translateY="1">
        <path android:fillColor="{SHADOW}" android:pathData="{BUBBLE_PATH}" />
    </group>
    <path android:fillColor="{BLUE}" android:pathData="{BUBBLE_PATH}" />
    <path android:fillColor="{WHITE}" android:pathData="{N_PATH}" />
</vector>
'''
    monochrome = f'''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="{WHITE}" android:fillType="evenOdd"
        android:pathData="{BUBBLE_PATH} {N_PATH}" />
</vector>
'''
    (drawable / "ic_launcher_foreground.xml").write_text(foreground, encoding="utf-8")
    (drawable / "ic_launcher_monochrome.xml").write_text(monochrome, encoding="utf-8")
    (RES / "values" / "colors.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
        f'    <color name="ic_launcher_background">{BACKGROUND}</color>\n</resources>\n',
        encoding="utf-8",
    )


def write_legacy() -> None:
    for density, size in LEGACY_SIZES.items():
        directory = RES / f"mipmap-{density}"
        directory.mkdir(parents=True, exist_ok=True)
        render_icon(size).save(directory / "ic_launcher.png", optimize=True)
        render_icon(size, round_icon=True).save(directory / "ic_launcher_round.png", optimize=True)


def preview_mask(shape: str, size: int) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    if shape == "Circle":
        draw.ellipse((0, 0, size - 1, size - 1), fill=255)
    elif shape == "Rounded":
        draw.rounded_rectangle((0, 0, size - 1, size - 1), radius=size // 4, fill=255)
    elif shape == "Squircle":
        radius = (size - 1) / 2
        center = radius
        n = 4.4
        points = []
        for step in range(361):
            angle = 2 * math.pi * step / 360
            cosine, sine = math.cos(angle), math.sin(angle)
            x = center + radius * math.copysign(abs(cosine) ** (2 / n), cosine)
            y = center + radius * math.copysign(abs(sine) ** (2 / n), sine)
            points.append((x, y))
        draw.polygon(points, fill=255)
    else:
        draw.rectangle((0, 0, size - 1, size - 1), fill=255)
    return mask


def write_preview() -> None:
    preview = Image.new("RGB", (1250, 320), "#EBEFF5")
    draw = ImageDraw.Draw(preview)
    try:
        font = ImageFont.truetype("arial.ttf", 22)
    except OSError:
        font = ImageFont.load_default()
    for index, shape in enumerate(("Square", "Circle", "Squircle", "Rounded")):
        icon = render_icon(208)
        icon.putalpha(preview_mask(shape, 208))
        x = 26 + index * 245
        preview.paste(icon, (x, 24), icon)
        draw.text((x + 104, 250), shape, fill="#29313D", font=font, anchor="mt")
    icon48 = render_icon(48)
    preview.paste(icon48, (1042, 46), icon48)
    preview.paste(icon48.resize((144, 144), Image.Resampling.NEAREST), (994, 116))
    draw.text((1066, 277), "48 px", fill="#29313D", font=font, anchor="mt")
    directory = ROOT / "design" / "icon-concepts"
    directory.mkdir(parents=True, exist_ok=True)
    preview.save(directory / "v8-blue-preview.png", optimize=True)
    before_path = directory / "v8-blue-preview-before.png"
    if before_path.exists():
        before = Image.open(before_path).convert("RGB")
        comparison = Image.new("RGB", (2500, 362), "#EBEFF5")
        comparison_draw = ImageDraw.Draw(comparison)
        comparison_draw.text((625, 8), "Before", fill="#29313D", font=font, anchor="mt")
        comparison_draw.text((1875, 8), "Centered", fill="#29313D", font=font, anchor="mt")
        comparison.paste(before, (0, 42))
        comparison.paste(preview, (1250, 42))
        comparison.save(directory / "v8-blue-centering-comparison.png", optimize=True)


def main() -> None:
    write_vectors()
    write_legacy()
    write_preview()


if __name__ == "__main__":
    main()
