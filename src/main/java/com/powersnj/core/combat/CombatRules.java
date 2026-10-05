package com.powersnj.core.combat;

import com.powersnj.core.config.PowersSettings;

/**
 * Damage balancing. Power damage is authored against mobs; players receive it through
 * {@code pvpDamageMultiplier} (default 0.35) so a ground slam that obliterates a zombie does not
 * one-shot a player, and bosses through {@code bossDamageMultiplier} so they are not trivialised.
 */
public final class CombatRules {

    /** Knockback against players is softened so PvP stays playable. */
    public static final double PVP_KNOCKBACK_FACTOR = 0.6D;

    private CombatRules() {
    }

    /**
     * @param baseDamage       authored ability damage (PvE value)
     * @param target           kind of victim
     * @param attackerIsPlayer whether a player caused the damage (mobs using powers are not rescaled)
     * @return final damage, 0 when PvP is disabled and the target is a player
     */
    public static float scaleDamage(float baseDamage, TargetKind target, boolean attackerIsPlayer, PowersSettings settings) {
        if (baseDamage <= 0F) {
            return 0F;
        }
        if (!attackerIsPlayer) {
            return baseDamage;
        }
        return switch (target) {
            case PLAYER -> settings.enablePvP() ? (float) (baseDamage * settings.pvpDamageMultiplier()) : 0F;
            case MOB -> (float) (baseDamage * settings.pveDamageMultiplier());
            case BOSS -> (float) (baseDamage * settings.pveDamageMultiplier() * settings.bossDamageMultiplier());
        };
    }

    public static boolean canHit(TargetKind target, boolean attackerIsPlayer, PowersSettings settings) {
        return !(attackerIsPlayer && target == TargetKind.PLAYER && !settings.enablePvP());
    }

    public static double scaleKnockback(double base, TargetKind target) {
        return target == TargetKind.PLAYER ? base * PVP_KNOCKBACK_FACTOR : base;
    }

    /**
     * Linear level scaling: level 1 = {@code base}, max level = {@code base * (1 + bonusAtMax)}.
     */
    public static float levelScaled(float base, int level, int maxLevel, float bonusAtMax) {
        if (maxLevel <= 1) {
            return base;
        }
        float t = (Math.max(1, Math.min(level, maxLevel)) - 1) / (float) (maxLevel - 1);
        return base * (1F + bonusAtMax * t);
    }
}
