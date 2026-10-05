package com.powersnj.core.energy;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A kind of power resource (stamina, biomass, speed force...). New characters register additional
 * types through {@link #register(String, int, boolean)}; the three launch types are built in.
 *
 * @param id        namespaced id, e.g. {@code powersnj:biomass}
 * @param color     ARGB color used by the HUD bar
 * @param isStamina true when the resource represents physical exertion (exposed as "stamina" by the HUD API)
 */
public record EnergyType(String id, int color, boolean isStamina) {

    private static final Map<String, EnergyType> REGISTRY = new LinkedHashMap<>();

    public static final EnergyType VILTRUMITE_STAMINA = register("powersnj:viltrumite_stamina", 0xFFE0B040, true);
    public static final EnergyType BIOMASS = register("powersnj:biomass", 0xFF3A3A55, false);
    public static final EnergyType NEGATIVE_SPEED_FORCE = register("powersnj:negative_speed_force", 0xFFD02020, false);

    public EnergyType {
        Objects.requireNonNull(id, "id");
        if (!id.contains(":")) {
            throw new IllegalArgumentException("Energy type id must be namespaced: " + id);
        }
    }

    public static synchronized EnergyType register(String id, int color, boolean isStamina) {
        EnergyType existing = REGISTRY.get(id);
        if (existing != null) {
            return existing;
        }
        EnergyType type = new EnergyType(id, color, isStamina);
        REGISTRY.put(id, type);
        return type;
    }

    public static Optional<EnergyType> byId(String id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }

    public static Collection<EnergyType> values() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    /**
     * Translation key suffix, e.g. {@code energy.powersnj.biomass}.
     */
    public String translationKey() {
        int idx = this.id.indexOf(':');
        return "energy." + this.id.substring(0, idx) + "." + this.id.substring(idx + 1);
    }
}
