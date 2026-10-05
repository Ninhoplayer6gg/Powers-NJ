package com.powersnj.registry;

import com.powersnj.PowersNJ;
import com.powersnj.core.destruction.DestructionClassifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Tag keys. Destruction tags override the hardness based classification of the DestructionEngine.
 */
public final class ModTags {

    public static final TagKey<Block> DESTRUCTION_FRAGILE = block(DestructionClassifier.TAG_FRAGILE);
    public static final TagKey<Block> DESTRUCTION_NORMAL = block(DestructionClassifier.TAG_NORMAL);
    public static final TagKey<Block> DESTRUCTION_HARD = block(DestructionClassifier.TAG_HARD);
    public static final TagKey<Block> DESTRUCTION_EXTREME = block(DestructionClassifier.TAG_EXTREME);
    public static final TagKey<Block> DESTRUCTION_PROTECTED = block(DestructionClassifier.TAG_PROTECTED);
    /** Blocks speedsters can never phase through. */
    public static final TagKey<Block> PHASE_PROOF = TagKey.create(Registries.BLOCK, PowersNJ.id("phase_proof"));

    public static final List<TagKey<Block>> DESTRUCTION_TAGS = List.of(DESTRUCTION_FRAGILE, DESTRUCTION_NORMAL, DESTRUCTION_HARD, DESTRUCTION_EXTREME, DESTRUCTION_PROTECTED);

    public static final TagKey<Item> BLUEPRINTS = TagKey.create(Registries.ITEM, PowersNJ.id("blueprints"));
    public static final TagKey<Item> SUITS = TagKey.create(Registries.ITEM, PowersNJ.id("suits"));

    private ModTags() {
    }

    private static TagKey<Block> block(String id) {
        return TagKey.create(Registries.BLOCK, new ResourceLocation(id));
    }
}
