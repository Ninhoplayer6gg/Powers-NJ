#!/usr/bin/env python3
"""
Renders animation previews of a suit rig without Minecraft (needs: pip install numpy pillow).

    python3 tools/suit-assets/preview.py art/suits/thragg/thragg.bbmodel --out build/previews
        [--clip animation.thragg.parado ...] [--frames 8] [--views front,side,three_quarter] [--gif]

One contact sheet per clip (rows = camera views, columns = frames). The pose is applied exactly like
the game does: the humanoid parts drive GeckoLib's armor bones, suit bones move locally, the root
moves the whole body. Dynamic game inputs (looking around, flight steering) are not simulated.
"""
import argparse
import io
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from suitlib import bbmodel, clips, rig  # noqa: E402

VIEWS = {'front': (0, 0), 'side': (90, 0), 'three_quarter': (-35, 8), 'back': (180, 0), 'left': (-90, 0), 'top': (0, 60)}


def render_clip(model, geo, texture, clip, frames, views, scale=5, size=(230, 260)):
    from suitlib import render
    images = []
    labels = []
    for view in views:
        yaw, pitch = VIEWS[view]
        for i in range(frames):
            t = clip.length * i / max(1, frames - (0 if clip.loop == 'loop' else 1))
            pose = clip.pose(t)
            quads = render.geo_quads(geo, pose)
            images.append(render.rasterize(quads, texture, yaw=yaw, pitch=pitch, scale=scale, size=size, center=(0.0, 14.0)))
            labels.append('%s t=%.2f' % (view, t))
    return render.sheet(images, frames, labels)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('bbmodel')
    parser.add_argument('--out', required=True)
    parser.add_argument('--clip', action='append', help='clip name (repeatable); default all')
    parser.add_argument('--frames', type=int, default=8)
    parser.add_argument('--views', default='front,side,three_quarter')
    parser.add_argument('--shell', type=float, default=0.25)
    parser.add_argument('--gif', action='store_true', help='also write an animated GIF (three quarter view)')
    parser.add_argument('--module', help='build the clips from a Python module (e.g. clips_thragg.combat) instead of the .bbmodel')
    args = parser.parse_args()

    from PIL import Image
    from suitlib import render
    model = bbmodel.load(args.bbmodel)
    geo = rig.export_geometry(model, 'geometry.preview', args.shell)
    texture = Image.open(io.BytesIO(bbmodel.texture_png(model)))
    os.makedirs(args.out, exist_ok=True)
    views = [v.strip() for v in args.views.split(',') if v.strip()]
    if args.module:
        import importlib
        module = importlib.import_module(args.module)
        source = [getattr(module, name)() for name in dir(module)
                  if callable(getattr(module, name)) and getattr(getattr(module, name), '__module__', '') == module.__name__
                  and not name.startswith('_') and getattr(module, name).__code__.co_argcount == 0]
    else:
        source = clips.read_bbmodel_animations(model)
    for clip in source:
        if not isinstance(clip, clips.Clip):
            continue
        if args.clip and clip.name not in args.clip and clip.name.split('.')[-1] not in args.clip:
            continue
        short = clip.name.split('.')[-1]
        render_clip(model, geo, texture, clip, args.frames, views).save(os.path.join(args.out, short + '.png'))
        if args.gif:
            frames = []
            count = max(2, int(round(clip.length * 15)))
            for i in range(count + 1):
                t = clip.length * i / count
                quads = render.geo_quads(geo, clip.pose(min(t, clip.length)))
                frames.append(render.rasterize(quads, texture, yaw=-35, pitch=8, scale=4, size=(200, 230), center=(0.0, 14.0)).convert('RGB'))
            frames[0].save(os.path.join(args.out, short + '.gif'), save_all=True, append_images=frames[1:], duration=66, loop=0)
        print('rendered', short)


if __name__ == '__main__':
    main()
