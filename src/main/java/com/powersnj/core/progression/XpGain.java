package com.powersnj.core.progression;

/**
 * Result of adding XP to a suit.
 *
 * @param added    XP actually applied (0 when the suit is maxed)
 * @param oldLevel level before the gain
 * @param newLevel level after the gain
 */
public record XpGain(long added, int oldLevel, int newLevel) {

    public static XpGain none(int level) {
        return new XpGain(0, level, level);
    }

    public boolean leveledUp() {
        return this.newLevel > this.oldLevel;
    }

    public int levelsGained() {
        return this.newLevel - this.oldLevel;
    }
}
