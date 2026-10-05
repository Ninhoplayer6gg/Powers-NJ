package com.powersnj.suit;

import com.powersnj.PowersNJ;
import com.powersnj.registry.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Every suit known at registration time. Adding a character = one {@link #register} line here, a
 * suit definition JSON, a Palladium power JSON, a fabrication recipe and the assets listed in
 * {@code ASSET_REQUIREMENTS.md}. Items, Palladium suit sets, creative tab entries and renderers are
 * derived automatically from this list.
 */
public final class SuitKinds {

    private static final List<SuitKind> ALL = new ArrayList<>();

    public static final SuitKind THRAGG = register(new SuitKind("thragg",
            new SuitArmorMaterial(PowersNJ.MOD_ID + ":thragg", 40, 4, 9, 7, 4, 12,
                    () -> SoundEvents.ARMOR_EQUIP_NETHERITE, 3.5F, 0.2F, () -> Ingredient.of(ModItems.VILTRUMITE_ALLOY.get())),
            () -> PowersNJ.id("viltrumite_blueprint")));

    public static final SuitKind VENOM = register(new SuitKind("venom",
            new SuitArmorMaterial(PowersNJ.MOD_ID + ":venom", 33, 3, 8, 6, 3, 15,
                    () -> SoundEvents.ARMOR_EQUIP_LEATHER, 2.0F, 0.1F, () -> Ingredient.of(ModItems.SYMBIOTIC_FIBER.get())),
            () -> PowersNJ.id("symbiote_blueprint")));

    public static final SuitKind REVERSE_FLASH = register(new SuitKind("reverse_flash",
            new SuitArmorMaterial(PowersNJ.MOD_ID + ":reverse_flash", 30, 3, 7, 6, 3, 15,
                    () -> SoundEvents.ARMOR_EQUIP_LEATHER, 1.5F, 0.0F, () -> Ingredient.of(ModItems.CONDUCTIVE_FABRIC.get())),
            () -> PowersNJ.id("speedster_blueprint")));

    private SuitKinds() {
    }

    private static SuitKind register(SuitKind kind) {
        ALL.add(kind);
        return kind;
    }

    public static List<SuitKind> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static Optional<SuitKind> byName(String name) {
        return ALL.stream().filter(k -> k.name().equals(name)).findFirst();
    }

    public static Optional<SuitKind> bySuitId(String suitId) {
        return ALL.stream().filter(k -> k.suitId().equals(suitId)).findFirst();
    }
}
