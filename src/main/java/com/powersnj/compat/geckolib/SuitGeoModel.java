package com.powersnj.compat.geckolib;

import com.powersnj.animation.SuitAnimations;
import com.powersnj.render.AssetAvailability;
import com.powersnj.render.SuitAssets;
import com.powersnj.suit.SuitArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for every suit: resources follow the {@link SuitAssets} conventions.
 */
public class SuitGeoModel extends GeoModel<SuitArmorItem> {

    @Override
    public ResourceLocation getModelResource(SuitArmorItem item) {
        return SuitAssets.geoModel(item.kind());
    }

    @Override
    public ResourceLocation getTextureResource(SuitArmorItem item) {
        return SuitAssets.geoTexture(item.kind());
    }

    @Override
    public ResourceLocation getAnimationResource(SuitArmorItem item) {
        ResourceLocation animation = SuitAssets.geoAnimation(item.kind());
        return AssetAvailability.has(animation) ? animation : SuitAnimations.EMPTY_ANIMATIONS;
    }
}
