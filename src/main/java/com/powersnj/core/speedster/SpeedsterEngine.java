package com.powersnj.core.speedster;

import com.powersnj.core.config.PowersSettings;
import com.powersnj.core.data.DataNode;
import com.powersnj.core.energy.EnergyPool;

import java.util.EnumSet;
import java.util.Set;

/**
 * Speed simulation shared by every speedster (Reverse-Flash today, more later).
 * <p>
 * The engine keeps a current and a target speed multiplier and moves between them with
 * acceleration/deceleration, drains energy proportionally to the speed, handles wall impacts,
 * wall running and water running, exposes the steering factor used by the client turning
 * control and validates observed movement on the server. The Minecraft side applies
 * {@link #currentMultiplier()} through a transient movement-speed attribute modifier (synced to the
 * client by vanilla), so there is no client-trusted speed value anywhere.
 */
public final class SpeedsterEngine {

    /** Allowed overshoot of observed movement vs the engine speed before a tick counts as a violation. */
    public static final double VALIDATION_TOLERANCE = 1.6D;
    /** Extra blocks/tick tolerated for knockback, jumps and latency. */
    public static final double VALIDATION_SLACK = 1.0D;
    public static final int MAX_VIOLATIONS = 8;

    private final SpeedsterProfile profile;
    private boolean active;
    private int speedLevel = 1;
    private double currentMultiplier = 1D;
    private boolean wallRunning;
    private boolean waterRunning;
    private boolean atTopSpeed;
    private int violations;

    public SpeedsterEngine(SpeedsterProfile profile) {
        this.profile = profile;
    }

    public SpeedsterProfile profile() {
        return this.profile;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            this.wallRunning = false;
            this.waterRunning = false;
        }
    }

    public int speedLevel() {
        return this.speedLevel;
    }

    public void setSpeedLevel(int level) {
        this.speedLevel = Math.max(1, Math.min(this.profile.speedLevels(), level));
    }

    /**
     * @return the new speed level
     */
    public int adjustSpeedLevel(int delta) {
        this.setSpeedLevel(this.speedLevel + delta);
        return this.speedLevel;
    }

    public double currentMultiplier() {
        return this.currentMultiplier;
    }

    public boolean isWallRunning() {
        return this.wallRunning;
    }

    public boolean isWaterRunning() {
        return this.waterRunning;
    }

    /**
     * Highest multiplier allowed by profile and server config.
     */
    public double effectiveMax(PowersSettings settings) {
        return Math.max(1D, this.profile.maxMultiplier() * settings.maxSpeedMultiplier());
    }

    /**
     * Multiplier the engine is heading to for the selected speed level.
     */
    public double targetMultiplier(PowersSettings settings) {
        if (!this.active) {
            return 1D;
        }
        double max = this.effectiveMax(settings);
        double base = Math.min(this.profile.baseMultiplier(), max);
        if (this.profile.speedLevels() <= 1) {
            return max;
        }
        double t = (this.speedLevel - 1) / (double) (this.profile.speedLevels() - 1);
        return base + (max - base) * t;
    }

    /**
     * 0 at normal speed, 1 at the effective maximum.
     */
    public double normalizedSpeed(PowersSettings settings) {
        double max = this.effectiveMax(settings);
        return max <= 1D ? 0D : Math.max(0D, Math.min(1D, (this.currentMultiplier - 1D) / (max - 1D)));
    }

    public SpeedsterTick tick(SpeedsterInput input, EnergyPool energy, PowersSettings settings) {
        Set<SpeedsterEvent> events = EnumSet.noneOf(SpeedsterEvent.class);
        float drained = 0F;

        // Energy: proportional to how fast we are going.
        if (this.active && this.currentMultiplier > 1D) {
            float drain = (float) (this.profile.drainAtMax() * this.normalizedSpeed(settings));
            if (drain > 0F) {
                if (energy == null || !energy.has(drain)) {
                    this.setActive(false);
                    events.add(SpeedsterEvent.OUT_OF_ENERGY);
                    events.add(SpeedsterEvent.STOPPED);
                } else {
                    drained = energy.drain(drain);
                }
            }
        }

        double target = input.moving() ? this.targetMultiplier(settings) : 1D;
        if (this.currentMultiplier < target) {
            this.currentMultiplier = Math.min(target, this.currentMultiplier + this.profile.acceleration());
        } else if (this.currentMultiplier > target) {
            this.currentMultiplier = Math.max(target, this.currentMultiplier - this.profile.deceleration());
        }

        boolean top = this.active && this.currentMultiplier >= this.effectiveMax(settings) - 1.0E-6;
        if (top && !this.atTopSpeed) {
            events.add(SpeedsterEvent.TOP_SPEED_REACHED);
        }
        this.atTopSpeed = top;

        // Collisions: run up the wall when possible, otherwise it is an impact that kills momentum.
        boolean wasWallRunning = this.wallRunning;
        if (this.active && input.horizontalCollision() && this.currentMultiplier >= this.profile.wallRunMin() && input.canWallRun()) {
            this.wallRunning = true;
        } else if (this.active && input.horizontalCollision() && !wasWallRunning && this.currentMultiplier >= this.profile.collisionMin()) {
            events.add(SpeedsterEvent.WALL_IMPACT);
            this.currentMultiplier = Math.max(1D, this.currentMultiplier * 0.3D);
            this.wallRunning = false;
        } else if (!input.horizontalCollision() || this.currentMultiplier < this.profile.wallRunMin()) {
            this.wallRunning = false;
        }
        if (this.wallRunning && !wasWallRunning) {
            events.add(SpeedsterEvent.WALL_RUN_START);
        } else if (!this.wallRunning && wasWallRunning) {
            events.add(SpeedsterEvent.WALL_RUN_END);
        }

        boolean wasWaterRunning = this.waterRunning;
        this.waterRunning = this.active && input.canWaterRun() && input.onWaterSurface() && this.currentMultiplier >= this.profile.waterRunMin();
        if (this.waterRunning && !wasWaterRunning) {
            events.add(SpeedsterEvent.WATER_RUN_START);
        } else if (!this.waterRunning && wasWaterRunning) {
            events.add(SpeedsterEvent.WATER_RUN_END);
        }

        return new SpeedsterTick(this.currentMultiplier, drained, this.wallRunning, this.waterRunning, events);
    }

    /**
     * Steering responsiveness for the client turning control: 1 at normal speed, falling linearly
     * to {@link SpeedsterProfile#turnRateAtMax()} at top speed.
     */
    public double turnFactor(PowersSettings settings) {
        double n = this.normalizedSpeed(settings);
        return 1D - (1D - this.profile.turnRateAtMax()) * n;
    }

    /**
     * Ramming/impact damage at the current speed.
     */
    public float collisionDamage() {
        if (this.currentMultiplier < this.profile.collisionMin()) {
            return 0F;
        }
        return (float) ((this.currentMultiplier - this.profile.collisionMin() + 1D) * this.profile.collisionDamage());
    }

    /**
     * Maximum horizontal displacement per tick the server accepts.
     *
     * @param baseBlocksPerTick the player's normal sprinting distance per tick (~0.28)
     */
    public double allowedDisplacement(double baseBlocksPerTick) {
        return baseBlocksPerTick * Math.max(1D, this.currentMultiplier) * VALIDATION_TOLERANCE + VALIDATION_SLACK;
    }

    /**
     * Server movement validation. Violations decay over time; too many in a short window means the
     * client moves faster than the engine allows.
     *
     * @return true when the movement is acceptable
     */
    public boolean validateMovement(double observedHorizontal, double baseBlocksPerTick) {
        boolean ok = observedHorizontal <= this.allowedDisplacement(baseBlocksPerTick);
        if (ok) {
            if (this.violations > 0) {
                this.violations--;
            }
        } else {
            this.violations += 2;
        }
        return ok;
    }

    public boolean isFlagged() {
        return this.violations >= MAX_VIOLATIONS;
    }

    public void resetViolations() {
        this.violations = 0;
    }

    public void write(DataNode node) {
        node.putBoolean("active", this.active);
        node.putInt("speed_level", this.speedLevel);
    }

    public void read(DataNode node) {
        this.setActive(node.getBoolean("active", false));
        this.setSpeedLevel(node.getInt("speed_level", 1));
        this.currentMultiplier = 1D;
    }

    /**
     * @param multiplier    speed multiplier to apply this tick
     * @param energyDrained energy removed this tick
     * @param wallRunning   currently running up a wall
     * @param waterRunning  currently running on water
     * @param events        what happened this tick
     */
    public record SpeedsterTick(double multiplier, float energyDrained, boolean wallRunning, boolean waterRunning, Set<SpeedsterEvent> events) {
    }
}
