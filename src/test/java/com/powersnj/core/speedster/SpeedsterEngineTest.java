package com.powersnj.core.speedster;

import com.powersnj.core.config.PowersSettings;
import com.powersnj.core.data.MapDataNode;
import com.powersnj.core.energy.EnergyPool;
import com.powersnj.core.energy.EnergyType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpeedsterEngineTest {

    static final SpeedsterProfile PROFILE = new SpeedsterProfile(2D, 10D, 5, 0.5D, 1D, 0.25D, 1F, 3D, 2.5D, 4D, 1.5D, 1D);
    static final SpeedsterInput RUNNING = new SpeedsterInput(true, true, false, false, true, true);
    static final SpeedsterInput IDLE = new SpeedsterInput(false, true, false, false, true, true);

    private static EnergyPool energy() {
        return new EnergyPool(EnergyType.NEGATIVE_SPEED_FORCE, 1000, 0, 0);
    }

    @Test
    void inactiveEngineIsNormalSpeed() {
        SpeedsterEngine engine = new SpeedsterEngine(PROFILE);
        assertEquals(1D, engine.tick(RUNNING, energy(), PowersSettings.DEFAULTS).multiplier(), 1e-9);
        assertEquals(1D, engine.targetMultiplier(PowersSettings.DEFAULTS), 1e-9);
    }

    @Test
    void acceleratesTowardsSelectedLevelAndDecelerates() {
        SpeedsterEngine engine = new SpeedsterEngine(PROFILE);
        EnergyPool pool = energy();
        engine.setActive(true);
        engine.setSpeedLevel(5);
        assertEquals(10D, engine.targetMultiplier(PowersSettings.DEFAULTS), 1e-9);
        assertEquals(1.5D, engine.tick(RUNNING, pool, PowersSettings.DEFAULTS).multiplier(), 1e-9, "acceleration is gradual");
        SpeedsterEngine.SpeedsterTick last = null;
        for (int i = 0; i < 40; i++) {
            last = engine.tick(RUNNING, pool, PowersSettings.DEFAULTS);
        }
        assertEquals(10D, last.multiplier(), 1e-9);
        assertTrue(pool.current() < 1000, "running drains Negative Speed Force");

        engine.tick(IDLE, pool, PowersSettings.DEFAULTS);
        assertEquals(9D, engine.currentMultiplier(), 1e-9, "deceleration without input");

        engine.setSpeedLevel(1);
        assertEquals(2D, engine.targetMultiplier(PowersSettings.DEFAULTS), 1e-9);
        assertEquals(3, engine.adjustSpeedLevel(2));
        assertEquals(5, engine.adjustSpeedLevel(99), "clamped to profile levels");
    }

    @Test
    void serverMaxSpeedMultiplierCapsTopSpeed() {
        PowersSettings half = new PowersSettings(true, 0.35, 1, 0.6, true, 48, 64, true, 1, 0.5, 1, 1, true, 8, true, true, 3, 2);
        SpeedsterEngine engine = new SpeedsterEngine(PROFILE);
        engine.setActive(true);
        engine.setSpeedLevel(5);
        assertEquals(5D, engine.targetMultiplier(half), 1e-9);
    }

    @Test
    void runningOutOfEnergyStops() {
        SpeedsterEngine engine = new SpeedsterEngine(PROFILE);
        EnergyPool pool = new EnergyPool(EnergyType.NEGATIVE_SPEED_FORCE, 2, 0, 0);
        engine.setActive(true);
        engine.setSpeedLevel(5);
        boolean stopped = false;
        for (int i = 0; i < 50 && !stopped; i++) {
            stopped = engine.tick(RUNNING, pool, PowersSettings.DEFAULTS).events().contains(SpeedsterEvent.OUT_OF_ENERGY);
        }
        assertTrue(stopped);
        assertFalse(engine.isActive());
    }

    @Test
    void wallImpactVersusWallRun() {
        SpeedsterEngine engine = new SpeedsterEngine(PROFILE);
        EnergyPool pool = energy();
        engine.setActive(true);
        engine.setSpeedLevel(5);
        for (int i = 0; i < 20; i++) {
            engine.tick(RUNNING, pool, PowersSettings.DEFAULTS);
        }
        SpeedsterEngine.SpeedsterTick impact = engine.tick(new SpeedsterInput(true, true, true, false, false, true), pool, PowersSettings.DEFAULTS);
        assertTrue(impact.events().contains(SpeedsterEvent.WALL_IMPACT));
        assertTrue(engine.currentMultiplier() < 4D, "impact kills momentum");

        for (int i = 0; i < 20; i++) {
            engine.tick(RUNNING, pool, PowersSettings.DEFAULTS);
        }
        SpeedsterEngine.SpeedsterTick climb = engine.tick(new SpeedsterInput(true, true, true, false, true, true), pool, PowersSettings.DEFAULTS);
        assertTrue(climb.wallRunning());
        assertTrue(climb.events().contains(SpeedsterEvent.WALL_RUN_START));
        assertTrue(engine.tick(RUNNING, pool, PowersSettings.DEFAULTS).events().contains(SpeedsterEvent.WALL_RUN_END));
    }

    @Test
    void waterRunningNeedsSpeed() {
        SpeedsterEngine engine = new SpeedsterEngine(PROFILE);
        EnergyPool pool = energy();
        engine.setActive(true);
        engine.setSpeedLevel(5);
        SpeedsterInput onWater = new SpeedsterInput(true, false, false, true, true, true);
        assertFalse(engine.tick(onWater, pool, PowersSettings.DEFAULTS).waterRunning(), "1.5x is too slow");
        for (int i = 0; i < 10; i++) {
            engine.tick(RUNNING, pool, PowersSettings.DEFAULTS);
        }
        assertTrue(engine.tick(onWater, pool, PowersSettings.DEFAULTS).waterRunning());
    }

    @Test
    void steeringAndValidation() {
        SpeedsterEngine engine = new SpeedsterEngine(PROFILE);
        EnergyPool pool = energy();
        assertEquals(1D, engine.turnFactor(PowersSettings.DEFAULTS), 1e-9);
        engine.setActive(true);
        engine.setSpeedLevel(5);
        for (int i = 0; i < 40; i++) {
            engine.tick(RUNNING, pool, PowersSettings.DEFAULTS);
        }
        assertEquals(0.25D, engine.turnFactor(PowersSettings.DEFAULTS), 1e-9);
        assertTrue(engine.collisionDamage() > 0F);

        double base = 0.28D;
        assertTrue(engine.validateMovement(base * 10, base));
        for (int i = 0; i < 4; i++) {
            engine.validateMovement(base * 100, base);
        }
        assertTrue(engine.isFlagged(), "repeated impossible movement is flagged");
        engine.resetViolations();
        assertFalse(engine.isFlagged());
    }

    @Test
    void persistsActivationAndLevel() {
        SpeedsterEngine engine = new SpeedsterEngine(PROFILE);
        engine.setActive(true);
        engine.setSpeedLevel(4);
        MapDataNode node = new MapDataNode();
        engine.write(node);
        SpeedsterEngine copy = new SpeedsterEngine(PROFILE);
        copy.read(node);
        assertTrue(copy.isActive());
        assertEquals(4, copy.speedLevel());
        assertEquals(1D, copy.currentMultiplier(), 1e-9, "speed is rebuilt after loading, never restored instantly");
    }
}
