package com.powersnj.core.speedster;

/**
 * Per-tick environment observed by the server for one speedster.
 *
 * @param moving              the player has movement input / is moving horizontally
 * @param onGround            standing on a block
 * @param horizontalCollision bumped into a wall this tick
 * @param onWaterSurface      feet at a water surface
 * @param canWallRun          wall running is unlocked/allowed
 * @param canWaterRun         water running is unlocked/allowed
 */
public record SpeedsterInput(boolean moving, boolean onGround, boolean horizontalCollision, boolean onWaterSurface,
                             boolean canWallRun, boolean canWaterRun) {
}
