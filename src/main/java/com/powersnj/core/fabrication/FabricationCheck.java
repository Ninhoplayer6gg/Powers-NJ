package com.powersnj.core.fabrication;

import java.util.List;

/**
 * Availability report shown by the Suit Forge screen and re-validated by the server.
 *
 * @param blueprintMatches the blueprint slot holds this recipe's blueprint
 * @param powerCorePresent the core slot holds the required power core
 * @param materials        per-material availability
 */
public record FabricationCheck(boolean blueprintMatches, boolean powerCorePresent, List<Entry> materials) {

    public FabricationCheck {
        materials = List.copyOf(materials);
    }

    public boolean materialsSatisfied() {
        return this.materials.stream().allMatch(Entry::satisfied);
    }

    public boolean canFabricate() {
        return this.blueprintMatches && this.powerCorePresent && this.materialsSatisfied();
    }

    /**
     * @param requirement what is needed
     * @param available   how many are present in the material slots
     */
    public record Entry(MaterialRequirement requirement, int available) {

        public boolean satisfied() {
            return this.available >= this.requirement.count();
        }
    }
}
