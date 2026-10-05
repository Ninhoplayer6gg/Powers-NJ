package com.powersnj.ability;

import com.powersnj.compat.palladium.PalladiumBridge;
import com.powersnj.compat.palladium.ability.PowersAbility;
import com.powersnj.core.ability.AbilityCost;
import com.powersnj.core.ability.AbilityGate;
import com.powersnj.core.ability.ActivationResult;
import com.powersnj.core.config.Settings;
import com.powersnj.core.energy.EnergyPool;
import com.powersnj.core.suit.SuitDefinition;
import com.powersnj.network.AbilityFeedbackPacket;
import com.powersnj.network.PowersNetwork;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.progression.ProgressionAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.threetag.palladium.power.ability.AbilityInstance;

/**
 * Runs a Powers NJ ability activation on the server:
 * gate (suit, config, weakness, skill, level, cooldown, energy) &rarr; behaviour &rarr; pay
 * (energy + cooldown scaled by the config, mirrored into Palladium's own cooldown so the ability
 * bar shows it) &rarr; XP &rarr; feedback. Nothing is consumed when the behaviour finds no target.
 */
public final class AbilityExecutor {

    private AbilityExecutor() {
    }

    public static ActivationResult activate(ServerPlayer player, AbilityInstance instance, PowersAbility ability) {
        return activate(player, instance, ability, true);
    }

    /**
     * Retry path for toggles/passives: no chat feedback.
     */
    public static ActivationResult activateQuietly(ServerPlayer player, AbilityInstance instance, PowersAbility ability) {
        return activate(player, instance, ability, false);
    }

    private static ActivationResult activate(ServerPlayer player, AbilityInstance instance, PowersAbility ability, boolean feedback) {
        PowersPlayerData data = PowersPlayerData.get(player).orElse(null);
        if (data == null) {
            return ActivationResult.SUIT_INACTIVE;
        }
        String key = PalladiumBridge.cooldownKey(instance);
        AbilityCost cost = ability.cost(instance, key);
        ActivationResult result = AbilityGate.check(cost, new ServerAbilityContext(player, data, ability.isDisabledByConfig()));
        if (result.allowed()) {
            if (!ability.onActivated(player, instance, data)) {
                result = ActivationResult.NO_TARGET;
            } else {
                EnergyPool pool = data.activeEnergy(false).orElse(null);
                int cooldown = AbilityGate.commit(cost, pool, data.cooldowns(), Settings.get());
                if (cooldown < 0) {
                    result = ActivationResult.NOT_ENOUGH_ENERGY;
                } else {
                    if (cooldown > 0) {
                        instance.startCooldown(player, cooldown);
                    }
                    data.activeAbilities().add(key);
                    rewardUse(player, data, ability, instance);
                }
            }
        }
        if (!ability.isPassive() && (feedback || result.allowed())) {
            feedback(player, data, key, result, feedback && ability.reportsNoTarget());
        }
        return result;
    }

    /**
     * Per-tick check for toggle/held abilities: the suit must still be active, no weakness may block
     * it and the per-tick energy must be payable.
     */
    public static boolean sustain(ServerPlayer player, AbilityInstance instance, PowersAbility ability, PowersPlayerData data) {
        String key = PalladiumBridge.cooldownKey(instance);
        if (!data.activeAbilities().contains(key)) {
            return false;
        }
        ServerAbilityContext context = new ServerAbilityContext(player, data, ability.isDisabledByConfig());
        if (!context.isSuitActive() || context.isBlockedByWeakness(key) || context.isDisabledByConfig(key)) {
            data.activeAbilities().remove(key);
            return false;
        }
        float drain = instance.getProperty(PowersAbility.ENERGY_PER_TICK);
        if (drain > 0F) {
            EnergyPool pool = data.activeEnergy(false).orElse(null);
            if (pool == null || !pool.has(drain)) {
                data.activeAbilities().remove(key);
                player.displayClientMessage(Component.translatable(ActivationResult.NOT_ENOUGH_ENERGY.translationKey()).withStyle(ChatFormatting.RED), true);
                return false;
            }
            pool.drain(drain);
        }
        return true;
    }

    private static void rewardUse(ServerPlayer player, PowersPlayerData data, PowersAbility ability, AbilityInstance instance) {
        String suit = data.activeSuit();
        SuitDefinition definition = data.activeDefinition(false).orElse(null);
        if (definition == null) {
            return;
        }
        ProgressionAPI.incrementStat(player, suit, "ability_uses", 1);
        float custom = instance.getProperty(PowersAbility.XP_REWARD);
        double xp = custom >= 0F ? custom : definition.xpRules().abilityUse();
        if (xp > 0) {
            ProgressionAPI.addSuitXp(player, suit, xp);
        }
    }

    private static void feedback(ServerPlayer player, PowersPlayerData data, String key, ActivationResult result, boolean reportNoTarget) {
        data.setLastAbility(key);
        PowersNetwork.sendTo(player, new AbilityFeedbackPacket(key, result));
        boolean notify = switch (result) {
            case ALLOWED, ON_COOLDOWN -> false;
            case NO_TARGET -> reportNoTarget;
            default -> true;
        };
        if (notify) {
            player.displayClientMessage(Component.translatable(result.translationKey()).withStyle(ChatFormatting.RED), true);
        }
    }
}
