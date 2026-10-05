package com.powersnj.core.speedster;

/**
 * Discrete things that happened during a speedster tick. The Minecraft side turns them into
 * sounds, particles, damage and visual hooks.
 */
public enum SpeedsterEvent {
    STARTED,
    STOPPED,
    OUT_OF_ENERGY,
    TOP_SPEED_REACHED,
    WALL_IMPACT,
    WALL_RUN_START,
    WALL_RUN_END,
    WATER_RUN_START,
    WATER_RUN_END
}
