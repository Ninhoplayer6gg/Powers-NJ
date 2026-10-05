package com.powersnj.compat.palladium.ability;

import com.powersnj.compat.palladium.PalladiumBridge;
import com.powersnj.flight.FlightMovementController;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.registry.ModParticles;
import com.powersnj.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.threetag.palladium.power.ability.AbilityInstance;

/**
 * {@code powersnj:flight_boost} - short burst of acceleration for any suit using the generic
 * flight controller.
 */
public class FlightBoostAbility extends PowersActionAbility {

    @Override
    public boolean onActivated(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        if (!(data.movement() instanceof FlightMovementController flight) || !PalladiumBridge.isFlying(player)) {
            return false;
        }
        if (!flight.controller().requestBoost()) {
            return false;
        }
        player.level().playSound(null, player.blockPosition(), ModSounds.FLIGHT_BOOST.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.serverLevel().sendParticles(ModParticles.SHOCKWAVE_DUST.get(), player.getX(), player.getY(), player.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
        return true;
    }

    @Override
    public boolean reportsNoTarget() {
        return true;
    }

    @Override
    public String getDocumentationDescription() {
        return "Powers NJ: flight boost (requires flying).";
    }
}
