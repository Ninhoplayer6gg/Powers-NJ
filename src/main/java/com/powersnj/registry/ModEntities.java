package com.powersnj.registry;

import com.powersnj.PowersNJ;
import com.powersnj.symbiote.TendrilEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, PowersNJ.MOD_ID);

    /** Symbiote tendril (grab / pull / swing). Rendered by TendrilRenderer, GeckoLib-ready. */
    public static final RegistryObject<EntityType<TendrilEntity>> TENDRIL = ENTITIES.register("tendril",
            () -> EntityType.Builder.<TendrilEntity>of(TendrilEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSummon()
                    .build(PowersNJ.id("tendril").toString()));

    private ModEntities() {
    }
}
