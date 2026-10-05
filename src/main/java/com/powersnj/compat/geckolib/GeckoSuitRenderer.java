package com.powersnj.compat.geckolib;

import com.powersnj.suit.SuitArmorItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * GeckoLib armor renderer used by suits whose {@code geo/suits/<suit>.geo.json} exists.
 */
public class GeckoSuitRenderer extends GeoArmorRenderer<SuitArmorItem> {

    public GeckoSuitRenderer() {
        super(new SuitGeoModel());
    }
}
