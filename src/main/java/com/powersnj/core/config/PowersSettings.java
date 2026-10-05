package com.powersnj.core.config;

/**
 * Immutable snapshot of every server-side balance setting.
 * <p>
 * The Forge config ({@code com.powersnj.config.PowersServerConfig}) publishes a new snapshot through
 * {@link Settings#update(PowersSettings)} whenever the server config is loaded or reloaded. Core logic
 * only ever reads snapshots, so it is deterministic and unit-testable without Forge.
 */
public record PowersSettings(
        boolean enablePvP,
        double pvpDamageMultiplier,
        double pveDamageMultiplier,
        double bossDamageMultiplier,
        boolean enableDestruction,
        int maxDestroyedBlocksPerAttack,
        int maxDestroyedBlocksPerTick,
        boolean allowObsidianDestruction,
        double xpMultiplier,
        double maxSpeedMultiplier,
        double energyRegenerationMultiplier,
        double cooldownMultiplier,
        boolean enablePhasing,
        int maxPhaseDistance,
        boolean enableVenomWeaknesses,
        boolean worldgenEnabled,
        int viltrumiteVeinsPerChunk,
        int speedCrystalVeinsPerChunk
) {

    public static final PowersSettings DEFAULTS = new PowersSettings(
            true, 0.35D, 1.0D, 0.6D,
            true, 48, 64, true,
            1.0D,
            1.0D,
            1.0D,
            1.0D,
            true, 8,
            true,
            true, 3, 2
    );

    public PowersSettings {
        if (maxDestroyedBlocksPerAttack < 0 || maxDestroyedBlocksPerTick < 1) {
            throw new IllegalArgumentException("Destruction limits must be positive");
        }
        if (cooldownMultiplier < 0 || xpMultiplier < 0 || energyRegenerationMultiplier < 0 || maxSpeedMultiplier < 0) {
            throw new IllegalArgumentException("Multipliers must not be negative");
        }
    }

    /**
     * Applies the cooldown multiplier to a base cooldown, never returning less than 0 ticks.
     */
    public int scaleCooldown(int baseTicks) {
        return (int) Math.max(0, Math.round(baseTicks * this.cooldownMultiplier));
    }
}
