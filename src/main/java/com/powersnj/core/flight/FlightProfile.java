package com.powersnj.core.flight;

import com.powersnj.core.suit.SuitDefinition;

/**
 * Tuning of the generic {@link FlightController}. Values are Palladium {@code flight_speed}
 * attribute units (1.0 &asymp; 30 blocks/s while sprint-flying).
 *
 * @param cruiseSpeed      normal flight speed
 * @param boostSpeed       speed during a boost
 * @param highSpeed        sustained high-speed flight
 * @param acceleration     max speed increase per tick
 * @param deceleration     max speed decrease per tick
 * @param boostDuration    boost length in ticks
 * @param highSpeedDrain   energy drained per tick in high-speed mode
 * @param impactMinSpeed   actual speed (blocks/tick) needed to deal ramming damage
 * @param impactDamage     damage per block/tick above {@code impactMinSpeed}
 */
public record FlightProfile(double cruiseSpeed, double boostSpeed, double highSpeed, double acceleration, double deceleration,
                            int boostDuration, float highSpeedDrain, double impactMinSpeed, double impactDamage) {

    /** Upper bound of Palladium's flight_speed attribute. */
    public static final double MAX_ATTRIBUTE = 32D;

    public static final FlightProfile DEFAULT = new FlightProfile(0.8D, 2.0D, 3.0D, 0.08D, 0.15D, 40, 0.6F, 1.2D, 6D);

    public FlightProfile {
        if (cruiseSpeed < 0 || boostSpeed < 0 || highSpeed < 0 || acceleration <= 0 || deceleration <= 0 || boostDuration < 0) {
            throw new IllegalArgumentException("Invalid flight profile");
        }
    }

    public static FlightProfile from(SuitDefinition definition) {
        FlightProfile d = DEFAULT;
        return new FlightProfile(
                definition.setting("cruise_speed", d.cruiseSpeed),
                definition.setting("boost_speed", d.boostSpeed),
                definition.setting("high_speed", d.highSpeed),
                definition.setting("acceleration", d.acceleration),
                definition.setting("deceleration", d.deceleration),
                (int) definition.setting("boost_duration", d.boostDuration),
                (float) definition.setting("high_speed_drain", d.highSpeedDrain),
                definition.setting("impact_min_speed", d.impactMinSpeed),
                definition.setting("impact_damage", d.impactDamage));
    }
}
