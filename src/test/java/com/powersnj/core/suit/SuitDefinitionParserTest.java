package com.powersnj.core.suit;

import com.google.gson.JsonParseException;
import com.powersnj.core.energy.EnergyType;
import com.powersnj.core.flight.FlightProfile;
import com.powersnj.core.speedster.SpeedsterProfile;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SuitDefinitionParserTest {

    static final String SAMPLE = """
            {
              "character": "Test Hero",
              "power_rating": 70,
              "max_level": 10,
              "xp_curve": {"base": 40, "exponent": 1.2},
              "palladium_power": "powersnj:test",
              "energy": {"type": "powersnj:biomass", "base_max": 120, "max_per_level": 8, "regen_per_tick": 0.3},
              "abilities": ["a", "b"],
              "passives": ["p"],
              "movement": {"type": "powersnj:speedster", "max_multiplier": 9, "speed_levels": 4},
              "systems": ["powersnj:venom_weaknesses"],
              "settings": {"regen_amount": 1.5},
              "hud_profile": "powersnj:symbiote",
              "xp": {"kill_mob": 3},
              "skill_tree": [
                {"id": "a", "required_level": 1, "cost": 1},
                {"id": "b", "required_level": 4, "cost": 2, "parents": ["a"], "requirement": {"stat": "kills", "min": 5}, "x": 1, "y": 2}
              ]
            }
            """;

    @Test
    void parsesEveryField() {
        SuitDefinition def = SuitDefinitionParser.parse("powersnj:test", SAMPLE);
        assertEquals("Test Hero", def.character());
        assertEquals(70, def.powerRating());
        assertEquals(10, def.maxLevel());
        assertEquals(40, def.curve().base(), 1e-9);
        assertSame(EnergyType.BIOMASS, def.energy().type());
        assertEquals(120 + 8 * 9, def.energy().maxAt(10), 1e-6);
        assertEquals(List.of("a", "b"), def.abilities());
        assertEquals("powersnj:speedster", def.movement());
        assertTrue(def.hasSystem("powersnj:venom_weaknesses"));
        assertEquals(1.5, def.setting("regen_amount", 0), 1e-9, "top-level settings are merged");
        assertEquals(9, def.setting("max_multiplier", 0), 1e-9, "movement settings are merged");
        assertEquals(42, def.setting("missing", 42), 1e-9);
        assertEquals(3, def.xpRules().killMob(), 1e-9);
        assertEquals(2, def.skillTree().size());
        assertEquals(5, def.skillTree().node("b").orElseThrow().requirement().min());
        assertEquals("test", def.path());

        SpeedsterProfile speed = SpeedsterProfile.from(def);
        assertEquals(9, speed.maxMultiplier(), 1e-9);
        assertEquals(4, speed.speedLevels());
        assertEquals(SpeedsterProfile.DEFAULT.acceleration(), speed.acceleration(), 1e-9, "missing settings fall back to defaults");
        assertEquals(FlightProfile.DEFAULT.cruiseSpeed(), FlightProfile.from(def).cruiseSpeed(), 1e-9);
    }

    @Test
    void rejectsInvalidData() {
        assertThrows(JsonParseException.class, () -> SuitDefinitionParser.parse("x:y", "{}"), "energy is mandatory");
        assertThrows(JsonParseException.class, () -> SuitDefinitionParser.parse("x:y",
                "{\"energy\":{\"type\":\"powersnj:unknown\"}}"));
        assertThrows(JsonParseException.class, () -> SuitDefinitionParser.parse("x:y",
                "{\"energy\":{\"type\":\"powersnj:biomass\"},\"skill_tree\":[{\"id\":\"a\",\"parents\":[\"b\"]}]}"));
        assertThrows(JsonParseException.class, () -> SuitDefinitionParser.parse("x:y",
                "{\"max_level\":5,\"energy\":{\"type\":\"powersnj:biomass\"},\"skill_tree\":[{\"id\":\"a\",\"required_level\":6}]}"));
        assertThrows(IllegalArgumentException.class, () -> SuitDefinitionParser.parse("x:y",
                "{\"power_rating\":150,\"energy\":{\"type\":\"powersnj:biomass\"}}"));
    }

    /**
     * Every shipped suit definition must parse and reference a known skill for each ability gate.
     */
    @Test
    void shippedDefinitionsAreValid() throws IOException {
        Path dir = ResourceRoot.get().resolve("data/powersnj/powersnj/suits");
        assertTrue(Files.isDirectory(dir), "missing " + dir);
        List<Path> files;
        try (Stream<Path> stream = Files.list(dir)) {
            files = stream.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
        assertEquals(3, files.size(), "Thragg, Venom and Reverse-Flash ship with the mod");
        for (Path file : files) {
            String name = file.getFileName().toString().replace(".json", "");
            SuitDefinition def = SuitDefinitionParser.parse("powersnj:" + name, Files.readString(file));
            assertEquals(20, def.maxLevel(), name + " must use levels 1-20");
            assertFalse(def.skillTree().isEmpty(), name + " needs a skill tree");
            assertTrue(def.skillTree().nodes().stream().anyMatch(n -> n.requiredLevel() == 1 && n.parents().isEmpty() && n.cost() <= 1),
                    name + " needs a skill unlockable at level 1");
            assertEquals("powersnj:" + name, def.palladiumPower());
        }
    }
}
