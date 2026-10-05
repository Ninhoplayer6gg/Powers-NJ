package com.powersnj.registry;

import com.powersnj.PowersNJ;
import com.powersnj.menu.SuitForgeMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, PowersNJ.MOD_ID);

    public static final RegistryObject<MenuType<SuitForgeMenu>> SUIT_FORGE = MENUS.register("suit_forge",
            () -> IForgeMenuType.create(SuitForgeMenu::fromNetwork));

    private ModMenus() {
    }
}
