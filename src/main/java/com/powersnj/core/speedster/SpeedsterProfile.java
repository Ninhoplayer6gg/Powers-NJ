package com.powersnj.core.speedster;

import com.powersnj.core.suit.SuitDefinition;

/**
 * Tuning of a speedster. Speeds are multipliers of the player's normal movement speed.
 *
 * @param baseMultiplier        multiplier at speed level 1
 * @param maxMultiplier         multiplier at the top speed level (before the server multiplier)
 * @param speedLevels           number of selectable speed levels
 * @param acceleration          multiplier gained per tick
 * @param deceleration          multiplier lost per tick when not moving / slowing down
 * @param turnRateAtMax         steering responsiveness at top speed (1 = instant, 0 = none)
 * @param drainAtMax            energy drained per tick at top speed (linear from 0 at 1x)
 * @param wallRunMin            multiplier needed to run up walls
 * @param waterRunMin           multiplier needed to run on water
 * @param collisionMin          multiplier above which hitting a wall/entity is an impact
 * @param collisionDamage       damage per multiplier point above {@code collisionMin}
 * @param stepHeightBonus       extra step height while running (blocks)
 */
public record SpeedsterProfile(double baseMultiplier, double maxMultiplier, int speedLevels, double acceleration, double deceleration,
                               double turnRateAtMax, float drainAtMax, double wallRunMin, double waterRunMin,
                               double collisionMin, double collisionDamage, double stepHeightBonus) {

    public static final SpeedsterProfile DEFAULT = new SpeedsterProfile(2.0D, 12.0D, 5, 0.12D, 0.45D, 0.25D, 0.45F, 3.0D, 2.5D, 4.0D, 1.5D, 1.0D);

    public SpeedsterProfile {
        if (baseMultiplier < 1 || maxMultiplier < baseMultiplier || speedLevels < 1 || acceleration <= 0 || deceleration <= 0) {
            throw new IllegalArgumentException("Invalid speedster profile");
        }
        turnRateAtMax = Math.max(0.01D, Math.min(1D, turnRateAtMax));
    }

    public static SpeedsterProfile from(SuitDefinition definition) {
        SpeedsterProfile d = DEFAULT;
        return new SpeedsterProfile(
                definition.setting("base_multiplier", d.baseMultiplier),
                definition.setting("max_multiplier", d.maxMultiplier),
                (int) definition.setting("speed_levels", d.speedLevels),
                definition.setting("acceleration", d.acceleration),
                definition.setting("deceleration", d.deceleration),
                definition.setting("turn_rate_at_max", d.turnRateAtMax),
                (float) definition.setting("drain_at_max", d.drainAtMax),
                definition.setting("wall_run_min", d.wallRunMin),
                definition.setting("water_run_min", d.waterRunMin),
                definition.setting("collision_min", d.collisionMin),
                definition.setting("collision_damage", d.collisionDamage),
                definition.setting("step_height_bonus", d.stepHeightBonus));
    }
}
