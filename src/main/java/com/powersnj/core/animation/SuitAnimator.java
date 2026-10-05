package com.powersnj.core.animation;

import java.util.List;
import java.util.Map;

/**
 * Per-entity animation state machine of a suit (client side, engine agnostic).
 * <p>
 * Base layer: one {@link LocomotionState} at a time, cross-faded on changes. GROUND blends idle with
 * the walk/run cycles, whose phase follows the vanilla limb swing (t = 0: right leg back). CROUCH
 * and GUARD hand the body back to vanilla while moving. JUMP, TAKEOFF and LAND are transitions that
 * end on their own.
 * <p>
 * Action layer: one-shot clips (abilities, server events, melee swings) laid over the base on the
 * bones they animate, faded in/out. While the wearer moves, the {@link AnimationSet#lowerBody()}
 * bones stay with the locomotion so the legs keep walking.
 * <p>
 * {@link #tick} runs once per game tick with fresh inputs, {@link #sample} once per rendered frame.
 * Times are seconds of a client clock.
 */
public final class SuitAnimator {

    public static final double ACTION_FADE_IN = 0.06D;
    public static final double ACTION_FADE_OUT = 0.15D;
    public static final double INTERRUPT_FADE = 0.1D;
    public static final double MASTER_FADE = 0.25D;
    public static final double SPRINT_FADE = 0.2D;
    /** Airtime after which touching the ground plays the landing. */
    public static final double LONG_FALL = 0.9D;
    /** Walking off a ledge for this long switches to the airborne pose. */
    public static final double LEDGE_FALL = 0.35D;
    /** Where the airborne pose starts in the jump clip when there was no jump. */
    public static final double FALL_POSE_TIME = 0.45D;
    /** A flight started this soon after leaving the ground still plays the takeoff. */
    public static final double TAKEOFF_GRACE = 0.4D;
    /** Limb swing to walk-cycle phase: vanilla legs swing with cos(limbSwing * 0.6662). */
    public static final double LIMB_SWING_TO_PHASE = 0.6662D / (2D * Math.PI);
    /** Limb swing amount at which the walk cycle fully replaces idle. */
    public static final float FULL_MOVE = 0.45F;
    public static final int PRIORITY_MELEE = 1;
    public static final int PRIORITY_ACTION = 2;

    private record Playback(AnimationClip clip, double start, int priority) {
    }

    private final AnimationSet set;
    private final float[] scratch = new float[AnimationPose.SIZE];

    private LocomotionState state = LocomotionState.GROUND;
    private double stateStart;
    private double stateOffset;
    private LocomotionState previous;
    private double previousStart;
    private double previousOffset;
    private double transitionStart = Double.NEGATIVE_INFINITY;
    private double transitionLength = 0.2D;

    private Playback action;
    private Playback fading;
    private double fadingSince;

    private int meleeIndex;
    private double lastMelee = Double.NEGATIVE_INFINITY;

    private double lastTick = Double.NaN;
    private float master;
    private float sprintBlend;
    private boolean wasFlying;
    private boolean wasOnGround = true;
    private double airborneSince = Double.NaN;
    private double lastAirtime;

    public SuitAnimator(AnimationSet set) {
        this.set = set;
    }

    public AnimationSet set() {
        return this.set;
    }

    public LocomotionState state() {
        return this.state;
    }

    public String currentAction() {
        return this.action == null ? "" : this.action.clip().name();
    }

    public float master() {
        return this.master;
    }

    // ------------------------------------------------------------------------------------- update

    public void tick(AnimatorInput in, double now) {
        double dt = Double.isNaN(this.lastTick) ? 0.05D : Math.max(0D, Math.min(0.25D, now - this.lastTick));
        this.lastTick = now;
        this.master = approach(this.master, in.active() ? 1F : 0F, (float) (dt / MASTER_FADE));
        this.sprintBlend = approach(this.sprintBlend, in.sprinting() ? 1F : 0F, (float) (dt / SPRINT_FADE));

        boolean landed = false;
        if (in.onGround()) {
            if (!this.wasOnGround && !Double.isNaN(this.airborneSince)) {
                this.lastAirtime = now - this.airborneSince;
                landed = true;
            }
            this.airborneSince = Double.NaN;
        } else if (Double.isNaN(this.airborneSince)) {
            this.airborneSince = now;
        }
        double airtime = Double.isNaN(this.airborneSince) ? 0D : now - this.airborneSince;

        Decision next = this.decide(in, now, airtime, landed);
        if (next.state != this.state) {
            this.transition(next.state, now, next.offset);
        }
        this.wasFlying = in.flying();
        this.wasOnGround = in.onGround();

        if (this.action != null && this.action.clip().isFinished(now - this.action.start())) {
            this.action = null;
        }
        if (this.fading != null && (now - this.fadingSince >= INTERRUPT_FADE || this.fading.clip().isFinished(now - this.fading.start()))) {
            this.fading = null;
        }
    }

    private record Decision(LocomotionState state, double offset) {
    }

    private Decision decide(AnimatorInput in, double now, double airtime, boolean landed) {
        if (in.flying()) {
            if (!this.wasFlying) {
                boolean fromGround = this.state.isGrounded() || (this.state == LocomotionState.JUMP && now - this.stateStart < TAKEOFF_GRACE);
                if (fromGround && this.has(LocomotionState.TAKEOFF)) {
                    return new Decision(LocomotionState.TAKEOFF, 0D);
                }
                return new Decision(this.flightState(in), 0D);
            }
            if (this.state == LocomotionState.TAKEOFF && in.flightBoost() <= 1F
                    && !this.set.state(LocomotionState.TAKEOFF).isFinished(now - this.stateStart)) {
                return new Decision(LocomotionState.TAKEOFF, 0D);
            }
            return new Decision(this.flightState(in), 0D);
        }
        if (this.wasFlying) {
            if (in.onGround()) {
                return this.has(LocomotionState.LAND) ? new Decision(LocomotionState.LAND, 0D) : new Decision(this.groundState(in), 0D);
            }
            return new Decision(LocomotionState.JUMP, FALL_POSE_TIME);
        }
        if (!in.onGround()) {
            if (this.state == LocomotionState.JUMP) {
                return new Decision(LocomotionState.JUMP, this.stateOffset);
            }
            if (airtime >= LEDGE_FALL && this.has(LocomotionState.JUMP)) {
                return new Decision(LocomotionState.JUMP, FALL_POSE_TIME);
            }
            return new Decision(this.state.isFlight() ? LocomotionState.JUMP : this.state, this.state.isFlight() ? FALL_POSE_TIME : this.stateOffset);
        }
        if (landed && this.lastAirtime >= LONG_FALL && this.has(LocomotionState.LAND)) {
            return new Decision(LocomotionState.LAND, 0D);
        }
        if (this.state == LocomotionState.LAND && !this.set.state(LocomotionState.LAND).isFinished(now - this.stateStart) && !in.sneaking()) {
            return new Decision(LocomotionState.LAND, 0D);
        }
        return new Decision(this.groundState(in), 0D);
    }

    private LocomotionState groundState(AnimatorInput in) {
        if (in.sneaking()) {
            if (in.inCombat() && this.has(LocomotionState.GUARD)) {
                return LocomotionState.GUARD;
            }
            return LocomotionState.CROUCH;
        }
        return LocomotionState.GROUND;
    }

    private LocomotionState flightState(AnimatorInput in) {
        if (in.flightBoost() > 1F) {
            boolean fast = in.fastFlight() || in.flightBoost() > 2.2F;
            if (fast && this.has(LocomotionState.FAST_FLIGHT)) {
                return LocomotionState.FAST_FLIGHT;
            }
            if (this.has(LocomotionState.FLY)) {
                return LocomotionState.FLY;
            }
        }
        return LocomotionState.HOVER;
    }

    private boolean has(LocomotionState state) {
        return this.set.state(state) != null;
    }

    private void transition(LocomotionState next, double now, double offset) {
        this.previous = this.state;
        this.previousStart = this.stateStart;
        this.previousOffset = this.stateOffset;
        this.state = next;
        this.stateStart = now;
        this.stateOffset = offset;
        this.transitionStart = now;
        this.transitionLength = switch (next) {
            case LAND -> 0.06D;
            case JUMP -> 0.1D;
            case TAKEOFF -> 0.12D;
            default -> this.set.fade();
        };
    }

    // ------------------------------------------------------------------------------------- events

    /** The wearer jumped (left the ground upwards without flying). */
    public void onJump(double now) {
        if (!this.state.isFlight() && this.has(LocomotionState.JUMP)) {
            this.transition(LocomotionState.JUMP, now, 0D);
        }
    }

    /**
     * An empty-handed attack swing: plays the next clip of the melee chain (or the after-sprint
     * clip). Never interrupts an ability/event action.
     */
    public boolean onSwing(boolean sprintedRecently, double now) {
        String name;
        if (sprintedRecently && this.set.clip(this.set.meleeAfterSprint()) != null) {
            name = this.set.meleeAfterSprint();
            this.meleeIndex = 0;
        } else {
            List<String> chain = this.set.meleeChain();
            if (chain.isEmpty()) {
                return false;
            }
            if (now - this.lastMelee > this.set.meleeWindow()) {
                this.meleeIndex = 0;
            }
            name = chain.get(this.meleeIndex % chain.size());
            this.meleeIndex++;
        }
        this.lastMelee = now;
        return this.start(this.set.clip(name), now, PRIORITY_MELEE);
    }

    /**
     * Plays a clip by name or a named event ({@code "#heavy_hit"} → {@link AnimationSet#events()}).
     *
     * @return false when the set has no such clip
     */
    public boolean play(String key, double now) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        String name = key.startsWith("#") ? this.set.events().get(key.substring(1)) : key;
        return this.start(this.set.clip(name), now, PRIORITY_ACTION);
    }

    private boolean start(AnimationClip clip, double now, int priority) {
        if (clip == null) {
            return false;
        }
        if (this.action != null) {
            double elapsed = now - this.action.start();
            boolean protectedAction = this.action.priority() > priority && elapsed < this.action.clip().length() - ACTION_FADE_OUT;
            if (protectedAction) {
                return false;
            }
            this.fading = this.action;
            this.fadingSince = now;
        }
        this.action = new Playback(clip, now, priority);
        return true;
    }

    // ------------------------------------------------------------------------------------- sample

    /**
     * Blends the current frame into {@code out} (cleared first).
     *
     * @param limbSwing       vanilla limb swing position (walk cycle phase)
     * @param limbSwingAmount vanilla limb swing amount (0 standing .. 1 running)
     */
    public void sample(double now, float limbSwing, float limbSwingAmount, AnimationPose out) {
        out.clear();
        out.setMaster(this.master);
        if (this.master <= 0F) {
            return;
        }
        float move = smoothstep(Math.min(1F, Math.max(0F, limbSwingAmount / FULL_MOVE)));
        float t = this.transitionLength <= 0D ? 1F : smoothstep((float) Math.min(1D, Math.max(0D, (now - this.transitionStart) / this.transitionLength)));
        if (t < 1F && this.previous != null) {
            this.sampleState(this.previous, this.previousStart, this.previousOffset, now, limbSwing, move, (1F - t) * this.master, out);
        }
        this.sampleState(this.state, this.stateStart, this.stateOffset, now, limbSwing, move, t * this.master, out);
        if (this.fading != null) {
            float envelope = this.envelope(this.fading, now) * (float) Math.max(0D, 1D - (now - this.fadingSince) / INTERRUPT_FADE);
            this.layer(this.fading, now, envelope * this.master, move, out);
        }
        if (this.action != null) {
            this.layer(this.action, now, this.envelope(this.action, now) * this.master, move, out);
        }
    }

    private void sampleState(LocomotionState s, double start, double offset, double now, float limbSwing, float move, float factor, AnimationPose out) {
        if (factor <= 0F) {
            return;
        }
        double elapsed = now - start;
        switch (s) {
            case GROUND -> {
                AnimationClip idle = this.set.state(LocomotionState.GROUND);
                AnimationClip walk = this.set.walk();
                AnimationClip run = this.set.run();
                float moving = (walk != null || run != null) ? move : 0F;
                if (idle != null) {
                    this.add(idle, idle.localTime(elapsed), factor * (1F - moving), out);
                }
                double phase = frac(limbSwing * LIMB_SWING_TO_PHASE);
                float runWeight = run == null ? 0F : (walk == null ? 1F : this.sprintBlend);
                if (walk != null) {
                    this.add(walk, phase * walk.length(), factor * moving * (1F - runWeight), out);
                }
                if (run != null) {
                    this.add(run, phase * run.length(), factor * moving * runWeight, out);
                }
            }
            case CROUCH, GUARD -> {
                AnimationClip clip = this.set.state(s);
                if (clip == null && s == LocomotionState.GUARD) {
                    clip = this.set.state(LocomotionState.CROUCH);
                }
                if (clip != null) {
                    float yield = factor * (1F - move);
                    this.add(clip, clip.localTime(elapsed), yield, out);
                    out.addCrouchCompensation(yield);
                }
            }
            default -> {
                AnimationClip clip = this.set.state(s);
                if (clip != null) {
                    this.add(clip, clip.localTime(offset + elapsed), factor, out);
                    if (s.steers()) {
                        out.addSteering(factor);
                        out.addLookSuppression(factor);
                    }
                }
            }
        }
    }

    private void add(AnimationClip clip, double time, float factor, AnimationPose out) {
        if (factor <= 0F) {
            return;
        }
        for (Map.Entry<String, BoneTrack> bone : clip.bones().entrySet()) {
            bone.getValue().sample(time, this.scratch);
            out.add(bone.getKey(), this.scratch, factor);
        }
    }

    private void layer(Playback playback, double now, float weight, float move, AnimationPose out) {
        if (weight <= 0F) {
            return;
        }
        AnimationClip clip = playback.clip();
        double time = clip.localTime(now - playback.start());
        for (Map.Entry<String, BoneTrack> bone : clip.bones().entrySet()) {
            float w = this.set.lowerBody().contains(bone.getKey()) ? weight * (1F - move) : weight;
            if (w <= 0F) {
                continue;
            }
            bone.getValue().sample(time, this.scratch);
            out.fade(bone.getKey(), 1F - w);
            out.add(bone.getKey(), this.scratch, w);
        }
    }

    private float envelope(Playback playback, double now) {
        double elapsed = now - playback.start();
        double length = playback.clip().length();
        float in = smoothstep((float) Math.min(1D, Math.max(0D, elapsed / ACTION_FADE_IN)));
        if (playback.clip().loop() != LoopMode.ONCE) {
            return in;
        }
        float out = smoothstep((float) Math.min(1D, Math.max(0D, (length - elapsed) / ACTION_FADE_OUT)));
        return Math.min(in, out);
    }

    // ------------------------------------------------------------------------------------- math

    static float smoothstep(float x) {
        return x * x * (3F - 2F * x);
    }

    static float approach(float value, float target, float step) {
        if (value < target) {
            return Math.min(target, value + step);
        }
        return Math.max(target, value - step);
    }

    static double frac(double v) {
        return v - Math.floor(v);
    }
}
