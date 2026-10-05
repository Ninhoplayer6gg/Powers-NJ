package com.powersnj.core.animation;

/**
 * Turns the blended {@code root} bone into the whole-body render transform of the wearer.
 * <p>
 * Frame: the entity render pose right after the vanilla body yaw rotation, before the model flip:
 * origin at the feet, Y up, the entity faces -Z, units are 1/16 block. This is exactly Blockbench's
 * frame, only scaled: the player model renders at {@link #PLAYER_SCALE}.
 * <p>
 * The result is {@code T(crouch) * Steer * T(root position) * R(root rotation)}, where Steer
 * rotates the whole body around the hitbox centre by the look pitch and the flight banking (fly
 * states only), returned as one translation plus Z-Y-X Euler angles (the order of
 * {@code PoseStack} rotations used by Palladium's body animations).
 */
public final class RootMotion {

    /** Vanilla player model scale (15/16). */
    public static final float PLAYER_SCALE = 0.9375F;
    /** Steering pivot: centre of the 1.8 block player hitbox, in 1/16 blocks. */
    public static final float STEER_PIVOT_Y = 14.4F;
    /** PlayerRenderer lowers crouching players by 0.125 blocks. */
    public static final float CROUCH_OFFSET = 2.0F;

    /**
     * @param x    translation, 1/16 blocks
     * @param xRot radians, applied last (Z, then Y, then X)
     */
    public record Result(float x, float y, float z, float xRot, float yRot, float zRot) {

        public static final Result IDENTITY = new Result(0F, 0F, 0F, 0F, 0F, 0F);
    }

    private RootMotion() {
    }

    /**
     * @param root              blended root: rotation degrees (Bedrock) [0..2], position pixels [3..5]
     * @param steering          0..1 weight of the look steering
     * @param pitchDegrees      look pitch (positive = looking down)
     * @param bankDegrees       roll around the forward axis (positive tilts the top to the left)
     * @param crouchCompensation 0..1 how much of vanilla's crouch offset to undo (only pass &gt; 0 while crouching)
     */
    public static Result compose(float[] root, float steering, float pitchDegrees, float bankDegrees, float crouchCompensation) {
        double[][] rc = euler(Math.toRadians(-root[0]), Math.toRadians(-root[1]), Math.toRadians(root[2]));
        double[] tc = {-root[3] * PLAYER_SCALE, root[4] * PLAYER_SCALE, root[5] * PLAYER_SCALE};
        double[][] rs = euler(Math.toRadians(-pitchDegrees * steering), 0D, Math.toRadians(bankDegrees * steering));
        double[][] r = mul(rs, rc);
        // t = crouch + c + Rs * (tc - c)
        double[] c = {0D, STEER_PIVOT_Y, 0D};
        double[] local = {tc[0] - c[0], tc[1] - c[1], tc[2] - c[2]};
        double[] rotated = apply(rs, local);
        double[] t = {c[0] + rotated[0], c[1] + rotated[1] + CROUCH_OFFSET * crouchCompensation, c[2] + rotated[2]};
        double[] angles = toEuler(r);
        return new Result((float) t[0], (float) t[1], (float) t[2], (float) angles[0], (float) angles[1], (float) angles[2]);
    }

    /** Rz(z) * Ry(y) * Rx(x). */
    static double[][] euler(double x, double y, double z) {
        return mul(rotZ(z), mul(rotY(y), rotX(x)));
    }

    /**
     * Inverse of {@link #euler}: {x, y, z} with y in [-pi/2, pi/2].
     */
    static double[] toEuler(double[][] m) {
        double sy = -m[2][0];
        sy = Math.max(-1D, Math.min(1D, sy));
        double y = Math.asin(sy);
        double x;
        double z;
        if (Math.abs(sy) < 0.999999D) {
            x = Math.atan2(m[2][1], m[2][2]);
            z = Math.atan2(m[1][0], m[0][0]);
        } else {
            x = Math.atan2(-m[1][2], m[1][1]);
            z = 0D;
        }
        return new double[]{x, y, z};
    }

    static double[][] rotX(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new double[][]{{1, 0, 0}, {0, c, -s}, {0, s, c}};
    }

    static double[][] rotY(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new double[][]{{c, 0, s}, {0, 1, 0}, {-s, 0, c}};
    }

    static double[][] rotZ(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new double[][]{{c, -s, 0}, {s, c, 0}, {0, 0, 1}};
    }

    static double[][] mul(double[][] a, double[][] b) {
        double[][] r = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                r[i][j] = a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j];
            }
        }
        return r;
    }

    static double[] apply(double[][] m, double[] v) {
        return new double[]{
                m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2],
                m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2],
                m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]};
    }
}
