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
    "M42,28 H66 Q78,28 78,40 V64 Q78,76 66,76 "
    "H53 L43,82 L41,76 Q30,75 30,64 V40 Q30,28 42,28 Z"
)
N_PATH = "M39,41 H46 L63,60 V41 H70 V69 H63 L46,50 V69 H39 Z"
N_POINTS = [(39, 41), (46, 41), (63, 60), (63, 41), (70, 41),
            (70, 69), (63, 69), (46, 50), (46, 69), (39, 69)]


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

    line(42, 28)
    line(66, 28)
    quad((66, 28), (78, 28), (78, 40))
    line(78, 64)
    quad((78, 64), (78, 76), (66, 76))
    line(53, 76)
    line(43, 82)
    line(41, 76)
    quad((41, 76), (30, 75), (30, 64))
    line(30, 40)
    quad((30, 40), (30, 28), (42, 28))
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


def main() -> None:
    write_vectors()
    write_legacy()
    write_preview()


if __name__ == "__main__":
    main()
