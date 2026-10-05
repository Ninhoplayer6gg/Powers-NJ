package com.powersnj.core.destruction;

import java.util.Set;

/**
 * Engine-agnostic description of a block state, extracted from Minecraft by
 * {@code com.powersnj.destruction.DestructionEngine}.
 *
 * @param id              block id
 * @param hardness        destroy time ({@code < 0} = unbreakable)
 * @param blastResistance explosion resistance
 * @param air             is air
 * @param fluid           is a fluid source/flow
 * @param hasBlockEntity  has a block entity (chests, spawners...)
 * @param tags            block tags (only the {@code powersnj:destruction/*} tags matter)
 */
public record BlockTraits(String id, float hardness, float blastResistance, boolean air, boolean fluid, boolean hasBlockEntity, Set<String> tags) {

    public static final BlockTraits AIR = new BlockTraits("minecraft:air", 0F, 0F, true, false, false, Set.of());

    public BlockTraits {
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }

    public static BlockTraits of(String id, float hardness, float blastResistance) {
        return new BlockTraits(id, hardness, blastResistance, false, false, false, Set.of());
    }

    public boolean hasTag(String tag) {
        return this.tags.contains(tag);
    }
}
