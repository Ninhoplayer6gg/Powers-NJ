package com.powersnj.core.progression;

import com.powersnj.core.data.MapDataNode;
import com.powersnj.core.skill.SkillNode;
import com.powersnj.core.skill.SkillTree;
import com.powersnj.core.skill.SkillUnlockResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProgressionTest {

    private static final LevelCurve CURVE = new LevelCurve(100D, 1D, 20);

    @Test
    void curveIsMonotonicAndCapped() {
        LevelCurve curve = LevelCurve.DEFAULT;
        long previous = 0;
        for (int level = 1; level < curve.maxLevel(); level++) {
            long needed = curve.xpToNext(level);
            assertTrue(needed >= previous, "XP per level must not decrease");
            previous = needed;
        }
        assertEquals(0, curve.xpToNext(curve.maxLevel()));
        assertEquals(20, curve.clampLevel(99));
        assertEquals(1, curve.clampLevel(-3));
    }

    @Test
    void addXpLevelsUpAcrossMultipleLevels() {
        SuitProgress progress = new SuitProgress("powersnj:thragg");
        // level 1 -> 2 needs 100, 2 -> 3 needs 200
        XpGain gain = progress.addXp(350, CURVE);
        assertEquals(1, gain.oldLevel());
        assertEquals(3, gain.newLevel());
        assertEquals(2, gain.levelsGained());
        assertEquals(50, progress.xp());
        assertEquals(350, gain.added());
    }

    @Test
    void xpStopsAtMaxLevel() {
        SuitProgress progress = new SuitProgress("powersnj:venom");
        XpGain gain = progress.addXp(Long.MAX_VALUE / 4, CURVE);
        assertEquals(20, progress.level());
        assertEquals(0, progress.xp());
        assertEquals(CURVE.totalXpFor(20), gain.added());
        assertFalse(progress.addXp(1000, CURVE).leveledUp());
        assertEquals(0, progress.addXp(1000, CURVE).added());
    }

    @Test
    void zeroOrNegativeXpIsIgnored() {
        SuitProgress progress = new SuitProgress("powersnj:venom");
        assertEquals(0, progress.addXp(0, CURVE).added());
        assertEquals(0, progress.addXp(-50, CURVE).added());
        assertEquals(1, progress.level());
    }

    @Test
    void skillPointsFollowLevels() {
        SuitProgress progress = new SuitProgress("powersnj:thragg");
        assertEquals(1, progress.availablePoints(), "level 1 grants one point");
        progress.addXp(100, CURVE);
        assertEquals(2, progress.availablePoints());
    }

    @Test
    void ledgerApiMatchesSpecification() {
        ProgressionLedger ledger = new ProgressionLedger();
        SkillTree tree = SkillTree.of(List.of(SkillNode.simple("slam", 1, 1), SkillNode.simple("quake", 2, 1, "slam")));

        assertEquals(1, ledger.getSuitLevel("powersnj:thragg"));
        assertFalse(ledger.isSkillUnlocked("powersnj:thragg", "slam"));

        assertEquals(SkillUnlockResult.SUCCESS, ledger.unlockSkill("powersnj:thragg", "slam", tree));
        assertTrue(ledger.isSkillUnlocked("powersnj:thragg", "slam"));
        assertEquals(SkillUnlockResult.LEVEL_TOO_LOW, ledger.unlockSkill("powersnj:thragg", "quake", tree));

        ledger.addSuitXp("powersnj:thragg", 100, CURVE);
        assertEquals(2, ledger.getSuitLevel("powersnj:thragg"));
        assertEquals(SkillUnlockResult.SUCCESS, ledger.unlockSkill("powersnj:thragg", "quake", tree));
        assertFalse(ledger.isSkillUnlocked("powersnj:venom", "slam"), "progress is per suit");
    }

    @Test
    void ledgerSurvivesSerialization() {
        ProgressionLedger ledger = new ProgressionLedger();
        SkillTree tree = SkillTree.of(List.of(SkillNode.simple("slam", 1, 1)));
        ledger.addSuitXp("powersnj:thragg", 450, CURVE);
        ledger.unlockSkill("powersnj:thragg", "slam", tree);
        ledger.progress("powersnj:thragg").incrementStat("kills", 7);
        ledger.addSuitXp("powersnj:venom", 20, CURVE);

        MapDataNode node = new MapDataNode();
        ledger.write(node);

        ProgressionLedger restored = new ProgressionLedger();
        restored.read(node, id -> CURVE);
        SuitProgress thragg = restored.find("powersnj:thragg").orElseThrow();
        assertEquals(3, thragg.level());
        assertEquals(150, thragg.xp());
        assertTrue(thragg.isUnlocked("slam"));
        assertEquals(1, thragg.spentPoints());
        assertEquals(7, thragg.stat("kills"));
        assertEquals(20, restored.find("powersnj:venom").orElseThrow().xp());
    }

    @Test
    void corruptedSaveIsClamped() {
        MapDataNode node = new MapDataNode();
        node.putInt("level", 999);
        node.putLong("xp", -5);
        SuitProgress progress = new SuitProgress("powersnj:thragg");
        progress.read(node, CURVE);
        assertEquals(20, progress.level());
        assertEquals(0, progress.xp());
    }

    @Test
    void xpRulesScaleWithMultiplier() {
        assertEquals(0, XpRules.scaled(5, 0));
        assertEquals(10, XpRules.scaled(5, 2));
        assertEquals(1, XpRules.scaled(0.1, 1), "positive XP always grants at least 1");
        assertTrue(XpRules.DEFAULT.forMobKill(200F) > XpRules.DEFAULT.forMobKill(20F));
    }
}
