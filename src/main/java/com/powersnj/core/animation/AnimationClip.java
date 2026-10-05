package com.powersnj.core.animation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A named animation (one entry of a Bedrock {@code .animation.json}).
 */
public final class AnimationClip {

    private final String name;
    private final double length;
    private final LoopMode loop;
    private final Map<String, BoneTrack> bones;

    public AnimationClip(String name, double length, LoopMode loop, Map<String, BoneTrack> bones) {
        this.name = name;
        this.length = Math.max(0D, length);
        this.loop = loop;
        this.bones = Collections.unmodifiableMap(new LinkedHashMap<>(bones));
    }

    public String name() {
        return this.name;
    }

    public double length() {
        return this.length;
    }

    public LoopMode loop() {
        return this.loop;
    }

    public Map<String, BoneTrack> bones() {
        return this.bones;
    }

    /**
     * Clip time for an elapsed time since the start: wraps for loops, clamps otherwise.
     */
    public double localTime(double elapsed) {
        if (elapsed <= 0D) {
            return 0D;
        }
        if (this.loop == LoopMode.LOOP && this.length > 0D) {
            return elapsed % this.length;
        }
        return Math.min(elapsed, this.length);
    }

    /**
     * Whether a once-played clip has reached its end ({@link LoopMode#HOLD} and loops never finish).
     */
    public boolean isFinished(double elapsed) {
        return this.loop == LoopMode.ONCE && elapsed >= this.length;
    }
}
