package com.powersnj.compat.palladium.ability;

import com.powersnj.movement.AttributeHelper;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.registry.ModParticles;
import com.powersnj.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.threetag.palladium.power.ability.AbilityInstance;

import java.util.UUID;

/**
 * {@code powersnj:symbiote_shield} - held/toggle: the symbiote hardens around the host. Absorbs
 * damage by spending biomass (see {@code CombatEvents#symbioteShield}) and grants knockback
 * resistance, at the cost of movement speed.
 */
public class SymbioteShieldAbility extends PowersAbility {

    private static final UUID KNOCKBACK_ID = UUID.fromString("5f0e0b8e-6b77-4d0f-9a4b-3f2f51f0d001");
    private static final UUID SLOW_ID = UUID.fromString("5f0e0b8e-6b77-4d0f-9a4b-3f2f51f0d002");

    @Override
    public boolean onActivated(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        data.setShieldActive(true);
        AttributeHelper.set(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, "powersnj symbiote shield", 0.8D, AttributeModifier.Operation.ADDITION, 0.01D);
        AttributeHelper.set(player, Attributes.MOVEMENT_SPEED, SLOW_ID, "powersnj symbiote shield", -0.35D, AttributeModifier.Operation.MULTIPLY_TOTAL, 0.01D);
        player.level().playSound(null, player.blockPosition(), ModSounds.SYMBIOTE_SHIELD.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    @Override
    protected void whileEnabled(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        data.setShieldActive(true);
        if (player.tickCount % 10 == 0) {
            player.serverLevel().sendParticles(ModParticles.SYMBIOTE_GOO.get(), player.getX(), player.getY(1.0D), player.getZ(), 3, 0.4, 0.6, 0.4, 0.01);
        }
    }

    @Override
    protected void onDisabled(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        data.setShieldActive(false);
        AttributeHelper.remove(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID);
        AttributeHelper.remove(player, Attributes.MOVEMENT_SPEED, SLOW_ID);
    }

    @Override
    public String getDocumentationDescription() {
        return "Powers NJ: biomass-fuelled damage absorbing shield.";
    }
}
