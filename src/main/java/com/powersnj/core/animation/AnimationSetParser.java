package com.powersnj.core.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Parses an animation set definition:
 * <pre>{@code
 * {
 *   "clips": "powersnj:animations/suits/thragg.animation.json",
 *   "fade": 0.2,
 *   "states": { "idle": "...", "walk": "...", "run": "...", "crouch": "...", "guard": "...", "jump": "...",
 *               "takeoff": "...", "hover": "...", "fly": "...", "fast_flight": "...", "land": "..." },
 *   "melee": { "chain": ["...", "..."], "after_sprint": "...", "chain_window": 1.0 },
 *   "events": { "heavy_hit": "...", "level_up": "...", "kill": "..." },
 *   "lower_body": ["root", "right_leg", "left_leg"],
 *   "combat_window": 5.0
 * }
 * }</pre>
 * {@code idle}, {@code walk} and {@code run} together form the GROUND state. Every entry is optional:
 * a missing clip leaves that situation to the vanilla animation.
 */
public final class AnimationSetParser {

    private static final Map<String, LocomotionState> STATE_KEYS = Map.of(
            "crouch", LocomotionState.CROUCH,
            "guard", LocomotionState.GUARD,
            "jump", LocomotionState.JUMP,
            "takeoff", LocomotionState.TAKEOFF,
            "hover", LocomotionState.HOVER,
            "fly", LocomotionState.FLY,
            "fast_flight", LocomotionState.FAST_FLIGHT,
            "land", LocomotionState.LAND);

    private AnimationSetParser() {
    }

    /**
     * Reads the clip file location referenced by a set without resolving it.
     */
    public static String clipFile(String json) {
        JsonObject root = object(json);
        if (!root.has("clips")) {
            throw new JsonParseException("Animation set has no \"clips\" file");
        }
        return root.get("clips").getAsString();
    }

    public static AnimationSet parse(String id, String json, Map<String, AnimationClip> clips) {
        JsonObject root = object(json);
        String clipFile = clipFile(json);
        Map<LocomotionState, String> states = new EnumMap<>(LocomotionState.class);
        Map<String, String> events = new LinkedHashMap<>();
        String walk = "";
        String run = "";
        if (root.has("states")) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("states").entrySet()) {
                String key = entry.getKey().toLowerCase(Locale.ROOT);
                String clip = entry.getValue().getAsString();
                switch (key) {
                    case "idle" -> states.put(LocomotionState.GROUND, clip);
                    case "walk" -> walk = clip;
                    case "run" -> run = clip;
                    default -> {
                        LocomotionState state = STATE_KEYS.get(key);
                        if (state == null) {
                            throw new JsonParseException("Unknown animation state \"" + entry.getKey() + "\" in " + id);
                        }
                        states.put(state, clip);
                    }
                }
            }
        }
        List<String> chain = new ArrayList<>();
        String afterSprint = "";
        double window = 1.0D;
        if (root.has("melee")) {
            JsonObject melee = root.getAsJsonObject("melee");
            if (melee.has("chain")) {
                for (JsonElement element : melee.getAsJsonArray("chain")) {
                    chain.add(element.getAsString());
                }
            }
            afterSprint = melee.has("after_sprint") ? melee.get("after_sprint").getAsString() : "";
            window = melee.has("chain_window") ? melee.get("chain_window").getAsDouble() : window;
        }
        if (root.has("events")) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("events").entrySet()) {
                events.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        Set<String> lowerBody = new LinkedHashSet<>();
        if (root.has("lower_body")) {
            JsonArray array = root.getAsJsonArray("lower_body");
            array.forEach(e -> lowerBody.add(e.getAsString()));
        } else {
            lowerBody.addAll(List.of(HumanoidBones.ROOT, HumanoidBones.RIGHT_LEG, HumanoidBones.LEFT_LEG));
        }
        double combatWindow = root.has("combat_window") ? root.get("combat_window").getAsDouble() : 5.0D;
        double fade = root.has("fade") ? root.get("fade").getAsDouble() : 0.2D;
        return new AnimationSet(id, clipFile, states, walk, run, chain, afterSprint, window, events, lowerBody, combatWindow, fade, clips);
    }

    private static JsonObject object(String json) {
        try {
            JsonElement element = JsonParser.parseString(json);
            if (!element.isJsonObject()) {
                throw new JsonParseException("Animation set must be a JSON object");
            }
            return element.getAsJsonObject();
        } catch (IllegalStateException e) {
            throw new JsonParseException("Invalid animation set: " + e.getMessage(), e);
        }
    }
}
