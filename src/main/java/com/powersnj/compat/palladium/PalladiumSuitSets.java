package com.powersnj.compat.palladium;

import com.powersnj.PowersNJ;
import com.powersnj.registry.ModItems;
import com.powersnj.suit.SuitKind;
import com.powersnj.suit.SuitKinds;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.threetag.palladium.item.SuitSet;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One Palladium {@link SuitSet} per {@link SuitKind} ({@code powersnj:<suit>}). Palladium grants
 * the power mapped in {@code data/powersnj/palladium/suit_set_powers/<suit>.json} while the full set
 * is worn: "suit equipped &rarr; Power Set".
 */
public final class PalladiumSuitSets {

    public static final DeferredRegister<SuitSet> SUIT_SETS = DeferredRegister.create(SuitSet.REGISTRY.getRegistryKey(), PowersNJ.MOD_ID);
    private static final Map<String, RegistryObject<SuitSet>> BY_SUIT = new LinkedHashMap<>();

    static {
        for (SuitKind kind : SuitKinds.all()) {
            BY_SUIT.put(kind.name(), SUIT_SETS.register(kind.name(), () -> new SuitSet(null, null,
                    () -> ModItems.suitPiece(kind, ArmorItem.Type.HELMET),
                    () -> ModItems.suitPiece(kind, ArmorItem.Type.CHESTPLATE),
                    () -> ModItems.suitPiece(kind, ArmorItem.Type.LEGGINGS),
                    () -> ModItems.suitPiece(kind, ArmorItem.Type.BOOTS))));
        }
    }

    private PalladiumSuitSets() {
    }

    public static RegistryObject<SuitSet> get(SuitKind kind) {
        return BY_SUIT.get(kind.name());
    }
}
