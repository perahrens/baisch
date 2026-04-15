from PIL import Image
import numpy as np
import shutil
import os

def make_red(src, dst):
    img = Image.open(src).convert('RGBA')
    data = np.array(img, dtype=np.float32)
    lum = 0.299*data[:,:,0] + 0.587*data[:,:,1] + 0.114*data[:,:,2]
    out = np.zeros((data.shape[0], data.shape[1], 4), dtype=np.uint8)
    dark = lum < 180
    out[dark] = [220, 20, 20, 255]
    out[~dark] = [0, 0, 0, 0]
    Image.fromarray(out, 'RGBA').save(dst)
    print('Written:', dst)

BASE = '/home/per-ahrens/source/repos/baisch'

make_red(f'{BASE}/data/skins/hearts.png',   f'{BASE}/data/skins/hearts_red.png')
make_red(f'{BASE}/data/skins/diamonds.png', f'{BASE}/data/skins/diamonds_red.png')

# Copy to android assets
for f in ['hearts_red.png', 'diamonds_red.png']:
    src = f'{BASE}/data/skins/{f}'
    for dst_dir in [
        f'{BASE}/android/assets/data/skins',
        f'{BASE}/html/war/assets/data/skins',
    ]:
        os.makedirs(dst_dir, exist_ok=True)
        shutil.copy2(src, f'{dst_dir}/{f}')
        print('Copied to:', f'{dst_dir}/{f}')

print('All done')
