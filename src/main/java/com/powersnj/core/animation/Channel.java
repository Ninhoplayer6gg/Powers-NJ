package com.powersnj.core.animation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A sampled property (rotation or position) of one bone. Sampling follows the Bedrock/GeckoLib
 * conventions and is kept identical to {@code tools/suit-assets/suitlib/clips.py}:
 * <ul>
 *     <li>the destination keyframe's {@link Interpolation} decides each segment;</li>
 *     <li>linear goes from the previous {@code post} to the destination {@code pre};</li>
 *     <li>Catmull-Rom is uniform and uses the neighbouring keyframes, clamped at the ends;</li>
 *     <li>before the first keyframe its {@code pre} holds, after the last one its {@code post}.</li>
 * </ul>
 */
public final class Channel {

    private final Keyframe[] keys;

    public Channel(List<Keyframe> keyframes) {
        if (keyframes.isEmpty()) {
            throw new IllegalArgumentException("A channel needs at least one keyframe");
        }
        List<Keyframe> sorted = new ArrayList<>(keyframes);
        sorted.sort(Comparator.comparingDouble(Keyframe::time));
        this.keys = sorted.toArray(new Keyframe[0]);
    }

    public int size() {
        return this.keys.length;
    }

    public Keyframe key(int index) {
        return this.keys[index];
    }

    public double lastTime() {
        return this.keys[this.keys.length - 1].time();
    }

    /**
     * Writes the value at {@code time} into {@code out[offset..offset+2]}.
     */
    public void sample(double time, float[] out, int offset) {
        Keyframe first = this.keys[0];
        if (time <= first.time()) {
            copy(first.pre(), out, offset);
            return;
        }
        Keyframe last = this.keys[this.keys.length - 1];
        if (time >= last.time()) {
            copy(last.post(), out, offset);
            return;
        }
        int index = 1;
        while (this.keys[index].time() <= time) {
            index++;
        }
        Keyframe a = this.keys[index - 1];
        Keyframe b = this.keys[index];
        double span = b.time() - a.time();
        float f = span <= 0 ? 0F : (float) ((time - a.time()) / span);
        switch (b.mode()) {
            case STEP -> copy(a.post(), out, offset);
            case CATMULLROM -> {
                float[] p0 = index >= 2 ? this.keys[index - 2].post() : a.post();
                float[] p3 = index + 1 < this.keys.length ? this.keys[index + 1].pre() : b.pre();
                for (int i = 0; i < 3; i++) {
                    out[offset + i] = catmullRom(p0[i], a.post()[i], b.pre()[i], p3[i], f);
                }
            }
            default -> {
                for (int i = 0; i < 3; i++) {
                    out[offset + i] = a.post()[i] + (b.pre()[i] - a.post()[i]) * f;
                }
            }
        }
    }

    static float catmullRom(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return 0.5F * (2F * p1 + (-p0 + p2) * t + (2F * p0 - 5F * p1 + 4F * p2 - p3) * t2 + (-p0 + 3F * p1 - 3F * p2 + p3) * t3);
    }

    private static void copy(float[] value, float[] out, int offset) {
        out[offset] = value[0];
        out[offset + 1] = value[1];
        out[offset + 2] = value[2];
    }
}
