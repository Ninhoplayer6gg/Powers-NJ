package com.powersnj.core.suit;

import com.powersnj.core.energy.EnergySpec;
import com.powersnj.core.progression.LevelCurve;
import com.powersnj.core.progression.XpRules;
import com.powersnj.core.skill.SkillTree;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Data-driven description of a suit / power set, loaded from
 * {@code data/<namespace>/powersnj/suits/<name>.json}. It holds everything that is not an active
 * ability (those live in the Palladium power referenced by {@link #palladiumPower()}).
 *
 * @param id               suit id ({@code namespace:name}, from the file path)
 * @param character        display name of the character
 * @param powerRating      0..100 rating shown in the Suit Forge and HUD
 * @param curve            level curve, including {@code max_level}
 * @param energy           primary resource and its scaling
 * @param palladiumPower   Palladium power granted while the full suit set is worn
 * @param abilities        Palladium ability keys shown by the HUD (in display order)
 * @param passives         Palladium ability keys that are passives (documentation + HUD)
 * @param movement         movement controller id, e.g. {@code powersnj:flight}
 * @param settings         numeric tuning for the movement controller and special systems
 * @param systems          special always-on systems, e.g. {@code powersnj:venom_weaknesses}
 * @param skillTree        validated skill tree
 * @param hudProfile       HUD layout id
 * @param xpRules          XP rewards
 */
public record SuitDefinition(
        String id,
        String character,
        int powerRating,
        LevelCurve curve,
        EnergySpec energy,
        String palladiumPower,
        List<String> abilities,
        List<String> passives,
        String movement,
        Map<String, Double> settings,
        List<String> systems,
        SkillTree skillTree,
        String hudProfile,
        XpRules xpRules
) {

    public SuitDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(curve, "curve");
        Objects.requireNonNull(energy, "energy");
        Objects.requireNonNull(skillTree, "skillTree");
        if (powerRating < 0 || powerRating > 100) {
            throw new IllegalArgumentException("power_rating must be within 0..100 for suit " + id);
        }
        abilities = List.copyOf(abilities);
        passives = List.copyOf(passives);
        systems = List.copyOf(systems);
        settings = Map.copyOf(settings);
    }

    public int maxLevel() {
        return this.curve.maxLevel();
    }

    public double setting(String key, double fallback) {
        Double value = this.settings.get(key);
        return value == null ? fallback : value;
    }

    public boolean hasSystem(String systemId) {
        return this.systems.contains(systemId);
    }

    /**
     * Path part of the id, used to build asset paths ({@code textures/suits/<path>/...}).
     */
    public String path() {
        int idx = this.id.indexOf(':');
        return idx < 0 ? this.id : this.id.substring(idx + 1);
    }
}
