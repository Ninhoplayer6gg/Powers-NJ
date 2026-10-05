package com.powersnj.compat.palladium;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.threetag.palladium.entity.PalladiumPlayerExtension;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.power.ability.AbilityUtil;

/**
 * Read-only helpers over Palladium's public API, kept in one place so a Palladium update only
 * touches this class.
 */
public final class PalladiumBridge {

    private PalladiumBridge() {
    }

    /**
     * Whether the player is currently using Palladium flight (state is mirrored on the server by
     * Palladium's own flight packets).
     */
    public static boolean isFlying(Player player) {
        if (player instanceof PalladiumPlayerExtension extension) {
            return extension.palladium$getFlightHandler().getFlightType().isNotNull();
        }
        return false;
    }

    /**
     * Whether a Palladium ability (by power id + key) is currently enabled.
     */
    public static boolean isAbilityEnabled(LivingEntity entity, ResourceLocation powerId, String abilityKey) {
        return AbilityUtil.isEnabled(entity, powerId, abilityKey);
    }

    public static boolean isAbilityUnlocked(LivingEntity entity, ResourceLocation powerId, String abilityKey) {
        return AbilityUtil.isUnlocked(entity, powerId, abilityKey);
    }

    /**
     * Cooldown key used across Powers NJ for a Palladium ability instance.
     */
    public static String cooldownKey(AbilityInstance instance) {
        return instance.getReference().toString();
    }
}
