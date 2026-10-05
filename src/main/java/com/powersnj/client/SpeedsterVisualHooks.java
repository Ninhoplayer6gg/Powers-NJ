package com.powersnj.client;

import com.powersnj.core.net.MovementSnapshot;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Extension point for speedster visuals (afterimages, lightning, trails, vibration). The default
 * particle hook is registered by {@link SpeedsterVisuals}; final art (Palladium trail definitions,
 * custom renderers) plugs in here without touching the engine.
 */
public final class SpeedsterVisualHooks {

    @FunctionalInterface
    public interface Hook {
        /**
         * Called every client tick for every visible player whose speedster engine is active.
         *
         * @param normalizedSpeed 0..1 estimate of how close to top speed the player is
         */
        void onSpeedsterTick(Player player, MovementSnapshot state, float normalizedSpeed);
    }

    private static final List<Hook> HOOKS = new CopyOnWriteArrayList<>();

    private SpeedsterVisualHooks() {
    }

    public static void register(Hook hook) {
        HOOKS.add(hook);
    }

    static void fire(Player player, MovementSnapshot state, float normalizedSpeed) {
        for (Hook hook : HOOKS) {
            hook.onSpeedsterTick(player, state, normalizedSpeed);
        }
    }
}
