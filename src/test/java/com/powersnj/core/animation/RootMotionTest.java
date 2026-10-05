package com.powersnj.core.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RootMotionTest {

    private static double[] point(RootMotion.Result r, double x, double y, double z) {
        double[][] m = RootMotion.euler(r.xRot(), r.yRot(), r.zRot());
        double[] p = RootMotion.apply(m, new double[]{x, y, z});
        return new double[]{p[0] + r.x(), p[1] + r.y(), p[2] + r.z()};
    }

    @Test
    void eulerRoundTrip() {
        double[][] m = RootMotion.euler(0.3, -0.7, 1.2);
        double[] e = RootMotion.toEuler(m);
        assertArrayEquals(new double[]{0.3, -0.7, 1.2}, e, 1e-9);
    }

    @Test
    void identityWithoutRootMotion() {
        RootMotion.Result r = RootMotion.compose(new float[6], 0F, 0F, 0F, 0F);
        assertArrayEquals(new float[6], new float[]{r.x(), r.y(), r.z(), r.xRot(), r.yRot(), r.zRot()}, 1e-6F);
    }

    @Test
    void positiveRootXLeansForwardAndPositionsAreScaled() {
        // root x = 90: the head (y = 28) ends up in front of the feet (-Z) on the ground plane
        RootMotion.Result r = RootMotion.compose(new float[]{90F, 0F, 0F, 0F, 0F, 0F}, 0F, 0F, 0F, 0F);
        double[] head = point(r, 0, 28, 0);
        assertEquals(0D, head[1], 1e-4);
        assertEquals(-28D, head[2], 1e-4);

        RootMotion.Result moved = RootMotion.compose(new float[]{0F, 0F, 0F, 2F, 16F, -4F}, 0F, 0F, 0F, 0F);
        assertEquals(-2F * RootMotion.PLAYER_SCALE, moved.x(), 1e-5, "Bedrock position x points to the character's left");
        assertEquals(16F * RootMotion.PLAYER_SCALE, moved.y(), 1e-5);
        assertEquals(-4F * RootMotion.PLAYER_SCALE, moved.z(), 1e-5, "negative z = forward");
    }

    @Test
    void steeringPitchesAroundTheHitboxCentre() {
        RootMotion.Result r = RootMotion.compose(new float[6], 1F, 90F, 0F, 0F);
        double[] centre = point(r, 0, RootMotion.STEER_PIVOT_Y, 0);
        assertEquals(RootMotion.STEER_PIVOT_Y, centre[1], 1e-4, "the pivot does not move");
        double[] top = point(r, 0, RootMotion.STEER_PIVOT_Y + 10, 0);
        assertEquals(-10D, top[2], 1e-4, "looking down tips the head forward");
        RootMotion.Result half = RootMotion.compose(new float[6], 0.5F, 90F, 0F, 0F);
        assertEquals(Math.toRadians(-45D), half.xRot(), 1e-6, "steering is weighted");
    }

    @Test
    void crouchCompensationLiftsTheBody() {
        RootMotion.Result r = RootMotion.compose(new float[6], 0F, 0F, 0F, 1F);
        assertEquals(RootMotion.CROUCH_OFFSET, r.y(), 1e-6);
    }
}
