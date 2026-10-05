package com.powersnj.core.destruction;

/**
 * Maps {@link BlockTraits} to a {@link DestructionTier}.
 * <p>
 * Explicit block tags always win (data packs can re-balance any block), then hard safety rules
 * (unbreakable, block entities), then hardness/blast-resistance thresholds:
 * <pre>
 *   hardness &lt; 0                          -> PROTECTED (bedrock, barrier, end portal frame...)
 *   block entity                            -> PROTECTED (chests, spawners... no griefing, no item loss)
 *   hardness &gt;= 30 or resistance &gt;= 600    -> EXTREME   (obsidian 50/1200, ancient debris 30/1200)
 *   hardness &gt;= 5                          -> HARD      (iron/diamond blocks, anvils)
 *   hardness &gt;= 0.5                        -> NORMAL    (dirt 0.5, stone 1.5, wood 2, deepslate 3)
 *   otherwise                               -> FRAGILE   (glass 0.3, leaves 0.2, flowers 0)
 * </pre>
 */
public final class DestructionClassifier {

    public static final String TAG_FRAGILE = "powersnj:destruction/fragile";
    public static final String TAG_NORMAL = "powersnj:destruction/normal";
    public static final String TAG_HARD = "powersnj:destruction/hard";
    public static final String TAG_EXTREME = "powersnj:destruction/extreme";
    public static final String TAG_PROTECTED = "powersnj:destruction/protected";

    public static final float EXTREME_HARDNESS = 30F;
    public static final float EXTREME_RESISTANCE = 600F;
    public static final float HARD_HARDNESS = 5F;
    public static final float NORMAL_HARDNESS = 0.5F;

    private DestructionClassifier() {
    }

    /**
     * @return the tier, or {@code null} for air/fluids (nothing to destroy)
     */
    public static DestructionTier classify(BlockTraits traits) {
        if (traits.air() || traits.fluid()) {
            return null;
        }
        // Unbreakable blocks are protected no matter which tags a data pack adds.
        if (traits.hardness() < 0F || traits.hasTag(TAG_PROTECTED)) {
            return DestructionTier.PROTECTED;
        }
        if (traits.hasTag(TAG_EXTREME)) {
            return DestructionTier.EXTREME;
        }
        if (traits.hasTag(TAG_HARD)) {
            return DestructionTier.HARD;
        }
        if (traits.hasTag(TAG_NORMAL)) {
            return DestructionTier.NORMAL;
        }
        if (traits.hasTag(TAG_FRAGILE)) {
            return DestructionTier.FRAGILE;
        }
        if (traits.hasBlockEntity()) {
            return DestructionTier.PROTECTED;
        }
        if (traits.hardness() >= EXTREME_HARDNESS || traits.blastResistance() >= EXTREME_RESISTANCE) {
            return DestructionTier.EXTREME;
        }
        if (traits.hardness() >= HARD_HARDNESS) {
            return DestructionTier.HARD;
        }
        if (traits.hardness() >= NORMAL_HARDNESS) {
            return DestructionTier.NORMAL;
        }
        return DestructionTier.FRAGILE;
    }

    /**
     * Full permission check for one block.
     *
     * @param attackPower      strongest tier the attack can break
     * @param allowExtreme     server {@code allowObsidianDestruction}
     */
    public static boolean canDestroy(BlockTraits traits, DestructionTier attackPower, boolean allowExtreme) {
        DestructionTier tier = classify(traits);
        if (tier == null || !tier.canBeBrokenBy(attackPower)) {
            return false;
        }
        return tier != DestructionTier.EXTREME || allowExtreme;
    }
}
