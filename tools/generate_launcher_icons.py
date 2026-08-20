"""Build Android launcher-icon rasters from the selected generated concept."""

from __future__ import annotations

import shutil
import sys
from pathlib import Path

from PIL import Image


BACKGROUND = (23, 19, 15, 255)  # Matches the app's warm-paper dark surface.
LEGACY_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}


def fitted_foreground(source: Image.Image, canvas_size: int, visual_ratio: float) -> Image.Image:
    source = source.convert("RGBA")
    # The generated source includes a very faint, wide glow (alpha < 8). It
    # should not determine the optical bounds or the launcher mark looks tiny.
    visible_alpha = source.getchannel("A").point(lambda value: 255 if value >= 8 else 0)
    bbox = visible_alpha.getbbox()
    if bbox is None:
        raise ValueError("The source icon has no visible pixels")
    padding = round(max(bbox[2] - bbox[0], bbox[3] - bbox[1]) * 0.07)
    bbox = (
        max(0, bbox[0] - padding),
        max(0, bbox[1] - padding),
        min(source.width, bbox[2] + padding),
        min(source.height, bbox[3] + padding),
    )
    cropped = source.crop(bbox)
    target = round(canvas_size * visual_ratio)
    scale = min(target / cropped.width, target / cropped.height)
    resized = cropped.resize(
        (round(cropped.width * scale), round(cropped.height * scale)),
        Image.Resampling.LANCZOS,
    )
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    offset = ((canvas_size - resized.width) // 2, (canvas_size - resized.height) // 2)
    canvas.alpha_composite(resized, offset)
    return canvas


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit("usage: generate_launcher_icons.py SOURCE_PNG PROJECT_ROOT")

    source_path = Path(sys.argv[1]).resolve()
    project_root = Path(sys.argv[2]).resolve()
    source = Image.open(source_path)

    concept_dir = project_root / "design" / "icon-concepts"
    concept_dir.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source_path, concept_dir / "option-1-source.png")

    # Adaptive foreground: 108dp rendered at xxxhdpi (432px). The visible mark
    # stays inside the central safe zone used by circle and squircle masks.
    adaptive = fitted_foreground(source, 432, 0.75)
    adaptive_dir = project_root / "app" / "src" / "main" / "res" / "drawable-xxxhdpi"
    adaptive_dir.mkdir(parents=True, exist_ok=True)
    adaptive.save(adaptive_dir / "ic_launcher_foreground.png", optimize=True)

    # Android 13 themed icon. Alpha is intentionally simplified to a single
    # tintable silhouette; the regular icon retains the generated colors.
    alpha = adaptive.getchannel("A").point(lambda value: 255 if value >= 48 else 0)
    monochrome = Image.new("RGBA", adaptive.size, (255, 255, 255, 0))
    monochrome.putalpha(alpha)
    monochrome.save(adaptive_dir / "ic_launcher_monochrome.png", optimize=True)

    preview_size = 1024
    preview = Image.new("RGBA", (preview_size, preview_size), BACKGROUND)
    preview.alpha_composite(fitted_foreground(source, preview_size, 0.75))
    preview.save(concept_dir / "option-1-adaptive-preview.png", optimize=True)

    # API 24–25 fallback icons. A full-bleed background lets device launchers
    # apply their own legacy mask without exposing transparent corners.
    legacy_master = Image.new("RGBA", (768, 768), BACKGROUND)
    legacy_master.alpha_composite(fitted_foreground(source, 768, 0.75))
    res_root = project_root / "app" / "src" / "main" / "res"
    for density, size in LEGACY_SIZES.items():
        output_dir = res_root / f"mipmap-{density}"
        output_dir.mkdir(parents=True, exist_ok=True)
        icon = legacy_master.resize((size, size), Image.Resampling.LANCZOS)
        icon.save(output_dir / "ic_launcher.png", optimize=True)
        icon.save(output_dir / "ic_launcher_round.png", optimize=True)


if __name__ == "__main__":
    main()
