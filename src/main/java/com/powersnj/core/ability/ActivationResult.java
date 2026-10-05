package com.powersnj.core.ability;

import java.util.Locale;

/**
 * Why an ability activation was accepted or refused by the server.
 */
public enum ActivationResult {
    ALLOWED,
    SUIT_INACTIVE,
    DISABLED_BY_CONFIG,
    SKILL_LOCKED,
    LEVEL_TOO_LOW,
    ON_COOLDOWN,
    NOT_ENOUGH_ENERGY,
    BLOCKED_BY_WEAKNESS,
    NO_TARGET;

    public boolean allowed() {
        return this == ALLOWED;
    }

    public String translationKey() {
        return "message.powersnj.ability." + this.name().toLowerCase(Locale.ROOT);
    }
}
