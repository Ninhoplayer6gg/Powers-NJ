package com.powersnj.core.ability;

import com.powersnj.core.data.DataNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Server-authoritative ability cooldowns of one player. Keys are {@code <power id>#<ability key>}.
 * Only keys that started or expired since the last sync are reported by {@link #drainDirty()}, so
 * cooldown packets are sent on change instead of every tick (clients count down locally).
 */
public final class CooldownTracker {

    private final Map<String, int[]> cooldowns = new LinkedHashMap<>();
    private final Set<String> dirty = new LinkedHashSet<>();

    public void start(String key, int ticks) {
        if (ticks <= 0) {
            if (this.cooldowns.remove(key) != null) {
                this.dirty.add(key);
            }
            return;
        }
        this.cooldowns.put(key, new int[]{ticks, ticks});
        this.dirty.add(key);
    }

    public void clear(String key) {
        if (this.cooldowns.remove(key) != null) {
            this.dirty.add(key);
        }
    }

    public void clearAll() {
        this.dirty.addAll(this.cooldowns.keySet());
        this.cooldowns.clear();
    }

    public boolean isOnCooldown(String key) {
        int[] entry = this.cooldowns.get(key);
        return entry != null && entry[0] > 0;
    }

    public int remaining(String key) {
        int[] entry = this.cooldowns.get(key);
        return entry == null ? 0 : entry[0];
    }

    public int total(String key) {
        int[] entry = this.cooldowns.get(key);
        return entry == null ? 0 : entry[1];
    }

    /**
     * @return 0 (ready) .. 1 (just started)
     */
    public float progress(String key) {
        int[] entry = this.cooldowns.get(key);
        return entry == null || entry[1] <= 0 ? 0F : (float) entry[0] / entry[1];
    }

    public void tick() {
        var iterator = this.cooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (--entry.getValue()[0] <= 0) {
                iterator.remove();
                this.dirty.add(entry.getKey());
            }
        }
    }

    public Set<String> activeKeys() {
        return Collections.unmodifiableSet(this.cooldowns.keySet());
    }

    /**
     * Returns and clears the keys whose state changed since the last call.
     */
    public Set<String> drainDirty() {
        if (this.dirty.isEmpty()) {
            return Set.of();
        }
        Set<String> copy = new LinkedHashSet<>(this.dirty);
        this.dirty.clear();
        return copy;
    }

    /**
     * Client-side mirror update from a sync packet.
     */
    public void applySync(String key, int remaining, int total) {
        if (remaining <= 0) {
            this.cooldowns.remove(key);
        } else {
            this.cooldowns.put(key, new int[]{remaining, Math.max(remaining, total)});
        }
    }

    public void write(DataNode node) {
        this.cooldowns.forEach((key, entry) -> {
            DataNode child = node.putChild(key);
            child.putInt("remaining", entry[0]);
            child.putInt("total", entry[1]);
        });
    }

    public void read(DataNode node) {
        this.cooldowns.clear();
        for (String key : node.keys()) {
            DataNode child = node.getChild(key);
            int remaining = child.getInt("remaining", 0);
            if (remaining > 0) {
                this.cooldowns.put(key, new int[]{remaining, Math.max(remaining, child.getInt("total", remaining))});
            }
        }
        this.dirty.addAll(this.cooldowns.keySet());
    }
}
