package com.powersnj.core.architecture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.powersnj.core.ability.ActivationResult;
import com.powersnj.core.energy.EnergyType;
import com.powersnj.core.phasing.PhasePlan;
import com.powersnj.core.skill.SkillNode;
import com.powersnj.core.skill.SkillStatus;
import com.powersnj.core.skill.SkillUnlockResult;
import com.powersnj.core.suit.ResourceRoot;
import com.powersnj.core.suit.SuitDefinition;
import com.powersnj.core.suit.SuitDefinitionParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cross-checks code, data packs and assets so broken references are caught at build time instead
 * of in game: translations (en_us and pt_br), skills referenced by Palladium powers, ability icons,
 * models/textures, sounds, particles, loot modifiers and item ids used in data files.
 */
class ResourceConsistencyTest {

    private static final List<String> SUITS = List.of("thragg", "venom", "reverse_flash");
    private static Path resources;
    private static Path assets;
    private static Path data;
    private static Path sources;
    private static Set<String> enKeys;
    private static Set<String> ptKeys;
    private static Set<String> registeredIds;

    @BeforeAll
    static void load() throws IOException {
        resources = ResourceRoot.get();
        assets = resources.resolve("assets/powersnj");
        data = resources.resolve("data");
        sources = ResourceRoot.sources();
        enKeys = JsonParser.parseString(Files.readString(assets.resolve("lang/en_us.json"))).getAsJsonObject().keySet();
        ptKeys = JsonParser.parseString(Files.readString(assets.resolve("lang/pt_br.json"))).getAsJsonObject().keySet();
        registeredIds = new HashSet<>();
        String items = Files.readString(sources.resolve("com/powersnj/registry/ModItems.java"));
        String blocks = Files.readString(sources.resolve("com/powersnj/registry/ModBlocks.java"));
        Matcher m = Pattern.compile("(?:material|blueprint|blockItem|BLOCKS\\.register)\\(\"([a-z0-9_]+)\"").matcher(items + blocks);
        while (m.find()) {
            registeredIds.add("powersnj:" + m.group(1));
        }
        for (String suit : SUITS) {
            for (String piece : List.of("helmet", "chestplate", "leggings", "boots")) {
                registeredIds.add("powersnj:" + suit + "_" + piece);
            }
        }
    }

    private static JsonObject json(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    @Test
    void languagesHaveTheSameKeys() {
        Set<String> missingPt = new TreeSet<>(enKeys);
        missingPt.removeAll(ptKeys);
        Set<String> missingEn = new TreeSet<>(ptKeys);
        missingEn.removeAll(enKeys);
        assertTrue(missingPt.isEmpty(), "Missing in pt_br: " + missingPt);
        assertTrue(missingEn.isEmpty(), "Missing in en_us: " + missingEn);
    }

    @Test
    void literalTranslationKeysInCodeExist() throws IOException {
        Pattern literal = Pattern.compile("Component\\.translatable\\(\"([a-z0-9_.]+)\"");
        Set<String> missing = new TreeSet<>();
        try (Stream<Path> files = Files.walk(sources)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher m = literal.matcher(Files.readString(file));
                while (m.find()) {
                    String key = m.group(1);
                    if (!key.endsWith(".") && !enKeys.contains(key)) {
                        missing.add(key + " (" + file.getFileName() + ")");
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "Untranslated keys: " + missing);
    }

    @Test
    void enumDrivenKeysExist() {
        List<String> keys = new ArrayList<>();
        for (ActivationResult r : ActivationResult.values()) {
            keys.add(r.translationKey());
        }
        for (SkillUnlockResult r : SkillUnlockResult.values()) {
            keys.add(r.translationKey());
        }
        for (PhasePlan.Outcome o : PhasePlan.Outcome.values()) {
            keys.add(o.translationKey());
        }
        for (SkillStatus s : SkillStatus.values()) {
            keys.add("gui.powersnj.skill_tree.status." + s.name().toLowerCase(Locale.ROOT));
        }
        for (EnergyType type : EnergyType.values()) {
            keys.add(type.translationKey());
        }
        for (String status : List.of("no_blueprint", "unknown_blueprint", "missing_core", "missing_materials", "output_blocked", "ready", "fabricating")) {
            keys.add("gui.powersnj.suit_forge.status." + status);
        }
        for (String suit : SUITS) {
            keys.add("suit.powersnj." + suit);
            keys.add("power.powersnj." + suit);
            keys.add("suitset.powersnj." + suit);
        }
        for (String id : registeredIds) {
            String path = id.substring("powersnj:".length());
            keys.add(enKeys.contains("block.powersnj." + path) ? "block.powersnj." + path : "item.powersnj." + path);
        }
        List<String> missing = keys.stream().filter(k -> !enKeys.contains(k)).toList();
        assertTrue(missing.isEmpty(), "Missing keys: " + missing);
    }

    @Test
    void powersReferenceExistingSkillsIconsAndTranslations() throws IOException {
        for (String suit : SUITS) {
            SuitDefinition definition = SuitDefinitionParser.parse("powersnj:" + suit, Files.readString(data.resolve("powersnj/powersnj/suits/" + suit + ".json")));
            JsonObject power = json(data.resolve("powersnj/palladium/powers/" + suit + ".json"));
            JsonObject abilities = power.getAsJsonObject("abilities");
            for (String key : definition.abilities()) {
                assertTrue(abilities.has(key), suit + ": definition lists ability '" + key + "' missing from the Palladium power");
            }
            for (String key : definition.passives()) {
                assertTrue(abilities.has(key), suit + ": definition lists passive '" + key + "' missing from the Palladium power");
            }
            for (SkillNode node : definition.skillTree().nodes()) {
                assertTrue(enKeys.contains(node.translationKey(definition.id())), "missing skill title " + node.translationKey(definition.id()));
                assertTrue(enKeys.contains(node.translationKey(definition.id()) + ".desc"), "missing skill description " + node.id());
                if (!node.icon().isEmpty()) {
                    assertTrue(Files.exists(texture(node.icon())), "missing skill icon " + node.icon());
                }
            }
            for (String key : abilities.keySet()) {
                JsonObject ability = abilities.getAsJsonObject(key);
                if (ability.has("title")) {
                    String t = ability.getAsJsonObject("title").get("translate").getAsString();
                    assertTrue(enKeys.contains(t), "missing ability title " + t);
                }
                if (ability.has("icon") && ability.get("icon").getAsString().endsWith(".png")) {
                    assertTrue(Files.exists(texture(ability.get("icon").getAsString())), "missing ability icon " + ability.get("icon"));
                }
                for (String skill : referencedSkills(ability)) {
                    assertTrue(definition.skillTree().node(skill).isPresent(), suit + "/" + key + " references unknown skill '" + skill + "'");
                }
                if (ability.has("abilities")) {
                    for (JsonElement wheelEntry : ability.getAsJsonArray("abilities")) {
                        assertTrue(abilities.has(wheelEntry.getAsString()), suit + " wheel references unknown ability " + wheelEntry);
                    }
                }
            }
            JsonObject suitSet = json(data.resolve("powersnj/palladium/suit_set_powers/" + suit + ".json"));
            assertEquals("powersnj:" + suit, suitSet.get("power").getAsString());
        }
    }

    private static List<String> referencedSkills(JsonObject ability) {
        List<String> skills = new ArrayList<>();
        if (ability.has("required_skill") && !ability.get("required_skill").getAsString().isEmpty()) {
            skills.add(ability.get("required_skill").getAsString());
        }
        if (ability.has("conditions")) {
            JsonObject conditions = ability.getAsJsonObject("conditions");
            for (String type : List.of("unlocking", "enabling")) {
                if (conditions.has(type)) {
                    JsonElement element = conditions.get(type);
                    JsonArray array = element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
                    if (element.isJsonObject()) {
                        array.add(element);
                    }
                    for (JsonElement c : array) {
                        JsonObject condition = c.getAsJsonObject();
                        if ("powersnj:skill_unlocked".equals(condition.get("type").getAsString())) {
                            skills.add(condition.get("skill").getAsString());
                        }
                    }
                }
            }
        }
        return skills;
    }

    private static Path texture(String location) {
        String[] parts = location.split(":", 2);
        return resources.resolve("assets/" + parts[0] + "/" + parts[1]);
    }

    @Test
    void modelsAndBlockstatesPointToExistingFiles() throws IOException {
        List<String> missing = new ArrayList<>();
        try (Stream<Path> files = Files.walk(assets.resolve("models"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                JsonObject model = json(file);
                if (model.has("textures")) {
                    for (var entry : model.getAsJsonObject("textures").entrySet()) {
                        String value = entry.getValue().getAsString();
                        if (value.startsWith("powersnj:")) {
                            Path png = resources.resolve("assets/powersnj/textures/" + value.substring(9) + ".png");
                            if (!Files.exists(png)) {
                                missing.add(file.getFileName() + " -> " + value);
                            }
                        }
                    }
                }
                if (model.has("parent") && model.get("parent").getAsString().startsWith("powersnj:")) {
                    Path parent = assets.resolve("models/" + model.get("parent").getAsString().substring(9) + ".json");
                    if (!Files.exists(parent)) {
                        missing.add(file.getFileName() + " parent " + model.get("parent"));
                    }
                }
            }
        }
        for (String id : registeredIds) {
            String path = id.substring(9);
            if (!Files.exists(assets.resolve("models/item/" + path + ".json"))) {
                missing.add("item model " + path);
            }
        }
        try (Stream<Path> files = Files.list(assets.resolve("blockstates"))) {
            for (Path file : files.toList()) {
                for (var variant : json(file).getAsJsonObject("variants").entrySet()) {
                    String model = variant.getValue().getAsJsonObject().get("model").getAsString();
                    if (!Files.exists(assets.resolve("models/" + model.substring(9) + ".json"))) {
                        missing.add(file.getFileName() + " -> " + model);
                    }
                }
            }
        }
        for (String suit : SUITS) {
            for (int layer = 1; layer <= 2; layer++) {
                Path png = assets.resolve("textures/suits/" + suit + "/" + suit + "_layer_" + layer + ".png");
                if (!Files.exists(png)) {
                    missing.add(png.toString());
                }
            }
        }
        assertTrue(missing.isEmpty(), "Broken asset references: " + missing);
    }

    @Test
    void soundsAndParticlesAreDefined() throws IOException {
        JsonObject soundsJson = json(assets.resolve("sounds.json"));
        Matcher sounds = Pattern.compile("register\\(\"([a-z0-9_.]+)\"\\)").matcher(Files.readString(sources.resolve("com/powersnj/registry/ModSounds.java")));
        while (sounds.find()) {
            assertTrue(soundsJson.has(sounds.group(1)), "sounds.json misses " + sounds.group(1));
            assertTrue(enKeys.contains("subtitles.powersnj." + sounds.group(1)), "missing subtitle " + sounds.group(1));
        }
        Matcher particles = Pattern.compile("PARTICLES\\.register\\(\"([a-z0-9_]+)\"").matcher(Files.readString(sources.resolve("com/powersnj/registry/ModParticles.java")));
        while (particles.find()) {
            Path definition = assets.resolve("particles/" + particles.group(1) + ".json");
            assertTrue(Files.exists(definition), "missing particle definition " + definition);
            for (JsonElement texture : json(definition).getAsJsonArray("textures")) {
                assertTrue(Files.exists(assets.resolve("textures/particle/" + texture.getAsString().substring(9) + ".png")), "missing particle texture " + texture);
            }
        }
    }

    @Test
    void dataFilesReferenceRegisteredItemsAndModifiers() throws IOException {
        Pattern id = Pattern.compile("\"(powersnj:[a-z0-9_]+)\"");
        Set<String> knownNonItems = new HashSet<>(List.of("powersnj:suit_fabrication", "powersnj:add_item", "powersnj:add_table", "powersnj:configured_count",
                "powersnj:viltrumite_ore", "powersnj:speed_crystal_ore", "powersnj:thragg", "powersnj:venom", "powersnj:reverse_flash"));
        List<String> unknown = new ArrayList<>();
        for (String dir : List.of("powersnj/recipes", "powersnj/loot_tables/blocks", "powersnj/tags/items", "minecraft/tags/blocks")) {
            try (Stream<Path> files = Files.walk(data.resolve(dir))) {
                for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                    Matcher m = id.matcher(Files.readString(file));
                    while (m.find()) {
                        if (!registeredIds.contains(m.group(1)) && !knownNonItems.contains(m.group(1))) {
                            unknown.add(file.getFileName() + ": " + m.group(1));
                        }
                    }
                }
            }
        }
        assertTrue(unknown.isEmpty(), "Unknown ids in data: " + unknown);

        JsonObject global = json(data.resolve("forge/loot_modifiers/global_loot_modifiers.json"));
        for (JsonElement entry : global.getAsJsonArray("entries")) {
            String path = entry.getAsString().substring(9);
            assertTrue(Files.exists(data.resolve("powersnj/loot_modifiers/" + path + ".json")), "missing loot modifier " + entry);
        }
        assertTrue(Files.exists(data.resolve("powersnj/structures/empty.nbt")), "GameTest template missing");
        for (String suit : SUITS) {
            assertTrue(Files.exists(data.resolve("powersnj/recipes/suit_forge/" + suit + ".json")), "missing fabrication recipe for " + suit);
        }
    }
}
