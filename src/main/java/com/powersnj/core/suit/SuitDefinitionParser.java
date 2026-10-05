package com.powersnj.core.suit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.powersnj.core.energy.EnergySpec;
import com.powersnj.core.energy.EnergyType;
import com.powersnj.core.progression.LevelCurve;
import com.powersnj.core.progression.XpRules;
import com.powersnj.core.skill.SkillNode;
import com.powersnj.core.skill.SkillRequirement;
import com.powersnj.core.skill.SkillTree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gson based parser for suit definition files. Only depends on Gson (shipped with Minecraft), so it
 * is shared by the server reload listener, the client sync packet and the unit tests.
 */
public final class SuitDefinitionParser {

    private SuitDefinitionParser() {
    }

    public static SuitDefinition parse(String id, String json) {
        return parse(id, JsonParser.parseString(json).getAsJsonObject());
    }

    public static SuitDefinition parse(String id, JsonObject json) {
        String character = string(json, "character", id);
        int rating = integer(json, "power_rating", 50);
        int maxLevel = integer(json, "max_level", LevelCurve.DEFAULT_MAX_LEVEL);

        LevelCurve curve;
        if (json.has("xp_curve")) {
            JsonObject c = object(json, "xp_curve");
            curve = new LevelCurve(number(c, "base", LevelCurve.DEFAULT.base()), number(c, "exponent", LevelCurve.DEFAULT.exponent()), maxLevel);
        } else {
            curve = new LevelCurve(LevelCurve.DEFAULT.base(), LevelCurve.DEFAULT.exponent(), maxLevel);
        }

        if (!json.has("energy")) {
            throw new JsonParseException("Suit '" + id + "' is missing 'energy'");
        }
        JsonObject e = object(json, "energy");
        String typeId = string(e, "type", "");
        EnergyType type = EnergyType.byId(typeId).orElseThrow(() -> new JsonParseException("Suit '" + id + "' uses unknown energy type '" + typeId + "'"));
        EnergySpec energy = new EnergySpec(type,
                (float) number(e, "base_max", 100),
                (float) number(e, "max_per_level", 0),
                (float) number(e, "regen_per_tick", 0.1),
                (float) number(e, "regen_per_level", 0),
                integer(e, "regen_delay", 20));

        String movementType = "powersnj:none";
        Map<String, Double> settings = new LinkedHashMap<>();
        if (json.has("movement")) {
            JsonObject m = object(json, "movement");
            movementType = string(m, "type", movementType);
            readNumbers(m, settings);
        }
        if (json.has("settings")) {
            readNumbers(object(json, "settings"), settings);
        }

        XpRules xp = XpRules.DEFAULT;
        if (json.has("xp")) {
            JsonObject x = object(json, "xp");
            xp = new XpRules(
                    number(x, "kill_mob", XpRules.DEFAULT.killMob()),
                    number(x, "kill_player", XpRules.DEFAULT.killPlayer()),
                    number(x, "kill_boss", XpRules.DEFAULT.killBoss()),
                    number(x, "ability_use", XpRules.DEFAULT.abilityUse()),
                    number(x, "damage_dealt_factor", XpRules.DEFAULT.damageDealtFactor()),
                    number(x, "travel_per_100_blocks", XpRules.DEFAULT.travelPer100Blocks()));
        }

        List<SkillNode> nodes = new ArrayList<>();
        if (json.has("skill_tree")) {
            for (JsonElement element : array(json, "skill_tree")) {
                nodes.add(parseSkill(id, element.getAsJsonObject()));
            }
        }
        SkillTree tree;
        try {
            tree = SkillTree.of(nodes);
        } catch (IllegalArgumentException ex) {
            throw new JsonParseException("Suit '" + id + "' has an invalid skill tree: " + ex.getMessage(), ex);
        }
        for (SkillNode node : tree.nodes()) {
            if (node.requiredLevel() > maxLevel) {
                throw new JsonParseException("Skill '" + node.id() + "' of suit '" + id + "' requires level " + node.requiredLevel() + " but max_level is " + maxLevel);
            }
        }

        return new SuitDefinition(id, character, rating, curve, energy,
                string(json, "palladium_power", id),
                strings(json, "abilities"),
                strings(json, "passives"),
                movementType,
                settings,
                strings(json, "systems"),
                tree,
                string(json, "hud_profile", "powersnj:default"),
                xp);
    }

    private static void readNumbers(JsonObject json, Map<String, Double> into) {
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (!entry.getKey().equals("type") && entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber()) {
                into.put(entry.getKey(), entry.getValue().getAsDouble());
            }
        }
    }

    private static SkillNode parseSkill(String suitId, JsonObject json) {
        String skillId = string(json, "id", "");
        if (skillId.isEmpty()) {
            throw new JsonParseException("Skill without 'id' in suit '" + suitId + "'");
        }
        SkillRequirement requirement = SkillRequirement.NONE;
        if (json.has("requirement")) {
            JsonObject r = object(json, "requirement");
            requirement = new SkillRequirement(string(r, "stat", ""), (long) number(r, "min", 0));
        }
        return new SkillNode(skillId,
                integer(json, "required_level", 1),
                strings(json, "parents"),
                integer(json, "cost", 1),
                requirement,
                string(json, "icon", ""),
                integer(json, "x", 0),
                integer(json, "y", 0));
    }

    private static JsonObject object(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonObject()) {
            throw new JsonParseException("Expected '" + key + "' to be an object");
        }
        return element.getAsJsonObject();
    }

    private static JsonArray array(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonArray()) {
            throw new JsonParseException("Expected '" + key + "' to be an array");
        }
        return element.getAsJsonArray();
    }

    private static String string(JsonObject json, String key, String fallback) {
        JsonElement element = json.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    private static double number(JsonObject json, String key, double fallback) {
        JsonElement element = json.get(key);
        if (element == null) {
            return fallback;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException("Expected '" + key + "' to be a number");
        }
        return element.getAsDouble();
    }

    private static int integer(JsonObject json, String key, int fallback) {
        return (int) Math.round(number(json, key, fallback));
    }

    private static List<String> strings(JsonObject json, String key) {
        List<String> result = new ArrayList<>();
        if (json.has(key)) {
            for (JsonElement element : array(json, key)) {
                result.add(element.getAsString());
            }
        }
        return result;
    }
}
