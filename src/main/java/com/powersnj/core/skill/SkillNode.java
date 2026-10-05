package com.powersnj.core.skill;

import java.util.List;
import java.util.Objects;

/**
 * A node of a suit skill tree.
 *
 * @param id            skill id, unique inside its tree (referenced by abilities and conditions)
 * @param requiredLevel minimum suit level
 * @param parents       skills that must be unlocked first
 * @param cost          skill points consumed
 * @param requirement   optional statistic requirement
 * @param icon          icon resource location (texture) used by the skill tree screen
 * @param x             column in the skill tree screen grid
 * @param y             row in the skill tree screen grid
 */
public record SkillNode(String id, int requiredLevel, List<String> parents, int cost, SkillRequirement requirement, String icon, int x, int y) {

    public SkillNode {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Skill id must not be blank");
        }
        if (requiredLevel < 1) {
            throw new IllegalArgumentException("requiredLevel must be >= 1 for skill " + id);
        }
        if (cost < 0) {
            throw new IllegalArgumentException("cost must be >= 0 for skill " + id);
        }
        parents = List.copyOf(parents);
        requirement = requirement == null ? SkillRequirement.NONE : requirement;
        icon = icon == null ? "" : icon;
    }

    public static SkillNode simple(String id, int requiredLevel, int cost, String... parents) {
        return new SkillNode(id, requiredLevel, List.of(parents), cost, SkillRequirement.NONE, "", 0, 0);
    }

    /**
     * Translation key used for the skill title. Description uses the same key + {@code .desc}.
     */
    public String translationKey(String suitId) {
        return "skill." + suitId.replace(':', '.') + "." + this.id;
    }
}
