"""Thragg flight clips: takeoff, hover, fly, fast flight, landing."""
from .common import *  # noqa: F401,F403
from .common import ClipBuilder, merge, pose, mirror, lean, tabards_for, wave, ground, complete, rig, STANCE, clips  # noqa: F401
import math  # noqa: E402


# ----------------------------------------------------------------------------------------- helpers

def _spline(poses, times, t):
    """Pose-level uniform Catmull-Rom through control poses (clamped ends): smooth, no corners."""
    full = [complete(p) for p in poses]
    if t <= times[0]:
        return full[0]
    if t >= times[-1]:
        return full[-1]
    i = 1
    while times[i] < t:
        i += 1
    f = (t - times[i - 1]) / (times[i] - times[i - 1])
    p0, p1, p2 = full[max(i - 2, 0)], full[i - 1], full[i]
    p3 = full[min(i + 1, len(full) - 1)]
    out = {}
    for bone, entry in p1.items():
        out[bone] = {}
        for channel in entry:
            vals = []
            for k in range(3):
                a, b, c, d = p0[bone][channel][k], p1[bone][channel][k], p2[bone][channel][k], p3[bone][channel][k]
                vals.append(0.5 * (2 * b + (-a + c) * f + (2 * a - 5 * b + 4 * c - d) * f * f + (-a + 3 * b - 3 * c + d) * f ** 3))
            out[bone][channel] = vals
    return out


SOLE = (-1.9, 0.0, -0.3)   # centre of the left sole, in left_shin space


def _plant(p, z=None):
    """ground() plus a horizontal lock: the left sole stays at z (no sliding)."""
    full = ground(p)
    if z is not None:
        full['root']['pos'][2] += z - rig().point('left_shin', SOLE, full)[2]
    return full


def _with_tabards(p, extra=-3.0):
    return merge(p, tabards_for(p, extra))


def _nudge(p, bone, offset):
    """Adds a positional offset to a bone (on top of what lean() gave it)."""
    out = merge(p)
    pos = out.setdefault(bone, {}).setdefault('pos', [0.0, 0.0, 0.0])
    out[bone]['pos'] = [a + b for a, b in zip(pos, offset)]
    return out


# ------------------------------------------------------------------------------------------ poses

# Regal hover: chest out, chin up, fists closed a little away from the hips, legs hanging relaxed.
HOVER = merge(lean(-4), pose(
    head=(-4, 0, 0),
    right_arm=(6, 0, 11), left_arm=(6, 0, -11), right_forearm=(-18, 0, 0), left_forearm=(-18, 0, 0),
    right_leg=(4, 0, 2.5), left_leg=(-14, 0, -2.5), right_shin=(10, 0, -1), left_shin=(30, 0, 1),
    cape=(12, 0, 0), cape_mid=(5, 0, 0), cape_lower=(5, 0, 0),
))

# Lying flat, face down, centred in the player's hitbox (flight states also get the look pitch in game).
FLAT = pose(root=(90, 0, 0), root_pos=(0, 14, 16))


def _hover_at(t, length=3.0):
    """The hover loop (shared with the end of the takeoff): slow heavy bob, limbs and cape lag behind."""
    s = wave(t, length)                 # body bob
    lag = wave(t, length, -0.12)        # limbs follow a little later
    breath = wave(t, length, 0.1)
    p = merge(HOVER, lean(-4 - 1.2 * breath), pose(
        root_pos=(0, 0.9 * s, 0), root=(0, 0, 0.8 * wave(t, length / 2, 0.2)),
        head=(-4 + 1.5 * lag, 0, 0),
        right_arm=(6 + 2 * lag, 0, 11 + 1.5 * breath), left_arm=(6 + 2 * lag, 0, -11 - 1.5 * breath),
        right_forearm=(-18 - 2 * lag, 0, 0), left_forearm=(-18 - 2 * lag, 0, 0),
        right_leg=(4 - 2 * lag, 0, 2.5), left_leg=(-14 - 3 * lag, 0, -2.5),
        right_shin=(10 + 2 * lag, 0, -1), left_shin=(30 + 4 * lag, 0, 1),
        cape=(12 + 4 * wave(t, length, -0.15), 0, 1.2 * wave(t, length / 2, -0.1)),
        cape_mid=(5 + 4 * wave(t, length, -0.27), 0, 0),
        cape_lower=(5 + 5 * wave(t, length, -0.4), 0, 0),
    ))
    return _with_tabards(p)


# ---------------------------------------------------------------------------------------- takeoff

def takeoff():
    """Short heavy dip, explosive launch with the right fist to the sky, the arm sweeps down into the hover."""
    b = ClipBuilder('decolar', 0.8, 'once')
    load = ground(merge(lean(16), pose(
        head=(-10, 0, 0),
        right_arm=(30, 0, 12), left_arm=(30, 0, -12), right_forearm=(-24, 0, 0), left_forearm=(-24, 0, 0),
        right_leg=(-30, 0, 4), left_leg=(-26, 0, -4), right_shin=(52, 0, -2), left_shin=(46, 0, 2),
        cape=(8, 0, 0), cape_mid=(3, 0, 0), cape_lower=(3, 0, 0))))
    b.key(0.0, _with_tabards(load, -4))
    # the launch: body stretched, fist straight up (a little outwards so it clears the head), toes pointed
    launch = merge(lean(-8), pose(
        root_pos=(0, 1.0, 0), head=(-16, 0, 0),
        right_arm=(-174, 0, -8), right_forearm=(-4, 0, 0), left_arm=(16, 0, -8), left_forearm=(-10, 0, 0),
        right_leg=(6, 0, 1), left_leg=(8, 0, -1), right_shin=(8, 0, 0), left_shin=(14, 0, 0),
        cape=(5, 0, 0), cape_mid=(2, 0, 0), cape_lower=(1, 0, 0)))
    b.key(0.1, _with_tabards(launch, -1), mode='linear')
    rising = merge(lean(-7), pose(
        root_pos=(0, 0.8, 0), head=(-14, 0, 0),
        right_arm=(-175, 0, -10), right_forearm=(-6, 0, 0), left_arm=(14, 0, -9), left_forearm=(-12, 0, 0),
        right_leg=(5, 0, 1.5), left_leg=(2, 0, -1.5), right_shin=(8, 0, 0), left_shin=(20, 0, 0),
        cape=(8, 0, 0), cape_mid=(5, 0, 0), cape_lower=(6, 0, 0)))
    b.key(0.3, _with_tabards(rising, -2), mode='linear')
    # the fist comes down in a wide diagonal arc, chest opening up into the hover
    sweep = merge(lean(-5), pose(
        root_pos=(0, 0.4, 0), head=(-8, 0, 0),
        right_arm=(-92, 42, 0), right_forearm=(-10, 0, 0), left_arm=(10, 0, -12), left_forearm=(-16, 0, 0),
        right_leg=(4, 0, 2), left_leg=(-8, 0, -2), right_shin=(9, 0, -1), left_shin=(26, 0, 1),
        cape=(12, 0, 0), cape_mid=(7, 0, 0), cape_lower=(8, 0, 0)))
    b.key(0.5, _with_tabards(sweep))
    settle = merge(_hover_at(0.0), pose(right_arm=(2, 4, 13), right_forearm=(-20, 0, 0)))
    b.key(0.68, settle)
    b.key(0.8, _hover_at(0.0))
    return b.build()


# ------------------------------------------------------------------------------------------ hover

def hover():
    """Regal hover: chest out, fists closed at the sides, slow heavy bob."""
    b = ClipBuilder('flutuar', 3.0, 'loop')
    return b.curve(_hover_at, 12).build()


# ------------------------------------------------------------------------------------------- fly

def fly():
    """Sprint flight: right fist leading, left fist tight against the side, body slightly arched."""
    length = 1.6
    b = ClipBuilder('voar', length, 'loop')

    def at(t):
        s = wave(t, length)
        flap = wave(t, length / 4)
        return merge(FLAT, lean(-5 + 1.0 * s), pose(
            root=(90, 0, 1.6 * s), root_pos=(0, 14 + 0.4 * wave(t, length, 0.25), 16),
            head=(-82, 0, 0),
            right_arm=(-172, 0, -6), right_forearm=(-4, 0, 0),
            left_arm=(12, 0, -5), left_forearm=(-16, 0, 0),
            right_leg=(4 + 1.5 * s, 0, 1), left_leg=(7 - 1.5 * s, 0, -1),
            right_shin=(6, 0, 0), left_shin=(16 + 3 * s, 0, 0),
            cape=(9 + 3 * flap, 0, 2.5 * s),
            cape_mid=(4 + 9 * wave(t, length / 4, -0.2), 0, 0),
            cape_lower=(6 + 12 * wave(t, length / 4, -0.4), 0, 0),
            tabard_right=(-4 + 3 * wave(t, length / 4, -0.1), 0, 0),
            tabard_left=(-4 + 3 * wave(t, length / 4, -0.35), 0, 0),
        ))
    return b.curve(at, 16).build()


def fast_flight():
    """Fast flight: both fists driving forward, head tucked between the arms, cape whipping."""
    length = 0.8
    b = ClipBuilder('voo_rapido', length, 'loop')

    def at(t):
        s = wave(t, length)
        flap = wave(t, length / 4)
        return merge(FLAT, lean(-2), pose(
            root=(90, 0, 1.2 * s), root_pos=(0, 14, 16),
            head=(-70, 0, 0),
            right_arm=(-178, 0, -2), left_arm=(-178, 0, 2), right_forearm=(-3, 0, 0), left_forearm=(-3, 0, 0),
            right_leg=(2 + 1 * s, 0, -0.5), left_leg=(2 - 1 * s, 0, 0.5), right_shin=(3, 0, 0), left_shin=(5, 0, 0),
            cape=(5 + 2 * flap, 0, 1.5 * s),
            cape_mid=(3 + 7 * wave(t, length / 4, -0.2), 0, 0),
            cape_lower=(4 + 10 * wave(t, length / 4, -0.4), 0, 0),
            tabard_right=(-3 + 3 * wave(t, length / 4, -0.1), 0, 0),
            tabard_left=(-3 + 3 * wave(t, length / 4, -0.35), 0, 0),
        ))
    return b.curve(at, 16).build()


# ----------------------------------------------------------------------------------------- landing

def _land_poses():
    # superhero landing: right knee down, left foot planted, right fist on the ground, left arm thrown back
    contact = merge(lean(50), pose(
        head=(14, 0, 0),
        right_arm=(-14, 0, 8), right_forearm=(-6, 0, 0), left_arm=(36, 0, -40), left_forearm=(-18, 0, 0),
        right_leg=(24, 0, 5), right_shin=(70, 0, 0), left_leg=(-74, 0, -8), left_shin=(80, 0, 4),
        cape=(48, 0, 0), cape_mid=(10, 0, 0), cape_lower=(8, 0, 0)))
    impact = merge(lean(60), pose(
        head=(16, 0, 0),
        right_arm=(-6, 0, 5), right_forearm=(-4, 0, 0), left_arm=(44, 0, -46), left_forearm=(-16, 0, 0),
        right_leg=(30, 0, 5), right_shin=(66, 0, 0), left_leg=(-80, 0, -8), left_shin=(86, 0, 4),
        cape=(30, 0, 0), cape_mid=(4, 0, 0), cape_lower=(2, 0, 0)))
    hold = merge(impact, lean(58), pose(
        head=(-6, 0, 0), right_arm=(-7, 0, 5),
        left_arm=(34, 0, -36), left_forearm=(-20, 0, 0),
        cape=(5, 0, 0), cape_mid=(-12, 0, 0), cape_lower=(-14, 0, 0)))
    # the weight sits on the planted fist: the right shoulder drops a little
    impact = _nudge(impact, 'right_arm', (0, -1.2, 0))
    hold = _nudge(hold, 'right_arm', (0, -1.2, 0))
    push = merge(lean(34), pose(
        head=(-6, 0, 0),
        right_arm=(-6, 0, 12), right_forearm=(-14, 0, 0), left_arm=(22, 0, -20), left_forearm=(-18, 0, 0),
        right_leg=(4, 0, 4), right_shin=(84, 0, -1), left_leg=(-56, 0, -6), left_shin=(62, 0, 4),
        cape=(18, 0, 0), cape_mid=(-4, 0, 0), cape_lower=(-6, 0, 0)))
    swing = merge(lean(20), pose(
        head=(-7, 0, 0),
        right_arm=(-2, 0, 10), right_forearm=(-18, 0, 0), left_arm=(14, 0, -14), left_forearm=(-16, 0, 0),
        right_leg=(-46, 0, 4), right_shin=(84, 0, -1), left_leg=(-28, 0, -5), left_shin=(34, 0, 4),
        cape=(12, 0, 0), cape_mid=(-2, 0, 0), cape_lower=(-2, 0, 0)))
    step = merge(lean(10), pose(
        head=(-8, 0, 0),
        right_arm=(4, 0, 9), right_forearm=(-16, 0, 0), left_arm=(8, 0, -10), left_forearm=(-14, 0, 0),
        right_leg=(-20, 0, 3), right_shin=(25, 0, -2), left_leg=(-20, 0, -3), left_shin=(25, 0, 2),
        cape=(8, 0, 0), cape_mid=(2, 0, 0), cape_lower=(2, 0, 0)))
    # rises a little prouder than the stance (chest out) before settling
    proud = merge(STANCE, lean(-4), pose(head=(-6, 0, 0), right_arm=(5, 0, 8), left_arm=(5, 0, -8),
                                         cape=(6, 0, 0), cape_mid=(3, 0, 0), cape_lower=(3, 0, 0)))
    return contact, impact, hold, push, swing, step, proud


def land():
    """Superhero landing: knee and fist hit the ground, a beat with the glare up, then a heavy rise."""
    b = ClipBuilder('pousar', 1.0, 'once')
    contact, impact, hold, push, swing, step, proud = _land_poses()
    foot_z = rig().point('left_shin', SOLE, complete({}))[2]   # the left foot never slides
    poses = [contact, impact, hold, push, swing, step, proud, STANCE]
    times = [0.0, 0.07, 0.4, 0.58, 0.68, 0.78, 0.9, 1.0]
    for t in (0.0, 0.035, 0.07, 0.16, 0.26, 0.34, 0.4, 0.46, 0.52, 0.58, 0.63, 0.68, 0.73, 0.78, 0.84, 0.9, 0.95, 1.0):
        p = _spline(poses, times, t)
        b.key(t, _with_tabards(_plant(p, z=foot_z), -5 + 2 * min(1.0, max(0.0, (t - 0.4) / 0.4))))
    return b.build()
