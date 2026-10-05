package com.powersnj.core.fabrication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Matching and consumption planning for the Suit Forge. Slots are described by
 * {@link SlotContent} (item id + count, {@code null} for an empty slot).
 */
public final class FabricationMatcher {

    private FabricationMatcher() {
    }

    public record SlotContent(String itemId, int count) {
    }

    /**
     * @param slot   material slot index
     * @param amount items to remove from it
     */
    public record SlotTake(int slot, int amount) {
    }

    /**
     * @return the recipe whose blueprint is in the blueprint slot
     */
    public static Optional<FabricationSpec> findByBlueprint(Collection<FabricationSpec> recipes, String blueprintItem) {
        if (blueprintItem == null) {
            return Optional.empty();
        }
        return recipes.stream().filter(r -> r.blueprint().equals(blueprintItem)).findFirst();
    }

    public static FabricationCheck check(FabricationSpec spec, String blueprintItem, String coreItem, List<SlotContent> materialSlots) {
        List<FabricationCheck.Entry> entries = new ArrayList<>();
        for (MaterialRequirement requirement : spec.materials()) {
            entries.add(new FabricationCheck.Entry(requirement, count(materialSlots, requirement.itemId())));
        }
        return new FabricationCheck(spec.blueprint().equals(blueprintItem), spec.powerCore().equals(coreItem), entries);
    }

    /**
     * Plans which material slots to decrement. Returns empty when the materials are insufficient,
     * so the caller can never consume a partial recipe.
     */
    public static Optional<List<SlotTake>> planConsumption(FabricationSpec spec, List<SlotContent> materialSlots) {
        int[] remainingInSlot = new int[materialSlots.size()];
        for (int i = 0; i < materialSlots.size(); i++) {
            SlotContent slot = materialSlots.get(i);
            remainingInSlot[i] = slot == null ? 0 : Math.max(0, slot.count());
        }
        List<SlotTake> takes = new ArrayList<>();
        for (MaterialRequirement requirement : spec.materials()) {
            int needed = requirement.count();
            for (int i = 0; i < materialSlots.size() && needed > 0; i++) {
                SlotContent slot = materialSlots.get(i);
                if (slot == null || !requirement.itemId().equals(slot.itemId()) || remainingInSlot[i] <= 0) {
                    continue;
                }
                int take = Math.min(needed, remainingInSlot[i]);
                remainingInSlot[i] -= take;
                needed -= take;
                takes.add(new SlotTake(i, take));
            }
            if (needed > 0) {
                return Optional.empty();
            }
        }
        return Optional.of(List.copyOf(takes));
    }

    private static int count(List<SlotContent> slots, String itemId) {
        int total = 0;
        for (SlotContent slot : slots) {
            if (slot != null && itemId.equals(slot.itemId())) {
                total += slot.count();
            }
        }
        return total;
    }
}
