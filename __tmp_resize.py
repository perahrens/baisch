#!/usr/bin/env python3
"""Resize bg_darkmoon.jpg to stay within 4096px on each axis.
Target: 2048×2896 (preserving 4949:7000 aspect ratio)
"""
import sys, os

SRC = '/home/per-ahrens/source/repos/baisch/android/assets/data/graphics/bg_darkmoon.jpg'
DST = SRC  # overwrite in place

MAX_DIM = 2048  # safe for all tablet/mobile GPUs

try:
    from PIL import Image
    img = Image.open(SRC)
    w, h = img.size
    print(f'Original: {w}x{h}')
    scale = MAX_DIM / max(w, h)
    nw, nh = round(w * scale), round(h * scale)
    print(f'Resizing to: {nw}x{nh}')
    img = img.resize((nw, nh), Image.LANCZOS)
    img.save(DST, 'JPEG', quality=88, optimize=True)
    size_mb = os.path.getsize(DST) / 1024 / 1024
    print(f'Saved {DST} ({size_mb:.2f} MB)')
except ImportError:
    print('Pillow not available, trying imageio...')
    try:
        import imageio, numpy as np
        img = imageio.imread(SRC)
        h, w = img.shape[:2]
        print(f'Original: {w}x{h}')
        from skimage.transform import resize as sk_resize
        scale = MAX_DIM / max(w, h)
        nw, nh = round(w * scale), round(h * scale)
        resized = sk_resize(img, (nh, nw), anti_aliasing=True, preserve_range=True).astype(np.uint8)
        imageio.imwrite(DST, resized, quality=88)
        print(f'Saved {DST}')
    except ImportError:
        print('No image library found')
        sys.exit(1)
