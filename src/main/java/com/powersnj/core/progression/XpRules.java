package com.powersnj.core.progression;

/**
 * How much suit XP each gameplay action grants (before the server {@code xpMultiplier}).
 *
 * @param killMob            XP for killing a regular mob (scaled by the mob's max health / 20)
 * @param killPlayer         XP for killing a player
 * @param killBoss           XP for killing a boss (entity tag {@code forge:bosses})
 * @param abilityUse         default XP per successful ability activation (abilities may override)
 * @param damageDealtFactor  XP per point of damage dealt while wearing the suit
 * @param travelPer100Blocks XP per 100 blocks travelled with the suit movement controller
 */
public record XpRules(double killMob, double killPlayer, double killBoss, double abilityUse, double damageDealtFactor, double travelPer100Blocks) {

    public static final XpRules DEFAULT = new XpRules(6D, 20D, 150D, 1D, 0.15D, 4D);

    public XpRules {
        if (killMob < 0 || killPlayer < 0 || killBoss < 0 || abilityUse < 0 || damageDealtFactor < 0 || travelPer100Blocks < 0) {
            throw new IllegalArgumentException("XP rules must not be negative");
        }
    }

    public static long scaled(double rawXp, double multiplier) {
        if (rawXp <= 0 || multiplier <= 0) {
            return 0;
        }
        return Math.max(1L, Math.round(rawXp * multiplier));
    }

    public double forMobKill(float mobMaxHealth) {
        return this.killMob * Math.max(0.25D, mobMaxHealth / 20D);
    }
}
