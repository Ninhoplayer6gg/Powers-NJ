"""
Reading/writing Blockbench project files (.bbmodel, format 4.x) and walking their outliner.

Blockbench stores every element in absolute model coordinates ("internal" space): Y up, the model
faces north (-Z) and the character's right side is +X. Group (bone) origins are absolute too.
Bedrock/GeckoLib files negate X (see rig.py), Minecraft model parts additionally flip Y.
"""
import base64
import copy
import json
import uuid as uuidlib

NAMESPACE = uuidlib.UUID('6f1c1d3e-2b7a-4c55-9a52-504f57455253')


def stable_uuid(*parts):
    """Deterministic UUID so regenerated files diff cleanly."""
    return str(uuidlib.uuid5(NAMESPACE, '/'.join(str(p) for p in parts)))


def load(path):
    with open(path, encoding='utf-8') as f:
        return json.load(f)


def save(model, path):
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(model, f, ensure_ascii=False, separators=(',', ':'))
        f.write('\n')


def elements_by_uuid(model):
    return {e['uuid']: e for e in model['elements']}


def iter_groups(outliner, parent=None):
    """Yields (group, parent_group) depth first."""
    for node in outliner:
        if isinstance(node, dict):
            yield node, parent
            yield from iter_groups(node.get('children', []), node)


def groups_by_name(model):
    result = {}
    for group, _ in iter_groups(model['outliner']):
        if group['name'] in result:
            raise ValueError('Duplicated group name "%s" (bone names must be unique)' % group['name'])
        result[group['name']] = group
    return result


def direct_elements(group, elements):
    return [elements[c] for c in group.get('children', []) if isinstance(c, str) and c in elements]


def child_groups(group):
    return [c for c in group.get('children', []) if isinstance(c, dict)]


def texture_png(model, index=0):
    """PNG bytes of an embedded texture."""
    source = model['textures'][index].get('source', '')
    if not source.startswith('data:image/png;base64,'):
        raise ValueError('Texture %d is not embedded as PNG (save the .bbmodel with the texture inside)' % index)
    return base64.b64decode(source.split(',', 1)[1])


def set_texture_png(model, png_bytes, index=0):
    model['textures'][index]['source'] = 'data:image/png;base64,' + base64.b64encode(png_bytes).decode('ascii')


def png_size(png_bytes):
    if png_bytes[:8] != b'\x89PNG\r\n\x1a\n':
        raise ValueError('Not a PNG file')
    return int.from_bytes(png_bytes[16:20], 'big'), int.from_bytes(png_bytes[20:24], 'big')


def new_group(name, origin, children=None, rotation=(0, 0, 0)):
    return {
        'name': name,
        'origin': [float(v) for v in origin],
        'rotation': [float(v) for v in rotation],
        'color': 0,
        'uuid': stable_uuid('group', name),
        'export': True,
        'isOpen': True,
        'visibility': True,
        'autouv': 0,
        'children': list(children or []),
    }


def clone(model):
    return copy.deepcopy(model)
