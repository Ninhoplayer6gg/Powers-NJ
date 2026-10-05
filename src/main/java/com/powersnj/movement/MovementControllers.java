package com.powersnj.movement;

import com.powersnj.PowersNJ;
import com.powersnj.core.net.MovementSnapshot;
import com.powersnj.core.suit.SuitDefinition;
import com.powersnj.flight.FlightMovementController;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.speedster.SpeedsterMovementController;
import com.powersnj.symbiote.SymbioteMovementController;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Registry of movement controller factories, keyed by the id used in suit definitions. Third-party
 * or future characters register new controllers with {@link #register}.
 */
public final class MovementControllers {

    public static final String NONE = "powersnj:none";
    public static final String FLIGHT = "powersnj:flight";
    public static final String SPEEDSTER = "powersnj:speedster";
    public static final String SYMBIOTE = "powersnj:symbiote";

    private static final Map<String, Function<SuitDefinition, MovementController>> FACTORIES = new LinkedHashMap<>();

    static {
        register(NONE, definition -> new NoMovement());
        register(FLIGHT, FlightMovementController::new);
        register(SPEEDSTER, SpeedsterMovementController::new);
        register(SYMBIOTE, SymbioteMovementController::new);
    }

    private MovementControllers() {
    }

    public static synchronized void register(String id, Function<SuitDefinition, MovementController> factory) {
        if (FACTORIES.putIfAbsent(id, factory) != null) {
            throw new IllegalStateException("Movement controller '" + id + "' registered twice");
        }
    }

    public static Set<String> ids() {
        return Collections.unmodifiableSet(FACTORIES.keySet());
    }

    public static MovementController create(SuitDefinition definition) {
        Function<SuitDefinition, MovementController> factory = FACTORIES.get(definition.movement());
        if (factory == null) {
            PowersNJ.LOGGER.warn("Suit {} uses unknown movement controller '{}', falling back to none", definition.id(), definition.movement());
            factory = FACTORIES.get(NONE);
        }
        return factory.apply(definition);
    }

    private static final class NoMovement implements MovementController {

        @Override
        public String id() {
            return NONE;
        }

        @Override
        public void tick(ServerPlayer player, PowersPlayerData data, SuitDefinition definition) {
        }

        @Override
        public MovementSnapshot snapshot(ServerPlayer player) {
            return new MovementSnapshot(player.getId(), NONE, "NONE", 0F, 0, 0);
        }
    }
}
