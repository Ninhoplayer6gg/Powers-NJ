"""Thragg power clips: charge, ground slam, shockwave clap, recoil, victory."""
from .common import *  # noqa: F401,F403
from .common import ClipBuilder, merge, pose, mirror, lean, tabards_for, wave, ground, STANCE, GUARD_ARMS, MIRROR, clips  # noqa: F401
import math  # noqa: E402

def charge():
    b = ClipBuilder('investida', 0.75, 'once')
    b.key(0.0, {})
    ram = pose(root=(22, 0, 0), root_pos=(0, 0, -1), head=(-22, 0, 0), body=(0, -22, 0),
               right_arm=(-22, -22, -6), right_forearm=(-120, 0, 0), left_arm=(44, 0, -12), left_forearm=(-50, 0, 0),
               right_leg=(-38, 0, 3), right_shin=(40, 0, -2), left_leg=(40, 0, -3), left_shin=(56, 0, 2),
               cape=(56, 0, 0), cape_mid=(12, 0, 0), cape_lower=(12, 0, 0))
    b.key(0.08, merge(ram, tabards_for(ram, -6)))
    b.key(0.34, merge(ram, tabards_for(ram, -6), pose(root=(23, 0, 1), cape=(60, 0, 3), cape_mid=(16, 0, 0), cape_lower=(18, 0, 0))))
    b.key(0.6, merge(ram, tabards_for(ram, -6), pose(root=(21, 0, -1), cape=(54, 0, -3), cape_mid=(10, 0, 0), cape_lower=(10, 0, 0))))
    b.key(0.75, {})
    return b.build()


def ground_slam():
    b = ClipBuilder('impacto_solo', 1.1, 'once')
    b.key(0.0, {})
    raise_ = pose(root_pos=(0, 0.3, 0), body=(-8, 0, 0), head=(-10, 0, 0),
                  right_arm=(-164, 0, 12), left_arm=(-164, 0, -12), right_forearm=(-40, 0, 0), left_forearm=(-40, 0, 0),
                  right_shin=(8, 0, 0), left_shin=(8, 0, 0), cape=(6, 0, 0))
    b.key(0.06, merge(raise_, tabards_for(raise_)))
    slam = merge(lean(55), pose(root_pos=(0, -6.2, 0), head=(-34, 0, 0),
                                right_arm=(-52, -6, 8), left_arm=(-52, 6, -8), right_forearm=(-12, 0, 0), left_forearm=(-12, 0, 0),
                                right_leg=(-70, 0, 8), left_leg=(-64, 0, -8), right_shin=(96, 0, -4), left_shin=(90, 0, 4),
                                cape=(62, 0, 0), cape_mid=(16, 0, 0), cape_lower=(16, 0, 0)))
    b.key(0.13, merge(slam, tabards_for(slam, -6)))
    b.key(0.45, merge(slam, tabards_for(slam, -6), pose(cape=(30, 0, 0), cape_mid=(6, 0, 0), cape_lower=(4, 0, 0))))
    rise = merge(lean(20), pose(root_pos=(0, -2.2, 0), head=(-12, 0, 0), right_arm=(-10, 0, 12), left_arm=(-10, 0, -12),
                                right_forearm=(-30, 0, 0), left_forearm=(-30, 0, 0), right_leg=(-24, 0, 4), left_leg=(-22, 0, -4),
                                right_shin=(44, 0, 0), left_shin=(42, 0, 0), cape=(16, 0, 0)))
    b.key(0.78, merge(rise, tabards_for(rise, -4)))
    b.key(1.1, {})
    return b.build()


def shockwave():
    """Thunder clap: arms open wide, the clap lands at 0.14 s."""
    b = ClipBuilder('onda_de_choque', 0.95, 'once')
    b.key(0.0, {})
    b.key(0.07, pose(root_pos=(0, 0.3, 0), body=(-10, 0, 0), head=(-6, 0, 0), right_arm=(-68, 6, 72), left_arm=(-68, -6, -72),
                     right_forearm=(-12, 0, 0), left_forearm=(-12, 0, 0), cape=(8, 0, 0)))
    clap = pose(root_pos=(0, -1.6, 0), body=(8, 0, 0), head=(2, 0, 0),
                right_arm=(-86, -17, 0), left_arm=(-86, 17, 0), right_forearm=(-6, 0, 0), left_forearm=(-6, 0, 0),
                right_leg=(-6, 0, 10), left_leg=(-6, 0, -10), right_shin=(16, 0, -8), left_shin=(16, 0, 8),
                cape=(30, 0, 0), cape_mid=(10, 0, 0), cape_lower=(12, 0, 0))
    b.key(0.14, merge(clap, tabards_for(clap, -6)))
    b.key(0.45, merge(clap, tabards_for(clap, -6), pose(cape=(16, 0, 0), cape_mid=(4, 0, 0), cape_lower=(4, 0, 0))))
    b.key(0.95, {})
    return b.build()


def recoil():
    b = ClipBuilder('recuar', 0.65, 'once')
    b.key(0.0, {})
    hit = pose(root=(-10, 0, 0), root_pos=(0, 0, 1.6), body=(-12, 0, 0), head=(-14, 0, 0),
               right_arm=(-34, 0, 30), left_arm=(-30, 0, -28), right_forearm=(-40, 0, 0), left_forearm=(-40, 0, 0),
               right_leg=(18, 0, 4), right_shin=(20, 0, -2), left_leg=(-8, 0, -3), left_shin=(12, 0, 2),
               cape=(-4, 0, 0), cape_mid=(-6, 0, 0), cape_lower=(-6, 0, 0))
    b.key(0.07, merge(hit, tabards_for(hit)))
    brace = merge(GUARD_ARMS, pose(root=(-2, 0, 0), root_pos=(0, -0.6, 0.6), body=(6, 0, 0), head=(4, 0, 0),
                                   right_leg=(10, 0, 4), left_leg=(-10, 0, -3), right_shin=(16, 0, -2), left_shin=(12, 0, 2),
                                   cape=(14, 0, 0), cape_mid=(6, 0, 0), cape_lower=(6, 0, 0)))
    b.key(0.28, merge(brace, tabards_for(brace)))
    b.key(0.65, {})
    return b.build()


def victory():
    b = ClipBuilder('vitoria', 2.6, 'once')
    b.key(0.0, {})
    triumph = pose(root_pos=(0, 0.3, 0), body=(-8, 0, 0), head=(-18, 8, 0),
                   right_arm=(-172, 0, 12), right_forearm=(-8, 0, 0), left_arm=(14, 40, -36), left_forearm=(-96, 0, 0),
                   right_leg=(0, 0, 7), left_leg=(0, 0, -7), right_shin=(2, 0, -6), left_shin=(2, 0, 6),
                   cape=(24, 0, 0), cape_mid=(8, 0, 0), cape_lower=(8, 0, 0))
    b.key(0.35, merge(triumph, tabards_for(triumph)))
    b.key(1.2, merge(triumph, tabards_for(triumph), pose(root_pos=(0, 0.45, 0), cape=(28, 0, 2), cape_mid=(12, 0, 0), cape_lower=(12, 0, 0))))
    b.key(1.95, merge(triumph, tabards_for(triumph), pose(right_arm=(-168, 0, 13), cape=(22, 0, -2), cape_mid=(8, 0, 0), cape_lower=(8, 0, 0))))
    b.key(2.6, {})
    return b.build()
