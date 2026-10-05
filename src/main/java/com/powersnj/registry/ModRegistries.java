package com.powersnj.registry;

import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Registers every Powers NJ deferred register on the mod event bus, in dependency order.
 */
public final class ModRegistries {

    private ModRegistries() {
    }

    public static void register(IEventBus modBus) {
        ModSounds.SOUNDS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModRecipes.TYPES.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        ModLootModifiers.SERIALIZERS.register(modBus);
        ModPlacementModifiers.TYPES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
    }
}
