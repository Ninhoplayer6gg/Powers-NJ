"""
The Powers NJ suit rig and its export to a GeckoLib armor geometry.

Authoring rig (the .bbmodel the artist edits and animates):

    root                (0, 0, 0)      whole body: drives the player render pose (jumps, leaning...)
    |- head             (0, 24, 0)     = vanilla head        -> helmet
    |- body             (0, 24, 0)     = vanilla body        -> chestplate (cape, tabards... as children)
    |- right_arm        (5, 22, 0)     = vanilla right arm   -> chestplate (right_forearm as child)
    |- left_arm         (-5, 22, 0)    = vanilla left arm    -> chestplate (left_forearm as child)
    |- right_leg        (1.9, 12, 0)   = vanilla right leg   -> leggings  (right_shin subtree -> boots)
    '- left_leg         (-1.9, 12, 0)  = vanilla left leg    -> leggings  (left_shin subtree -> boots)

Pivots are Blockbench ("internal") coordinates: the character's right side is +X, it faces -Z.
head/body/arms/legs are siblings exactly like Minecraft's HumanoidModel parts, so an animation that
looks right in Blockbench looks the same in game. Every other group is a "suit bone" (cape,
forearms, shins...) animated locally on top of the vanilla pose.

Export (GeckoLib armor geometry, Bedrock format):

    root -> armorHead, armorBody, armorRightArm, armorLeftArm,
            armorRightLeg, armorRightBoot, armorLeftLeg, armorLeftBoot

GeoArmorRenderer copies the vanilla part rotations onto the armor bones and offsets the leg bones by
0.1 px (it assumes leg pivots at +-2), which the export compensates. A thin shell is added around each
part (default 0.25 px per side) so a partially worn suit never z-fights with the player skin.
"""

PLAYER_PARTS = {
    # name: (geo bone, pivot, reference box min, reference box max, slot)
    'head': ('armorHead', (0.0, 24.0, 0.0), (-4.0, 24.0, -4.0), (4.0, 32.0, 4.0), 'head'),
    'body': ('armorBody', (0.0, 24.0, 0.0), (-4.0, 12.0, -2.0), (4.0, 24.0, 2.0), 'chest'),
    'right_arm': ('armorRightArm', (5.0, 22.0, 0.0), (4.0, 12.0, -2.0), (8.0, 24.0, 2.0), 'chest'),
    'left_arm': ('armorLeftArm', (-5.0, 22.0, 0.0), (-8.0, 12.0, -2.0), (-4.0, 24.0, 2.0), 'chest'),
    'right_leg': ('armorRightLeg', (1.9, 12.0, 0.0), (-0.1, 0.0, -2.0), (3.9, 12.0, 2.0), 'legs'),
    'left_leg': ('armorLeftLeg', (-1.9, 12.0, 0.0), (-3.9, 0.0, -2.0), (0.1, 12.0, 2.0), 'legs'),
}
ROOT = 'root'
BOOTS = {'right_leg': ('right_shin', 'armorRightBoot'), 'left_leg': ('left_shin', 'armorLeftBoot')}
# GeoArmorRenderer#applyBaseTransformations moves the leg bones by (leg.x +- 2) px, i.e. 0.1 px for
# the vanilla pivots (+-1.9). Exported leg geometry is shifted by the opposite amount (internal X).
GECKO_LEG_SHIFT = {'right_leg': 0.1, 'left_leg': -0.1}
ARMOR_BONES = ['armorHead', 'armorBody', 'armorRightArm', 'armorLeftArm', 'armorRightLeg', 'armorRightBoot',
               'armorLeftLeg', 'armorLeftBoot']
FACES = ('north', 'east', 'south', 'west', 'up', 'down')
EPS = 1e-3


class RigError(ValueError):
    pass


def _close(a, b):
    return all(abs(x - y) <= EPS for x, y in zip(a, b))


def validate(model):
    """Checks the authoring rig conventions. Returns the list of warnings, raises RigError on errors."""
    from . import bbmodel
    errors = []
    warnings = []
    roots = [n for n in model['outliner'] if isinstance(n, dict)]
    loose = [n for n in model['outliner'] if isinstance(n, str)]
    if loose:
        errors.append('%d element(s) outside any group' % len(loose))
    if len(roots) != 1 or roots[0]['name'] != ROOT:
        errors.append('The outliner must have exactly one top-level group named "root"')
        raise RigError('\n'.join(errors))
    root = roots[0]
    if not _close(root.get('origin', [0, 0, 0]), (0, 0, 0)) or any(abs(r) > EPS for r in root.get('rotation', [0, 0, 0])):
        errors.append('"root" must have pivot (0, 0, 0) and no rotation')
    elements = bbmodel.elements_by_uuid(model)
    if bbmodel.direct_elements(root, elements):
        errors.append('"root" must not contain cubes directly')
    names = set()
    for group, _ in bbmodel.iter_groups(model['outliner']):
        if group['name'] in names:
            errors.append('Duplicated bone name "%s"' % group['name'])
        names.add(group['name'])
        if group['name'] in ARMOR_BONES:
            errors.append('"%s" is reserved for the exported GeckoLib armor bones' % group['name'])
    children = {g['name']: g for g in bbmodel.child_groups(root)}
    for name in children:
        if name not in PLAYER_PARTS:
            errors.append('"root" may only contain %s, found "%s"' % (', '.join(PLAYER_PARTS), name))
    for name, (_, pivot, _, _, _) in PLAYER_PARTS.items():
        group = children.get(name)
        if group is None:
            errors.append('Missing part "%s"' % name)
            continue
        if not _close(group['origin'], pivot):
            errors.append('"%s" pivot must be %s (Minecraft model part), found %s' % (name, list(pivot), group['origin']))
        if any(abs(r) > EPS for r in group.get('rotation', [0, 0, 0])):
            errors.append('"%s" must not be rotated at rest' % name)
    for name, (shin, _) in BOOTS.items():
        if name in children and shin not in [g['name'] for g in bbmodel.child_groups(children[name])]:
            warnings.append('"%s" has no "%s" child: the boots will be empty' % (name, shin))
    for element in model['elements']:
        if element.get('type', 'cube') != 'cube':
            errors.append('Element "%s" is a %s: only cubes are supported by GeckoLib' % (element['name'], element.get('type')))
    if errors:
        raise RigError('\n'.join(errors))
    return warnings


class ShellTransform:
    """Affine part transform: optional shell scale around the reference box plus the GeckoLib leg shift."""

    def __init__(self, part, shell):
        _, _, lo, hi, _ = PLAYER_PARTS[part]
        self.center = [(a + b) / 2.0 for a, b in zip(lo, hi)]
        self.scale = [((b - a) + 2.0 * shell) / (b - a) for a, b in zip(lo, hi)]
        self.shift = (GECKO_LEG_SHIFT.get(part, 0.0), 0.0, 0.0)

    def point(self, p):
        return [self.center[i] + (p[i] - self.center[i]) * self.scale[i] + self.shift[i] for i in range(3)]

    def box(self, lo, hi):
        a, b = self.point(lo), self.point(hi)
        return [min(a[i], b[i]) for i in range(3)], [max(a[i], b[i]) for i in range(3)]


def _num(v):
    v = round(float(v), 4)
    return 0.0 if v == 0 else (int(v) if v == int(v) and abs(v) < 1e9 else v)


def _vec(values):
    return [_num(v) for v in values]


def _cube(element, transform):
    lo = [min(a, b) for a, b in zip(element['from'], element['to'])]
    hi = [max(a, b) for a, b in zip(element['from'], element['to'])]
    lo, hi = transform.box(lo, hi)
    size = [hi[i] - lo[i] for i in range(3)]
    cube = {'origin': _vec([-hi[0], lo[1], lo[2]]), 'size': _vec(size)}
    if element.get('inflate'):
        cube['inflate'] = _num(element['inflate'])
    rotation = element.get('rotation', [0, 0, 0])
    if any(abs(r) > EPS for r in rotation):
        pivot = transform.point(element.get('origin', [0, 0, 0]))
        cube['pivot'] = _vec([-pivot[0], pivot[1], pivot[2]])
        cube['rotation'] = _vec([-rotation[0], -rotation[1], rotation[2]])
    if element.get('box_uv'):
        cube['uv'] = _vec(element.get('uv_offset', [0, 0]))
        if element.get('mirror_uv'):
            cube['mirror'] = True
        return cube
    uv = {}
    for face in FACES:
        data = element.get('faces', {}).get(face)
        if not data or data.get('texture') is None:
            continue
        x1, y1, x2, y2 = data['uv']
        if face in ('up', 'down'):
            # Blockbench stores up/down rotated by 180 degrees relative to Bedrock (and GeckoLib).
            entry = {'uv': _vec([x2, y2]), 'uv_size': _vec([x1 - x2, y1 - y2])}
        else:
            entry = {'uv': _vec([x1, y1]), 'uv_size': _vec([x2 - x1, y2 - y1])}
        if data.get('rotation'):
            entry['uv_rotation'] = int(data['rotation'])
        uv[face] = entry
    cube['uv'] = uv
    return cube


def _bone(name, parent, pivot, rotation=(0, 0, 0)):
    bone = {'name': name}
    if parent:
        bone['parent'] = parent
    bone['pivot'] = _vec([-pivot[0], pivot[1], pivot[2]])
    if any(abs(r) > EPS for r in rotation):
        bone['rotation'] = _vec([-rotation[0], -rotation[1], rotation[2]])
    return bone


def export_geometry(model, identifier, shell=0.25):
    """Builds the GeckoLib armor .geo.json (dict) from a validated authoring rig."""
    from . import bbmodel
    validate(model)
    elements = bbmodel.elements_by_uuid(model)
    root = [n for n in model['outliner'] if isinstance(n, dict)][0]
    parts = {g['name']: g for g in bbmodel.child_groups(root)}
    bones = [_bone(ROOT, None, (0, 0, 0))]

    def emit(group, parent_name, transform):
        if not group.get('export', True):
            return
        bone = _bone(group['name'], parent_name, transform.point(group['origin']), group.get('rotation', (0, 0, 0)))
        cubes = [_cube(e, transform) for e in bbmodel.direct_elements(group, elements) if e.get('export', True)]
        if cubes:
            bone['cubes'] = cubes
        bones.append(bone)
        for child in bbmodel.child_groups(group):
            emit(child, group['name'], transform)

    for name, (armor_bone, pivot, _, _, _) in PLAYER_PARTS.items():
        group = parts[name]
        transform = ShellTransform(name, shell)
        shift = GECKO_LEG_SHIFT.get(name, 0.0)
        armor_pivot = (pivot[0] + shift, pivot[1], pivot[2])
        bone = _bone(armor_bone, ROOT, armor_pivot)
        cubes = [_cube(e, transform) for e in bbmodel.direct_elements(group, elements) if e.get('export', True)]
        if cubes:
            bone['cubes'] = cubes
        bones.append(bone)
        boot_child, boot_bone = BOOTS.get(name, (None, None))
        for child in bbmodel.child_groups(group):
            if child['name'] != boot_child:
                emit(child, armor_bone, transform)
        if boot_bone:
            bones.append(_bone(boot_bone, ROOT, armor_pivot))
            for child in bbmodel.child_groups(group):
                if child['name'] == boot_child:
                    emit(child, boot_bone, transform)

    resolution = model.get('resolution', {'width': 64, 'height': 64})
    return {
        'format_version': '1.12.0',
        'minecraft:geometry': [{
            'description': {
                'identifier': identifier,
                'texture_width': resolution['width'],
                'texture_height': resolution['height'],
                'visible_bounds_width': 3,
                'visible_bounds_height': 3.5,
                'visible_bounds_offset': [0, 1.25, 0],
            },
            'bones': bones,
        }],
    }


def suit_bones(model):
    """Names of the locally animated suit bones (everything below the player parts)."""
    from . import bbmodel
    names = []
    for group, parent in bbmodel.iter_groups(model['outliner']):
        if group['name'] != ROOT and group['name'] not in PLAYER_PARTS:
            names.append(group['name'])
    return names
