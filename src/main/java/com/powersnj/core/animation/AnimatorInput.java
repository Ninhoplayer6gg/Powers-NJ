package com.powersnj.core.animation;

/**
 * What {@link SuitAnimator} needs to know about the wearer each tick.
 *
 * @param active        whether suit animations may play (full suit, not sleeping/riding/swimming/gliding...)
 * @param onGround      standing on a block
 * @param flying        a flight power is flying the entity
 * @param flightBoost   Palladium flight boost (0..3, &gt; 1 = horizontal sprint flight)
 * @param fastFlight    the flight is in a fast mode (boost / high speed)
 * @param sneaking      crouching
 * @param sprinting     sprinting
 * @param inCombat      hurt or attacked recently
 */
public record AnimatorInput(boolean active, boolean onGround, boolean flying, float flightBoost, boolean fastFlight,
                            boolean sneaking, boolean sprinting, boolean inCombat) {
}
