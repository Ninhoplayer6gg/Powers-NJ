package com.powersnj.animation;

import com.powersnj.PowersNJ;
import com.powersnj.network.PowersNetwork;
import com.powersnj.network.SuitAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side entry point of the suit animations (safe on both sides).
 * <p>
 * Clips live in {@code assets/<ns>/animations/suits/<suit>.animation.json} (Bedrock format, made with
 * tools/suit-assets), the set that maps them to states and events in
 * {@code assets/<ns>/animation_sets/<suit>.json}. Clients pick the set of the suit an entity wears;
 * the server only announces one-shot clips: ability activations (the {@code animation} property of
 * every Powers NJ ability) and events ({@code #level_up}, {@code #kill}, {@code #heavy_hit}).
 */
public final class SuitAnimations {

    /** Valid GeckoLib animation file without animations, for models that have none yet. */
    public static final ResourceLocation EMPTY_ANIMATIONS = PowersNJ.id("animations/empty.animation.json");

    public static final String EVENT_LEVEL_UP = "#level_up";
    public static final String EVENT_KILL = "#kill";
    public static final String EVENT_HEAVY_HIT = "#heavy_hit";

    private SuitAnimations() {
    }

    /**
     * Plays a clip (name) or an event ({@code #event}) on the player for everyone tracking it.
     */
    public static void play(ServerPlayer player, String key) {
        if (key == null || key.isEmpty() || key.length() > SuitAnimationPacket.MAX_KEY_LENGTH) {
            return;
        }
        PowersNetwork.sendToTrackingAndSelf(player, new SuitAnimationPacket(player.getId(), key));
    }
}
