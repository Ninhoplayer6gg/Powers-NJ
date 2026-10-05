package com.powersnj.core.suit;

import com.powersnj.core.progression.LevelCurve;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loaded suit definitions for one logical side. The server instance is filled by the data pack
 * reload listener; the client instance is filled from the server sync packet, so clients always use
 * exactly the server's numbers.
 */
public final class SuitDefinitions {

    public static final SuitDefinitions SERVER = new SuitDefinitions();
    public static final SuitDefinitions CLIENT = new SuitDefinitions();

    private volatile Map<String, SuitDefinition> definitions = Map.of();
    private volatile Map<String, String> rawJson = Map.of();

    public static SuitDefinitions side(boolean clientSide) {
        return clientSide ? CLIENT : SERVER;
    }

    public void replaceAll(Map<String, SuitDefinition> newDefinitions, Map<String, String> newRawJson) {
        this.definitions = Map.copyOf(new LinkedHashMap<>(newDefinitions));
        this.rawJson = Map.copyOf(new LinkedHashMap<>(newRawJson));
    }

    public Optional<SuitDefinition> get(String id) {
        return Optional.ofNullable(this.definitions.get(id));
    }

    public Collection<SuitDefinition> all() {
        return this.definitions.values();
    }

    public boolean isEmpty() {
        return this.definitions.isEmpty();
    }

    /**
     * Raw JSON per suit id, sent to clients on login / reload.
     */
    public Map<String, String> rawJson() {
        return this.rawJson;
    }

    public LevelCurve curveOf(String suitId) {
        SuitDefinition definition = this.definitions.get(suitId);
        return definition == null ? LevelCurve.DEFAULT : definition.curve();
    }
}
