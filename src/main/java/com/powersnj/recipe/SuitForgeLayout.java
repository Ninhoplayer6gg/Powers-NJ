package com.powersnj.recipe;

import com.powersnj.core.fabrication.FabricationMatcher;
import net.minecraft.world.Container;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Slot layout shared by the Suit Forge block entity, menu, screen and recipe.
 */
public final class SuitForgeLayout {

    public static final int BLUEPRINT = 0;
    public static final int POWER_CORE = 1;
    public static final int MATERIAL_START = 2;
    public static final int MATERIAL_COUNT = 6;
    public static final int OUTPUT_START = MATERIAL_START + MATERIAL_COUNT;
    public static final int OUTPUT_COUNT = 4;
    public static final int SIZE = OUTPUT_START + OUTPUT_COUNT;

    /** Output slot order: helmet, chestplate, leggings, boots. */
    public static final List<ArmorItem.Type> OUTPUT_ORDER = List.of(ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS);

    private SuitForgeLayout() {
    }

    public static boolean isMaterialSlot(int slot) {
        return slot >= MATERIAL_START && slot < OUTPUT_START;
    }

    public static boolean isOutputSlot(int slot) {
        return slot >= OUTPUT_START && slot < SIZE;
    }

    public static List<FabricationMatcher.SlotContent> materialContents(Container container) {
        List<FabricationMatcher.SlotContent> list = new ArrayList<>(MATERIAL_COUNT);
        for (int i = 0; i < MATERIAL_COUNT; i++) {
            list.add(content(container.getItem(MATERIAL_START + i)));
        }
        return list;
    }

    public static List<FabricationMatcher.SlotContent> materialContents(IItemHandler handler) {
        List<FabricationMatcher.SlotContent> list = new ArrayList<>(MATERIAL_COUNT);
        for (int i = 0; i < MATERIAL_COUNT; i++) {
            list.add(content(handler.getStackInSlot(MATERIAL_START + i)));
        }
        return list;
    }

    private static FabricationMatcher.SlotContent content(ItemStack stack) {
        return stack.isEmpty() ? null : new FabricationMatcher.SlotContent(SuitFabricationRecipe.key(stack.getItem()), stack.getCount());
    }
}
