package com.powersnj.core.fabrication;

import com.powersnj.core.fabrication.FabricationMatcher.SlotContent;
import com.powersnj.core.fabrication.FabricationMatcher.SlotTake;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FabricationTest {

    static final FabricationSpec THRAGG = new FabricationSpec("powersnj:suit_forge/thragg", "powersnj:viltrumite_blueprint", "powersnj:power_core",
            List.of(new MaterialRequirement("powersnj:viltrumite_alloy", 8), new MaterialRequirement("powersnj:reinforced_viltrumite_fabric", 6)),
            "powersnj:thragg", 200, false);

    @Test
    void reportsAvailabilityPerMaterial() {
        List<SlotContent> slots = Arrays.asList(new SlotContent("powersnj:viltrumite_alloy", 5), null,
                new SlotContent("powersnj:viltrumite_alloy", 5), new SlotContent("powersnj:reinforced_viltrumite_fabric", 2), null, null);
        FabricationCheck check = FabricationMatcher.check(THRAGG, "powersnj:viltrumite_blueprint", "powersnj:power_core", slots);
        assertTrue(check.blueprintMatches());
        assertTrue(check.powerCorePresent());
        assertEquals(10, check.materials().get(0).available());
        assertTrue(check.materials().get(0).satisfied());
        assertFalse(check.materials().get(1).satisfied());
        assertFalse(check.canFabricate());
    }

    @Test
    void blueprintIsMandatory() {
        List<SlotContent> slots = List.of(new SlotContent("powersnj:viltrumite_alloy", 64), new SlotContent("powersnj:reinforced_viltrumite_fabric", 64));
        assertFalse(FabricationMatcher.check(THRAGG, null, "powersnj:power_core", slots).canFabricate());
        assertFalse(FabricationMatcher.check(THRAGG, "powersnj:symbiote_blueprint", "powersnj:power_core", slots).canFabricate());
        assertFalse(FabricationMatcher.check(THRAGG, "powersnj:viltrumite_blueprint", null, slots).canFabricate(), "power core is mandatory");
        assertTrue(FabricationMatcher.check(THRAGG, "powersnj:viltrumite_blueprint", "powersnj:power_core", slots).canFabricate());
    }

    @Test
    void consumptionPlanSpansSlotsAndNeverOverdraws() {
        List<SlotContent> slots = Arrays.asList(new SlotContent("powersnj:viltrumite_alloy", 5), new SlotContent("powersnj:reinforced_viltrumite_fabric", 6),
                new SlotContent("powersnj:viltrumite_alloy", 5));
        List<SlotTake> plan = FabricationMatcher.planConsumption(THRAGG, slots).orElseThrow();
        assertEquals(List.of(new SlotTake(0, 5), new SlotTake(2, 3), new SlotTake(1, 6)), plan);

        List<SlotContent> missing = List.of(new SlotContent("powersnj:viltrumite_alloy", 7), new SlotContent("powersnj:reinforced_viltrumite_fabric", 6));
        assertTrue(FabricationMatcher.planConsumption(THRAGG, missing).isEmpty(), "partial recipes are never consumed");
    }

    @Test
    void findsRecipeByBlueprint() {
        FabricationSpec venom = new FabricationSpec("powersnj:suit_forge/venom", "powersnj:symbiote_blueprint", "powersnj:power_core",
                List.of(new MaterialRequirement("powersnj:symbiotic_fiber", 8)), "powersnj:venom", 200, false);
        assertEquals(venom, FabricationMatcher.findByBlueprint(List.of(THRAGG, venom), "powersnj:symbiote_blueprint").orElseThrow());
        assertTrue(FabricationMatcher.findByBlueprint(List.of(THRAGG, venom), "powersnj:speedster_blueprint").isEmpty());
        assertTrue(FabricationMatcher.findByBlueprint(List.of(THRAGG, venom), null).isEmpty());
    }

    @Test
    void validatesSpecs() {
        assertThrows(IllegalArgumentException.class, () -> new FabricationSpec("a", "b", "c", List.of(), "d", 10, false));
        assertThrows(IllegalArgumentException.class, () -> new FabricationSpec("a", "b", "c",
                List.of(new MaterialRequirement("x", 1), new MaterialRequirement("x", 2)), "d", 10, false));
        assertThrows(IllegalArgumentException.class, () -> new MaterialRequirement("x", 0));
        assertThrows(IllegalArgumentException.class, () -> new FabricationSpec("a", "b", "c", List.of(new MaterialRequirement("x", 1)), "d", 0, false));
    }
}
