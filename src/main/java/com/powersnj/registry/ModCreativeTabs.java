package com.powersnj.registry;

import com.powersnj.PowersNJ;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PowersNJ.MOD_ID);

    public static final RegistryObject<CreativeModeTab> POWERS_NJ = TABS.register("powers_nj", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.powersnj"))
            .icon(() -> new ItemStack(ModItems.POWER_CORE.get()))
            .displayItems((parameters, output) -> ModItems.ordered().forEach(item -> output.accept(item.get())))
            .build());

    private ModCreativeTabs() {
    }
}
