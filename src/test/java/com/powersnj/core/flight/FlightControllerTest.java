package com.powersnj.core.flight;

import com.powersnj.core.energy.EnergyPool;
import com.powersnj.core.energy.EnergyType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlightControllerTest {

    static final FlightProfile PROFILE = new FlightProfile(1D, 3D, 4D, 0.5D, 1D, 4, 2F, 1D, 5D);

    @Test
    void cruiseIsAvailableImmediately() {
        FlightController controller = new FlightController(PROFILE);
        FlightController.FlightTick tick = controller.tick(true, null, 1D);
        assertEquals(FlightController.Mode.CRUISE, tick.mode());
        assertEquals(1D, tick.attributeValue(), 1e-9);
    }

    @Test
    void boostAcceleratesThenReturnsToCruise() {
        FlightController controller = new FlightController(PROFILE);
        controller.tick(true, null, 1D);
        assertTrue(controller.requestBoost());
        assertEquals(1.5D, controller.tick(true, null, 1D).attributeValue(), 1e-9);
        assertEquals(2.0D, controller.tick(true, null, 1D).attributeValue(), 1e-9);
        controller.tick(true, null, 1D);
        controller.tick(true, null, 1D);
        FlightController.FlightTick after = controller.tick(true, null, 1D);
        assertEquals(FlightController.Mode.CRUISE, after.mode());
        assertTrue(after.attributeValue() < 3D, "decelerates back to cruise");
    }

    @Test
    void highSpeedDrainsAndCancelsWithoutEnergy() {
        FlightController controller = new FlightController(PROFILE);
        EnergyPool pool = new EnergyPool(EnergyType.VILTRUMITE_STAMINA, 5, 0, 0);
        controller.setHighSpeed(true);
        FlightController.FlightTick first = controller.tick(true, pool, 1D);
        assertEquals(FlightController.Mode.HIGH_SPEED, first.mode());
        assertEquals(2F, first.energyDrained(), 1e-6);
        controller.tick(true, pool, 1D);
        FlightController.FlightTick cancelled = controller.tick(true, pool, 1D);
        assertTrue(cancelled.highSpeedCancelled());
        assertEquals(FlightController.Mode.CRUISE, cancelled.mode());
        assertFalse(controller.isHighSpeedRequested());
    }

    @Test
    void noDrainWhileGrounded() {
        FlightController controller = new FlightController(PROFILE);
        EnergyPool pool = new EnergyPool(EnergyType.VILTRUMITE_STAMINA, 5, 0, 0);
        controller.setHighSpeed(true);
        assertEquals(0F, controller.tick(false, pool, 1D).energyDrained(), 1e-6);
    }

    @Test
    void disabledAndMultiplier() {
        FlightController controller = new FlightController(PROFILE);
        controller.setEnabled(false);
        assertFalse(controller.requestBoost());
        assertEquals(0D, controller.tick(true, null, 1D).attributeValue(), 1e-9);
        controller.setEnabled(true);
        assertEquals(2D, controller.tick(true, null, 2D).attributeValue(), 1e-9, "maxSpeedMultiplier scales flight");
        assertEquals(0F, controller.impactDamage(0.5D), 1e-6);
        assertTrue(controller.impactDamage(3D) > controller.impactDamage(1.5D));
    }
}
