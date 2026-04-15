#!/usr/bin/env python3
"""Convert black-on-white card PNGs so the black parts become red."""
from PIL import Image
import os

SKINS = "/home/per-ahrens/source/repos/baisch/data/skins"
ANDROID_SKINS = "/home/per-ahrens/source/repos/baisch/android/assets/data/skins"

def make_red(src_path, dst_path):
    img = Image.open(src_path).convert("RGBA")
    pixels = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = pixels[x, y]
            lum = int(0.299 * r + 0.587 * g + 0.114 * b)
            # black -> red, white -> white
            pixels[x, y] = (255, lum, lum, a)
    img.save(dst_path)
    print(f"  Saved {dst_path}")

for name in ("hearts", "diamonds"):
    src = os.path.join(SKINS, f"{name}.png")
    dst = os.path.join(SKINS, f"{name}_red.png")
    print(f"Processing {src} ...")
    make_red(src, dst)
    # also copy to android assets
    dst_android = os.path.join(ANDROID_SKINS, f"{name}_red.png")
    make_red(src, dst_android)

print("Done.")
