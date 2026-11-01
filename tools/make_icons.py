from PIL import Image
import numpy as np, os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
src = Image.open(os.path.join(ROOT, 'docs/brand-source/logo-source.jpg')).convert('RGB')
a = np.asarray(src).astype(int)
mask = (a[:, :, 1] < 140) & (a[:, :, 0] < 120)
ys, xs = np.where(mask)
y0, y1, x0, x1 = ys.min(), ys.max(), xs.min(), xs.max()
print('bbox', x0, y0, x1, y1, 'size', src.size)
mark = src.crop((x0, y0, x1 + 1, y1 + 1))

LIME = (0xDB, 0xE6, 0xA1, 255)


def make(size, pad_ratio):
    canvas = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    inner = int(size * pad_ratio)
    m = mark.copy().convert('RGBA')
    m.thumbnail((inner, inner), Image.LANCZOS)
    canvas.paste(m, ((size - m.width) // 2, (size - m.height) // 2), m)
    return canvas


base = os.path.join(ROOT, 'app/src/main/res')
dens = {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}
for d, s in dens.items():
    p = f'{base}/mipmap-{d}'
    os.makedirs(p, exist_ok=True)
    legacy = Image.new('RGBA', (s, s), LIME)
    legacy.alpha_composite(make(s, 0.66))
    legacy.convert('RGB').save(f'{p}/ic_launcher.png')
    legacy.convert('RGB').save(f'{p}/ic_launcher_round.png')
    make(s, 0.60).save(f'{p}/ic_launcher_foreground.png')

os.makedirs(f'{base}/drawable-nodpi', exist_ok=True)
make(512, 0.92).save(f'{base}/drawable-nodpi/ic_sproutly_mark.png')

# 1080x1080 brand splash art for docs / store listing
splash = Image.new('RGBA', (1080, 1080), LIME)
splash.alpha_composite(make(1080, 0.55))
splash.convert('RGB').save(os.path.join(ROOT, 'docs/brand-logo-1080.png'))
print('icons written')
