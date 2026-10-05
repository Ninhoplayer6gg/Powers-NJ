"""Thragg melee clips: punches, kick, combo (heavy punch), uppercut (devastating punch)."""
from .common import *  # noqa: F401,F403
from .common import ClipBuilder, merge, pose, mirror, lean, tabards_for, wave, ground, STANCE, GUARD_ARMS, MIRROR, clips  # noqa: F401
import math  # noqa: E402

def punch_right():
    b = ClipBuilder('soco_direito', 0.48, 'once')
    b.key(0.0, {})
    b.key(0.06, merge(GUARD_ARMS, pose(body=(2, 12, 0), right_arm=(-48, 4, 10), right_forearm=(-112, 0, 0))))
    impact = merge(pose(root=(0, -6, 0), root_pos=(0, -0.4, -0.6), body=(6, -22, 0),
                        right_arm=(-90, -10, 2), right_forearm=(-4, 0, 0), left_arm=(-46, 22, -6), left_forearm=(-116, 0, 0),
                        right_leg=(10, 0, 3), left_leg=(-12, 0, -3), right_shin=(6, 0, -2), left_shin=(10, 0, 2),
                        cape=(10, 0, 7), cape_mid=(4, 0, 0)))
    b.key(0.12, merge(impact, tabards_for(impact)))
    b.key(0.24, merge(impact, tabards_for(impact), pose(right_arm=(-86, -8, 3), right_forearm=(-14, 0, 0), body=(5, -18, 0))))
    b.key(0.48, {})
    return b.build()


def punch_left():
    b = ClipBuilder('soco_esquerdo', 0.48, 'once')
    right = punch_right()
    for bone, channels in right.bones.items():
        target = MIRROR.get(bone, bone)
        for channel, keys in channels.items():
            for key in keys:
                x, y, z = key.post
                value = [-x, y, z] if channel == 'position' else [x, -y, -z]
                b.clip.add(target, channel, clips.Key(key.time, value, mode=key.mode))
    return b.clip


def kick():
    b = ClipBuilder('chute', 0.66, 'once')
    b.key(0.0, {})
    chamber = merge(GUARD_ARMS, pose(body=(-6, 0, 0), right_leg=(-74, 0, 4), right_shin=(96, 0, 0), left_leg=(2, 0, -3), left_shin=(8, 0, 0),
                                     right_arm=(-40, -10, 16), left_arm=(-44, 10, -16), cape=(12, 0, 0)))
    b.key(0.1, merge(chamber, tabards_for(chamber, -6)))
    extend = pose(root=(-5, 0, 0), body=(-14, 0, 0), head=(6, 0, 0),
                  right_leg=(-88, 0, 3), right_shin=(2, 0, 0), left_leg=(4, 0, -3), left_shin=(10, 0, 0),
                  right_arm=(-16, 0, 36), left_arm=(-26, 0, -32), right_forearm=(-30, 0, 0), left_forearm=(-40, 0, 0),
                  cape=(22, 0, 0), cape_mid=(8, 0, 0), cape_lower=(10, 0, 0))
    b.key(0.2, merge(extend, tabards_for(extend, -8)))
    b.key(0.32, merge(extend, tabards_for(extend, -8), pose(right_leg=(-84, 0, 3), right_shin=(8, 0, 0))))
    b.key(0.46, merge(chamber, tabards_for(chamber, -6), pose(right_leg=(-62, 0, 4), right_shin=(84, 0, 0))))
    b.key(0.66, {})
    return b.build()


def combo():
    """Heavy punch ability: the big right straight lands at 0.12 s, a left hook follows."""
    b = ClipBuilder('combo', 1.0, 'once')
    b.key(0.0, {})
    b.key(0.05, merge(GUARD_ARMS, pose(body=(2, 14, 0), right_arm=(-40, 4, 12), right_forearm=(-118, 0, 0))))
    cross = pose(root=(0, -8, 0), root_pos=(0, -0.6, -1.2), body=(10, -28, 0), head=(4, 0, 0),
                 right_arm=(-92, -12, 2), right_forearm=(0, 0, 0), left_arm=(-48, 24, -6), left_forearm=(-118, 0, 0),
                 right_leg=(16, 0, 3), left_leg=(-18, 0, -3), right_shin=(8, 0, -2), left_shin=(14, 0, 2),
                 cape=(18, 0, 8), cape_mid=(6, 0, 0), cape_lower=(8, 0, 0))
    b.key(0.12, merge(cross, tabards_for(cross)))
    b.key(0.22, merge(cross, tabards_for(cross), pose(right_arm=(-88, -10, 4), right_forearm=(-12, 0, 0))))
    hook = pose(root=(0, 4, 0), root_pos=(0, -0.5, -0.8), body=(8, 26, 0), head=(4, 0, 0),
                right_arm=(-52, -24, 6), right_forearm=(-110, 0, 0), left_arm=(-84, 44, -16), left_forearm=(-82, 0, 0),
                right_leg=(10, 0, 3), left_leg=(-14, 0, -3), right_shin=(8, 0, -2), left_shin=(12, 0, 2),
                cape=(16, 0, -8), cape_mid=(6, 0, 0), cape_lower=(8, 0, 0))
    b.key(0.42, merge(hook, tabards_for(hook)))
    b.key(0.62, merge(GUARD_ARMS, pose(body=(4, 0, 0), right_leg=(6, 0, 3), left_leg=(-8, 0, -3), cape=(10, 0, 0))))
    b.key(1.0, {})
    return b.build()


def uppercut():
    """Devastating punch: dip, then the rising uppercut lands at 0.14 s."""
    b = ClipBuilder('uppercut', 0.95, 'once')
    b.key(0.0, {})
    dip = merge(lean(14), pose(root_pos=(0, -2.4, 0), body=(14, 12, 0), right_arm=(14, 0, 12), right_forearm=(-116, 0, 0),
                                left_arm=(-48, 22, -6), left_forearm=(-110, 0, 0),
                                right_leg=(-26, 0, 4), left_leg=(-20, 0, -4), right_shin=(46, 0, -2), left_shin=(40, 0, 2)))
    b.key(0.06, merge(dip, tabards_for(dip, -4)))
    strike = merge(lean(-12), pose(root=(-5, 0, 0), root_pos=(0, 1.2, 0), body=(-12, -24, 0), head=(-18, 0, 0),
                                   right_arm=(-170, -10, 4), right_forearm=(-34, 0, 0), left_arm=(34, 0, -12), left_forearm=(-40, 0, 0),
                                   right_leg=(8, 0, 3), left_leg=(-6, 0, -3), right_shin=(4, 0, -2), left_shin=(6, 0, 2),
                                   cape=(34, 0, 4), cape_mid=(10, 0, 0), cape_lower=(12, 0, 0)))
    b.key(0.14, merge(strike, tabards_for(strike, -14)))
    b.key(0.4, merge(strike, tabards_for(strike, -10), pose(root_pos=(0, 0.8, 0), right_arm=(-164, -8, 5), right_forearm=(-42, 0, 0),
                                                           cape=(20, 0, 2), cape_mid=(8, 0, 0))))
    b.key(0.95, {})
    return b.build()
