package com.powersnj.core.ability;

/**
 * Activation requirements of one ability, read from its Palladium ability properties.
 *
 * @param key           cooldown key ({@code <power id>#<ability key>})
 * @param requiredSkill skill that must be unlocked (empty = none)
 * @param minLevel      minimum suit level
 * @param energyCost    energy consumed on activation
 * @param cooldownTicks base cooldown before the server multiplier
 */
public record AbilityCost(String key, String requiredSkill, int minLevel, float energyCost, int cooldownTicks) {

    public AbilityCost {
        requiredSkill = requiredSkill == null ? "" : requiredSkill;
        minLevel = Math.max(1, minLevel);
        energyCost = Math.max(0F, energyCost);
        cooldownTicks = Math.max(0, cooldownTicks);
    }
}
