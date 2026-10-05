package com.powersnj.registry;

import com.powersnj.PowersNJ;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

/**
 * Damage type keys. Definitions live in {@code data/powersnj/damage_type/*.json}.
 */
public final class ModDamageTypes {

    public static final ResourceKey<DamageType> SUPER_STRENGTH = key("super_strength");
    public static final ResourceKey<DamageType> SHOCKWAVE = key("shockwave");
    public static final ResourceKey<DamageType> TENDRIL = key("tendril");
    public static final ResourceKey<DamageType> SPEED_STRIKE = key("speed_strike");
    public static final ResourceKey<DamageType> IMPACT = key("impact");

    private ModDamageTypes() {
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, PowersNJ.id(name));
    }
}
