package com.powersnj.suit;

import com.powersnj.PowersNJ;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Static (registration-time) description of a suit: everything the item registry needs before data
 * packs exist. Gameplay numbers live in the data-driven suit definition
 * ({@code data/powersnj/powersnj/suits/<name>.json}) and the Palladium power
 * ({@code data/powersnj/palladium/powers/<name>.json}).
 *
 * @param name     path of the suit id, e.g. {@code thragg}
 * @param material armor stats
 * @param blueprint item id of the blueprint that unlocks fabrication (documentation + tooltips)
 */
public record SuitKind(String name, SuitArmorMaterial material, Supplier<ResourceLocation> blueprint) {

    public ResourceLocation id() {
        return PowersNJ.id(this.name);
    }

    public String suitId() {
        return this.id().toString();
    }
}
