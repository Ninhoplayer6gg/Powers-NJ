package com.powersnj.core.animation;

/**
 * How a channel moves into a keyframe (the mode of the destination keyframe decides the segment),
 * matching Bedrock animation files: {@code "lerp_mode": "catmullrom"} or a plain/pre-post value.
 */
public enum Interpolation {
    LINEAR,
    CATMULLROM,
    /** Holds the previous value until the keyframe, then jumps. */
    STEP
}
