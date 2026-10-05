package com.powersnj.registry;

import com.powersnj.PowersNJ;
import com.powersnj.recipe.SuitFabricationRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {

    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, PowersNJ.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, PowersNJ.MOD_ID);

    public static final RegistryObject<RecipeType<SuitFabricationRecipe>> SUIT_FABRICATION = TYPES.register("suit_fabrication",
            () -> RecipeType.simple(PowersNJ.id("suit_fabrication")));

    public static final RegistryObject<RecipeSerializer<SuitFabricationRecipe>> SUIT_FABRICATION_SERIALIZER = SERIALIZERS.register("suit_fabrication",
            SuitFabricationRecipe.Serializer::new);

    private ModRecipes() {
    }
}
