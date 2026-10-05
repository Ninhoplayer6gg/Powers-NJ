package com.powersnj.core.energy;

/**
 * How a suit's resource scales with its level. Parsed from the suit definition JSON
 * ({@code "energy": {...}}).
 *
 * @param type           resource kind
 * @param baseMax        capacity at level 1
 * @param maxPerLevel    extra capacity for every level above 1
 * @param regenPerTick   base regeneration per tick at level 1
 * @param regenPerLevel  extra regeneration per tick for every level above 1
 * @param regenDelay     ticks after a consumption before regeneration resumes
 */
public record EnergySpec(EnergyType type, float baseMax, float maxPerLevel, float regenPerTick, float regenPerLevel, int regenDelay) {

    public EnergySpec {
        if (baseMax <= 0) {
            throw new IllegalArgumentException("baseMax must be > 0");
        }
        if (maxPerLevel < 0 || regenPerTick < 0 || regenPerLevel < 0 || regenDelay < 0) {
            throw new IllegalArgumentException("Energy scaling values must not be negative");
        }
    }

    public float maxAt(int level) {
        return this.baseMax + this.maxPerLevel * Math.max(0, level - 1);
    }

    public float regenAt(int level) {
        return this.regenPerTick + this.regenPerLevel * Math.max(0, level - 1);
    }
}
