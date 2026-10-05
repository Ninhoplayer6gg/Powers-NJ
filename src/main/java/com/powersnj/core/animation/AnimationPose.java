package com.powersnj.core.animation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Blended output of a {@link SuitAnimator} for one frame. Every contribution is premultiplied by
 * its weight, so cross-fades and layers compose exactly:
 * <ul>
 *     <li>override parts ({@link HumanoidBones#OVERRIDE_PARTS}): {@code lerp(vanilla, normalized(), weight())};</li>
 *     <li>head and root: {@link #accumulated} is added to the vanilla look / render pose;</li>
 *     <li>suit bones: {@link #accumulated} is the local transform on top of the rest pose.</li>
 * </ul>
 * Values: rotation x, y, z in degrees then position x, y, z in pixels (Bedrock conventions).
 */
public final class AnimationPose {

    public static final int SIZE = 6;

    private static final class Slot {
        float weight;
        final float[] sum = new float[SIZE];
    }

    private final Map<String, Slot> slots = new HashMap<>();
    private float lookSuppression;
    private float steering;
    private float crouchCompensation;
    private float master;

    public void clear() {
        for (Slot slot : this.slots.values()) {
            slot.weight = 0F;
            java.util.Arrays.fill(slot.sum, 0F);
        }
        this.lookSuppression = 0F;
        this.steering = 0F;
        this.crouchCompensation = 0F;
        this.master = 0F;
    }

    void add(String bone, float[] value, float factor) {
        if (factor <= 0F) {
            return;
        }
        Slot slot = this.slots.computeIfAbsent(bone, b -> new Slot());
        slot.weight += factor;
        for (int i = 0; i < SIZE; i++) {
            slot.sum[i] += value[i] * factor;
        }
    }

    /**
     * Scales what was accumulated for {@code bone} so an override layer can be laid on top.
     */
    void fade(String bone, float factor) {
        Slot slot = this.slots.get(bone);
        if (slot != null) {
            slot.weight *= factor;
            for (int i = 0; i < SIZE; i++) {
                slot.sum[i] *= factor;
            }
        }
    }

    void addLookSuppression(float amount) {
        this.lookSuppression += amount;
    }

    void addSteering(float amount) {
        this.steering += amount;
    }

    void addCrouchCompensation(float amount) {
        this.crouchCompensation += amount;
    }

    void setMaster(float master) {
        this.master = master;
    }

    public Set<String> bones() {
        return this.slots.keySet();
    }

    public boolean has(String bone) {
        Slot slot = this.slots.get(bone);
        return slot != null && slot.weight > 1.0E-4F;
    }

    /** Total weight of a bone, clamped to 1. */
    public float weight(String bone) {
        Slot slot = this.slots.get(bone);
        return slot == null ? 0F : Math.min(1F, slot.weight);
    }

    /** Weighted average of the contributions (the target pose of an override part). */
    public void normalized(String bone, float[] out) {
        Slot slot = this.slots.get(bone);
        if (slot == null || slot.weight <= 1.0E-6F) {
            java.util.Arrays.fill(out, 0, SIZE, 0F);
            return;
        }
        for (int i = 0; i < SIZE; i++) {
            out[i] = slot.sum[i] / slot.weight;
        }
    }

    /** Sum of weighted contributions (additive bones and suit bones). */
    public void accumulated(String bone, float[] out) {
        Slot slot = this.slots.get(bone);
        if (slot == null) {
            java.util.Arrays.fill(out, 0, SIZE, 0F);
            return;
        }
        System.arraycopy(slot.sum, 0, out, 0, SIZE);
    }

    /** 1 = the head follows the player's look, 0 = the clip sets the head (fast flight). */
    public float lookWeight() {
        return 1F - Math.min(1F, this.lookSuppression);
    }

    /** How much the body orientation follows the look pitch / banking (fly states). */
    public float steering() {
        return Math.min(1F, this.steering);
    }

    /** How much of vanilla's crouch render offset the clips already account for. */
    public float crouchCompensation() {
        return Math.min(1F, this.crouchCompensation);
    }

    /** Global fade of the suit animations (0 = vanilla). */
    public float master() {
        return this.master;
    }
}
