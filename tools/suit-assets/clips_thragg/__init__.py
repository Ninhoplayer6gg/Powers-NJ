"""
Thragg's animation clips (one module per group). build_all() returns them in a stable order.
"""
from . import combat, flight, locomotion, powers


def build_all():
    return [
        locomotion.idle(), locomotion.walk(), locomotion.run(), locomotion.crouch(), locomotion.guard(), locomotion.jump(),
        flight.takeoff(), flight.hover(), flight.fly(), flight.fast_flight(), flight.land(),
        combat.punch_right(), combat.punch_left(), combat.kick(), combat.combo(), combat.uppercut(),
        powers.charge(), powers.ground_slam(), powers.shockwave(), powers.recoil(), powers.victory(),
    ]
