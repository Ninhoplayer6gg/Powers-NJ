package com.powersnj.power;

import com.powersnj.PowersNJ;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Multi-tick server effects (expanding shockwaves, charges, vortexes). Each task ticks until it
 * reports completion or its level unloads. Bounded to protect the server.
 */
@Mod.EventBusSubscriber(modid = PowersNJ.MOD_ID)
public final class ServerTasks {

    public static final int MAX_TASKS = 512;

    @FunctionalInterface
    public interface Task {
        /**
         * @return true when finished
         */
        boolean tick(ServerLevel level, int age);
    }

    private record Entry(ServerLevel level, Task task, int[] age) {
    }

    private static final List<Entry> TASKS = new ArrayList<>();

    private ServerTasks() {
    }

    public static void schedule(ServerLevel level, Task task) {
        if (TASKS.size() >= MAX_TASKS) {
            PowersNJ.LOGGER.warn("Too many Powers NJ tasks running, ignoring new task");
            return;
        }
        TASKS.add(new Entry(level, task, new int[]{0}));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TASKS.isEmpty()) {
            return;
        }
        List<Entry> snapshot = new ArrayList<>(TASKS);
        TASKS.clear();
        Iterator<Entry> iterator = snapshot.iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            boolean done;
            try {
                done = entry.level().getServer().getLevel(entry.level().dimension()) != entry.level() || entry.task().tick(entry.level(), entry.age()[0]++);
            } catch (RuntimeException e) {
                PowersNJ.LOGGER.error("Powers NJ task failed", e);
                done = true;
            }
            if (!done) {
                TASKS.add(entry);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TASKS.clear();
    }
}
