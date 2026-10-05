package com.powersnj.ability;

import com.powersnj.core.ability.AbilityContext;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.symbiote.SymbioteEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server view of the activating player for {@link com.powersnj.core.ability.AbilityGate}.
 */
public final class ServerAbilityContext implements AbilityContext {

    private final ServerPlayer player;
    private final PowersPlayerData data;
    private final boolean disabledByConfig;

    public ServerAbilityContext(ServerPlayer player, PowersPlayerData data, boolean disabledByConfig) {
        this.player = player;
        this.data = data;
        this.disabledByConfig = disabledByConfig;
    }

    @Override
    public boolean isSuitActive() {
        return this.data.hasActiveSuit();
    }

    @Override
    public int suitLevel() {
        return this.data.activeLevel();
    }

    @Override
    public boolean isSkillUnlocked(String skillId) {
        return this.data.ledger().isSkillUnlocked(this.data.activeSuit(), skillId);
    }

    @Override
    public boolean isOnCooldown(String key) {
        return this.data.cooldowns().isOnCooldown(key);
    }

    @Override
    public boolean hasEnergy(float amount) {
        return amount <= 0F || this.data.activeEnergy(false).map(pool -> pool.has(amount)).orElse(false);
    }

    @Override
    public boolean isBlockedByWeakness(String key) {
        return this.data.activeDefinition(false).map(def -> def.hasSystem(SymbioteEvents.SYSTEM_WEAKNESSES)).orElse(false)
                && SymbioteEvents.isDestabilized(this.player);
    }

    @Override
    public boolean isDisabledByConfig(String key) {
        return this.disabledByConfig;
    }
}
