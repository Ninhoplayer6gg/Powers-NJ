package com.powersnj.core.animation;

/**
 * Base layer states of {@link SuitAnimator}. Each maps to a role of the {@link AnimationSet}.
 */
public enum LocomotionState {
    /** Standing: idle blended with walk/run by the limb swing. */
    GROUND,
    CROUCH,
    GUARD,
    JUMP,
    TAKEOFF,
    HOVER,
    FLY,
    FAST_FLIGHT,
    LAND;

    public boolean isFlight() {
        return this == TAKEOFF || this == HOVER || this == FLY || this == FAST_FLIGHT;
    }

    public boolean isGrounded() {
        return this == GROUND || this == CROUCH || this == GUARD || this == LAND;
    }

    /** States whose body orientation follows the look direction (fast, horizontal flight). */
    public boolean steers() {
        return this == FLY || this == FAST_FLIGHT;
    }
}
