package com.powersnj.compat.geckolib;

import com.powersnj.PowersNJ;
import com.powersnj.animation.SuitAnimations;
import com.powersnj.render.AssetAvailability;
import com.powersnj.symbiote.TendrilEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib renderer of the tendril tip, active once {@link #MODEL} exists.
 */
public class TendrilGeoRenderer extends GeoEntityRenderer<TendrilEntity> {

    public static final ResourceLocation MODEL = PowersNJ.id("geo/entity/tendril.geo.json");
    public static final ResourceLocation TEXTURE = PowersNJ.id("textures/entity/tendril_geo.png");
    public static final ResourceLocation ANIMATION = PowersNJ.id("animations/entity/tendril.animation.json");

    public TendrilGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(TendrilEntity animatable) {
                return MODEL;
            }

            @Override
            public ResourceLocation getTextureResource(TendrilEntity animatable) {
                return TEXTURE;
            }

            @Override
            public ResourceLocation getAnimationResource(TendrilEntity animatable) {
                return AssetAvailability.has(ANIMATION) ? ANIMATION : SuitAnimations.EMPTY_ANIMATIONS;
            }
        });
    }
}
