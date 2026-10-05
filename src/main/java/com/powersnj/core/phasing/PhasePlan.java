package com.powersnj.core.phasing;

import java.util.Locale;

/**
 * Result of {@link PhasePlanner#plan}.
 *
 * @param outcome      what the planner decided
 * @param x            destination block x (feet), valid on {@link Outcome#SUCCESS}
 * @param y            destination block y (feet)
 * @param z            destination block z (feet)
 * @param blocksPassed solid blocks crossed
 * @param distance     horizontal distance travelled
 */
public record PhasePlan(Outcome outcome, int x, int y, int z, int blocksPassed, double distance) {

    public enum Outcome {
        /** A wall was crossed and a safe exit found. */
        SUCCESS,
        /** No solid block in range: nothing to phase through. */
        NOTHING_TO_PHASE,
        /** A phase-proof block (bedrock...) is in the way. */
        BLOCKED,
        /** The wall is thicker than the allowed distance or every exit is unsafe. */
        NO_SAFE_EXIT;

        public String translationKey() {
            return "message.powersnj.phase." + this.name().toLowerCase(Locale.ROOT);
        }
    }

    public static PhasePlan failure(Outcome outcome, int blocksPassed, double distance) {
        return new PhasePlan(outcome, 0, 0, 0, blocksPassed, distance);
    }

    public boolean success() {
        return this.outcome == Outcome.SUCCESS;
    }
}
