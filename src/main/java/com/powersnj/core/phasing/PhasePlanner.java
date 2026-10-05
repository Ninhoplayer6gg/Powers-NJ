package com.powersnj.core.phasing;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Plans a phase through solid matter. This is NOT noclip: the server computes the complete path
 * up-front and only accepts it when it ends in a verified safe spot (body fits, no hazard, inside the
 * world), within the allowed distance and without crossing phase-proof blocks. The player is then
 * moved to that spot in one server-authoritative step.
 * <p>
 * {@link #findNearestSafe} is the anti-stuck fallback used by the phasing watchdog whenever a player
 * ends up inside blocks for any reason.
 */
public final class PhasePlanner {

    private static final double STEP = 0.25D;
    private static final int[] VERTICAL_TOLERANCE = {0, 1, -1};

    private PhasePlanner() {
    }

    /**
     * @param feetX       start position (feet), world coordinates
     * @param feetY       start position (feet)
     * @param feetZ       start position (feet)
     * @param dirX        horizontal direction x (need not be normalised)
     * @param dirZ        horizontal direction z
     * @param maxDistance maximum horizontal distance in blocks
     * @param bodyHeight  body height in blocks (2 for a standing player)
     */
    public static PhasePlan plan(double feetX, double feetY, double feetZ, double dirX, double dirZ, int maxDistance, int bodyHeight, BlockQuery world) {
        double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (len < 1.0E-6 || maxDistance <= 0) {
            return PhasePlan.failure(PhasePlan.Outcome.NOTHING_TO_PHASE, 0, 0);
        }
        double nx = dirX / len;
        double nz = dirZ / len;
        int feetBlockY = floor(feetY + 1.0E-3);

        boolean enteredSolid = false;
        Set<Long> crossed = new HashSet<>();
        int lastBx = floor(feetX);
        int lastBz = floor(feetZ);

        for (double d = STEP; d <= maxDistance + 1.0E-6; d += STEP) {
            int bx = floor(feetX + nx * d);
            int bz = floor(feetZ + nz * d);
            if (bx == lastBx && bz == lastBz) {
                continue;
            }
            lastBx = bx;
            lastBz = bz;

            for (int h = 0; h < bodyHeight; h++) {
                if (world.isPhaseProof(bx, feetBlockY + h, bz)) {
                    return PhasePlan.failure(PhasePlan.Outcome.BLOCKED, crossed.size(), d);
                }
            }

            // A column is "standable" when the body fits at the walking height or one block up/down
            // (steps and slopes are terrain, not walls).
            int standY = Integer.MIN_VALUE;
            for (int dy : VERTICAL_TOLERANCE) {
                if (isSafeSpot(bx, feetBlockY + dy, bz, bodyHeight, world)) {
                    standY = feetBlockY + dy;
                    break;
                }
            }

            if (standY == Integer.MIN_VALUE) {
                boolean solid = false;
                for (int h = 0; h < bodyHeight; h++) {
                    if (world.isSolid(bx, feetBlockY + h, bz)) {
                        solid = true;
                        crossed.add(pack(bx, feetBlockY + h, bz));
                    }
                }
                // Inside a wall (or above a hazard): keep going, never stop here.
                enteredSolid |= solid;
                continue;
            }

            if (enteredSolid) {
                return new PhasePlan(PhasePlan.Outcome.SUCCESS, bx, standY, bz, crossed.size(), d);
            }
        }
        return PhasePlan.failure(enteredSolid ? PhasePlan.Outcome.NO_SAFE_EXIT : PhasePlan.Outcome.NOTHING_TO_PHASE, crossed.size(), maxDistance);
    }

    /**
     * A spot is safe when the whole body fits, nothing at body height or directly below is a hazard,
     * and it is inside the world.
     */
    public static boolean isSafeSpot(int x, int y, int z, int bodyHeight, BlockQuery world) {
        if (world.isOutOfWorld(x, y, z) || world.isOutOfWorld(x, y + bodyHeight - 1, z)) {
            return false;
        }
        for (int h = 0; h < bodyHeight; h++) {
            if (world.isSolid(x, y + h, z) || world.isHazard(x, y + h, z)) {
                return false;
            }
        }
        return !world.isHazard(x, y - 1, z);
    }

    /**
     * Breadth-first search (by Manhattan shell) for the closest safe spot around a block.
     *
     * @return {x, y, z} of the closest safe feet position within {@code radius}
     */
    public static Optional<int[]> findNearestSafe(int x, int y, int z, int radius, int bodyHeight, BlockQuery world) {
        if (isSafeSpot(x, y, z, bodyHeight, world)) {
            return Optional.of(new int[]{x, y, z});
        }
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(new int[]{x, y, z});
        visited.add(pack(x, y, z));
        int[][] dirs = {{0, 1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, -1, 0}};
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            for (int[] dir : dirs) {
                int nx = cur[0] + dir[0];
                int ny = cur[1] + dir[1];
                int nz = cur[2] + dir[2];
                if (Math.abs(nx - x) + Math.abs(ny - y) + Math.abs(nz - z) > radius) {
                    continue;
                }
                if (!visited.add(pack(nx, ny, nz))) {
                    continue;
                }
                if (world.isOutOfWorld(nx, ny, nz)) {
                    continue;
                }
                if (isSafeSpot(nx, ny, nz, bodyHeight, world)) {
                    return Optional.of(new int[]{nx, ny, nz});
                }
                queue.add(new int[]{nx, ny, nz});
            }
        }
        return Optional.empty();
    }

    private static int floor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }
}
