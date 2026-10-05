package com.powersnj.client;

import com.powersnj.core.net.MovementSnapshot;
import com.powersnj.core.speedster.SpeedsterProfile;
import com.powersnj.core.suit.SuitDefinitions;
import com.powersnj.movement.MovementControllers;
import com.powersnj.registry.ModParticles;
import com.powersnj.suit.SuitDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Default speedster visuals: Negative Speed Force lightning and sparks trailing behind running
 * speedsters (all players, from the synced movement state), and the afterimage/lightning/trail
 * hook dispatch.
 */
public final class SpeedsterVisuals {

    private static boolean defaultsRegistered;

    private SpeedsterVisuals() {
    }

    static void tick(Minecraft minecraft) {
        if (!defaultsRegistered) {
            defaultsRegistered = true;
            SpeedsterVisualHooks.register(SpeedsterVisuals::defaultTrail);
        }
        if (minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        for (Player player : minecraft.level.players()) {
            MovementSnapshot state = ClientPowerState.movement(player.getId()).orElse(null);
            if (state == null || !MovementControllers.SPEEDSTER.equals(state.controller()) || !state.has(MovementSnapshot.FLAG_ACTIVE)) {
                continue;
            }
            SpeedsterHelpers.Normalizer normalizer = SpeedsterHelpers.normalizer(player);
            SpeedsterVisualHooks.fire(player, state, normalizer.normalize(state.speed()));
        }
    }

    private static void defaultTrail(Player player, MovementSnapshot state, float normalized) {
        Vec3 motion = new Vec3(player.getX() - player.xo, 0, player.getZ() - player.zo);
        if (normalized < 0.1F || motion.lengthSqr() < 0.04D) {
            return;
        }
        var level = player.level();
        int count = 1 + (int) (normalized * 3);
        for (int i = 0; i < count; i++) {
            double t = level.random.nextDouble();
            double x = player.xo + (player.getX() - player.xo) * t;
            double z = player.zo + (player.getZ() - player.zo) * t;
            double y = player.getY() + 0.2D + level.random.nextDouble() * 1.4D;
            level.addParticle(ModParticles.NEGATIVE_LIGHTNING.get(), x, y, z, 0, 0, 0);
            if (level.random.nextFloat() < normalized) {
                level.addParticle(ModParticles.SPEED_SPARK.get(), x, y, z, -motion.x * 0.1D, 0.02D, -motion.z * 0.1D);
            }
        }
    }

    /**
     * Shared helpers for client speedster code.
     */
    public static final class SpeedsterHelpers {

        private SpeedsterHelpers() {
        }

        public interface Normalizer {
            float normalize(float multiplier);

            SpeedsterProfile profile();
        }

        public static Normalizer normalizer(Player player) {
            String suit = SuitDetector.activeSuitId(player, true);
            SpeedsterProfile profile = SuitDefinitions.CLIENT.get(suit).map(SpeedsterProfile::from).orElse(SpeedsterProfile.DEFAULT);
            return new Normalizer() {
                @Override
                public float normalize(float multiplier) {
                    double max = Math.max(1.0001D, profile.maxMultiplier());
                    return (float) Math.max(0D, Math.min(1D, (multiplier - 1D) / (max - 1D)));
                }

                @Override
                public SpeedsterProfile profile() {
                    return profile;
                }
            };
        }
    }
}
