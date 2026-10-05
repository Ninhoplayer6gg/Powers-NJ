package com.powersnj.core.fabrication;

/**
 * One ingredient of a suit fabrication recipe.
 *
 * @param itemId item id, e.g. {@code powersnj:viltrumite_alloy}
 * @param count  amount required
 */
public record MaterialRequirement(String itemId, int count) {

    public MaterialRequirement {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Material item id must not be blank");
        }
        if (count < 1) {
            throw new IllegalArgumentException("Material count must be >= 1");
        }
    }
}
