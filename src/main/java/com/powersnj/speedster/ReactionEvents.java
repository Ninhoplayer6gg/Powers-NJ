package com.powersnj.speedster;

import com.powersnj.PowersNJ;
import com.powersnj.core.config.Settings;
import com.powersnj.core.suit.SuitDefinition;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.registry.ModParticles;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * {@code powersnj:reactions} system: a running speedster perceives projectiles in slow motion and
 * dodges them. Dodge chance = {@code reaction_dodge_chance} x normalised speed. Server decided.
 */
@Mod.EventBusSubscriber(modid = PowersNJ.MOD_ID)
public final class ReactionEvents {

    public static final String SYSTEM_ID = "powersnj:reactions";

    private ReactionEvents() {
    }

    @SubscribeEvent
    public static void dodge(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
            return;
        }
        PowersPlayerData data = PowersPlayerData.get(player).orElse(null);
        if (data == null || !(data.movement() instanceof SpeedsterMovementController speedster) || !speedster.engine().isActive()) {
            return;
        }
        SuitDefinition definition = data.activeDefinition(false).orElse(null);
        if (definition == null || !definition.hasSystem(SYSTEM_ID)) {
            return;
        }
        double chance = definition.setting("reaction_dodge_chance", 0.6D) * speedster.engine().normalizedSpeed(Settings.get());
        if (player.getRandom().nextDouble() < chance) {
            event.setCanceled(true);
            player.serverLevel().sendParticles(ModParticles.SPEED_SPARK.get(), player.getX(), player.getY(1.0D), player.getZ(), 6, 0.4, 0.5, 0.4, 0.05);
        }
    }
}
