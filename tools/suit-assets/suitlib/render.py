"""
Offline preview renderer (needs numpy + Pillow: pip install numpy pillow).

Two independent paths are implemented on purpose:

* render_bbmodel: Blockbench semantics, straight from the .bbmodel (absolute "internal" coordinates,
  Java block-model face UV orientation);
* render_geo: GeckoLib semantics, from the exported .geo.json, porting GeckoLib's
  BakedModelFactory/GeoQuad vertex and UV construction and its bone matrix order.

Rendering the same pose with both paths and comparing the images validates the exporter. Poses are
dictionaries bone -> {"rotation": [x, y, z] (Bedrock degrees), "position": [x, y, z] (px)} using the
authoring rig names (root, head, body, right_arm, ..., cape, right_forearm, ...).
"""
import math

import numpy as np
from PIL import Image

from . import bbmodel, rig

PART_TO_ARMOR = {part: spec[0] for part, spec in rig.PLAYER_PARTS.items()}
ARMOR_TO_PART = {v: k for k, v in PART_TO_ARMOR.items()}
ARMOR_TO_PART['armorRightBoot'] = 'right_leg'
ARMOR_TO_PART['armorLeftBoot'] = 'left_leg'


# --------------------------------------------------------------------------------------------- math

def rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]], dtype=float)


def rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]], dtype=float)


def rot_z(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]], dtype=float)


def translate(x, y, z):
    m = np.identity(4)
    m[:3, 3] = (x, y, z)
    return m


def zyx(rx, ry, rz):
    """Rz * Ry * Rx (radians): GeckoLib / Minecraft ModelPart / three.js 'ZYX' order."""
    return rot_z(rz) @ rot_y(ry) @ rot_x(rx)


def pose_of(pose, bone):
    entry = (pose or {}).get(bone) or {}
    return entry.get('rotation', (0, 0, 0)), entry.get('position', (0, 0, 0))


# --------------------------------------------------------------------------------- Blockbench path

BB_FACE_CORNERS = {
    # face: corners (x, y, z selectors 0=min 1=max) for (u1,v1) (u2,v1) (u2,v2) (u1,v2)
    'north': [(1, 1, 0), (0, 1, 0), (0, 0, 0), (1, 0, 0)],
    'south': [(0, 1, 1), (1, 1, 1), (1, 0, 1), (0, 0, 1)],
    'east': [(1, 1, 1), (1, 1, 0), (1, 0, 0), (1, 0, 1)],
    'west': [(0, 1, 0), (0, 1, 1), (0, 0, 1), (0, 0, 0)],
    'up': [(0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)],
    'down': [(0, 0, 1), (1, 0, 1), (1, 0, 0), (0, 0, 0)],
}


def _rotate_uv_corners(uvs, rotation):
    steps = (int(rotation) // 90) % 4
    return uvs[-steps:] + uvs[:-steps] if steps else uvs


def bbmodel_quads(model, pose=None):
    """List of (corners[4] xyz in px, uvs[4] in texture px) for the posed model (Blockbench semantics)."""
    elements = bbmodel.elements_by_uuid(model)
    quads = []

    def walk(group, parent_matrix):
        origin = group.get('origin', [0, 0, 0])
        rotation = [math.radians(v) for v in group.get('rotation', [0, 0, 0])]
        anim_rot, anim_pos = pose_of(pose, group['name'])
        # Blockbench BoneAnimator: rotation.x -= x, rotation.y -= y, rotation.z += z; position x -= x
        rx = rotation[0] - math.radians(anim_rot[0])
        ry = rotation[1] - math.radians(anim_rot[1])
        rz = rotation[2] + math.radians(anim_rot[2])
        local = translate(-anim_pos[0], anim_pos[1], anim_pos[2]) @ translate(*origin) @ zyx(rx, ry, rz) @ translate(*[-v for v in origin])
        matrix = parent_matrix @ local
        for element in bbmodel.direct_elements(group, elements):
            if not element.get('visibility', True):
                continue
            lo = [min(a, b) - element.get('inflate', 0) for a, b in zip(element['from'], element['to'])]
            hi = [max(a, b) + element.get('inflate', 0) for a, b in zip(element['from'], element['to'])]
            eo = element.get('origin', [0, 0, 0])
            er = [math.radians(v) for v in element.get('rotation', [0, 0, 0])]
            em = matrix @ translate(*eo) @ zyx(*er) @ translate(*[-v for v in eo])
            for face, sel in BB_FACE_CORNERS.items():
                data = element.get('faces', {}).get(face)
                if not data or data.get('texture') is None:
                    continue
                corners = []
                for s in sel:
                    p = np.array([hi[0] if s[0] else lo[0], hi[1] if s[1] else lo[1], hi[2] if s[2] else lo[2], 1.0])
                    corners.append((em @ p)[:3])
                x1, y1, x2, y2 = data['uv']
                uvs = _rotate_uv_corners([(x1, y1), (x2, y1), (x2, y2), (x1, y2)], data.get('rotation', 0))
                quads.append((corners, uvs))
        for child in bbmodel.child_groups(group):
            walk(child, matrix)

    for top in model['outliner']:
        if isinstance(top, dict):
            walk(top, np.identity(4))
    return quads


# ------------------------------------------------------------------------------------ GeckoLib path

def _vertex_set(origin, size, inflate):
    ox, oy, oz = origin
    sx, sy, sz = size
    i = inflate
    return {
        'bottomLeftBack': (ox - i, oy - i, oz - i),
        'bottomRightBack': (ox - i, oy - i, oz + sz + i),
        'topLeftBack': (ox - i, oy + sy + i, oz - i),
        'topRightBack': (ox - i, oy + sy + i, oz + sz + i),
        'topLeftFront': (ox + sx + i, oy + sy + i, oz - i),
        'topRightFront': (ox + sx + i, oy + sy + i, oz + sz + i),
        'bottomLeftFront': (ox + sx + i, oy - i, oz - i),
        'bottomRightFront': (ox + sx + i, oy - i, oz + sz + i),
    }


GECKO_QUADS = {
    'west': ['topRightBack', 'topLeftBack', 'bottomLeftBack', 'bottomRightBack'],
    'east': ['topLeftFront', 'topRightFront', 'bottomRightFront', 'bottomLeftFront'],
    'north': ['topLeftBack', 'topLeftFront', 'bottomLeftFront', 'bottomLeftBack'],
    'south': ['topRightFront', 'topRightBack', 'bottomRightBack', 'bottomRightFront'],
    'up': ['topRightBack', 'topRightFront', 'topLeftFront', 'topLeftBack'],
    'down': ['bottomLeftBack', 'bottomLeftFront', 'bottomRightFront', 'bottomRightBack'],
}


def _gecko_vertices(face, mirror, box_uv):
    if face == 'west' and mirror:
        face = 'east'
    elif face == 'east' and mirror:
        face = 'west'
    elif face == 'up' and mirror and not box_uv:
        face = 'down'
    elif face == 'down' and mirror and not box_uv:
        face = 'up'
    return GECKO_QUADS[face]


def _gecko_uvs(u, v, us, vs, rotation, w, h, mirror):
    u_width = (u + us) / w
    v_height = (v + vs) / h
    u /= w
    v /= h
    if not mirror:
        u, u_width = u_width, u
    steps = (int(rotation) % 360) // 90
    if steps == 0:
        flat = [u, v, u_width, v, u_width, v_height, u, v_height]
    elif steps == 1:
        flat = [u_width, v, u_width, v_height, u, v_height, u, v]
    elif steps == 2:
        flat = [u_width, v_height, u, v_height, u, v, u_width, v]
    else:
        flat = [u, v_height, u, v, u_width, v, u_width, v_height]
    return [(flat[0] * w, flat[1] * h), (flat[2] * w, flat[3] * h), (flat[4] * w, flat[5] * h), (flat[6] * w, flat[7] * h)]


def _box_uv(face, uv, size):
    sx, sy, sz = [math.floor(s) for s in size]
    u, v = uv
    return {
        'west': ((u + sz + sx, v + sz), (sz, sy)),
        'east': ((u, v + sz), (sz, sy)),
        'north': ((u + sz, v + sz), (sx, sy)),
        'south': ((u + sz + sx + sz, v + sz), (sx, sy)),
        'up': ((u + sz, v), (sx, sz)),
        'down': ((u + sz + sx, v + sz), (sx, -sz)),
    }[face]


def geo_quads(geo, pose=None, in_game=True):
    """
    Quads (corners in px, internal frame; uvs in texture px) of a GeckoLib geometry. With in_game=True
    the armor bones are driven like GeoArmorRenderer does from vanilla model parts posed by the clip
    (the runtime writes the clip rotation into the vanilla part, GeckoLib copies it back).
    """
    model = geo['minecraft:geometry'][0]
    w = model['description']['texture_width']
    h = model['description']['texture_height']
    bones = model['bones']
    children = {}
    for bone in bones:
        children.setdefault(bone.get('parent'), []).append(bone)
    quads = []

    def bone_pose(name):
        if in_game and name in ARMOR_TO_PART:
            part = ARMOR_TO_PART[name]
            rot, pos = pose_of(pose, part)
            shift = rig.GECKO_LEG_SHIFT.get(part, 0.0)
            # GeoArmorRenderer: updatePosition(part.x +- offset, ...) with part.x = vanilla x + clip x
            return rot, (pos[0] + shift, pos[1], pos[2])
        return pose_of(pose, name)

    def walk(bone, parent_matrix):
        pivot = bone.get('pivot', [0, 0, 0])
        gp = (-pivot[0], pivot[1], pivot[2])
        rest = bone.get('rotation', [0, 0, 0])
        anim_rot, anim_pos = bone_pose(bone['name'])
        rx = math.radians(-rest[0]) + math.radians(-anim_rot[0])
        ry = math.radians(-rest[1]) + math.radians(-anim_rot[1])
        rz = math.radians(rest[2]) + math.radians(anim_rot[2])
        # RenderUtils.prepMatrixForBone: translate(-posX, posY, posZ), pivot, rotate Z Y X, -pivot
        matrix = parent_matrix @ translate(-anim_pos[0], anim_pos[1], anim_pos[2]) @ translate(*gp) @ zyx(rx, ry, rz) @ translate(*[-v for v in gp])
        for cube in bone.get('cubes', []):
            size = cube['size']
            o = cube['origin']
            origin = (-(o[0] + size[0]), o[1], o[2])
            inflate = cube.get('inflate', 0.0)
            mirror = bool(cube.get('mirror', False))
            cp = cube.get('pivot', [0, 0, 0])
            cr = cube.get('rotation', [0, 0, 0])
            gcp = (-cp[0], cp[1], cp[2])
            cm = matrix @ translate(*gcp) @ zyx(math.radians(-cr[0]), math.radians(-cr[1]), math.radians(cr[2])) @ translate(*[-v for v in gcp])
            verts = _vertex_set(origin, size, inflate)
            box = not isinstance(cube.get('uv'), dict)
            for face in ('west', 'east', 'north', 'south', 'up', 'down'):
                if box:
                    (u, v), (us, vs) = _box_uv(face, cube['uv'], size)
                    rotation = 0
                else:
                    entry = cube['uv'].get(face)
                    if entry is None:
                        continue
                    u, v = entry['uv']
                    us, vs = entry['uv_size']
                    rotation = entry.get('uv_rotation', 0)
                names = _gecko_vertices(face, mirror, box)
                corners = [(cm @ np.array([*verts[n], 1.0]))[:3] for n in names]
                quads.append((corners, _gecko_uvs(u, v, us, vs, rotation, w, h, mirror)))
        for child in children.get(bone['name'], []):
            walk(child, matrix)

    for top in children.get(None, []):
        walk(top, np.identity(4))
    return quads


# ------------------------------------------------------------------------------------- rasterizer

LIGHT = np.array([-0.35, 0.85, -0.55])
LIGHT = LIGHT / np.linalg.norm(LIGHT)


def rasterize(quads, texture, yaw=0.0, pitch=0.0, scale=6, size=(260, 300), center=(0.0, 16.0), background=(48, 52, 60, 255)):
    """
    Orthographic render. yaw 0 = looking at the character's face (camera at -Z). Returns a PIL image.
    """
    tex = np.asarray(texture.convert('RGBA'), dtype=np.uint8)
    th, tw = tex.shape[:2]
    width, height = size
    color = np.zeros((height, width, 4), dtype=np.uint8)
    color[:] = background
    depth = np.full((height, width), np.inf)
    view = rot_x(math.radians(pitch)) @ rot_y(math.radians(yaw))
    for corners, uvs in quads:
        pts = np.array([(view @ np.array([*c, 1.0]))[:3] for c in corners])
        normal = np.cross(pts[1] - pts[0], pts[3] - pts[0])
        norm = np.linalg.norm(normal)
        if norm < 1e-9:
            normal = np.cross(pts[2] - pts[1], pts[0] - pts[1])
            norm = np.linalg.norm(normal)
        shade = 1.0 if norm < 1e-9 else 0.55 + 0.45 * abs(float(np.dot(normal / norm, LIGHT)))
        sx = width / 2.0 - (pts[:, 0] - center[0]) * scale
        sy = height / 2.0 - (pts[:, 1] - center[1]) * scale
        sz = pts[:, 2]
        uv = np.array(uvs, dtype=float)
        for tri in ((0, 1, 2), (0, 2, 3)):
            _triangle(color, depth, sx[list(tri)], sy[list(tri)], sz[list(tri)], uv[list(tri)], tex, tw, th, shade)
    return Image.fromarray(color, 'RGBA')


def _triangle(color, depth, xs, ys, zs, uv, tex, tw, th, shade):
    height, width = depth.shape
    x0, x1 = max(int(math.floor(xs.min())), 0), min(int(math.ceil(xs.max())), width - 1)
    y0, y1 = max(int(math.floor(ys.min())), 0), min(int(math.ceil(ys.max())), height - 1)
    if x0 > x1 or y0 > y1:
        return
    area = (xs[1] - xs[0]) * (ys[2] - ys[0]) - (xs[2] - xs[0]) * (ys[1] - ys[0])
    if abs(area) < 1e-9:
        return
    gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
    w0 = ((xs[1] - gx) * (ys[2] - gy) - (xs[2] - gx) * (ys[1] - gy)) / area
    w1 = ((xs[2] - gx) * (ys[0] - gy) - (xs[0] - gx) * (ys[2] - gy)) / area
    w2 = 1.0 - w0 - w1
    inside = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
    if not inside.any():
        return
    z = w0 * zs[0] + w1 * zs[1] + w2 * zs[2]
    u = w0 * uv[0, 0] + w1 * uv[1, 0] + w2 * uv[2, 0]
    v = w0 * uv[0, 1] + w1 * uv[1, 1] + w2 * uv[2, 1]
    ui = np.clip(np.floor(u).astype(int), 0, tw - 1)
    vi = np.clip(np.floor(v).astype(int), 0, th - 1)
    texel = tex[vi, ui]
    region = depth[y0:y1 + 1, x0:x1 + 1]
    mask = inside & (texel[..., 3] > 25) & (z < region - 1e-4)
    if not mask.any():
        return
    region[mask] = z[mask]
    rgb = (texel[..., :3].astype(float) * shade).clip(0, 255).astype(np.uint8)
    target = color[y0:y1 + 1, x0:x1 + 1]
    target[mask, :3] = rgb[mask]
    target[mask, 3] = 255


def sheet(images, columns, labels=None, pad=4, background=(30, 32, 38, 255)):
    """Grid of images (same size) with optional labels drawn by PIL's default font."""
    from PIL import ImageDraw
    w, h = images[0].size
    rows = (len(images) + columns - 1) // columns
    label_h = 14 if labels else 0
    out = Image.new('RGBA', (columns * (w + pad) + pad, rows * (h + pad + label_h) + pad), background)
    draw = ImageDraw.Draw(out)
    for i, img in enumerate(images):
        x = pad + (i % columns) * (w + pad)
        y = pad + (i // columns) * (h + pad + label_h)
        out.paste(img, (x, y + label_h))
        if labels:
            draw.text((x + 2, y), labels[i], fill=(230, 230, 230, 255))
    return out
