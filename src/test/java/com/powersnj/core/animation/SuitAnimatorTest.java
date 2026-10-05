package com.powersnj.core.animation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SuitAnimatorTest {

    private static final String CLIPS = """
            {"animations": {
              "a.idle":    {"loop": true, "animation_length": 1, "bones": {"body": {"rotation": [1, 0, 0]}, "head": {"rotation": [-3, 0, 0]}}},
              "a.walk":    {"loop": true, "animation_length": 1, "bones": {"right_leg": {"rotation": {"0.0": [30, 0, 0], "0.5": [-30, 0, 0], "1.0": [30, 0, 0]}}}},
              "a.run":     {"loop": true, "animation_length": 1, "bones": {"right_leg": {"rotation": [50, 0, 0]}}},
              "a.crouch":  {"loop": true, "animation_length": 1, "bones": {"body": {"rotation": [20, 0, 0]}}},
              "a.guard":   {"loop": true, "animation_length": 1, "bones": {"body": {"rotation": [25, 0, 0]}}},
              "a.jump":    {"loop": "hold_on_last_frame", "animation_length": 0.7, "bones": {"right_leg": {"rotation": {"0.0": [0, 0, 0], "0.7": [-40, 0, 0]}}}},
              "a.takeoff": {"animation_length": 0.5, "bones": {"body": {"rotation": [-5, 0, 0]}}},
              "a.hover":   {"loop": true, "animation_length": 1, "bones": {"body": {"rotation": [3, 0, 0]}}},
              "a.fly":     {"loop": true, "animation_length": 1, "bones": {"body": {"rotation": [4, 0, 0]}, "head": {"rotation": [-80, 0, 0]}, "root": {"rotation": [90, 0, 0]}}},
              "a.fast":    {"loop": true, "animation_length": 1, "bones": {"body": {"rotation": [6, 0, 0]}}},
              "a.land":    {"animation_length": 0.5, "bones": {"body": {"rotation": [40, 0, 0]}}},
              "a.punch_r": {"animation_length": 0.4, "bones": {"right_arm": {"rotation": [-90, 0, 0]}}},
              "a.punch_l": {"animation_length": 0.4, "bones": {"left_arm": {"rotation": [-90, 0, 0]}}},
              "a.kick":    {"animation_length": 0.6, "bones": {"right_leg": {"rotation": [-80, 0, 0]}}},
              "a.big":     {"animation_length": 1.0, "bones": {"right_arm": {"rotation": [-170, 0, 0]}, "right_leg": {"rotation": [10, 0, 0]}}},
              "a.recoil":  {"animation_length": 0.5, "bones": {"body": {"rotation": [-12, 0, 0]}}}
            }}
            """;

    private static final String SET = """
            {"clips": "test:animations/a.animation.json", "fade": 0.2,
             "states": {"idle": "a.idle", "walk": "a.walk", "run": "a.run", "crouch": "a.crouch", "guard": "a.guard", "jump": "a.jump",
                        "takeoff": "a.takeoff", "hover": "a.hover", "fly": "a.fly", "fast_flight": "a.fast", "land": "a.land"},
             "melee": {"chain": ["a.punch_r", "a.punch_l"], "after_sprint": "a.kick", "chain_window": 1.0},
             "events": {"heavy_hit": "a.recoil"},
             "lower_body": ["root", "right_leg", "left_leg"]}
            """;

    private SuitAnimator animator;
    private final AnimationPose pose = new AnimationPose();
    private final float[] v = new float[AnimationPose.SIZE];
    private double now;

    private static AnimatorInput standing() {
        return new AnimatorInput(true, true, false, 0F, false, false, false, false);
    }

    @BeforeEach
    void setUp() {
        Map<String, AnimationClip> clips = AnimationClipParser.parse(CLIPS);
        AnimationSet set = AnimationSetParser.parse("test", SET, clips);
        assertTrue(set.missingClips().isEmpty(), set.missingClips().toString());
        this.animator = new SuitAnimator(set);
        this.now = 10D;
        this.run(standing(), 0.5D);
    }

    /** Ticks every 0.05 s for {@code seconds}. */
    private void run(AnimatorInput in, double seconds) {
        for (double t = 0; t < seconds - 1e-9; t += 0.05D) {
            this.now += 0.05D;
            this.animator.tick(in, this.now);
        }
    }

    private float body(float limbSwingAmount) {
        this.animator.sample(this.now, 0F, limbSwingAmount, this.pose);
        this.pose.normalized(HumanoidBones.BODY, this.v);
        return this.v[0];
    }

    @Test
    void fadesInAndShowsIdle() {
        assertEquals(1F, this.animator.master());
        assertEquals(1F, this.body(0F), 1e-4);
        assertEquals(1F, this.pose.weight(HumanoidBones.BODY), 1e-4);
        this.pose.accumulated(HumanoidBones.HEAD, this.v);
        assertEquals(-3F, this.v[0], 1e-4, "the head is additive");
        assertEquals(1F, this.pose.lookWeight(), 1e-4);
    }

    @Test
    void walkingReplacesIdleAndFollowsTheLimbSwingPhase() {
        this.animator.sample(this.now, 0F, 1F, this.pose);
        assertEquals(0F, this.pose.weight(HumanoidBones.BODY), 1e-4, "idle fully faded out when moving");
        this.pose.normalized(HumanoidBones.RIGHT_LEG, this.v);
        assertEquals(30F, this.v[0], 1e-3, "phase 0 = right leg back");
        float halfCycle = (float) (0.5D / SuitAnimator.LIMB_SWING_TO_PHASE);
        this.animator.sample(this.now, halfCycle, 1F, this.pose);
        this.pose.normalized(HumanoidBones.RIGHT_LEG, this.v);
        assertEquals(-30F, this.v[0], 1e-2);

        this.run(new AnimatorInput(true, true, false, 0F, false, false, true, false), 0.3D);
        this.animator.sample(this.now, 0F, 1F, this.pose);
        this.pose.normalized(HumanoidBones.RIGHT_LEG, this.v);
        assertEquals(50F, this.v[0], 1e-3, "sprinting blends to the run cycle");
    }

    @Test
    void sneakingCrouchesGuardsInCombatAndYieldsWhenMoving() {
        this.run(new AnimatorInput(true, true, false, 0F, false, true, false, false), 0.3D);
        assertEquals(LocomotionState.CROUCH, this.animator.state());
        assertEquals(20F, this.body(0F), 1e-4);
        assertEquals(1F, this.pose.crouchCompensation(), 1e-4);
        this.body(1F);
        assertEquals(0F, this.pose.weight(HumanoidBones.BODY), 1e-4, "vanilla crouch walking takes over");

        this.run(new AnimatorInput(true, true, false, 0F, false, true, false, true), 0.3D);
        assertEquals(LocomotionState.GUARD, this.animator.state());
        assertEquals(25F, this.body(0F), 1e-4);
    }

    @Test
    void jumpsAndLandsAfterLongFalls() {
        this.animator.onJump(this.now);
        AnimatorInput air = new AnimatorInput(true, false, false, 0F, false, false, false, false);
        this.run(air, 0.3D);
        assertEquals(LocomotionState.JUMP, this.animator.state());
        this.run(standing(), 0.05D);
        assertEquals(LocomotionState.GROUND, this.animator.state(), "a short hop does not land heavily");

        this.run(air, 1.5D);
        assertEquals(LocomotionState.JUMP, this.animator.state(), "walking off a ledge switches to the airborne pose");
        this.run(standing(), 0.05D);
        assertEquals(LocomotionState.LAND, this.animator.state());
        this.run(standing(), 0.6D);
        assertEquals(LocomotionState.GROUND, this.animator.state(), "the landing ends on its own");
    }

    @Test
    void flightTakesOffHoversFliesAndLands() {
        this.run(new AnimatorInput(true, false, true, 0F, false, false, false, false), 0.1D);
        assertEquals(LocomotionState.TAKEOFF, this.animator.state());
        this.run(new AnimatorInput(true, false, true, 0F, false, false, false, false), 0.6D);
        assertEquals(LocomotionState.HOVER, this.animator.state());

        this.run(new AnimatorInput(true, false, true, 1.5F, false, false, true, false), 0.5D);
        assertEquals(LocomotionState.FLY, this.animator.state());
        this.animator.sample(this.now, 0F, 0F, this.pose);
        assertEquals(1F, this.pose.steering(), 1e-4);
        assertEquals(0F, this.pose.lookWeight(), 1e-4, "the head is set by the clip in fly states");
        this.pose.accumulated(HumanoidBones.ROOT, this.v);
        assertEquals(90F, this.v[0], 1e-3);

        this.run(new AnimatorInput(true, false, true, 2F, true, false, true, false), 0.5D);
        assertEquals(LocomotionState.FAST_FLIGHT, this.animator.state());

        this.run(standing(), 0.05D);
        assertEquals(LocomotionState.LAND, this.animator.state(), "touching down after flying lands");
    }

    @Test
    void actionsOverrideTheirBonesAndFadeAway() {
        assertTrue(this.animator.play("a.big", this.now));
        this.now += 0.3D;
        this.animator.tick(standing(), this.now);
        this.animator.sample(this.now, 0F, 0F, this.pose);
        this.pose.normalized(HumanoidBones.RIGHT_ARM, this.v);
        assertEquals(-170F, this.v[0], 1e-3);
        assertEquals(1F, this.pose.weight(HumanoidBones.RIGHT_ARM), 1e-4);
        this.pose.normalized(HumanoidBones.RIGHT_LEG, this.v);
        assertEquals(10F, this.v[0], 1e-3, "standing still: the action drives the legs");

        this.animator.sample(this.now, 0F, 1F, this.pose);
        this.pose.normalized(HumanoidBones.RIGHT_LEG, this.v);
        assertEquals(30F, this.v[0], 1e-3, "moving: the legs keep walking (lower body)");

        assertFalse(this.animator.onSwing(false, this.now), "melee never interrupts an ability clip");
        this.run(standing(), 1.0D);
        assertEquals("", this.animator.currentAction());
        assertTrue(this.animator.play("#heavy_hit", this.now));
        assertEquals("a.recoil", this.animator.currentAction());
        assertFalse(this.animator.play("#unknown", this.now));
        assertFalse(this.animator.play("a.missing", this.now));
    }

    @Test
    void meleeChainAlternatesResetsAndKicksAfterSprinting() {
        assertTrue(this.animator.onSwing(false, this.now));
        assertEquals("a.punch_r", this.animator.currentAction());
        this.now += 0.4D;
        this.animator.onSwing(false, this.now);
        assertEquals("a.punch_l", this.animator.currentAction());
        this.now += 0.4D;
        this.animator.onSwing(false, this.now);
        assertEquals("a.punch_r", this.animator.currentAction());
        this.now += 2D;
        this.animator.onSwing(false, this.now);
        this.now += 0.4D;
        this.animator.onSwing(false, this.now);
        assertEquals("a.punch_l", this.animator.currentAction(), "the chain restarted after the window");
        this.now += 0.4D;
        this.animator.onSwing(true, this.now);
        assertEquals("a.kick", this.animator.currentAction());
    }

    @Test
    void deactivationFadesBackToVanilla() {
        this.run(new AnimatorInput(false, true, false, 0F, false, false, false, false), 0.3D);
        assertEquals(0F, this.animator.master());
        this.animator.sample(this.now, 0F, 0F, this.pose);
        assertFalse(this.pose.has(HumanoidBones.BODY));
        assertEquals(1F, this.pose.lookWeight(), 1e-4);
    }
}
