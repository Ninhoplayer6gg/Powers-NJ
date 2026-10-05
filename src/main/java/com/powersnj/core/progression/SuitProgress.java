package com.powersnj.core.progression;

import com.powersnj.core.data.DataNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Progression of one player with one suit: level, XP inside the current level, unlocked skills,
 * spent skill points and gameplay statistics (used by skill requirements).
 * <p>
 * One skill point is earned per level (level 1 already grants one point).
 */
public final class SuitProgress {

    public static final int POINTS_PER_LEVEL = 1;

    private final String suitId;
    private int level = 1;
    private long xp;
    private int spentPoints;
    private final Set<String> unlockedSkills = new LinkedHashSet<>();
    private final Map<String, Long> stats = new LinkedHashMap<>();
    private boolean dirty = true;

    public SuitProgress(String suitId) {
        this.suitId = suitId;
    }

    /**
     * Read-only mirror built from network data (client skill tree screen). Statistics are not
     * synced, so statistic requirements are displayed but only validated by the server.
     */
    public static SuitProgress mirror(String suitId, int level, long xp, java.util.Collection<String> skills, int availablePoints) {
        SuitProgress progress = new SuitProgress(suitId);
        progress.level = Math.max(1, level);
        progress.xp = Math.max(0, xp);
        progress.unlockedSkills.addAll(skills);
        progress.spentPoints = Math.max(0, progress.earnedPoints() - Math.max(0, availablePoints));
        progress.dirty = false;
        return progress;
    }

    public String suitId() {
        return this.suitId;
    }

    public int level() {
        return this.level;
    }

    public long xp() {
        return this.xp;
    }

    public int spentPoints() {
        return this.spentPoints;
    }

    public int earnedPoints() {
        return this.level * POINTS_PER_LEVEL;
    }

    public int availablePoints() {
        return Math.max(0, this.earnedPoints() - this.spentPoints);
    }

    public Set<String> unlockedSkills() {
        return Collections.unmodifiableSet(this.unlockedSkills);
    }

    public boolean isUnlocked(String skillId) {
        return this.unlockedSkills.contains(skillId);
    }

    public long stat(String key) {
        return this.stats.getOrDefault(key, 0L);
    }

    public Map<String, Long> stats() {
        return Collections.unmodifiableMap(this.stats);
    }

    public void incrementStat(String key, long amount) {
        if (amount != 0) {
            this.stats.merge(key, amount, Long::sum);
            this.dirty = true;
        }
    }

    /**
     * Adds XP, levelling up as many times as the curve allows. XP beyond the cap is discarded.
     */
    public XpGain addXp(long amount, LevelCurve curve) {
        if (amount <= 0 || this.level >= curve.maxLevel()) {
            return XpGain.none(this.level);
        }
        int oldLevel = this.level;
        long applied = 0;
        long remaining = amount;
        while (remaining > 0 && this.level < curve.maxLevel()) {
            long needed = curve.xpToNext(this.level) - this.xp;
            if (remaining >= needed) {
                remaining -= needed;
                applied += needed;
                this.level++;
                this.xp = 0;
            } else {
                this.xp += remaining;
                applied += remaining;
                remaining = 0;
            }
        }
        this.dirty = true;
        return new XpGain(applied, oldLevel, this.level);
    }

    /**
     * Admin/debug helper: forces a level (XP inside the level is reset). Spent points above the new
     * budget are refunded by re-locking nothing; the player simply has 0 available points.
     */
    public void setLevel(int level, LevelCurve curve) {
        this.level = curve.clampLevel(level);
        this.xp = 0;
        this.dirty = true;
    }

    /**
     * Records an unlock without validation. Gameplay code must go through
     * {@code SkillTree#unlock}, which validates level, parents, points and requirements first.
     */
    public void recordUnlock(String skillId, int cost) {
        this.unlockedSkills.add(skillId);
        this.spentPoints += Math.max(0, cost);
        this.dirty = true;
    }

    /**
     * Clears all unlocked skills and refunds every point.
     */
    public void resetSkills() {
        this.unlockedSkills.clear();
        this.spentPoints = 0;
        this.dirty = true;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void markClean() {
        this.dirty = false;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void write(DataNode node) {
        node.putString("suit", this.suitId);
        node.putInt("level", this.level);
        node.putLong("xp", this.xp);
        node.putInt("spent_points", this.spentPoints);
        node.putStringList("skills", this.unlockedSkills);
        DataNode statsNode = node.putChild("stats");
        this.stats.forEach(statsNode::putLong);
    }

    public void read(DataNode node, LevelCurve curve) {
        this.level = curve.clampLevel(node.getInt("level", 1));
        this.xp = Math.max(0, Math.min(node.getLong("xp", 0), Math.max(0, curve.xpToNext(this.level) - 1)));
        this.spentPoints = Math.max(0, node.getInt("spent_points", 0));
        this.unlockedSkills.clear();
        this.unlockedSkills.addAll(node.getStringList("skills"));
        this.stats.clear();
        DataNode statsNode = node.getChild("stats");
        for (String key : statsNode.keys()) {
            this.stats.put(key, statsNode.getLong(key, 0));
        }
        this.dirty = true;
    }
}
