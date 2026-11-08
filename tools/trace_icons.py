#!/usr/bin/env python3
"""Traces the two supplied outline icons into Android vector drawables.

The source files are bitmaps (a price tag and a gamepad). potrace turns the black
shapes into paths, which are then scaled onto a 24x24 viewport with a 256-unit
grid — the same viewport the other icons use, so they line up optically.

    python3 tools/trace_icons.py /scratch/input/473576.png tag
"""
import sys
from pathlib import Path

import numpy as np
import potrace
from PIL import Image

VIEWPORT = 256

DRAWABLE = """<?xml version="1.0" encoding="utf-8"?>
<!-- Traced from the supplied artwork by tools/trace_icons.py -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="256"
    android:viewportHeight="256">
{paths}
</vector>
"""

PATH = """    <path
        android:fillType="evenOdd"
        android:pathData="{d}"
        android:fillColor="#FF000000" />
"""


def load_mask(path: Path) -> np.ndarray:
    image = Image.open(path).convert("RGBA")
    background = Image.new("RGBA", image.size, (255, 255, 255, 255))
    flattened = Image.alpha_composite(background, image).convert("L")
    # Ink is dark; trace where it is.
    return np.array(flattened) < 128


def to_paths(mask: np.ndarray) -> list[str]:
    bitmap = potrace.Bitmap(mask)
    path = bitmap.trace(turdsize=4, alphamax=1.0, opticurve=1, opttolerance=0.2)

    height, width = mask.shape
    scale = VIEWPORT / max(width, height)
    paths = []

    for curve in path:
        start = curve.start_point
        parts = [f"M{start.x * scale:.2f},{start.y * scale:.2f}"]
        for segment in curve.segments:
            if segment.is_corner:
                corner = segment.c
                end = segment.end_point
                parts.append(
                    f"L{corner.x * scale:.2f},{corner.y * scale:.2f} "
                    f"L{end.x * scale:.2f},{end.y * scale:.2f}"
                )
            else:
                c1, c2 = segment.c1, segment.c2
                end = segment.end_point
                parts.append(
                    f"C{c1.x * scale:.2f},{c1.y * scale:.2f} "
                    f"{c2.x * scale:.2f},{c2.y * scale:.2f} "
                    f"{end.x * scale:.2f},{end.y * scale:.2f}"
                )
        parts.append("Z")
        paths.append(" ".join(parts))

    return paths


def main() -> int:
    if len(sys.argv) != 3:
        print(__doc__)
        return 2

    source = Path(sys.argv[1])
    name = sys.argv[2]

    mask = load_mask(source)
    paths = to_paths(mask)
    if not paths:
        print("nothing traced")
        return 1

    root = Path(__file__).resolve().parent.parent
    out = root / "app/src/main/res/drawable" / f"ic_ph_{name}.xml"
    out.write_text(
        DRAWABLE.format(paths="".join(PATH.format(d=p) for p in paths)),
        encoding="utf-8",
    )
    print(f"{out} ({len(paths)} contours)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
