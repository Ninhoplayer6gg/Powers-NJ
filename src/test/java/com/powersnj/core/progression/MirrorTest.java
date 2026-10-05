package com.powersnj.core.progression;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MirrorTest {

    @Test
    void mirrorReproducesPointsAndSkills() {
        SuitProgress mirror = SuitProgress.mirror("powersnj:venom", 5, 40, List.of("a", "b"), 2);
        assertEquals(5, mirror.level());
        assertEquals(40, mirror.xp());
        assertEquals(2, mirror.availablePoints());
        assertEquals(3, mirror.spentPoints());
        assertTrue(mirror.isUnlocked("b"));
        assertFalse(mirror.isDirty());
    }
}
