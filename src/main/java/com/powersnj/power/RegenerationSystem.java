package com.powersnj.power;

import com.powersnj.core.energy.EnergyPool;
import com.powersnj.core.suit.SuitDefinition;
import com.powersnj.player.PowersPlayerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code powersnj:regeneration}: heals the wearer over time, scaling with suit level and paid with
 * the suit resource. Used for Viltrumite regeneration, symbiote healing and the speedster's
 * accelerated healing (faster while running, setting {@code regen_running_multiplier}).
 * <p>
 * Settings: {@code regen_amount}, {@code regen_per_level}, {@code regen_interval},
 * {@code regen_energy_cost}, {@code regen_running_multiplier}.
 */
public final class RegenerationSystem implements PowerSystem {

    public static final String ID = "powersnj:regeneration";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void tick(ServerPlayer player, PowersPlayerData data, SuitDefinition definition) {
        if (player.getHealth() >= player.getMaxHealth() || player.isDeadOrDying()) {
            return;
        }
        int interval = Math.max(1, (int) definition.setting("regen_interval", 40));
        boolean running = data.movement() != null && data.lastSentMovement() != null && data.lastSentMovement().has(com.powersnj.core.net.MovementSnapshot.FLAG_ACTIVE);
        if (running) {
            interval = Math.max(1, (int) Math.round(interval / Math.max(1D, definition.setting("regen_running_multiplier", 1D))));
        }
        if (player.tickCount % interval != 0) {
            return;
        }
        int level = Math.max(1, data.activeLevel());
        float amount = (float) (definition.setting("regen_amount", 1D) + definition.setting("regen_per_level", 0.05D) * (level - 1));
        float cost = (float) definition.setting("regen_energy_cost", 2D);
        EnergyPool pool = data.activeEnergy(false).orElse(null);
        if (cost > 0F && (pool == null || !pool.tryConsume(cost))) {
            return;
        }
        player.heal(amount);
    }
}
