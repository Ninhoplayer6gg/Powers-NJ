package com.powersnj.core.math;

/**
 * Minimal immutable double vector for engine-agnostic movement math.
 */
public record Vec3d(double x, double y, double z) {

    public static final Vec3d ZERO = new Vec3d(0, 0, 0);

    public Vec3d add(Vec3d o) {
        return new Vec3d(this.x + o.x, this.y + o.y, this.z + o.z);
    }

    public Vec3d subtract(Vec3d o) {
        return new Vec3d(this.x - o.x, this.y - o.y, this.z - o.z);
    }

    public Vec3d scale(double s) {
        return new Vec3d(this.x * s, this.y * s, this.z * s);
    }

    public double length() {
        return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public double horizontalLength() {
        return Math.sqrt(this.x * this.x + this.z * this.z);
    }

    public Vec3d horizontal() {
        return new Vec3d(this.x, 0, this.z);
    }

    public Vec3d normalize() {
        double len = this.length();
        return len < 1.0E-8 ? ZERO : new Vec3d(this.x / len, this.y / len, this.z / len);
    }

    public boolean isZero() {
        return Math.abs(this.x) < 1.0E-8 && Math.abs(this.y) < 1.0E-8 && Math.abs(this.z) < 1.0E-8;
    }

    public double dot(Vec3d o) {
        return this.x * o.x + this.y * o.y + this.z * o.z;
    }

    /**
     * Rotates the horizontal part of {@code from} towards {@code to} by at most {@code maxBlend}
     * (0 = keep {@code from}, 1 = snap to {@code to}), preserving the horizontal speed of {@code from}.
     */
    public static Vec3d steerHorizontal(Vec3d from, Vec3d to, double maxBlend) {
        double speed = from.horizontalLength();
        Vec3d fromDir = from.horizontal().normalize();
        Vec3d toDir = to.horizontal().normalize();
        if (speed < 1.0E-6 || toDir.isZero()) {
            return from;
        }
        if (fromDir.isZero()) {
            return new Vec3d(toDir.x * speed, from.y, toDir.z * speed);
        }
        double blend = Math.max(0, Math.min(1, maxBlend));
        Vec3d mixed = fromDir.scale(1 - blend).add(toDir.scale(blend)).normalize();
        if (mixed.isZero()) {
            mixed = toDir;
        }
        return new Vec3d(mixed.x * speed, from.y, mixed.z * speed);
    }
}
