#!/usr/bin/env python3
"""
Exports a suit rig (.bbmodel, see suitlib/rig.py) into the mod resources:

    assets/powersnj/geo/suits/<suit>.geo.json                 GeckoLib armor geometry
    assets/powersnj/textures/suits/<suit>/<suit>_geo.png      texture (embedded in the .bbmodel)
    assets/powersnj/animations/suits/<suit>.animation.json    animation clips (Bedrock format)

    python3 tools/suit-assets/export_suit.py art/suits/thragg/thragg.bbmodel --suit thragg [--shell 0.25]

Pure Python, no dependencies. Fails (exit 1) when the rig breaks a convention.
"""
import argparse
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from suitlib import bbmodel, clips, rig  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'powersnj')


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')


def check_clips(model, suit, clip_list):
    errors = []
    bones = {g['name'] for g, _ in bbmodel.iter_groups(model['outliner'])}
    prefix = 'animation.%s.' % suit
    for clip in clip_list:
        if not clip.name.startswith(prefix):
            errors.append('Clip "%s" must start with "%s"' % (clip.name, prefix))
        if clip.length <= 0:
            errors.append('Clip "%s" has no length' % clip.name)
        for bone, channels in clip.bones.items():
            if bone not in bones:
                errors.append('Clip "%s" animates unknown bone "%s"' % (clip.name, bone))
            if bone == rig.ROOT and 'scale' in channels:
                errors.append('Clip "%s": the root bone cannot be scaled' % clip.name)
            for channel, keys in channels.items():
                if any(k.time < -1e-6 or k.time > clip.length + 1e-6 for k in keys):
                    errors.append('Clip "%s" bone "%s" %s has keyframes outside 0..%s' % (clip.name, bone, channel, clip.length))
    return errors


def export(path, suit, shell):
    model = bbmodel.load(path)
    warnings = rig.validate(model)
    geo = rig.export_geometry(model, 'geometry.powersnj.' + suit, shell)
    clip_list = clips.read_bbmodel_animations(model, warnings)
    errors = check_clips(model, suit, clip_list)
    if errors:
        raise rig.RigError('\n'.join(errors))
    png = bbmodel.texture_png(model)
    width, height = bbmodel.png_size(png)
    res = model.get('resolution', {})
    if (width, height) != (res.get('width'), res.get('height')):
        warnings.append('Texture is %dx%d but the project UV resolution is %sx%s' % (width, height, res.get('width'), res.get('height')))

    outputs = {
        'geo': os.path.join(ASSETS, 'geo', 'suits', suit + '.geo.json'),
        'texture': os.path.join(ASSETS, 'textures', 'suits', suit, suit + '_geo.png'),
        'animations': os.path.join(ASSETS, 'animations', 'suits', suit + '.animation.json'),
    }
    write_json(outputs['geo'], geo)
    os.makedirs(os.path.dirname(outputs['texture']), exist_ok=True)
    with open(outputs['texture'], 'wb') as f:
        f.write(png)
    write_json(outputs['animations'], clips.to_bedrock(clip_list))
    return outputs, warnings, clip_list


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('bbmodel')
    parser.add_argument('--suit', required=True, help='suit name (SuitKind name), e.g. thragg')
    parser.add_argument('--shell', type=float, default=0.25, help='px added around each part (avoids z-fighting with the skin)')
    args = parser.parse_args()
    try:
        outputs, warnings, clip_list = export(args.bbmodel, args.suit, args.shell)
    except rig.RigError as e:
        print('error:', e, file=sys.stderr)
        sys.exit(1)
    for warning in warnings:
        print('warning:', warning)
    for kind, path in outputs.items():
        print('%-10s %s' % (kind, os.path.relpath(path, ROOT)))
    print('%d animation clip(s): %s' % (len(clip_list), ', '.join(c.name for c in clip_list)))


if __name__ == '__main__':
    main()
