package com.powersnj.core.destruction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Selects which blocks an attack destroys. Plans are always bounded by a block budget (the server
 * {@code maxDestroyedBlocksPerAttack}) and sorted closest-first so the budget is spent where the
 * impact happened. Execution is spread over ticks by the Minecraft-side scheduler.
 */
public final class DestructionPlanner {

    /** Hard cap regardless of config, protects servers from absurd values. */
    public static final int ABSOLUTE_MAX_BLOCKS = 512;
    public static final double MAX_RADIUS = 8D;

    private DestructionPlanner() {
    }

    @FunctionalInterface
    public interface TraitsLookup {
        BlockTraits at(int x, int y, int z);
    }

    public record Target(int x, int y, int z, DestructionTier tier, double distanceSq) {
    }

    /**
     * Plans a spherical crater.
     *
     * @param radius       crater radius (clamped to {@link #MAX_RADIUS})
     * @param attackPower  strongest tier the attack breaks
     * @param allowExtreme server {@code allowObsidianDestruction}
     * @param budget       maximum blocks (clamped to {@link #ABSOLUTE_MAX_BLOCKS})
     */
    public static List<Target> sphere(int cx, int cy, int cz, double radius, DestructionTier attackPower, boolean allowExtreme, int budget, TraitsLookup lookup) {
        double r = Math.max(0D, Math.min(MAX_RADIUS, radius));
        int limit = Math.max(0, Math.min(ABSOLUTE_MAX_BLOCKS, budget));
        if (limit == 0 || r <= 0D) {
            return List.of();
        }
        int ri = (int) Math.ceil(r);
        double r2 = r * r;
        List<Target> candidates = new ArrayList<>();
        for (int dx = -ri; dx <= ri; dx++) {
            for (int dy = -ri; dy <= ri; dy++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    double d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 > r2) {
                        continue;
                    }
                    int x = cx + dx;
                    int y = cy + dy;
                    int z = cz + dz;
                    BlockTraits traits = lookup.at(x, y, z);
                    if (traits == null) {
                        continue;
                    }
                    DestructionTier tier = DestructionClassifier.classify(traits);
                    if (tier == null || !tier.canBeBrokenBy(attackPower)) {
                        continue;
                    }
                    if (tier == DestructionTier.EXTREME && !allowExtreme) {
                        continue;
                    }
                    candidates.add(new Target(x, y, z, tier, d2));
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(Target::distanceSq).thenComparingInt(Target::y).thenComparingInt(Target::x).thenComparingInt(Target::z));
        return candidates.size() > limit ? List.copyOf(candidates.subList(0, limit)) : List.copyOf(candidates);
    }

    /**
     * Plans a straight tunnel (charges, thrown bodies): {@code width} x {@code height} section along
     * a horizontal direction.
     */
    public static List<Target> line(int sx, int sy, int sz, double dirX, double dirZ, int length, int width, int height,
                                    DestructionTier attackPower, boolean allowExtreme, int budget, TraitsLookup lookup) {
        double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
        int limit = Math.max(0, Math.min(ABSOLUTE_MAX_BLOCKS, budget));
        if (len < 1.0E-6 || limit == 0) {
            return List.of();
        }
        double nx = dirX / len;
        double nz = dirZ / len;
        double px = -nz;
        double pz = nx;
        List<Target> result = new ArrayList<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        int half = Math.max(0, (width - 1) / 2);
        for (int step = 1; step <= length && result.size() < limit; step++) {
            for (int w = -half; w <= half && result.size() < limit; w++) {
                for (int h = 0; h < height && result.size() < limit; h++) {
                    int x = (int) Math.floor(sx + 0.5D + nx * step + px * w);
                    int z = (int) Math.floor(sz + 0.5D + nz * step + pz * w);
                    int y = sy + h;
                    long key = ((long) x << 38) ^ ((long) z << 12) ^ (y & 0xFFF);
                    if (!seen.add(key)) {
                        continue;
                    }
                    BlockTraits traits = lookup.at(x, y, z);
                    if (traits != null && DestructionClassifier.canDestroy(traits, attackPower, allowExtreme)) {
                        result.add(new Target(x, y, z, DestructionClassifier.classify(traits), (double) step * step));
                    }
                }
            }
        }
        return List.copyOf(result);
    }
}
