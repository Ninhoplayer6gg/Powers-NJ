package com.powersnj.core.fabrication;

import java.util.List;
import java.util.Objects;

/**
 * Engine-agnostic copy of a {@code powersnj:suit_fabrication} recipe:
 * Blueprint + materials + Power Core &rarr; suit.
 *
 * @param id               recipe id
 * @param blueprint        required blueprint item id (mandatory)
 * @param powerCore        required power core item id
 * @param materials        material requirements (merged by item id)
 * @param resultSuit       suit id produced (its four armor pieces)
 * @param processingTicks  fabrication duration
 * @param consumeBlueprint whether the blueprint is used up
 */
public record FabricationSpec(String id, String blueprint, String powerCore, List<MaterialRequirement> materials, String resultSuit,
                              int processingTicks, boolean consumeBlueprint) {

    public static final int MAX_MATERIAL_TYPES = 6;

    public FabricationSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(blueprint, "blueprint");
        Objects.requireNonNull(powerCore, "powerCore");
        Objects.requireNonNull(resultSuit, "resultSuit");
        materials = List.copyOf(materials);
        if (materials.isEmpty() || materials.size() > MAX_MATERIAL_TYPES) {
            throw new IllegalArgumentException("Recipe " + id + " must have 1.." + MAX_MATERIAL_TYPES + " material types");
        }
        if (materials.stream().map(MaterialRequirement::itemId).distinct().count() != materials.size()) {
            throw new IllegalArgumentException("Recipe " + id + " lists the same material twice");
        }
        if (processingTicks < 1) {
            throw new IllegalArgumentException("processingTicks must be >= 1");
        }
    }
}
