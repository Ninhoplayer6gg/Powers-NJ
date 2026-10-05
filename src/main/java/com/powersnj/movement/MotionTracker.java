package com.powersnj.movement;

import net.minecraft.world.phys.Vec3;

/**
 * Measures how far an entity moved since the previous server tick. Teleports (jumps above
 * {@link #TELEPORT_THRESHOLD}) are ignored so they never count as movement or speed violations.
 */
public final class MotionTracker {

    public static final double TELEPORT_THRESHOLD = 48D;

    private Vec3 last;
    private double lastHorizontal;
    private double lastTotal;

    /**
     * @return horizontal distance moved since the last call
     */
    public double update(Vec3 position) {
        if (this.last == null) {
            this.last = position;
            return 0D;
        }
        double dx = position.x - this.last.x;
        double dy = position.y - this.last.y;
        double dz = position.z - this.last.z;
        this.last = position;
        double total = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (total > TELEPORT_THRESHOLD) {
            this.lastHorizontal = 0D;
            this.lastTotal = 0D;
            return 0D;
        }
        this.lastHorizontal = Math.sqrt(dx * dx + dz * dz);
        this.lastTotal = total;
        return this.lastHorizontal;
    }

    public double lastHorizontal() {
        return this.lastHorizontal;
    }

    public double lastTotal() {
        return this.lastTotal;
    }

    public void reset() {
        this.last = null;
    }
}
