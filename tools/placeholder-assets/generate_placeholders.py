#!/usr/bin/env python3
"""
Generates the PLACEHOLDER textures of Powers NJ (pure Python, no dependencies).

Every file listed here is part of the asset contract in ASSET_REQUIREMENTS.md. Existing files are
never overwritten (final art wins) unless --force is passed:

    python3 tools/placeholder-assets/generate_placeholders.py [--force]
"""
import json
import os
import random
import struct
import sys
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'powersnj')
FORCE = '--force' in sys.argv
written = []


def png(path, width, height, pixels):
    """pixels: list of rows, each a list of (r, g, b, a)."""
    full = os.path.join(ASSETS, path)
    if os.path.exists(full) and not FORCE:
        return
    os.makedirs(os.path.dirname(full), exist_ok=True)
    raw = b''.join(b'\x00' + b''.join(struct.pack('BBBB', *p) for p in row) for row in pixels)

    def chunk(kind, data):
        c = struct.pack('>I', len(data)) + kind + data
        return c + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
    data += chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
    with open(full, 'wb') as f:
        f.write(data)
    written.append(path)


def canvas(w, h, color=(0, 0, 0, 0)):
    return [[color for _ in range(w)] for _ in range(h)]


def noise_fill(img, base, spread, seed):
    rnd = random.Random(seed)
    for y in range(len(img)):
        for x in range(len(img[0])):
            d = rnd.randint(-spread, spread)
            img[y][x] = tuple(max(0, min(255, c + d)) for c in base[:3]) + (base[3] if len(base) > 3 else 255,)
    return img


def rect(img, x0, y0, x1, y1, color):
    for y in range(max(0, y0), min(len(img), y1)):
        for x in range(max(0, x0), min(len(img[0]), x1)):
            img[y][x] = color


def border(img, color, inset=0):
    h, w = len(img), len(img[0])
    for x in range(inset, w - inset):
        img[inset][x] = color
        img[h - 1 - inset][x] = color
    for y in range(inset, h - inset):
        img[y][inset] = color
        img[y][w - 1 - inset] = color


def speckles(img, color, count, seed, size=2):
    rnd = random.Random(seed)
    h, w = len(img), len(img[0])
    for _ in range(count):
        x, y = rnd.randint(1, w - size - 1), rnd.randint(1, h - size - 1)
        rect(img, x, y, x + size, y + size, color)
    return img


def stable(value):
    return zlib.crc32(value.encode('utf-8')) & 0xff


# 3x5 pixel font for letters on icons
FONT = {
    'A': ['010', '101', '111', '101', '101'], 'B': ['110', '101', '110', '101', '110'], 'C': ['011', '100', '100', '100', '011'],
    'D': ['110', '101', '101', '101', '110'], 'E': ['111', '100', '110', '100', '111'], 'F': ['111', '100', '110', '100', '100'],
    'G': ['011', '100', '101', '101', '011'], 'H': ['101', '101', '111', '101', '101'], 'I': ['111', '010', '010', '010', '111'],
    'J': ['001', '001', '001', '101', '010'], 'K': ['101', '110', '100', '110', '101'], 'L': ['100', '100', '100', '100', '111'],
    'M': ['101', '111', '111', '101', '101'], 'N': ['110', '101', '101', '101', '101'], 'O': ['010', '101', '101', '101', '010'],
    'P': ['110', '101', '110', '100', '100'], 'Q': ['010', '101', '101', '110', '011'], 'R': ['110', '101', '110', '101', '101'],
    'S': ['011', '100', '010', '001', '110'], 'T': ['111', '010', '010', '010', '010'], 'U': ['101', '101', '101', '101', '111'],
    'V': ['101', '101', '101', '101', '010'], 'W': ['101', '101', '111', '111', '101'], 'X': ['101', '101', '010', '101', '101'],
    'Y': ['101', '101', '010', '010', '010'], 'Z': ['111', '001', '010', '100', '111'],
}


def text(img, s, x, y, color):
    for i, ch in enumerate(s.upper()):
        glyph = FONT.get(ch)
        if not glyph:
            continue
        for gy, row in enumerate(glyph):
            for gx, bit in enumerate(row):
                if bit == '1':
                    px, py = x + i * 4 + gx, y + gy
                    if 0 <= py < len(img) and 0 <= px < len(img[0]):
                        img[py][px] = color


def initials(name):
    parts = name.split('_')
    return (parts[0][0] + (parts[1][0] if len(parts) > 1 else parts[0][1])).upper()


PALETTE = {
    'thragg': ((205, 205, 210, 255), (170, 30, 30, 255), (30, 30, 35, 255)),
    'venom': ((22, 22, 28, 255), (235, 235, 240, 255), (60, 60, 90, 255)),
    'reverse_flash': ((215, 175, 30, 255), (180, 25, 25, 255), (40, 20, 10, 255)),
}

# ---------------- blocks ----------------
stone = (125, 125, 125, 255)
deep = (70, 70, 78, 255)
png('textures/block/viltrumite_ore.png', 16, 16, speckles(noise_fill(canvas(16, 16), stone, 12, 1), (200, 200, 215, 255), 6, 2))
png('textures/block/deepslate_viltrumite_ore.png', 16, 16, speckles(noise_fill(canvas(16, 16), deep, 10, 3), (200, 200, 215, 255), 6, 4))
png('textures/block/speed_crystal_ore.png', 16, 16, speckles(noise_fill(canvas(16, 16), stone, 12, 5), (240, 200, 40, 255), 7, 6))
png('textures/block/deepslate_speed_crystal_ore.png', 16, 16, speckles(noise_fill(canvas(16, 16), deep, 10, 7), (240, 200, 40, 255), 7, 8))
img = noise_fill(canvas(16, 16), (175, 175, 190, 255), 8, 9)
border(img, (120, 120, 135, 255))
png('textures/block/viltrumite_block.png', 16, 16, img)
for face, accent in [('top', (90, 90, 100, 255)), ('side', (70, 70, 80, 255)), ('front', (70, 70, 80, 255)), ('front_on', (70, 70, 80, 255))]:
    img = noise_fill(canvas(16, 16), (60, 60, 68, 255), 6, stable(face))
    border(img, (35, 35, 40, 255))
    if face == 'top':
        rect(img, 3, 3, 13, 13, accent)
        rect(img, 6, 6, 10, 10, (224, 176, 64, 255))
    elif face.startswith('front'):
        rect(img, 3, 4, 13, 12, (20, 20, 24, 255))
        glow = (255, 190, 60, 255) if face == 'front_on' else (90, 70, 30, 255)
        rect(img, 4, 9, 12, 11, glow)
        rect(img, 5, 5, 11, 8, (40, 120, 200, 255) if face == 'front_on' else (30, 50, 70, 255))
    else:
        rect(img, 2, 7, 14, 9, (224, 176, 64, 255))
    png('textures/block/suit_forge_%s.png' % face, 16, 16, img)
img = noise_fill(canvas(16, 16), (150, 150, 158, 255), 6, 11)
border(img, (90, 90, 98, 255))
png('textures/block/suit_stand.png', 16, 16, img)

# ---------------- items ----------------
ITEMS = {
    'power_core': (60, 160, 230), 'advanced_circuit': (40, 140, 60), 'reinforced_fabric': (150, 130, 100), 'energy_conductor': (210, 120, 50),
    'raw_viltrumite': (170, 170, 185), 'viltrumite_ingot': (200, 200, 215), 'viltrumite_alloy': (225, 225, 240), 'reinforced_viltrumite_fabric': (180, 50, 50),
    'organic_sample': (120, 160, 90), 'biomass': (50, 50, 70), 'symbiotic_fiber': (30, 30, 40), 'organic_compound': (100, 190, 140),
    'speed_crystal': (245, 205, 50), 'negative_energy_fragment': (200, 30, 30), 'conductive_fabric': (220, 170, 40), 'advanced_conductor': (250, 120, 30),
}
for name, (r, g, b) in ITEMS.items():
    img = canvas(16, 16)
    rect(img, 3, 3, 13, 13, (r, g, b, 255))
    rect(img, 4, 4, 12, 12, (min(255, r + 30), min(255, g + 30), min(255, b + 30), 255))
    border(img, (20, 20, 20, 255), 2)
    text(img, initials(name), 5, 6, (15, 15, 15, 255))
    png('textures/item/%s.png' % name, 16, 16, img)
for name, suit in [('viltrumite_blueprint', 'thragg'), ('symbiote_blueprint', 'venom'), ('speedster_blueprint', 'reverse_flash')]:
    img = noise_fill(canvas(16, 16), (40, 80, 170, 255), 8, stable(name))
    border(img, (220, 230, 255, 255), 1)
    rect(img, 3, 3, 13, 4, (220, 230, 255, 255))
    rect(img, 3, 12, 13, 13, (220, 230, 255, 255))
    text(img, initials(suit + '_x')[0] + 'B', 5, 6, PALETTE[suit][1])
    png('textures/item/%s.png' % name, 16, 16, img)
PIECE_SHAPES = {
    'helmet': [(3, 3, 13, 9), (3, 9, 5, 12), (11, 9, 13, 12)],
    'chestplate': [(2, 2, 14, 6), (4, 6, 12, 14)],
    'leggings': [(4, 2, 12, 6), (4, 6, 7, 14), (9, 6, 12, 14)],
    'boots': [(3, 8, 7, 13), (9, 8, 13, 13)],
}
for suit, (main, accent, dark) in PALETTE.items():
    for piece, shapes in PIECE_SHAPES.items():
        img = canvas(16, 16)
        for (x0, y0, x1, y1) in shapes:
            rect(img, x0, y0, x1, y1, main)
        rect(img, 7, 3, 9, 6, accent)
        png('textures/item/%s_%s.png' % (suit, piece), 16, 16, img)

# ---------------- suit armor layers (vanilla 64x32 layout) ----------------
for suit, (main, accent, dark) in PALETTE.items():
    layer1 = noise_fill(canvas(64, 32), main, 6, stable(suit))
    rect(layer1, 8, 8, 16, 16, dark)             # face
    rect(layer1, 9, 11, 15, 13, accent)           # eyes / visor
    rect(layer1, 20, 20, 28, 32, accent)          # chest front emblem area
    rect(layer1, 22, 22, 26, 26, dark)
    rect(layer1, 44, 20, 48, 32, dark)            # arm stripe
    layer1[0][0] = (0, 0, 0, 0)
    png('textures/suits/%s/%s_layer_1.png' % (suit, suit), 64, 32, layer1)
    layer2 = noise_fill(canvas(64, 32), main, 6, stable(suit + '2'))
    rect(layer2, 0, 20, 16, 32, dark)
    rect(layer2, 20, 16, 36, 20, accent)          # belt
    png('textures/suits/%s/%s_layer_2.png' % (suit, suit), 64, 32, layer2)

# ---------------- ability / skill icons (16x16) ----------------
KEYS_FILE = os.path.join(os.path.dirname(__file__), 'icons.json')
icons = json.load(open(KEYS_FILE))
for suit, keys in icons.items():
    main, accent, dark = PALETTE[suit]
    for key in keys:
        img = canvas(16, 16)
        rect(img, 1, 1, 15, 15, dark)
        border(img, accent, 1)
        text(img, initials(key), 5, 6, main if suit != 'venom' else accent)
        png('textures/abilities/%s/%s.png' % (suit, key), 16, 16, img)

# ---------------- particles (8x8, 4 frames) ----------------
PARTICLES = {'speed_spark': (255, 230, 120), 'negative_lightning': (230, 30, 30), 'symbiote_goo': (25, 25, 35),
             'shockwave_dust': (160, 140, 110), 'viltrumite_impact': (255, 255, 255)}
for name, (r, g, b) in PARTICLES.items():
    for frame in range(4):
        img = canvas(8, 8)
        size = 4 - frame
        o = (8 - size * 2) // 2
        rect(img, o, o, 8 - o, 8 - o, (r, g, b, 255 - frame * 50))
        if name == 'negative_lightning':
            img = canvas(8, 8)
            for i in range(8):
                x = (i * 3 + frame * 2) % 6 + 1
                img[i][x] = (r, g, b, 255)
                img[i][min(7, x + 1)] = (255, 120, 120, 200)
        png('textures/particle/%s_%d.png' % (name, frame), 8, 8, img)

# ---------------- entity ----------------
img = noise_fill(canvas(16, 64), (25, 25, 32, 255), 8, 21)
for y in range(0, 64, 8):
    rect(img, 6, y, 10, y + 2, (70, 70, 110, 255))
png('textures/entity/tendril.png', 16, 64, img)

# ---------------- GUI: Suit Forge (256x256 canvas, 176x222 used) ----------------
W, H = 256, 256
img = canvas(W, H)
rect(img, 0, 0, 176, 222, (198, 198, 198, 255))
border_col = (85, 85, 85, 255)
for x in range(176):
    img[0][x] = (255, 255, 255, 255)
    img[221][x] = border_col
for y in range(222):
    img[y][0] = (255, 255, 255, 255)
    img[y][175] = border_col


def slot(x, y):
    rect(img, x - 1, y - 1, x + 17, y + 17, (55, 55, 55, 255))
    rect(img, x, y, x + 17, y + 17, (255, 255, 255, 255))
    rect(img, x, y, x + 16, y + 16, (139, 139, 139, 255))


slot(8, 20)
slot(8, 56)
for i in range(6):
    slot(30 + (i % 3) * 18, 20 + (i // 3) * 18)
for i in range(4):
    slot(116 + (i % 2) * 18, 20 + (i // 2) * 18)
for row in range(3):
    for col in range(9):
        slot(8 + col * 18, 140 + row * 18)
for col in range(9):
    slot(8 + col * 18, 198)
rect(img, 88, 33, 112, 39, (60, 60, 60, 255))                 # progress bar track
rect(img, 6, 74, 170, 127, (150, 150, 150, 255))              # info panel
rect(img, 7, 75, 169, 126, (175, 175, 180, 255))
text(img, 'BP', 10, 40, (80, 80, 80, 255))
text(img, 'PC', 10, 76 - 2, (80, 80, 80, 255))
png('textures/gui/suit_forge.png', W, H, img)

print('Generated %d placeholder files' % len(written))
for p in written:
    print('  ' + p)
