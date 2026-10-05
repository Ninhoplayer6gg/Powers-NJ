package com.powersnj.compat.palladium.ability;

import com.powersnj.player.PowersPlayerData;
import com.powersnj.speedster.SpeedsterMovementController;
import net.minecraft.server.level.ServerPlayer;
import net.threetag.palladium.power.ability.AbilityInstance;

/**
 * {@code powersnj:super_speed} - toggle that switches the SpeedsterEngine on/off. Speed, energy
 * drain, collisions and validation are handled by {@link SpeedsterMovementController}.
 */
public class SuperSpeedAbility extends PowersAbility {

    @Override
    public boolean onActivated(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        if (!(data.movement() instanceof SpeedsterMovementController speedster)) {
            return false;
        }
        speedster.setActive(player, true);
        return true;
    }

    @Override
    protected void whileEnabled(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        if (data.movement() instanceof SpeedsterMovementController speedster && !speedster.engine().isActive() && player.tickCount % 40 == 0) {
            // The engine stopped by itself (energy / validation); keep the toggle meaningful once recovered.
            data.activeEnergy(false).filter(pool -> pool.fraction() > 0.25F).ifPresent(pool -> speedster.setActive(player, true));
        }
    }

    @Override
    protected void onDisabled(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        if (data.movement() instanceof SpeedsterMovementController speedster) {
            speedster.setActive(player, false);
        }
    }

    @Override
    public String getDocumentationDescription() {
        return "Powers NJ: speedster super speed toggle.";
    }
}
