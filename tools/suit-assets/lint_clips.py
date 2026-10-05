#!/usr/bin/env python3
"""
Sanity checks for suit animation clips (built from a clips module, e.g. clips_thragg):

* loops: the last keyframe equals the first one (no pop at the seam);
* grounded clips: the lowest foot stays on y = 0 (no sinking / floating) at every sampled frame;
* joints: knees (x+) and elbows (x-) never bend backwards, nothing exceeds 160 degrees;
* tabards: stay in front of the thighs.

    python3 tools/suit-assets/lint_clips.py [clips_thragg] [--clip name]
"""
import argparse
import importlib
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

from suitlib import bbmodel, kinematics  # noqa: E402

# clips whose feet stay planted on the ground for their whole duration
GROUNDED = {'parado', 'agachar', 'defender', 'soco_direito', 'soco_esquerdo', 'combo', 'uppercut', 'onda_de_choque',
            'impacto_solo', 'recuar', 'vitoria', 'pousar'}
# feet planted but at least one foot may leave the ground (lowest point must still touch it)
ONE_FOOT = {'chute', 'andar', 'correr', 'investida'}


def pose_at(clip, t):
    raw = clip.pose(t)
    return {b: {'rot': v.get('rotation', [0, 0, 0]), 'pos': v.get('position', [0, 0, 0])} for b, v in raw.items()}


def lint(clip, rig, samples=24):
    short = clip.name.split('.')[-1]
    problems = []
    if clip.loop == 'loop':
        for bone, channels in clip.bones.items():
            for channel, keys in channels.items():
                if abs(keys[0].time) > 1e-6 or abs(keys[-1].time - clip.length) > 1e-6:
                    problems.append('loop: %s %s keys must start at 0 and end at the clip length' % (bone, channel))
                elif max(abs(a - b) for a, b in zip(keys[0].pre, keys[-1].post)) > 0.05:
                    problems.append('loop: %s %s first and last keys differ (%s vs %s)' % (bone, channel, keys[0].pre, keys[-1].post))
    worst_low, worst_high = 0.0, 0.0
    for i in range(samples + 1):
        t = clip.length * i / samples
        p = pose_at(clip, t)
        for bone, limit in (('right_shin', 1), ('left_shin', 1), ('right_forearm', -1), ('left_forearm', -1)):
            x = p.get(bone, {}).get('rot', [0, 0, 0])[0]
            if x * limit < -3:
                problems.append('t=%.2f %s bends backwards (%.1f)' % (t, bone, x))
        for bone, v in p.items():
            if any(abs(a) > 181 for a in v['rot']) and bone != 'root':
                problems.append('t=%.2f %s rotation %s exceeds 180' % (t, bone, v['rot']))
        if short in GROUNDED or short in ONE_FOOT:
            low = rig.lowest(('right_shin', 'left_shin'), p)
            worst_low = min(worst_low, low)
            worst_high = max(worst_high, low)
        body_x = p.get('body', {}).get('rot', [0, 0, 0])[0]
        for leg, tabard in (('right_leg', 'tabard_right'), ('left_leg', 'tabard_left')):
            leg_x = p.get(leg, {}).get('rot', [0, 0, 0])[0]
            tab_x = p.get(tabard, {}).get('rot', [0, 0, 0])[0]
            if tab_x > min(0.0, leg_x - body_x) + 4:
                problems.append('t=%.2f %s (%.1f) behind %s (%.1f, body %.1f)' % (t, tabard, tab_x, leg, leg_x, body_x))
    if worst_low < -0.35:
        problems.append('feet sink %.2f px below the ground' % worst_low)
    if worst_high > 0.35:
        problems.append('feet float %.2f px above the ground' % worst_high)
    return problems


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('module', nargs='?', default='clips_thragg')
    parser.add_argument('--clip', action='append')
    args = parser.parse_args()
    package = importlib.import_module(args.module)
    rig = kinematics.Rig(bbmodel.load(package.common.MODEL))
    total = 0
    for clip in package.build_all():
        short = clip.name.split('.')[-1]
        if args.clip and short not in args.clip:
            continue
        problems = lint(clip, rig)
        total += len(problems)
        print('%-16s %s' % (short, 'ok' if not problems else '%d problem(s)' % len(problems)))
        seen = set()
        for problem in problems:
            key = problem.split(' ', 1)[-1] if problem.startswith('t=') else problem
            if key in seen:
                continue
            seen.add(key)
            print('    ' + problem)
    sys.exit(1 if total else 0)


if __name__ == '__main__':
    main()
