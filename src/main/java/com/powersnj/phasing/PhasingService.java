package com.powersnj.phasing;

import com.powersnj.PowersNJ;
import com.powersnj.core.config.Settings;
import com.powersnj.core.phasing.PhasePlan;
import com.powersnj.core.phasing.PhasePlanner;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.registry.ModParticles;
import com.powersnj.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

/**
 * Dedicated, server-authoritative phasing (not noclip). A phase is planned completely on the
 * server ({@link PhasePlanner}): direction, distance limit, phase-proof blocks, hazards and a
 * verified safe exit. Only then is the player moved, in a single teleport.
 * <p>
 * A watchdog rescues players who recently phased (or whose suit can phase) if they ever end up
 * inside blocks, so nobody stays stuck permanently.
 */
@Mod.EventBusSubscriber(modid = PowersNJ.MOD_ID)
public final class PhasingService {

    /** Ticks after a phase during which the watchdog is armed. */
    public static final int WATCHDOG_WINDOW = 100;
    public static final int RESCUE_RADIUS = 8;

    private PhasingService() {
    }

    /**
     * Plans the phase (no side effects).
     */
    public static PhasePlan plan(ServerPlayer player, int requestedDistance) {
        int distance = Math.max(1, Math.min(requestedDistance, Settings.get().maxPhaseDistance()));
        Vec3 look = player.getLookAngle();
        return PhasePlanner.plan(player.getX(), player.getY(), player.getZ(), look.x, look.z, distance, bodyHeight(player), new LevelBlockQuery(player.level()));
    }

    /**
     * Executes a planned phase after a last vanilla collision check of the destination.
     */
    public static boolean execute(ServerPlayer player, PhasePlan plan) {
        if (!plan.success()) {
            return false;
        }
        double x = plan.x() + 0.5D;
        double y = plan.y();
        double z = plan.z() + 0.5D;
        AABB destinationBox = player.getBoundingBox().move(x - player.getX(), y - player.getY(), z - player.getZ());
        if (!player.level().noCollision(player, destinationBox)) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        level.sendParticles(ModParticles.NEGATIVE_LIGHTNING.get(), player.getX(), player.getY(1.0D), player.getZ(), 15, 0.3, 0.6, 0.3, 0.05);
        player.teleportTo(x, y, z);
        player.fallDistance = 0F;
        player.resetFallDistance();
        PowersPlayerData.get(player).ifPresent(data -> data.setLastPhaseTick(player.tickCount));
        level.playSound(null, player.blockPosition(), ModSounds.PHASE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ModParticles.NEGATIVE_LIGHTNING.get(), x, y + 1.0D, z, 15, 0.3, 0.6, 0.3, 0.05);
        return true;
    }

    private static int bodyHeight(ServerPlayer player) {
        return Math.max(1, (int) Math.ceil(player.getBbHeight() - 1.0E-3));
    }

    /**
     * Watchdog: if a phaser is inside a solid block, move them to the nearest safe spot.
     */
    @SubscribeEvent
    public static void watchdog(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.isSpectator() || player.tickCount % 5 != 0) {
            return;
        }
        PowersPlayerData data = PowersPlayerData.get(player).orElse(null);
        if (data == null || player.tickCount - data.lastPhaseTick() > WATCHDOG_WINDOW) {
            return;
        }
        if (!player.isInWall()) {
            return;
        }
        BlockPos feet = player.blockPosition();
        Optional<int[]> safe = PhasePlanner.findNearestSafe(feet.getX(), feet.getY(), feet.getZ(), RESCUE_RADIUS, bodyHeight(player), new LevelBlockQuery(player.level()));
        if (safe.isPresent()) {
            int[] spot = safe.get();
            player.teleportTo(spot[0] + 0.5D, spot[1], spot[2] + 0.5D);
            PowersNJ.LOGGER.info("Phasing watchdog moved {} out of blocks to {} {} {}", player.getGameProfile().getName(), spot[0], spot[1], spot[2]);
        } else {
            // Nothing safe nearby: send them to the world spawn rather than leaving them stuck.
            BlockPos spawn = player.serverLevel().getSharedSpawnPos();
            player.teleportTo(spawn.getX() + 0.5D, player.serverLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ()), spawn.getZ() + 0.5D);
            PowersNJ.LOGGER.warn("Phasing watchdog found no safe spot near {}, moved to spawn", player.getGameProfile().getName());
        }
    }
}
