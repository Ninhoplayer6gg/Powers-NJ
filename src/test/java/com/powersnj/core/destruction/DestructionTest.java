package com.powersnj.core.destruction;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.powersnj.core.destruction.DestructionTier.*;
import static org.junit.jupiter.api.Assertions.*;

class DestructionTest {

    // Vanilla 1.20.1 hardness / blast resistance values.
    static final BlockTraits GLASS = BlockTraits.of("minecraft:glass", 0.3F, 0.3F);
    static final BlockTraits LEAVES = BlockTraits.of("minecraft:oak_leaves", 0.2F, 0.2F);
    static final BlockTraits DIRT = BlockTraits.of("minecraft:dirt", 0.5F, 0.5F);
    static final BlockTraits PLANKS = BlockTraits.of("minecraft:oak_planks", 2.0F, 3.0F);
    static final BlockTraits STONE = BlockTraits.of("minecraft:stone", 1.5F, 6.0F);
    static final BlockTraits IRON_BLOCK = BlockTraits.of("minecraft:iron_block", 5.0F, 6.0F);
    static final BlockTraits OBSIDIAN = BlockTraits.of("minecraft:obsidian", 50F, 1200F);
    static final BlockTraits BEDROCK = BlockTraits.of("minecraft:bedrock", -1F, 3600000F);
    static final BlockTraits CHEST = new BlockTraits("minecraft:chest", 2.5F, 2.5F, false, false, true, Set.of());

    @Test
    void classifiesVanillaBlocks() {
        assertEquals(FRAGILE, DestructionClassifier.classify(GLASS));
        assertEquals(FRAGILE, DestructionClassifier.classify(LEAVES));
        assertEquals(NORMAL, DestructionClassifier.classify(DIRT));
        assertEquals(NORMAL, DestructionClassifier.classify(PLANKS));
        assertEquals(NORMAL, DestructionClassifier.classify(STONE));
        assertEquals(HARD, DestructionClassifier.classify(IRON_BLOCK));
        assertEquals(EXTREME, DestructionClassifier.classify(OBSIDIAN));
        assertEquals(PROTECTED, DestructionClassifier.classify(BEDROCK));
        assertEquals(PROTECTED, DestructionClassifier.classify(CHEST), "block entities are never destroyed by default");
        assertNull(DestructionClassifier.classify(BlockTraits.AIR));
    }

    @Test
    void tagsOverrideThresholdsButNeverUnprotectBedrock() {
        BlockTraits taggedStone = new BlockTraits("minecraft:stone", 1.5F, 6F, false, false, false, Set.of(DestructionClassifier.TAG_HARD));
        assertEquals(HARD, DestructionClassifier.classify(taggedStone));
        BlockTraits fragileBedrock = new BlockTraits("minecraft:bedrock", -1F, 3600000F, false, false, false, Set.of(DestructionClassifier.TAG_FRAGILE));
        assertEquals(PROTECTED, DestructionClassifier.classify(fragileBedrock), "bedrock is never breakable");
        BlockTraits protectedDirt = new BlockTraits("minecraft:dirt", 0.5F, 0.5F, false, false, false, Set.of(DestructionClassifier.TAG_PROTECTED));
        assertEquals(PROTECTED, DestructionClassifier.classify(protectedDirt));
    }

    @Test
    void tierOrderingAndPermissions() {
        assertTrue(FRAGILE.canBeBrokenBy(NORMAL));
        assertTrue(NORMAL.canBeBrokenBy(NORMAL));
        assertFalse(HARD.canBeBrokenBy(NORMAL));
        assertTrue(EXTREME.canBeBrokenBy(EXTREME));
        assertFalse(PROTECTED.canBeBrokenBy(EXTREME));
        assertFalse(PROTECTED.canBeBrokenBy(PROTECTED), "nothing breaks protected blocks");
        assertTrue(DestructionClassifier.canDestroy(OBSIDIAN, EXTREME, true));
        assertFalse(DestructionClassifier.canDestroy(OBSIDIAN, EXTREME, false), "allowObsidianDestruction=false");
        assertFalse(DestructionClassifier.canDestroy(BEDROCK, EXTREME, true));
        assertEquals(EXTREME, DestructionTier.byName("Extreme").orElseThrow());
    }

    @Test
    void sphereRespectsBudgetAndProtection() {
        // Floor of obsidian at y=0 with a bedrock pillar at the centre; stone everywhere else.
        DestructionPlanner.TraitsLookup world = (x, y, z) -> {
            if (x == 0 && z == 0) {
                return BEDROCK;
            }
            return y == 0 ? OBSIDIAN : STONE;
        };
        List<DestructionPlanner.Target> plan = DestructionPlanner.sphere(0, 0, 0, 3, EXTREME, false, 1000, world);
        assertFalse(plan.isEmpty());
        assertTrue(plan.stream().noneMatch(t -> t.x() == 0 && t.z() == 0), "bedrock never planned");
        assertTrue(plan.stream().noneMatch(t -> t.tier() == EXTREME), "obsidian disabled by config");

        List<DestructionPlanner.Target> limited = DestructionPlanner.sphere(0, 0, 0, 3, EXTREME, true, 10, world);
        assertEquals(10, limited.size(), "maxDestroyedBlocksPerAttack");
        for (int i = 1; i < limited.size(); i++) {
            assertTrue(limited.get(i).distanceSq() >= limited.get(i - 1).distanceSq(), "closest blocks first");
        }
        assertTrue(DestructionPlanner.sphere(0, 0, 0, 3, NORMAL, true, 0, world).isEmpty());
        assertTrue(DestructionPlanner.sphere(0, 0, 0, 100, NORMAL, true, 100000, world).size() <= DestructionPlanner.ABSOLUTE_MAX_BLOCKS);
    }

    @Test
    void lineCarvesTunnel() {
        DestructionPlanner.TraitsLookup world = (x, y, z) -> x > 5 ? IRON_BLOCK : DIRT;
        List<DestructionPlanner.Target> tunnel = DestructionPlanner.line(0, 64, 0, 1, 0, 8, 1, 2, NORMAL, false, 100, world);
        assertEquals(10, tunnel.size(), "5 dirt columns x 2 high, iron is too hard");
        assertTrue(tunnel.stream().allMatch(t -> t.x() <= 5));
    }
}
