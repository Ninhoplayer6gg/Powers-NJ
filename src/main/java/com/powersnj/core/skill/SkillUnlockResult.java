package com.powersnj.core.skill;

/**
 * Outcome of a skill unlock attempt. Every value except {@link #SUCCESS} has a translated message
 * ({@code message.powersnj.skill.<lowercase name>}).
 */
public enum SkillUnlockResult {
    SUCCESS,
    UNKNOWN_SKILL,
    ALREADY_UNLOCKED,
    LEVEL_TOO_LOW,
    MISSING_PARENT,
    NOT_ENOUGH_POINTS,
    REQUIREMENT_NOT_MET;

    public boolean isSuccess() {
        return this == SUCCESS;
    }

    public String translationKey() {
        return "message.powersnj.skill." + this.name().toLowerCase(java.util.Locale.ROOT);
    }
}
