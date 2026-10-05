#!/usr/bin/env python3
"""
Converts a humanoid Blockbench model into the Powers NJ suit rig (see suitlib/rig.py).

The input must contain groups named head, body, right_arm, left_arm, right_leg and left_leg (any
nesting, any extra child groups such as forearms, shins, capes). The script:

* moves head and arms out of body so every part is a sibling, like Minecraft's HumanoidModel;
* puts all parts under a single "root" group at (0, 0, 0);
* snaps the part pivots to the vanilla ones and centres the legs on the vanilla legs;
* optionally replaces the embedded texture (--texture) and drops the old animations.

    python3 tools/suit-assets/rig_humanoid.py input.bbmodel output.bbmodel [--texture skin.png] [--name thragg]
"""
import argparse
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from suitlib import bbmodel, rig  # noqa: E402


def subtree_elements(group, elements):
    result = list(bbmodel.direct_elements(group, elements))
    for child in bbmodel.child_groups(group):
        result.extend(subtree_elements(child, elements))
    return result


def subtree_groups(group):
    result = [group]
    for child in bbmodel.child_groups(group):
        result.extend(subtree_groups(child))
    return result


def shift_subtree(group, elements, dx):
    if abs(dx) < 1e-9:
        return
    for element in subtree_elements(group, elements):
        element['from'][0] += dx
        element['to'][0] += dx
        element['origin'][0] += dx
    for g in subtree_groups(group)[1:]:
        g['origin'][0] += dx


def convert(model, texture=None, name=None):
    elements = bbmodel.elements_by_uuid(model)
    by_name = bbmodel.groups_by_name(model)
    missing = [p for p in rig.PLAYER_PARTS if p not in by_name]
    if missing:
        raise SystemExit('Missing groups: ' + ', '.join(missing))
    parents = {id(g): p for g, p in bbmodel.iter_groups(model['outliner'])}
    parts = {}
    for part in rig.PLAYER_PARTS:
        group = by_name[part]
        parent = parents[id(group)]
        siblings = parent['children'] if parent is not None else model['outliner']
        siblings[:] = [c for c in siblings if c is not group]
        parts[part] = group

    for part, (_, pivot, lo, hi, _) in rig.PLAYER_PARTS.items():
        group = parts[part]
        if part.endswith('_leg'):
            boxes = subtree_elements(group, elements)
            if boxes:
                min_x = min(min(e['from'][0], e['to'][0]) for e in boxes)
                max_x = max(max(e['from'][0], e['to'][0]) for e in boxes)
                shift_subtree(group, elements, (lo[0] + hi[0]) / 2.0 - (min_x + max_x) / 2.0)
        group['origin'] = [float(v) for v in pivot]
        group['rotation'] = [0.0, 0.0, 0.0]

    # everything that is not a part must not survive at the top level
    leftovers = [n for n in model['outliner'] if isinstance(n, dict)]
    stray = []
    for top in leftovers:
        for group, _ in bbmodel.iter_groups([top]):
            stray.extend(bbmodel.direct_elements(group, elements))
    if stray:
        raise SystemExit('Cubes outside the humanoid parts: ' + ', '.join(e['name'] for e in stray))

    root = bbmodel.new_group(rig.ROOT, (0, 0, 0), [parts[p] for p in rig.PLAYER_PARTS])
    root['uuid'] = leftovers[0]['uuid'] if leftovers else root['uuid']
    model['outliner'] = [root]
    model['animations'] = []
    model['history'] = []
    model['history_index'] = 0
    if name:
        model['name'] = name
        model['model_identifier'] = name
    if texture:
        with open(texture, 'rb') as f:
            data = f.read()
        bbmodel.set_texture_png(model, data)
        width, height = bbmodel.png_size(data)
        model['textures'][0]['width'] = width
        model['textures'][0]['height'] = height
    return rig.validate(model)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('input')
    parser.add_argument('output')
    parser.add_argument('--texture', help='PNG that replaces the embedded texture')
    parser.add_argument('--name', help='model name / identifier')
    args = parser.parse_args()
    model = bbmodel.load(args.input)
    for warning in convert(model, args.texture, args.name):
        print('warning:', warning)
    bbmodel.save(model, args.output)
    print('Wrote', args.output)


if __name__ == '__main__':
    main()
