package com.powersnj.core.ability;

import com.powersnj.core.config.PowersSettings;
import com.powersnj.core.energy.EnergyPool;

/**
 * The single server-side validation pipeline every Powers NJ ability passes through:
 * suit &rarr; config &rarr; weakness &rarr; skill &rarr; level &rarr; cooldown &rarr; energy.
 * Clients never decide any of these; they only send Palladium key presses.
 */
public final class AbilityGate {

    private AbilityGate() {
    }

    public static ActivationResult check(AbilityCost cost, AbilityContext context) {
        if (!context.isSuitActive()) {
            return ActivationResult.SUIT_INACTIVE;
        }
        if (context.isDisabledByConfig(cost.key())) {
            return ActivationResult.DISABLED_BY_CONFIG;
        }
        if (context.isBlockedByWeakness(cost.key())) {
            return ActivationResult.BLOCKED_BY_WEAKNESS;
        }
        if (!cost.requiredSkill().isEmpty() && !context.isSkillUnlocked(cost.requiredSkill())) {
            return ActivationResult.SKILL_LOCKED;
        }
        if (context.suitLevel() < cost.minLevel()) {
            return ActivationResult.LEVEL_TOO_LOW;
        }
        if (context.isOnCooldown(cost.key())) {
            return ActivationResult.ON_COOLDOWN;
        }
        if (!context.hasEnergy(cost.energyCost())) {
            return ActivationResult.NOT_ENOUGH_ENERGY;
        }
        return ActivationResult.ALLOWED;
    }

    /**
     * Pays for an activation that already passed {@link #check}: consumes energy and starts the
     * cooldown (scaled by {@link PowersSettings#cooldownMultiplier()}).
     *
     * @param pool may be null for abilities without energy cost
     * @return the cooldown actually applied, or -1 when the energy could no longer be paid
     */
    public static int commit(AbilityCost cost, EnergyPool pool, CooldownTracker cooldowns, PowersSettings settings) {
        if (cost.energyCost() > 0) {
            if (pool == null || !pool.tryConsume(cost.energyCost())) {
                return -1;
            }
        }
        int cooldown = settings.scaleCooldown(cost.cooldownTicks());
        if (cooldown > 0) {
            cooldowns.start(cost.key(), cooldown);
        }
        return cooldown;
    }
}
