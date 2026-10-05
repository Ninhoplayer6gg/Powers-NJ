package com.powersnj.power;

import com.powersnj.core.suit.SuitDefinition;
import com.powersnj.player.PowersPlayerData;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of {@link PowerSystem}s. Some systems are pure event handlers and only need to be
 * listed in a definition (e.g. {@code powersnj:venom_weaknesses}, {@code powersnj:biomass_feeding},
 * {@code powersnj:reactions}); ticking systems register an implementation here.
 */
public final class PowerSystems {

    private static final Map<String, PowerSystem> SYSTEMS = new LinkedHashMap<>();

    static {
        register(new RegenerationSystem());
    }

    private PowerSystems() {
    }

    public static synchronized void register(PowerSystem system) {
        if (SYSTEMS.putIfAbsent(system.id(), system) != null) {
            throw new IllegalStateException("Power system '" + system.id() + "' registered twice");
        }
    }

    public static Optional<PowerSystem> get(String id) {
        return Optional.ofNullable(SYSTEMS.get(id));
    }

    public static Map<String, PowerSystem> all() {
        return Collections.unmodifiableMap(SYSTEMS);
    }

    public static void tick(ServerPlayer player, PowersPlayerData data, SuitDefinition definition) {
        for (String id : definition.systems()) {
            PowerSystem system = SYSTEMS.get(id);
            if (system != null) {
                system.tick(player, data, definition);
            }
        }
    }
}
