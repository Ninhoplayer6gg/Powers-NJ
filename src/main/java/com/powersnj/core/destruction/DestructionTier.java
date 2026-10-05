package com.powersnj.core.destruction;

import java.util.Locale;
import java.util.Optional;

/**
 * Resistance class of a block, ordered from weakest to unbreakable.
 * <ul>
 *     <li>FRAGILE - glass, leaves, plants</li>
 *     <li>NORMAL - dirt, wood, stone</li>
 *     <li>HARD - metal blocks and other resistant materials</li>
 *     <li>EXTREME - obsidian-class blocks (also gated by {@code allowObsidianDestruction})</li>
 *     <li>PROTECTED - bedrock, barriers, command blocks, block entities. Never broken.</li>
 * </ul>
 */
public enum DestructionTier {
    FRAGILE,
    NORMAL,
    HARD,
    EXTREME,
    PROTECTED;

    /**
     * @param attackPower the strongest tier an attack can break
     */
    public boolean canBeBrokenBy(DestructionTier attackPower) {
        return this != PROTECTED && attackPower != PROTECTED && this.ordinal() <= attackPower.ordinal();
    }

    public String serializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static Optional<DestructionTier> byName(String name) {
        for (DestructionTier tier : values()) {
            if (tier.serializedName().equalsIgnoreCase(name)) {
                return Optional.of(tier);
            }
        }
        return Optional.empty();
    }
}
