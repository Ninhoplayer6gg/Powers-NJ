package com.powersnj.core.animation;

/**
 * Animated channels of one bone; either may be {@code null}. Scale channels are not used by suits.
 */
public record BoneTrack(Channel rotation, Channel position) {

    /**
     * Writes rotation (degrees) into {@code out[0..2]} and position (pixels) into {@code out[3..5]};
     * missing channels write zeros.
     */
    public void sample(double time, float[] out) {
        if (this.rotation != null) {
            this.rotation.sample(time, out, 0);
        } else {
            out[0] = out[1] = out[2] = 0F;
        }
        if (this.position != null) {
            this.position.sample(time, out, 3);
        } else {
            out[3] = out[4] = out[5] = 0F;
        }
    }
}
