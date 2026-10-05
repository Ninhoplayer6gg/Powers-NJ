package com.powersnj.destruction;

import com.powersnj.PowersNJ;
import com.powersnj.core.config.Settings;
import com.powersnj.core.destruction.DestructionClassifier;
import com.powersnj.core.destruction.DestructionPlanner;
import com.powersnj.core.destruction.DestructionTier;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Executes destruction plans gradually: at most {@code maxDestroyedBlocksPerTick} blocks per server
 * tick across all dimensions. Every block is re-checked at execution time (still breakable, chunk
 * loaded, spawn protection / claims through {@link BlockEvent.BreakEvent}).
 */
@Mod.EventBusSubscriber(modid = PowersNJ.MOD_ID)
public final class DestructionScheduler {

    private static final Map<ResourceKey<Level>, ArrayDeque<Job>> QUEUES = new HashMap<>();
    /** Pending jobs above this are discarded to protect the server. */
    private static final int MAX_PENDING = 8192;

    private record Job(BlockPos pos, DestructionTier power, @Nullable UUID attacker, boolean drops) {
    }

    private DestructionScheduler() {
    }

    public static void enqueue(ServerLevel level, @Nullable Entity attacker, List<DestructionPlanner.Target> plan, DestructionTier power) {
        if (plan.isEmpty()) {
            return;
        }
        ArrayDeque<Job> queue = QUEUES.computeIfAbsent(level.dimension(), k -> new ArrayDeque<>());
        UUID attackerId = attacker == null ? null : attacker.getUUID();
        for (DestructionPlanner.Target target : plan) {
            if (queue.size() >= MAX_PENDING) {
                PowersNJ.LOGGER.warn("Destruction queue full in {}, dropping remaining blocks", level.dimension().location());
                break;
            }
            // Only a fraction of normal/hard blocks drop items: craters must not flood the world with item entities.
            boolean drops = target.tier() != DestructionTier.FRAGILE && level.random.nextFloat() < 0.2F;
            queue.add(new Job(new BlockPos(target.x(), target.y(), target.z()), power, attackerId, drops));
        }
    }

    public static int pending() {
        return QUEUES.values().stream().mapToInt(ArrayDeque::size).sum();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || QUEUES.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        int budget = Settings.get().maxDestroyedBlocksPerTick();
        var iterator = QUEUES.entrySet().iterator();
        while (iterator.hasNext() && budget > 0) {
            var entry = iterator.next();
            ServerLevel level = server.getLevel(entry.getKey());
            ArrayDeque<Job> queue = entry.getValue();
            if (level == null) {
                iterator.remove();
                continue;
            }
            while (!queue.isEmpty() && budget > 0) {
                Job job = queue.poll();
                if (execute(level, job)) {
                    budget--;
                }
            }
            if (queue.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private static boolean execute(ServerLevel level, Job job) {
        BlockPos pos = job.pos();
        if (!level.isLoaded(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        var traits = DestructionEngine.traits(level, pos);
        if (!DestructionClassifier.canDestroy(traits, job.power(), Settings.get().allowObsidianDestruction())) {
            return false;
        }
        Entity attacker = job.attacker() == null ? null : level.getEntity(job.attacker());
        if (attacker instanceof ServerPlayer player) {
            if (!level.mayInteract(player, pos)) {
                return false;
            }
            BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(level, pos, state, player);
            if (MinecraftForge.EVENT_BUS.post(breakEvent)) {
                return false;
            }
        }
        return level.destroyBlock(pos, job.drops(), attacker);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        QUEUES.clear();
    }
}
