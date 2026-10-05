package com.powersnj.client;

import com.powersnj.PowersNJ;
import com.powersnj.animation.SuitBodyAnimation;
import net.threetag.palladium.event.PalladiumClientEvents;

/**
 * Client hooks that must exist before the first resource reload (called from the mod constructor on
 * the physical client only).
 */
public final class ClientBootstrap {

    /** Above Palladium's flight/hover animations (-30..-10), below its aim animation (100). */
    public static final int SUIT_ANIMATION_PRIORITY = 50;
    private static boolean initialized;

    private ClientBootstrap() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        PalladiumClientEvents.REGISTER_ANIMATIONS.register(registry ->
                registry.accept(PowersNJ.id("suit_body"), new SuitBodyAnimation(SUIT_ANIMATION_PRIORITY)));
    }
}
