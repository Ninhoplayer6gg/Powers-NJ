package com.powersnj.registry;

import com.powersnj.PowersNJ;
import com.powersnj.block.SuitForgeBlockEntity;
import com.powersnj.block.SuitStandBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, PowersNJ.MOD_ID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<SuitForgeBlockEntity>> SUIT_FORGE = BLOCK_ENTITIES.register("suit_forge",
            () -> BlockEntityType.Builder.of(SuitForgeBlockEntity::new, ModBlocks.SUIT_FORGE.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<SuitStandBlockEntity>> SUIT_STAND = BLOCK_ENTITIES.register("suit_stand",
            () -> BlockEntityType.Builder.of(SuitStandBlockEntity::new, ModBlocks.SUIT_STAND.get()).build(null));

    private ModBlockEntities() {
    }
}
