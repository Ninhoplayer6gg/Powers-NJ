package com.powersnj.render;

import com.powersnj.PowersNJ;
import com.powersnj.suit.SuitKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Asset path conventions for suits (the contract documented in ASSET_REQUIREMENTS.md).
 * Safe on both sides: only builds resource locations.
 */
public final class SuitAssets {

    private SuitAssets() {
    }

    /**
     * Vanilla-style armor layer (64x32): layer 1 for head/chest/feet, layer 2 for legs.
     */
    public static ResourceLocation armorTexture(SuitKind kind, EquipmentSlot slot) {
        int layer = slot == EquipmentSlot.LEGS ? 2 : 1;
        return PowersNJ.id("textures/suits/" + kind.name() + "/" + kind.name() + "_layer_" + layer + ".png");
    }

    /** GeckoLib model, bones must follow GeckoLib's armor naming (armorHead, armorBody, ...). */
    public static ResourceLocation geoModel(SuitKind kind) {
        return PowersNJ.id("geo/suits/" + kind.name() + ".geo.json");
    }

    /** GeckoLib texture for the model above. */
    public static ResourceLocation geoTexture(SuitKind kind) {
        return PowersNJ.id("textures/suits/" + kind.name() + "/" + kind.name() + "_geo.png");
    }

    /** GeckoLib animations (idle, flight, run...). */
    public static ResourceLocation geoAnimation(SuitKind kind) {
        return PowersNJ.id("animations/suits/" + kind.name() + ".animation.json");
    }

    /** Ability icon used by the Palladium power JSON and the HUD. */
    public static ResourceLocation abilityIcon(String suitName, String ability) {
        return PowersNJ.id("textures/abilities/" + suitName + "/" + ability + ".png");
    }
}
