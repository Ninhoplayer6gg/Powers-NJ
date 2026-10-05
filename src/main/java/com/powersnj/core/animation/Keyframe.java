package com.powersnj.core.animation;

/**
 * One keyframe of a channel. {@code pre} is the value reached when arriving at {@link #time()},
 * {@code post} the value the next segment starts from (equal unless the file splits the keyframe).
 *
 * @param time seconds
 * @param pre  x, y, z (degrees for rotations, pixels for positions)
 * @param post x, y, z
 * @param mode interpolation of the segment that ends at this keyframe
 */
public record Keyframe(double time, float[] pre, float[] post, Interpolation mode) {

    public Keyframe {
        if (pre.length != 3 || post.length != 3) {
            throw new IllegalArgumentException("Keyframe values need 3 components");
        }
    }

    public static Keyframe of(double time, float x, float y, float z) {
        float[] value = {x, y, z};
        return new Keyframe(time, value, value.clone(), Interpolation.LINEAR);
    }
}
