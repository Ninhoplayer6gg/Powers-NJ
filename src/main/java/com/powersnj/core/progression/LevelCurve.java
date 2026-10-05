package com.powersnj.core.progression;

/**
 * XP needed per level: {@code base * level^exponent}, capped at {@link #maxLevel()}.
 * With the defaults (base 50, exponent 1.35, 20 levels) a full suit needs roughly 21k XP.
 */
public record LevelCurve(double base, double exponent, int maxLevel) {

    public static final int DEFAULT_MAX_LEVEL = 20;
    public static final LevelCurve DEFAULT = new LevelCurve(50D, 1.35D, DEFAULT_MAX_LEVEL);

    public LevelCurve {
        if (base <= 0 || exponent < 0) {
            throw new IllegalArgumentException("Invalid level curve " + base + "/" + exponent);
        }
        if (maxLevel < 1 || maxLevel > 100) {
            throw new IllegalArgumentException("maxLevel must be within 1..100: " + maxLevel);
        }
    }

    /**
     * @return XP required to advance from {@code level} to {@code level + 1}, or 0 at the cap
     */
    public long xpToNext(int level) {
        if (level >= this.maxLevel) {
            return 0L;
        }
        return Math.max(1L, Math.round(this.base * Math.pow(Math.max(1, level), this.exponent)));
    }

    /**
     * @return total XP needed to reach {@code level} starting from level 1
     */
    public long totalXpFor(int level) {
        long total = 0;
        for (int l = 1; l < Math.min(level, this.maxLevel); l++) {
            total += this.xpToNext(l);
        }
        return total;
    }

    public int clampLevel(int level) {
        return Math.max(1, Math.min(this.maxLevel, level));
    }
}
