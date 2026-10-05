package com.powersnj.core.flight;

import com.powersnj.core.energy.EnergyPool;

/**
 * Reusable, server-authoritative flight speed controller (not tied to any character).
 * <p>
 * Palladium's flight handler moves the player using the {@code palladium:flight_speed} attribute.
 * This controller owns that value: it smoothly accelerates/decelerates between cruise, boost and
 * high-speed modes, drains energy for high-speed flight and computes ramming damage for aerial
 * combat. The Minecraft side ({@code com.powersnj.flight.FlightService}) applies
 * {@link FlightTick#attributeValue()} as a transient attribute modifier, which Minecraft syncs to
 * the client automatically.
 */
public final class FlightController {

    public enum Mode {
        DISABLED, CRUISE, BOOST, HIGH_SPEED
    }

    private final FlightProfile profile;
    private boolean enabled = true;
    private boolean highSpeedRequested;
    private int boostTicks;
    private double currentSpeed;
    private Mode mode = Mode.CRUISE;

    public FlightController(FlightProfile profile) {
        this.profile = profile;
    }

    public FlightProfile profile() {
        return this.profile;
    }

    public Mode mode() {
        return this.mode;
    }

    public double currentSpeed() {
        return this.currentSpeed;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            this.highSpeedRequested = false;
            this.boostTicks = 0;
        }
    }

    public boolean isHighSpeedRequested() {
        return this.highSpeedRequested;
    }

    public void setHighSpeed(boolean highSpeed) {
        this.highSpeedRequested = highSpeed && this.enabled;
    }

    /**
     * Starts a boost. Energy for the boost is paid by the ability gate, not here.
     */
    public boolean requestBoost() {
        if (!this.enabled) {
            return false;
        }
        this.boostTicks = this.profile.boostDuration();
        return true;
    }

    public int boostTicks() {
        return this.boostTicks;
    }

    /**
     * @param airborne        whether the entity is currently flying
     * @param energy          resource drained by high-speed flight (may be null)
     * @param speedMultiplier server {@code maxSpeedMultiplier}
     */
    public FlightTick tick(boolean airborne, EnergyPool energy, double speedMultiplier) {
        float drained = 0F;
        boolean highSpeedCancelled = false;

        if (!this.enabled) {
            this.mode = Mode.DISABLED;
        } else if (this.boostTicks > 0) {
            this.mode = Mode.BOOST;
        } else if (this.highSpeedRequested) {
            this.mode = Mode.HIGH_SPEED;
        } else {
            this.mode = Mode.CRUISE;
        }

        if (this.mode == Mode.HIGH_SPEED && airborne && this.profile.highSpeedDrain() > 0) {
            if (energy == null || !energy.has(this.profile.highSpeedDrain())) {
                this.highSpeedRequested = false;
                highSpeedCancelled = true;
                this.mode = Mode.CRUISE;
            } else {
                drained = energy.drain(this.profile.highSpeedDrain());
            }
        }

        double target = switch (this.mode) {
            case DISABLED -> 0D;
            case CRUISE -> this.profile.cruiseSpeed();
            case BOOST -> this.profile.boostSpeed();
            case HIGH_SPEED -> this.profile.highSpeed();
        };
        target = Math.max(0D, Math.min(FlightProfile.MAX_ATTRIBUTE, target * Math.max(0D, speedMultiplier)));

        if (this.mode == Mode.DISABLED) {
            this.currentSpeed = 0D;
        } else if (this.currentSpeed < target) {
            // Never start below cruise speed: flight must be usable immediately after equipping.
            double floor = Math.min(target, this.profile.cruiseSpeed() * Math.max(0D, speedMultiplier));
            this.currentSpeed = Math.max(floor, Math.min(target, this.currentSpeed + this.profile.acceleration()));
        } else if (this.currentSpeed > target) {
            this.currentSpeed = Math.max(target, this.currentSpeed - this.profile.deceleration());
        }

        if (this.boostTicks > 0) {
            this.boostTicks--;
        }
        return new FlightTick(this.mode, this.currentSpeed, drained, highSpeedCancelled);
    }

    /**
     * Ramming damage for aerial combat.
     *
     * @param actualSpeed measured speed in blocks per tick
     */
    public float impactDamage(double actualSpeed) {
        if (actualSpeed < this.profile.impactMinSpeed()) {
            return 0F;
        }
        return (float) ((actualSpeed - this.profile.impactMinSpeed() + 0.5D) * this.profile.impactDamage());
    }

    /**
     * @param mode               resulting mode
     * @param attributeValue     value to apply to {@code palladium:flight_speed}
     * @param energyDrained      energy removed this tick
     * @param highSpeedCancelled true when high-speed mode was dropped for lack of energy
     */
    public record FlightTick(Mode mode, double attributeValue, float energyDrained, boolean highSpeedCancelled) {
    }
}
