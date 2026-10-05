package com.powersnj.core.energy;

import com.powersnj.core.data.DataNode;

/**
 * A single resource bar: current/max, regeneration, consumption and change tracking for
 * bandwidth-friendly synchronisation (the server only sends an update when the value moved by at
 * least {@link #SYNC_THRESHOLD} or when it reaches a bound).
 */
public final class EnergyPool {

    public static final float SYNC_THRESHOLD = 1.0F;

    private final EnergyType type;
    private float current;
    private float max;
    private float regenPerTick;
    private int regenDelay;
    private int regenCooldown;
    private float lastSyncedCurrent = Float.NaN;
    private float lastSyncedMax = Float.NaN;

    public EnergyPool(EnergyType type, float max, float regenPerTick, int regenDelay) {
        this.type = type;
        this.max = Math.max(1F, max);
        this.current = this.max;
        this.regenPerTick = Math.max(0F, regenPerTick);
        this.regenDelay = Math.max(0, regenDelay);
    }

    public EnergyType type() {
        return this.type;
    }

    public float current() {
        return this.current;
    }

    public float max() {
        return this.max;
    }

    public float regenPerTick() {
        return this.regenPerTick;
    }

    public float fraction() {
        return this.max <= 0 ? 0 : this.current / this.max;
    }

    public boolean isFull() {
        return this.current >= this.max;
    }

    public boolean has(float amount) {
        return amount <= 0 || this.current >= amount;
    }

    /**
     * Consumes {@code amount} only if it is fully available.
     *
     * @return whether the energy was consumed
     */
    public boolean tryConsume(float amount) {
        if (amount <= 0) {
            return true;
        }
        if (this.current < amount) {
            return false;
        }
        this.current -= amount;
        this.regenCooldown = this.regenDelay;
        return true;
    }

    /**
     * Removes up to {@code amount} (clamped at zero), e.g. continuous drains or weakness damage.
     *
     * @return the amount actually removed
     */
    public float drain(float amount) {
        if (amount <= 0) {
            return 0;
        }
        float removed = Math.min(this.current, amount);
        this.current -= removed;
        this.regenCooldown = this.regenDelay;
        return removed;
    }

    /**
     * @return the amount actually added
     */
    public float add(float amount) {
        if (amount <= 0) {
            return 0;
        }
        float added = Math.min(this.max - this.current, amount);
        this.current += added;
        return added;
    }

    public void set(float value) {
        this.current = clamp(value, 0, this.max);
    }

    /**
     * Rescales the pool (e.g. after a level up). The current value keeps its absolute amount but is
     * clamped to the new maximum.
     */
    public void reconfigure(float newMax, float newRegenPerTick, int newRegenDelay) {
        this.max = Math.max(1F, newMax);
        this.regenPerTick = Math.max(0F, newRegenPerTick);
        this.regenDelay = Math.max(0, newRegenDelay);
        this.current = clamp(this.current, 0, this.max);
    }

    /**
     * Advances regeneration by one tick.
     *
     * @param regenMultiplier global multiplier from the server config
     * @return true when the value changed
     */
    public boolean tick(double regenMultiplier) {
        if (this.regenCooldown > 0) {
            this.regenCooldown--;
            return false;
        }
        if (this.current >= this.max || this.regenPerTick <= 0 || regenMultiplier <= 0) {
            return false;
        }
        this.current = Math.min(this.max, this.current + (float) (this.regenPerTick * regenMultiplier));
        return true;
    }

    public boolean needsSync() {
        if (Float.isNaN(this.lastSyncedCurrent) || this.lastSyncedMax != this.max) {
            return true;
        }
        if (this.current != this.lastSyncedCurrent && (this.current <= 0 || this.current >= this.max)) {
            return true;
        }
        return Math.abs(this.current - this.lastSyncedCurrent) >= SYNC_THRESHOLD;
    }

    public void markSynced() {
        this.lastSyncedCurrent = this.current;
        this.lastSyncedMax = this.max;
    }

    public void forceResync() {
        this.lastSyncedCurrent = Float.NaN;
    }

    public void write(DataNode node) {
        node.putString("type", this.type.id());
        node.putFloat("current", this.current);
        node.putFloat("max", this.max);
        node.putFloat("regen", this.regenPerTick);
        node.putInt("regen_delay", this.regenDelay);
        node.putInt("regen_cooldown", this.regenCooldown);
    }

    public void read(DataNode node) {
        this.max = Math.max(1F, node.getFloat("max", this.max));
        this.regenPerTick = Math.max(0F, node.getFloat("regen", this.regenPerTick));
        this.regenDelay = Math.max(0, node.getInt("regen_delay", this.regenDelay));
        this.regenCooldown = Math.max(0, node.getInt("regen_cooldown", 0));
        this.current = clamp(node.getFloat("current", this.current), 0, this.max);
        this.forceResync();
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }
}
