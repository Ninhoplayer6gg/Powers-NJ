package com.powersnj.client;

import com.powersnj.PowersNJ;
import com.powersnj.animation.SuitAnimationClient;
import com.powersnj.core.ability.ActivationResult;
import com.powersnj.core.net.CooldownSnapshot;
import com.powersnj.core.net.DefinitionsPayload;
import com.powersnj.core.net.EnergySnapshot;
import com.powersnj.core.net.MovementSnapshot;
import com.powersnj.core.net.PowerStateSnapshot;
import com.powersnj.core.net.ProgressSnapshot;
import com.powersnj.core.suit.SuitDefinition;
import com.powersnj.core.suit.SuitDefinitionParser;
import com.powersnj.core.suit.SuitDefinitions;
import net.minecraft.client.Minecraft;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side packet handling (only loaded on the physical client).
 */
public final class ClientPacketHandler {

    private ClientPacketHandler() {
    }

    public static void handlePowerState(PowerStateSnapshot snapshot) {
        ClientPowerState.apply(snapshot);
    }

    public static void handleEnergy(List<EnergySnapshot> energies) {
        ClientPowerState.applyEnergy(energies);
    }

    public static void handleCooldowns(List<CooldownSnapshot> cooldowns) {
        ClientPowerState.applyCooldowns(cooldowns);
    }

    public static void handleProgress(ProgressSnapshot progress, boolean levelUp) {
        ClientPowerState.applyProgress(progress);
    }

    public static void handleMovement(MovementSnapshot snapshot) {
        ClientPowerState.applyMovement(snapshot);
    }

    public static void handleForm(int entityId, String form) {
        ClientPowerState.applyForm(entityId, form);
    }

    public static void handleAbilityFeedback(String key, ActivationResult result) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPowerState.applyAbilityFeedback(key, result, minecraft.level == null ? 0L : minecraft.level.getGameTime());
    }

    public static void handleSuitAnimation(int entityId, String key) {
        SuitAnimationClient.play(entityId, key);
    }

    public static void handleDefinitions(DefinitionsPayload payload) {
        Map<String, SuitDefinition> parsed = new LinkedHashMap<>();
        payload.definitions().forEach((id, json) -> {
            try {
                parsed.put(id, SuitDefinitionParser.parse(id, json));
            } catch (RuntimeException e) {
                PowersNJ.LOGGER.error("Server sent an invalid suit definition {}: {}", id, e.getMessage());
            }
        });
        SuitDefinitions.CLIENT.replaceAll(parsed, payload.definitions());
    }
}
