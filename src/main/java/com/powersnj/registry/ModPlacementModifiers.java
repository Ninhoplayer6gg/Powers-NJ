package com.powersnj.registry;

import com.powersnj.PowersNJ;
import com.powersnj.world.ConfiguredCountPlacement;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModPlacementModifiers {

    public static final DeferredRegister<PlacementModifierType<?>> TYPES = DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, PowersNJ.MOD_ID);

    /** Vein count read from the server config at generation time (0 when worldgen is disabled). */
    public static final RegistryObject<PlacementModifierType<ConfiguredCountPlacement>> CONFIGURED_COUNT = TYPES.register("configured_count",
            () -> () -> ConfiguredCountPlacement.CODEC);

    private ModPlacementModifiers() {
    }
}
