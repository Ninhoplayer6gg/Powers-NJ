package com.powersnj.core.skill;

/**
 * Display state of a skill node for a given progress.
 */
public enum SkillStatus {
    /** Already unlocked. */
    UNLOCKED,
    /** Every requirement is met; the player can unlock it now. */
    AVAILABLE,
    /** Parents are unlocked but level, points or statistic requirements are missing. */
    REACHABLE,
    /** At least one parent is still locked. */
    LOCKED
}
