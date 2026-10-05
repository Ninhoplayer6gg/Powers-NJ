package com.powersnj.core.energy;

import com.powersnj.core.data.MapDataNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnergyTest {

    @Test
    void builtInTypesAreRegistered() {
        assertTrue(EnergyType.byId("powersnj:viltrumite_stamina").isPresent());
        assertTrue(EnergyType.byId("powersnj:biomass").isPresent());
        assertTrue(EnergyType.byId("powersnj:negative_speed_force").isPresent());
        assertTrue(EnergyType.VILTRUMITE_STAMINA.isStamina());
        assertFalse(EnergyType.BIOMASS.isStamina());
        assertEquals("energy.powersnj.biomass", EnergyType.BIOMASS.translationKey());
        assertSame(EnergyType.BIOMASS, EnergyType.register("powersnj:biomass", 0, true), "re-registering returns the original");
        assertThrows(IllegalArgumentException.class, () -> new EnergyType("no_namespace", 0, false));
    }

    @Test
    void consumeOnlyWhenAvailable() {
        EnergyPool pool = new EnergyPool(EnergyType.BIOMASS, 100, 1, 0);
        assertTrue(pool.tryConsume(60));
        assertEquals(40, pool.current(), 1e-6);
        assertFalse(pool.tryConsume(41));
        assertEquals(40, pool.current(), 1e-6, "failed consumption must not change the pool");
        assertEquals(40, pool.drain(100), 1e-6, "drain clamps at zero");
        assertEquals(0, pool.current(), 1e-6);
    }

    @Test
    void regenerationRespectsDelayAndMultiplier() {
        EnergyPool pool = new EnergyPool(EnergyType.VILTRUMITE_STAMINA, 100, 2, 3);
        pool.tryConsume(50);
        assertFalse(pool.tick(1));
        assertFalse(pool.tick(1));
        assertFalse(pool.tick(1));
        assertTrue(pool.tick(1));
        assertEquals(52, pool.current(), 1e-6);
        assertTrue(pool.tick(2.5));
        assertEquals(57, pool.current(), 1e-6);
        assertFalse(pool.tick(0), "multiplier 0 disables regeneration");
        pool.set(99.5F);
        pool.tick(1);
        assertEquals(100, pool.current(), 1e-6, "regeneration caps at max");
        assertFalse(pool.tick(1));
    }

    @Test
    void syncThresholdAvoidsSpam() {
        EnergyPool pool = new EnergyPool(EnergyType.BIOMASS, 100, 0.1F, 0);
        assertTrue(pool.needsSync(), "never synced yet");
        pool.markSynced();
        pool.drain(0.5F);
        assertFalse(pool.needsSync(), "sub-threshold change is not sent");
        pool.drain(0.6F);
        assertTrue(pool.needsSync());
        pool.markSynced();
        pool.drain(200);
        assertTrue(pool.needsSync(), "reaching zero is always sent");
    }

    @Test
    void specScalesWithLevel() {
        EnergySpec spec = new EnergySpec(EnergyType.NEGATIVE_SPEED_FORCE, 100, 10, 0.5F, 0.05F, 20);
        assertEquals(100, spec.maxAt(1), 1e-6);
        assertEquals(290, spec.maxAt(20), 1e-6);
        assertEquals(0.5F + 0.05F * 19, spec.regenAt(20), 1e-6);
        assertThrows(IllegalArgumentException.class, () -> new EnergySpec(EnergyType.BIOMASS, 0, 0, 0, 0, 0));
    }

    @Test
    void bankReconfiguresAndPersists() {
        EnergySpec spec = new EnergySpec(EnergyType.BIOMASS, 100, 10, 0.5F, 0, 20);
        EnergyBank bank = new EnergyBank();
        EnergyPool pool = bank.configure(spec, 1);
        pool.tryConsume(30);
        bank.configure(spec, 5);
        assertEquals(140, pool.max(), 1e-6);
        assertEquals(70, pool.current(), 1e-6, "level up keeps the absolute amount");

        MapDataNode node = new MapDataNode();
        bank.write(node);
        EnergyBank restored = new EnergyBank();
        restored.read(node);
        EnergyPool copy = restored.get(EnergyType.BIOMASS).orElseThrow();
        assertEquals(70, copy.current(), 1e-6);
        assertEquals(140, copy.max(), 1e-6);
        assertTrue(copy.needsSync(), "loaded pools are always re-sent");
    }
}
