package com.powersnj.core.phasing;

/**
 * Read-only world view used by the phasing planner. Implemented over a {@code Level} in game and
 * over a hand-built grid in unit tests.
 */
public interface BlockQuery {

    /**
     * @return true when the block has a collision shape a body cannot stand inside
     */
    boolean isSolid(int x, int y, int z);

    /**
     * @return true when the block can never be phased through (bedrock, barriers, protected tags)
     */
    boolean isPhaseProof(int x, int y, int z);

    /**
     * @return true for dangerous blocks to end up in or above (lava, fire, magma...)
     */
    boolean isHazard(int x, int y, int z);

    /**
     * @return true when the position is outside the buildable world (void / height limit)
     */
    default boolean isOutOfWorld(int x, int y, int z) {
        return false;
    }
}
