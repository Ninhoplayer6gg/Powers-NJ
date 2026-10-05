"""
Shared authoring tools for Thragg's clips (see the conventions in tools/suit-assets/README.md):

    root   x+ lean forward (pivot at the feet)  y+ turn right  z+ roll left   pos: y+ up, z+ backwards
    body   x+ bend forward                      y+ chest turns right (right shoulder back)
    head   x+ look down (added to where the player looks, except in the fast flight states)
    arms   x- raise forward    right z+ / left z- lift sideways    y+ swing towards the right
    legs   x- forward          right z+ / left z- spread
    *_forearm x- bend the elbow      *_shin x+ bend the knee
    cape   x+ away from the back     tabard_* x- forward (keep them in front of the thighs)

Every key is a complete pose (missing bones come from STANCE), so interpolation never falls back
to an unexpected neighbour. ground() keeps grounded poses standing on y = 0.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.dirname(HERE)
sys.path.insert(0, TOOLS)

from suitlib import bbmodel, clips, kinematics  # noqa: E402

ROOT = os.path.abspath(os.path.join(TOOLS, '..', '..'))
MODEL = os.path.join(ROOT, 'art', 'suits', 'thragg', 'thragg.bbmodel')
PREFIX = 'animation.thragg.'

BONES = ['root', 'head', 'body', 'right_arm', 'left_arm', 'right_forearm', 'left_forearm', 'right_leg', 'left_leg',
         'right_shin', 'left_shin', 'cape', 'cape_mid', 'cape_lower', 'tabard_right', 'tabard_left']
POSITIONAL = {'root', 'body', 'head', 'right_arm', 'left_arm', 'right_leg', 'left_leg'}
MIRROR = {'right_arm': 'left_arm', 'right_forearm': 'left_forearm', 'right_leg': 'left_leg', 'right_shin': 'left_shin',
          'tabard_right': 'tabard_left'}
MIRROR.update({v: k for k, v in MIRROR.items()})
FEET = ('right_shin', 'left_shin')
_RIG = None


def rig():
    global _RIG
    if _RIG is None:
        _RIG = kinematics.Rig(bbmodel.load(MODEL))
    return _RIG


def pose(**bones):
    """pose(body=(x, y, z), root_pos=(x, y, z), ...): rotations by bone name, positions with _pos."""
    result = {}
    for key, value in bones.items():
        if key.endswith('_pos'):
            result.setdefault(key[:-4], {})['pos'] = [float(v) for v in value]
        else:
            result.setdefault(key, {})['rot'] = [float(v) for v in value]
    return result


def merge(*poses):
    result = {}
    for p in poses:
        for bone, entry in p.items():
            target = result.setdefault(bone, {})
            for channel, value in entry.items():
                target[channel] = list(value)
    return result


def mirror(p):
    """Left/right mirror: swaps sides and negates Y/Z rotations and X positions."""
    result = {}
    for bone, entry in p.items():
        target = result.setdefault(MIRROR.get(bone, bone), {})
        if 'rot' in entry:
            x, y, z = entry['rot']
            target['rot'] = [x, -y, -z]
        if 'pos' in entry:
            x, y, z = entry['pos']
            target['pos'] = [-x, y, z]
    return result


def lean(body_x):
    """
    Body leans pivot at the neck, so its bottom moves; hips (and slightly the shoulders) follow it the
    way vanilla's crouch does. Returns the matching leg and arm positions.
    """
    a = math.radians(body_x)
    hip = (0.0, 12.0 * (1 - math.cos(a)), 12.0 * math.sin(a))
    shoulder = (0.0, 2.0 * (1 - math.cos(a)), 2.0 * math.sin(a))
    return pose(right_leg_pos=hip, left_leg_pos=hip, right_arm_pos=shoulder, left_arm_pos=shoulder, body=(body_x, 0, 0))


def tabards_for(p, extra=-3.0):
    """Keeps the front tabards ahead of the thighs (they hang from the belt, which leans with the body)."""
    body_x = p.get('body', {}).get('rot', [0, 0, 0])[0]
    out = {}
    for leg, tabard in (('right_leg', 'tabard_right'), ('left_leg', 'tabard_left')):
        leg_x = p.get(leg, {}).get('rot', [0, 0, 0])[0]
        out[tabard] = {'rot': [min(0.0, leg_x * 1.08 - body_x) + extra, 0.0, 0.0]}
    return out


STANCE = pose(
    root=(0, 0, 0), root_pos=(0, 0, 0),
    head=(-3, 0, 0),
    body=(-2, 0, 0),
    right_arm=(3, 0, 7), left_arm=(3, 0, -7),
    right_forearm=(-12, 0, 0), left_forearm=(-12, 0, 0),
    right_leg=(0, 0, 3), left_leg=(0, 0, -3),
    right_shin=(0, 0, -3), left_shin=(0, 0, 3),
    cape=(4, 0, 0), cape_mid=(2, 0, 0), cape_lower=(2, 0, 0),
    tabard_right=(-2, 0, 0), tabard_left=(-2, 0, 0),
)

GUARD_ARMS = pose(right_arm=(-52, -24, 6), right_forearm=(-108, 0, 0), left_arm=(-56, 24, -6), left_forearm=(-112, 0, 0))


def complete(p, base=None):
    """Every bone gets rotation (and position for the humanoid parts) so keys never fall back."""
    full = merge(base if base is not None else STANCE, p)
    for bone in BONES:
        entry = full.setdefault(bone, {})
        entry.setdefault('rot', [0.0, 0.0, 0.0])
        if bone in POSITIONAL:
            entry.setdefault('pos', [0.0, 0.0, 0.0])
    return full


class ClipBuilder:
    def __init__(self, name, length, loop='once', mode='catmullrom'):
        self.clip = clips.Clip(PREFIX + name, length, loop)
        self.mode = mode
        self.keys = []

    def key(self, time, p, base=None, mode=None):
        self.keys.append((time, complete(p, base), mode or self.mode))
        return self

    def curve(self, fn, samples, base=None):
        """Cyclic/procedural motion: fn(t) -> pose, sampled at evenly spaced times (inclusive end)."""
        for i in range(samples + 1):
            t = self.clip.length * i / samples
            self.key(t, fn(t), base)
        return self

    def build(self):
        for time, p, mode in sorted(self.keys, key=lambda k: k[0]):
            for bone, entry in p.items():
                if 'rot' in entry:
                    self.clip.add(bone, 'rotation', clips.Key(time, entry['rot'], mode=mode))
                if 'pos' in entry:
                    self.clip.add(bone, 'position', clips.Key(time, entry['pos'], mode=mode))
        # drop channels that never move (keeps the file readable)
        for bone in list(self.clip.bones):
            for channel in list(self.clip.bones[bone]):
                keys = self.clip.bones[bone][channel]
                if all(k.post == [0.0, 0.0, 0.0] and k.pre == [0.0, 0.0, 0.0] for k in keys):
                    del self.clip.bones[bone][channel]
            if not self.clip.bones[bone]:
                del self.clip.bones[bone]
        return self.clip


def wave(t, period, phase=0.0):
    return math.sin(2 * math.pi * (t / period + phase))


# ---------------------------------------------------------------------------------- locomotion


def ground(p, base=None, feet=FEET, z=None):
    """
    Moves the root so the lowest point of the given feet touches y = 0 (and, when z is given, so the
    feet are centred on that z). Use for every pose that stands on the ground.
    """
    full = complete(p, base)
    rig_ = rig()
    lowest = rig_.lowest(feet, full)
    root = full['root']['pos']
    root[1] -= lowest
    if z is not None:
        centre = rig_.centre(feet, full)
        root[2] += z - centre[2]
    return full


def report(p, base=None):
    """Debug helper: world position of the feet/fists/head for a pose."""
    full = complete(p, base)
    rig_ = rig()
    out = {}
    for name in ('right_shin', 'left_shin', 'right_forearm', 'left_forearm', 'head'):
        c = rig_.centre([name], full)
        out[name] = [round(v, 2) for v in c]
    out['lowest_foot'] = round(rig_.lowest(FEET, full), 2)
    return out
