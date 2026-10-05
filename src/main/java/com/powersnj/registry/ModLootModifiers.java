package com.powersnj.registry;

import com.mojang.serialization.Codec;
import com.powersnj.PowersNJ;
import com.powersnj.world.AddItemLootModifier;
import com.powersnj.world.AddLootTableModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Global loot modifier codecs. Instances are data-driven in {@code data/powersnj/loot_modifiers}.
 */
public final class ModLootModifiers {

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, PowersNJ.MOD_ID);

    public static final RegistryObject<Codec<AddItemLootModifier>> ADD_ITEM = SERIALIZERS.register("add_item", () -> AddItemLootModifier.CODEC);
    public static final RegistryObject<Codec<AddLootTableModifier>> ADD_TABLE = SERIALIZERS.register("add_table", () -> AddLootTableModifier.CODEC);

    private ModLootModifiers() {
    }
}
