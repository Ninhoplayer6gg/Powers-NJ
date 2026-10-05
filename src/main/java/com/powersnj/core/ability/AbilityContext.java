package com.powersnj.core.ability;

/**
 * Server-side view of the activating player used by {@link AbilityGate}.
 */
public interface AbilityContext {

    boolean isSuitActive();

    int suitLevel();

    boolean isSkillUnlocked(String skillId);

    boolean isOnCooldown(String key);

    boolean hasEnergy(float amount);

    /**
     * @return true when a weakness (e.g. symbiote destabilised by fire/sound) blocks this ability
     */
    boolean isBlockedByWeakness(String key);

    /**
     * @return true when the server config disabled the system this ability belongs to
     */
    boolean isDisabledByConfig(String key);
}
