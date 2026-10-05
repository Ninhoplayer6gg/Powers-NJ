package com.powersnj.client;

import com.powersnj.core.ability.ActivationResult;
import com.powersnj.core.ability.CooldownTracker;
import com.powersnj.core.net.CooldownSnapshot;
import com.powersnj.core.net.EnergySnapshot;
import com.powersnj.core.net.MovementSnapshot;
import com.powersnj.core.net.PowerStateSnapshot;
import com.powersnj.core.net.ProgressSnapshot;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Client mirror of the server power state. Only written by packets; read by the HUD API, screens,
 * movement prediction and visuals. Never authoritative.
 */
public final class ClientPowerState {

    private static String activeSuit = "";
    private static ProgressSnapshot progress;
    private static final Map<String, EnergySnapshot> ENERGIES = new LinkedHashMap<>();
    private static final CooldownTracker COOLDOWNS = new CooldownTracker();
    private static final Map<Integer, MovementSnapshot> MOVEMENT = new HashMap<>();
    private static final Map<Integer, String> FORMS = new HashMap<>();
    private static String lastAbility = "";
    private static ActivationResult lastResult = ActivationResult.ALLOWED;
    private static long lastAbilityGameTime;

    private ClientPowerState() {
    }

    static void apply(PowerStateSnapshot snapshot) {
        activeSuit = snapshot.activeSuit();
        progress = snapshot.progress();
        ENERGIES.clear();
        snapshot.energies().forEach(e -> ENERGIES.put(e.type(), e));
        COOLDOWNS.clearAll();
        COOLDOWNS.drainDirty();
        snapshot.cooldowns().forEach(c -> COOLDOWNS.applySync(c.key(), c.remaining(), c.total()));
    }

    static void applyEnergy(Collection<EnergySnapshot> energies) {
        energies.forEach(e -> ENERGIES.put(e.type(), e));
    }

    static void applyCooldowns(Collection<CooldownSnapshot> cooldowns) {
        cooldowns.forEach(c -> COOLDOWNS.applySync(c.key(), c.remaining(), c.total()));
    }

    static void applyProgress(ProgressSnapshot snapshot) {
        if (snapshot.suitId().equals(activeSuit)) {
            progress = snapshot;
        }
    }

    static void applyMovement(MovementSnapshot snapshot) {
        MOVEMENT.put(snapshot.entityId(), snapshot);
    }

    static void applyForm(int entityId, String form) {
        if (form.isEmpty()) {
            FORMS.remove(entityId);
        } else {
            FORMS.put(entityId, form);
        }
    }

    static void applyAbilityFeedback(String key, ActivationResult result, long gameTime) {
        lastAbility = key;
        lastResult = result;
        lastAbilityGameTime = gameTime;
    }

    /**
     * Client tick: cooldowns count down locally between server updates.
     */
    static void tick() {
        COOLDOWNS.tick();
        COOLDOWNS.drainDirty();
    }

    public static void reset() {
        activeSuit = "";
        progress = null;
        ENERGIES.clear();
        COOLDOWNS.clearAll();
        COOLDOWNS.drainDirty();
        MOVEMENT.clear();
        FORMS.clear();
        lastAbility = "";
        lastResult = ActivationResult.ALLOWED;
    }

    public static String activeSuit() {
        return activeSuit;
    }

    public static Optional<ProgressSnapshot> progress() {
        return Optional.ofNullable(progress);
    }

    public static Map<String, EnergySnapshot> energies() {
        return Collections.unmodifiableMap(ENERGIES);
    }

    public static CooldownTracker cooldowns() {
        return COOLDOWNS;
    }

    public static Optional<MovementSnapshot> movement(int entityId) {
        return Optional.ofNullable(MOVEMENT.get(entityId));
    }

    public static String form(int entityId) {
        return FORMS.getOrDefault(entityId, "");
    }

    public static String lastAbility() {
        return lastAbility;
    }

    public static ActivationResult lastResult() {
        return lastResult;
    }

    public static long lastAbilityGameTime() {
        return lastAbilityGameTime;
    }
}
