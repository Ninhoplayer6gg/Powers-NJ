package com.powersnj.core.skill;

import com.powersnj.core.progression.LevelCurve;
import com.powersnj.core.progression.SuitProgress;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SkillTreeTest {

    private static final LevelCurve CURVE = new LevelCurve(100D, 1D, 20);

    private static SkillTree tree() {
        return SkillTree.of(List.of(
                SkillNode.simple("root", 1, 1),
                SkillNode.simple("branch", 3, 2, "root"),
                new SkillNode("veteran", 1, List.of("root"), 1, new SkillRequirement("kills", 10), "", 1, 1),
                SkillNode.simple("capstone", 5, 1, "branch", "veteran")));
    }

    @Test
    void unlockFlowValidatesEveryRule() {
        SkillTree tree = tree();
        SuitProgress progress = new SuitProgress("powersnj:thragg");

        assertEquals(SkillUnlockResult.UNKNOWN_SKILL, tree.unlock(progress, "nope"));
        assertEquals(SkillUnlockResult.MISSING_PARENT, tree.unlock(progress, "branch"));
        assertEquals(SkillUnlockResult.SUCCESS, tree.unlock(progress, "root"));
        assertEquals(SkillUnlockResult.ALREADY_UNLOCKED, tree.unlock(progress, "root"));
        assertEquals(SkillUnlockResult.LEVEL_TOO_LOW, tree.unlock(progress, "branch"));

        progress.addXp(300, CURVE); // level 3, 3 points earned, 1 spent
        assertEquals(3, progress.level());
        assertEquals(SkillUnlockResult.SUCCESS, tree.unlock(progress, "branch"));
        assertEquals(0, progress.availablePoints());

        assertEquals(SkillUnlockResult.NOT_ENOUGH_POINTS, tree.unlock(progress, "veteran"));
        progress.addXp(300, CURVE); // level 4
        assertEquals(SkillUnlockResult.REQUIREMENT_NOT_MET, tree.unlock(progress, "veteran"));
        progress.incrementStat("kills", 10);
        assertEquals(SkillUnlockResult.SUCCESS, tree.unlock(progress, "veteran"));
    }

    @Test
    void statusReflectsProgress() {
        SkillTree tree = tree();
        SuitProgress progress = new SuitProgress("powersnj:thragg");
        assertEquals(SkillStatus.AVAILABLE, tree.status(progress, "root"));
        assertEquals(SkillStatus.LOCKED, tree.status(progress, "branch"));
        tree.unlock(progress, "root");
        assertEquals(SkillStatus.UNLOCKED, tree.status(progress, "root"));
        assertEquals(SkillStatus.REACHABLE, tree.status(progress, "branch"));
    }

    @Test
    void rejectsUnknownParents() {
        assertThrows(IllegalArgumentException.class, () -> SkillTree.of(List.of(SkillNode.simple("a", 1, 1, "ghost"))));
    }

    @Test
    void rejectsCycles() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> SkillTree.of(List.of(
                SkillNode.simple("a", 1, 1, "c"),
                SkillNode.simple("b", 1, 1, "a"),
                SkillNode.simple("c", 1, 1, "b"))));
        assertTrue(ex.getMessage().contains("cycle"));
    }

    @Test
    void rejectsDuplicatesAndInvalidNodes() {
        assertThrows(IllegalArgumentException.class, () -> SkillTree.of(List.of(SkillNode.simple("a", 1, 1), SkillNode.simple("a", 2, 1))));
        assertThrows(IllegalArgumentException.class, () -> SkillNode.simple("a", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> SkillNode.simple("a", 1, -1));
        assertThrows(IllegalArgumentException.class, () -> SkillNode.simple(" ", 1, 1));
    }

    @Test
    void resetRefundsPoints() {
        SkillTree tree = tree();
        SuitProgress progress = new SuitProgress("powersnj:thragg");
        tree.unlock(progress, "root");
        assertEquals(0, progress.availablePoints());
        progress.resetSkills();
        assertEquals(1, progress.availablePoints());
        assertFalse(progress.isUnlocked("root"));
    }
}
