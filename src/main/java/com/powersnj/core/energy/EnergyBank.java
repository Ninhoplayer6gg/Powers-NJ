package com.powersnj.core.energy;

import com.powersnj.core.data.DataNode;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * All energy pools owned by one player, keyed by {@link EnergyType#id()}. Pools persist even while
 * the matching suit is unequipped so a player cannot refill a resource by swapping suits.
 */
public final class EnergyBank {

    private final Map<String, EnergyPool> pools = new LinkedHashMap<>();

    public Optional<EnergyPool> get(EnergyType type) {
        return Optional.ofNullable(this.pools.get(type.id()));
    }

    public Optional<EnergyPool> get(String typeId) {
        return Optional.ofNullable(this.pools.get(typeId));
    }

    /**
     * Ensures the pool for {@code spec} exists and matches the capacity/regeneration of {@code level}.
     */
    public EnergyPool configure(EnergySpec spec, int level) {
        EnergyPool pool = this.pools.get(spec.type().id());
        if (pool == null) {
            pool = new EnergyPool(spec.type(), spec.maxAt(level), spec.regenAt(level), spec.regenDelay());
            this.pools.put(spec.type().id(), pool);
        } else {
            pool.reconfigure(spec.maxAt(level), spec.regenAt(level), spec.regenDelay());
        }
        return pool;
    }

    public Collection<EnergyPool> pools() {
        return Collections.unmodifiableCollection(this.pools.values());
    }

    public void write(DataNode node) {
        for (EnergyPool pool : this.pools.values()) {
            pool.write(node.putChild(pool.type().id()));
        }
    }

    public void read(DataNode node) {
        this.pools.clear();
        for (String key : node.keys()) {
            DataNode child = node.getChild(key);
            EnergyType type = EnergyType.byId(child.getString("type", key)).orElse(null);
            if (type == null) {
                continue;
            }
            EnergyPool pool = new EnergyPool(type, child.getFloat("max", 100F), child.getFloat("regen", 0F), child.getInt("regen_delay", 0));
            pool.read(child);
            this.pools.put(type.id(), pool);
        }
    }
}
