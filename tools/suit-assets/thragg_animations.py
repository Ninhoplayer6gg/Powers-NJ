#!/usr/bin/env python3
"""
Builds Thragg's animation clips (tools/suit-assets/clips_thragg/*), writes them into
art/suits/thragg/thragg.bbmodel (preview/edit them in Blockbench) and re-exports the mod assets.
Needs no dependencies; the preview renderer (preview.py) needs numpy and Pillow.

    python3 tools/suit-assets/thragg_animations.py [--no-export]
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import clips_thragg  # noqa: E402
from clips_thragg.common import MODEL, ROOT  # noqa: E402
from suitlib import bbmodel, clips  # noqa: E402


def main():
    model = bbmodel.load(MODEL)
    clip_list = clips_thragg.build_all()
    clips.write_bbmodel_animations(model, clip_list)
    bbmodel.save(model, MODEL)
    print('Wrote %d clips into %s' % (len(clip_list), os.path.relpath(MODEL, ROOT)))
    if '--no-export' not in sys.argv:
        import export_suit
        outputs, warnings, _ = export_suit.export(MODEL, 'thragg', 0.25)
        for warning in warnings:
            print('warning:', warning)
        for kind, path in outputs.items():
            print('%-10s %s' % (kind, os.path.relpath(path, ROOT)))


if __name__ == '__main__':
    main()
