package com.powersnj.progression;

import com.powersnj.PowersNJ;
import com.powersnj.combat.CombatService;
import com.powersnj.core.combat.TargetKind;
import com.powersnj.core.suit.SuitDefinition;
import com.powersnj.player.PowersPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * XP sources while wearing a suit: kills (mob / player / boss), damage dealt, ability use (granted
 * by the ability executor) and travel (reported by movement controllers). All server side.
 */
@Mod.EventBusSubscriber(modid = PowersNJ.MOD_ID)
public final class ProgressionEvents {

    private ProgressionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onKill(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim == player) {
            return;
        }
        PowersPlayerData data = PowersPlayerData.get(player).orElse(null);
        if (data == null || !data.hasActiveSuit()) {
            return;
        }
        SuitDefinition definition = data.activeDefinition(false).orElse(null);
        if (definition == null) {
            return;
        }
        TargetKind kind = CombatService.kindOf(victim);
        double xp = switch (kind) {
            case PLAYER -> definition.xpRules().killPlayer();
            case BOSS -> definition.xpRules().killBoss();
            case MOB -> definition.xpRules().forMobKill(victim.getMaxHealth());
        };
        String suit = data.activeSuit();
        ProgressionAPI.incrementStat(player, suit, "kills", 1);
        if (kind == TargetKind.BOSS) {
            ProgressionAPI.incrementStat(player, suit, "boss_kills", 1);
        } else if (kind == TargetKind.PLAYER) {
            ProgressionAPI.incrementStat(player, suit, "player_kills", 1);
        }
        ProgressionAPI.addSuitXp(player, suit, xp);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamageDealt(LivingDamageEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0 || !(event.getSource().getEntity() instanceof ServerPlayer player) || event.getEntity() == player) {
            return;
        }
        PowersPlayerData data = PowersPlayerData.get(player).orElse(null);
        if (data == null || !data.hasActiveSuit()) {
            return;
        }
        SuitDefinition definition = data.activeDefinition(false).orElse(null);
        if (definition == null) {
            return;
        }
        float dealt = Math.min(event.getAmount(), event.getEntity().getHealth());
        ProgressionAPI.incrementStat(player, data.activeSuit(), "damage_dealt", Math.round(dealt));
        long whole = data.addPendingXp(dealt * definition.xpRules().damageDealtFactor());
        if (whole > 0) {
            ProgressionAPI.addSuitXp(player, data.activeSuit(), whole);
        }
    }

    /**
     * Called by movement controllers with the distance moved this tick using the suit's movement.
     */
    public static void onTravel(ServerPlayer player, PowersPlayerData data, double blocks) {
        if (blocks <= 0 || !data.hasActiveSuit()) {
            return;
        }
        int hundreds = data.addTravel(blocks);
        if (hundreds > 0) {
            SuitDefinition definition = data.activeDefinition(false).orElse(null);
            if (definition != null) {
                ProgressionAPI.incrementStat(player, data.activeSuit(), "distance", hundreds * 100L);
                ProgressionAPI.addSuitXp(player, data.activeSuit(), definition.xpRules().travelPer100Blocks() * hundreds);
            }
        }
    }
}
