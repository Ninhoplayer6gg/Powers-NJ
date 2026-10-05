package com.powersnj.compat.palladium.ability;

import com.powersnj.flight.FlightMovementController;
import com.powersnj.player.PowersPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.threetag.palladium.power.ability.AbilityInstance;

/**
 * {@code powersnj:high_speed_flight} - toggle: sustained high-speed flight, draining the suit
 * resource every tick while airborne (handled by the flight controller).
 */
public class HighSpeedFlightAbility extends PowersAbility {

    @Override
    public boolean onActivated(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        if (!(data.movement() instanceof FlightMovementController flight)) {
            return false;
        }
        flight.controller().setHighSpeed(true);
        return true;
    }

    @Override
    protected void whileEnabled(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        if (data.movement() instanceof FlightMovementController flight && !flight.controller().isHighSpeedRequested() && player.tickCount % 40 == 0) {
            // Re-arm after the controller dropped high speed for lack of energy and energy recovered.
            flight.controller().setHighSpeed(true);
        }
    }

    @Override
    protected void onDisabled(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        if (data.movement() instanceof FlightMovementController flight) {
            flight.controller().setHighSpeed(false);
        }
    }

    @Override
    public String getDocumentationDescription() {
        return "Powers NJ: high speed flight toggle.";
    }
}
