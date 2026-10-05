"""
Thragg locomotion clips: idle, walk, run, crouch, guard, jump.

Thragg is the Grand Regent: heavy, upright and economical. Chest out, chin up, fists closed; nothing
wasted, nothing hurried. Grounded clips are planted per frame with ground(); the walk and the run are
phase locked to vanilla's limb swing (t = 0: right leg fully back, left leg forward, right arm forward)
and every limb of the left side is the right side half a cycle later, mirrored.
"""
import math

from .common import *  # noqa: F401,F403
from .common import (ClipBuilder, merge, pose, mirror, lean, tabards_for, wave, ground, complete, rig,  # noqa: F401
                     bbmodel, kinematics, STANCE, GUARD_ARMS)

TAU = 2 * math.pi


# ------------------------------------------------------------------------------------------ helpers

def _bump(phase, centre, width):
    """Smooth periodic bump (1 at centre, ~0.6 at +-width), phase in cycles."""
    return math.exp((math.cos(TAU * (phase - centre)) - 1.0) / (TAU * width) ** 2)


def _loop(*keys):
    """
    Periodic smooth curve through (phase, value) keys (phase in cycles, sorted, first key at 0): cubic
    Hermite with finite-difference tangents, so uneven key spacing does not overshoot much.
    """
    ps = [k[0] for k in keys]
    vs = [k[1] for k in keys]
    n = len(keys)

    def at(i):
        wraps, j = divmod(i, n)
        return ps[j] + wraps, vs[j]

    def tangent(i):
        p0, v0 = at(i - 1)
        p1, v1 = at(i + 1)
        return (v1 - v0) / (p1 - p0)

    def f(phase):
        phase %= 1.0
        i = max(j for j in range(n) if ps[j] <= phase)
        p0, v0 = at(i)
        p1, v1 = at(i + 1)
        h = p1 - p0
        s = (phase - p0) / h
        h00, h10, h01, h11 = 2 * s ** 3 - 3 * s ** 2 + 1, s ** 3 - 2 * s ** 2 + s, -2 * s ** 3 + 3 * s ** 2, s ** 3 - s ** 2
        return h00 * v0 + h10 * h * tangent(i) + h01 * v1 + h11 * h * tangent(i + 1)
    return f


def _torso(x=0.0, y=0.0, z=0.0):
    """Body rotation (pivot at the neck) with the shoulder and hip joints following the torso exactly."""
    m = rig().matrices({'body': {'rot': [x, y, z]}})['body']
    out = pose(body=(x, y, z))
    for bone, joint in (('right_arm', (5.0, 22.0, 0.0)), ('left_arm', (-5.0, 22.0, 0.0)),
                        ('right_leg', (1.9, 12.0, 0.0)), ('left_leg', (-1.9, 12.0, 0.0))):
        w = kinematics.apply(m, joint)
        out[bone] = {'pos': [joint[0] - w[0], w[1] - joint[1], w[2] - joint[2]]}
    return out


_BOXES = {}


def _boxes(bone):
    if bone not in _BOXES:
        r = rig()
        out = []
        for e in bbmodel.direct_elements(r.groups[bone], r.elements):
            lo = [min(a, b) for a, b in zip(e['from'], e['to'])]
            hi = [max(a, b) for a, b in zip(e['from'], e['to'])]
            out.append((lo, hi))
        _BOXES[bone] = out
    return _BOXES[bone]


def _points(bone, n=2):
    pts = []
    for lo, hi in _boxes(bone):
        for i in range(n + 1):
            for j in range(n + 1):
                for k in range(n + 1):
                    pts.append((lo[0] + (hi[0] - lo[0]) * i / n, lo[1] + (hi[1] - lo[1]) * j / n,
                                lo[2] + (hi[2] - lo[2]) * k / n))
    return pts


def _inverse(m):
    rt = [[m[j][i] for j in range(3)] for i in range(3)]
    t = [m[i][3] for i in range(3)]
    ti = [-sum(rt[i][k] * t[k] for k in range(3)) for i in range(3)]
    return [rt[0] + [ti[0]], rt[1] + [ti[1]], rt[2] + [ti[2]], [0, 0, 0, 1]]


def _push(full, sheet, world_points, forward, limit):
    """
    Rotates a hanging cloth sheet (cape segment or tabard) about its hinge until none of the given world
    points sits on the wrong side of it inside its footprint: forward=True (tabards) keeps the points
    behind the sheet's back face by rotating x-, forward=False (cape) keeps them in front of its front face
    by rotating x+. Points within 1.5 px of the hinge cannot be cleared by rotating and are ignored.
    """
    r = rig()
    hinge_y = r.groups[sheet]['origin'][1]
    start = full[sheet]['rot'][0]
    for _ in range(10):
        inv = _inverse(r.matrices(full)[sheet])
        need = 0.0
        for w in world_points:
            local = kinematics.apply(inv, w)
            for lo, hi in _boxes(sheet):
                if not (lo[0] - 0.2 <= local[0] <= hi[0] + 0.2 and lo[1] <= local[1] <= hi[1]):
                    continue
                lever = hinge_y - local[1]
                if lever < 1.5:
                    continue
                depth = (hi[2] + 0.15 - local[2]) if forward else (local[2] - lo[2] + 0.15)
                if depth > 0:
                    need = max(need, math.degrees(math.atan2(depth, lever)))
        if need < 0.05:
            break
        full[sheet]['rot'][0] += (-1 if forward else 1) * (need + 0.25)
        if abs(full[sheet]['rot'][0] - start) > limit:
            break


def _softmin(a, b, k=2.0):
    m = min(a, b)
    return m - k * math.log(math.exp((m - a) / k) + math.exp((m - b) / k))


def _tabard_need(rel):
    """
    Tabard x that clears the thigh for a given thigh-minus-body angle (fitted to the rig: the thigh pivots
    2.25 px behind the tabard hinge, so the panel has to swing further than the thigh does).
    """
    return 1.2 * rel - 0.004 * rel * rel if rel < 0 else 0.0


def _cloth(p, base=None, tabard_extra=-1.5):
    """
    Final pass for every key: each tabard is eased (soft minimum) in front of its thigh, the cape segments
    are pushed away from the back until no torso or leg point is behind them. Returns a complete pose.
    """
    full = complete(p, base)
    body_x = full['body']['rot'][0]
    for leg, tabard in (('right_leg', 'tabard_right'), ('left_leg', 'tabard_left')):
        rel = full[leg]['rot'][0] - body_x
        rot = full[tabard]['rot']
        rot[0] = _softmin(rot[0], _tabard_need(rel) + tabard_extra)
    mats = rig().matrices(full)
    blockers = [kinematics.apply(mats[bone], pt)
                for bone in ('body', 'right_leg', 'left_leg', 'right_shin', 'left_shin') for pt in _points(bone)]
    for seg in ('cape', 'cape_mid', 'cape_lower'):
        _push(full, seg, blockers, False, 80)
    return full


def _planted(p, base=None, z=None, tabard_extra=-1.5):
    """Cloth pass then ground(): the lowest foot on y = 0 (feet centred on z when given)."""
    return ground(_cloth(p, base, tabard_extra), z=z)


# --------------------------------------------------------------------------------------------- idle

def idle():
    """Regal stance: two slow breaths and a barely visible weight shift (6.4 s, no visible repetition)."""
    length = 6.4
    b = ClipBuilder('parado', length, 'loop')

    def at(t):
        ph = t / length
        breath = 0.5 - 0.5 * math.cos(2 * TAU * ph)          # two breaths per loop
        sway = math.sin(TAU * ph)                             # one weight shift per loop
        p = merge(_torso(-3.0 - 1.4 * breath, 0.0, 0.0), pose(
            root=(0, 0, 0.5 * sway),
            head=(-4.0 - 0.8 * breath, 0, -0.5 * sway),
            right_arm=(1.5 + 1.2 * breath, 0, 8.0 + 1.6 * breath), left_arm=(1.5 + 1.2 * breath, 0, -8.0 - 1.6 * breath),
            right_forearm=(-13 - 3.0 * breath, 0, 0), left_forearm=(-13 - 3.0 * breath, 0, 0),
            right_leg=(0, 0, 4.0), left_leg=(0, 0, -4.0),
            right_shin=(0, 0, -4.0), left_shin=(0, 0, 4.0),
            cape=(4.5 + 1.6 * math.sin(2 * TAU * ph - 0.6), 0, 0.9 * math.sin(TAU * ph - 0.5)),
            cape_mid=(2 + 1.6 * math.sin(2 * TAU * ph - 1.2), 0, 0.6 * math.sin(TAU * ph - 1.0)),
            cape_lower=(2 + 2.2 * math.sin(2 * TAU * ph - 1.8), 0, 0.6 * math.sin(TAU * ph - 1.5)),
            tabard_right=(-2.0 - 0.8 * math.sin(2 * TAU * ph - 0.8), 0, 0),
            tabard_left=(-2.0 - 0.8 * math.sin(2 * TAU * ph - 1.0), 0, 0),
        ))
        return _planted(p)
    return b.curve(at, 16).build()


# --------------------------------------------------------------------------------------------- walk

def _walk_leg(ph):
    """Right leg at phase ph (0 = fully back / toe off, 0.5 = heel strike): thigh x, knee x."""
    c = math.cos(TAU * ph)
    thigh = 24.0 * c - 2.0
    knee = 4.0 + 50.0 * _bump(ph, 0.21, 0.09) + 10.0 * _bump(ph, 0.62, 0.07) + 15.0 * _bump(ph, 0.97, 0.06)
    return thigh, knee


def walk():
    """Heavy, unhurried stride: stance knee loads at contact, shoulders counter the hips, chest stays proud."""
    length = 1.2
    b = ClipBuilder('andar', length, 'loop')

    def at(t):
        ph = t / length
        c = math.cos(TAU * ph)
        s = math.sin(TAU * ph)
        rt, rk = _walk_leg(ph)
        lt, lk = _walk_leg(ph + 0.5)
        swing = 18.0
        p = merge(_torso(2.5, -5.0 * c, 0.0), pose(
            root=(0, 0, 1.4 * s),
            head=(-4, 1.5 * c, -1.4 * s),
            right_arm=(-swing * c + 1, 0, 8.5), left_arm=(swing * c + 1, 0, -8.5),
            right_forearm=(-14 - 16 * max(0.0, c) ** 1.3, 0, 0), left_forearm=(-14 - 16 * max(0.0, -c) ** 1.3, 0, 0),
            right_leg=(rt, 0, 2.5), left_leg=(lt, 0, -2.5),
            right_shin=(rk, 0, -2.5), left_shin=(lk, 0, 2.5),
            cape=(12 + 2.5 * math.cos(2 * TAU * ph - 0.7), 0, -1.8 * s),
            cape_mid=(4 + 3.0 * math.cos(2 * TAU * ph - 1.4), 0, -1.0 * math.sin(TAU * ph - 0.5)),
            cape_lower=(4 + 4.0 * math.cos(2 * TAU * ph - 2.1), 0, -1.0 * math.sin(TAU * ph - 1.0)),
            tabard_right=(-2 - 2.0 * math.cos(2 * TAU * ph - 0.9), 0, 0),
            tabard_left=(-2 - 2.0 * math.cos(2 * TAU * ph - 0.9), 0, 0),
        ))
        return _planted(p)
    return b.curve(at, 24).build()


# ---------------------------------------------------------------------------------------------- run

# right leg, relative to the root (which leans forward): 0 = fully back at toe off, knee driven up and
# forward around 0.38, the foot reaches down to land under the knee at ~0.56, compresses, pushes off.
_RUN_THIGH = _loop((0.0, 26.0), (0.1, 16.0), (0.24, -24.0), (0.38, -50.0), (0.5, -40.0), (0.58, -28.0),
                   (0.72, -8.0), (0.9, 16.0))
_RUN_KNEE = _loop((0.0, 22.0), (0.08, 62.0), (0.2, 104.0), (0.32, 92.0), (0.44, 52.0), (0.56, 24.0),
                  (0.68, 40.0), (0.84, 30.0))


def _run_leg(ph):
    """Right leg at phase ph: thigh x, knee x."""
    return _RUN_THIGH(ph), _RUN_KNEE(ph)


def run():
    """Power sprint: strong forward lean, knee drive, heel kick, piston arms with closed fists, chin up."""
    length = 0.72
    b = ClipBuilder('correr', length, 'loop')

    def at(t):
        ph = t / length
        c = math.cos(TAU * ph)
        s = math.sin(TAU * ph)
        rt, rk = _run_leg(ph)
        lt, lk = _run_leg(ph + 0.5)
        flap = math.sin(2 * TAU * ph - 0.4)
        p = merge(_torso(13.0, -10.0 * c, 0.0), pose(
            root=(9, 0, 1.2 * s),
            head=(-12, 2.5 * c, -1.2 * s),
            right_arm=(-14 - 46 * c, -4 * (0.5 + 0.5 * c), 10), left_arm=(-14 + 46 * c, 4 * (0.5 - 0.5 * c), -10),
            right_forearm=(-80 - 24 * (0.5 + 0.5 * c), 0, 0), left_forearm=(-80 - 24 * (0.5 - 0.5 * c), 0, 0),
            right_leg=(rt, 0, 2.5), left_leg=(lt, 0, -2.5),
            right_shin=(rk, 0, -2.5), left_shin=(lk, 0, 2.5),
            cape=(22 + 3 * flap, 0, -3.0 * c),
            cape_mid=(7 + 4 * math.sin(2 * TAU * ph - 1.3), 0, -1.5 * math.cos(TAU * ph - 0.6)),
            cape_lower=(8 + 5.5 * math.sin(2 * TAU * ph - 2.2), 0, -1.5 * math.cos(TAU * ph - 1.2)),
            tabard_right=(-4 - 3 * math.sin(2 * TAU * ph - 0.8), 0, 0),
            tabard_left=(-4 - 3 * math.sin(2 * TAU * ph - 0.8), 0, 0),
        ))
        return _planted(p, tabard_extra=-3.0)
    return b.curve(at, 24).build()


# ------------------------------------------------------------------------------------------- crouch

def crouch():
    """Sneaking: a low, coiled predator crouch, weight over the balls of the feet, fists ready."""
    length = 2.4
    b = ClipBuilder('agachar', length, 'loop')

    def at(t):
        ph = t / length
        breath = 0.5 - 0.5 * math.cos(TAU * ph)
        p = merge(_torso(28.0 + 2.0 * breath, 0.0, 0.0), pose(
            head=(-4 - 0.6 * breath, 0, 0),
            right_arm=(-26 - 2 * breath, 0, 13), left_arm=(-22 - 2 * breath, 0, -13),
            right_forearm=(-34 - 3 * breath, 0, 0), left_forearm=(-30 - 3 * breath, 0, 0),
            right_leg=(-58 - 1.2 * breath, 0, 7), left_leg=(-50 - 1.2 * breath, 0, -7),
            right_shin=(70 + 2 * breath, 0, -7), left_shin=(62 + 2 * breath, 0, 7),
            cape=(-14 + 1.5 * math.sin(TAU * ph - 0.6), 0, 0), cape_mid=(8, 0, 0),
            cape_lower=(6 + 2 * math.sin(TAU * ph - 1.4), 0, 0),
            tabard_right=(0, 0, 0), tabard_left=(0, 0, 0),
        ))
        return _planted(p, z=0.0, tabard_extra=-3.0)
    return b.curve(at, 8).build()


# -------------------------------------------------------------------------------------------- guard

def guard():
    """Sneaking in combat: bladed brawler stance, left lead, fists up in front of the chin, weight low."""
    length = 2.0
    b = ClipBuilder('defender', length, 'loop')

    def at(t):
        ph = t / length
        breath = 0.5 - 0.5 * math.cos(TAU * ph)
        p = merge(_torso(14.0 + 1.5 * breath, 16.0, 0.0), GUARD_ARMS, pose(
            head=(0 - 0.5 * breath, 0, 0),
            right_leg=(12, 8, 7), left_leg=(-30 - 1.5 * breath, -6, -5),
            right_shin=(26 + 2 * breath, 0, -6), left_shin=(32 + 3 * breath, 0, 5),
            cape=(4 + 1.5 * math.sin(TAU * ph - 0.6), 0, 0), cape_mid=(4, 0, 0), cape_lower=(4, 0, 0),
        ))
        return _planted(p, z=0.0)
    return b.curve(at, 8).build()


# --------------------------------------------------------------------------------------------- jump

def jump():
    """Explosive push-off, knees driven up, then a controlled regal airborne pose (held while in the air)."""
    b = ClipBuilder('saltar', 0.6, 'hold')
    push = merge(_torso(-4), pose(
        head=(-6, 0, 0),
        right_arm=(-64, 0, 14), left_arm=(-58, 0, -14), right_forearm=(-34, 0, 0), left_forearm=(-30, 0, 0),
        right_leg=(8, 0, 2), left_leg=(4, 0, -2), right_shin=(10, 0, -2), left_shin=(6, 0, 2),
        cape=(0, 0, 0), cape_mid=(0, 0, 0), cape_lower=(0, 0, 0)))
    b.key(0.0, _cloth(push))
    rise = merge(_torso(2), pose(
        head=(-5, 0, 0),
        right_arm=(-36, 0, 20), left_arm=(-32, 0, -20), right_forearm=(-38, 0, 0), left_forearm=(-34, 0, 0),
        right_leg=(-34, 0, 3), left_leg=(-14, 0, -3), right_shin=(60, 0, -2), left_shin=(38, 0, 2),
        cape=(10, 0, 0), cape_mid=(4, 0, 0), cape_lower=(4, 0, 0)))
    b.key(0.12, _cloth(rise))
    tuck = merge(_torso(6), pose(
        head=(-4, 0, 0),
        right_arm=(-18, 0, 24), left_arm=(-16, 0, -24), right_forearm=(-42, 0, 0), left_forearm=(-38, 0, 0),
        right_leg=(-56, 0, 4), left_leg=(-26, 0, -4), right_shin=(86, 0, -2), left_shin=(62, 0, 2),
        cape=(22, 0, 0), cape_mid=(10, 0, 0), cape_lower=(12, 0, 0)))
    b.key(0.26, _cloth(tuck))
    air = merge(_torso(1), pose(
        head=(-4, 0, 0),
        right_arm=(-12, 0, 20), left_arm=(-10, 0, -19), right_forearm=(-30, 0, 0), left_forearm=(-28, 0, 0),
        right_leg=(-44, 0, 4), left_leg=(4, 0, -3), right_shin=(68, 0, -2), left_shin=(22, 0, 2),
        cape=(26, 0, 0), cape_mid=(10, 0, 0), cape_lower=(12, 0, 0)))
    b.key(0.45, _cloth(air))
    b.key(0.6, _cloth(merge(air, pose(cape=(28, 0, 0), cape_mid=(11, 0, 0), cape_lower=(13, 0, 0)))))
    return b.build()
