package com.powersnj.core.phasing;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PhasePlannerTest {

    /** Simple voxel world: solid floor at y=63, configurable walls. */
    static final class Grid implements BlockQuery {
        final Set<String> solid = new HashSet<>();
        final Set<String> proof = new HashSet<>();
        final Set<String> hazard = new HashSet<>();

        Grid wall(int x, int fromY, int toY, int fromZ, int toZ) {
            for (int y = fromY; y <= toY; y++) {
                for (int z = fromZ; z <= toZ; z++) {
                    this.solid.add(x + "," + y + "," + z);
                }
            }
            return this;
        }

        @Override public boolean isSolid(int x, int y, int z) { return y <= 63 || this.solid.contains(x + "," + y + "," + z); }
        @Override public boolean isPhaseProof(int x, int y, int z) { return this.proof.contains(x + "," + y + "," + z); }
        @Override public boolean isHazard(int x, int y, int z) { return this.hazard.contains(x + "," + y + "," + z); }
        @Override public boolean isOutOfWorld(int x, int y, int z) { return y < -64 || y > 319; }
    }

    @Test
    void phasesThroughThinWall() {
        Grid grid = new Grid().wall(3, 64, 70, -2, 2).wall(4, 64, 70, -2, 2);
        PhasePlan plan = PhasePlanner.plan(0.5, 64, 0.5, 1, 0, 8, 2, grid);
        assertEquals(PhasePlan.Outcome.SUCCESS, plan.outcome());
        assertEquals(5, plan.x());
        assertEquals(64, plan.y());
        assertEquals(0, plan.z());
        assertEquals(4, plan.blocksPassed());
        assertTrue(PhasePlanner.isSafeSpot(plan.x(), plan.y(), plan.z(), 2, grid));
    }

    @Test
    void neverEndsInsideThickWalls() {
        Grid grid = new Grid();
        for (int x = 2; x <= 20; x++) {
            grid.wall(x, 64, 70, -1, 1);
        }
        assertEquals(PhasePlan.Outcome.NO_SAFE_EXIT, PhasePlanner.plan(0.5, 64, 0.5, 1, 0, 8, 2, grid).outcome());
    }

    @Test
    void phaseProofBlocksStopThePhase() {
        Grid grid = new Grid().wall(3, 64, 70, -1, 1);
        grid.proof.add("3,65,0");
        assertEquals(PhasePlan.Outcome.BLOCKED, PhasePlanner.plan(0.5, 64, 0.5, 1, 0, 8, 2, grid).outcome());
    }

    @Test
    void skipsHazardousExits() {
        Grid grid = new Grid().wall(3, 64, 70, -1, 1);
        grid.hazard.add("4,64,0");
        grid.hazard.add("4,63,0");
        PhasePlan plan = PhasePlanner.plan(0.5, 64, 0.5, 1, 0, 8, 2, grid);
        assertEquals(PhasePlan.Outcome.SUCCESS, plan.outcome());
        assertEquals(5, plan.x(), "lava right behind the wall is skipped");
    }

    @Test
    void nothingToPhaseInOpenField() {
        assertEquals(PhasePlan.Outcome.NOTHING_TO_PHASE, PhasePlanner.plan(0.5, 64, 0.5, 0, 1, 8, 2, new Grid()).outcome());
        assertEquals(PhasePlan.Outcome.NOTHING_TO_PHASE, PhasePlanner.plan(0.5, 64, 0.5, 0, 0, 8, 2, new Grid()).outcome());
    }

    @Test
    void stepUpExitIsAccepted() {
        // Behind the wall the ground is one block higher.
        Grid grid = new Grid().wall(3, 64, 70, -1, 1);
        for (int x = 4; x <= 10; x++) {
            grid.wall(x, 64, 64, -1, 1);
        }
        PhasePlan plan = PhasePlanner.plan(0.5, 64, 0.5, 1, 0, 8, 2, grid);
        assertEquals(PhasePlan.Outcome.SUCCESS, plan.outcome());
        assertEquals(65, plan.y());
    }

    @Test
    void antiStuckFindsNearestFreeSpot() {
        Grid grid = new Grid();
        for (int x = -3; x <= 3; x++) {
            grid.wall(x, 64, 66, -3, 3);
        }
        int[] spot = PhasePlanner.findNearestSafe(0, 65, 0, 8, 2, grid).orElseThrow();
        assertTrue(PhasePlanner.isSafeSpot(spot[0], spot[1], spot[2], 2, grid));
        assertEquals(67, spot[1], "closest exit is straight up");
        assertTrue(PhasePlanner.findNearestSafe(0, 65, 0, 1, 2, grid).isEmpty());
    }
}
