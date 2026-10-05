package com.powersnj.core.combat;

import com.powersnj.core.config.PowersSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CombatRulesTest {

    @Test
    void pvpAndPveUseDifferentMultipliers() {
        PowersSettings s = PowersSettings.DEFAULTS;
        assertEquals(20F * (float) s.pveDamageMultiplier(), CombatRules.scaleDamage(20F, TargetKind.MOB, true, s), 1e-5);
        assertEquals(20F * (float) s.pvpDamageMultiplier(), CombatRules.scaleDamage(20F, TargetKind.PLAYER, true, s), 1e-5);
        assertEquals(20F * (float) (s.pveDamageMultiplier() * s.bossDamageMultiplier()), CombatRules.scaleDamage(20F, TargetKind.BOSS, true, s), 1e-5);
        assertTrue(CombatRules.scaleDamage(20F, TargetKind.PLAYER, true, s) < CombatRules.scaleDamage(20F, TargetKind.MOB, true, s),
                "players never take raw mob-balanced damage");
        assertEquals(20F, CombatRules.scaleDamage(20F, TargetKind.PLAYER, false, s), 1e-6, "non-player attackers are not rescaled");
    }

    @Test
    void pvpCanBeDisabled() {
        PowersSettings noPvp = new PowersSettings(false, 0.35, 1, 0.6, true, 48, 64, true, 1, 1, 1, 1, true, 8, true, true, 3, 2);
        assertEquals(0F, CombatRules.scaleDamage(50F, TargetKind.PLAYER, true, noPvp), 1e-6);
        assertFalse(CombatRules.canHit(TargetKind.PLAYER, true, noPvp));
        assertTrue(CombatRules.canHit(TargetKind.MOB, true, noPvp));
    }

    @Test
    void levelScaling() {
        assertEquals(10F, CombatRules.levelScaled(10F, 1, 20, 1F), 1e-6);
        assertEquals(20F, CombatRules.levelScaled(10F, 20, 20, 1F), 1e-6);
        assertEquals(20F, CombatRules.levelScaled(10F, 50, 20, 1F), 1e-6, "clamped to max level");
        assertTrue(CombatRules.scaleKnockback(2D, TargetKind.PLAYER) < CombatRules.scaleKnockback(2D, TargetKind.MOB));
    }

    @Test
    void settingsValidation() {
        assertThrows(IllegalArgumentException.class, () -> new PowersSettings(true, 1, 1, 1, true, -1, 64, true, 1, 1, 1, 1, true, 8, true, true, 3, 2));
        assertThrows(IllegalArgumentException.class, () -> new PowersSettings(true, 1, 1, 1, true, 10, 64, true, 1, 1, 1, -1, true, 8, true, true, 3, 2));
    }
}
