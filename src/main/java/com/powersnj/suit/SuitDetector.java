package com.powersnj.suit;

import com.powersnj.core.suit.SuitDefinitions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Detects which suit (if any) an entity wears. A suit is active only with all four pieces of the
 * same {@link SuitKind}, mirroring the Palladium suit set that grants the power.
 */
public final class SuitDetector {

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private SuitDetector() {
    }

    /**
     * @return the worn suit kind, or null
     */
    public static SuitKind wornKind(LivingEntity entity) {
        SuitKind kind = null;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!(stack.getItem() instanceof SuitArmorItem piece)) {
                return null;
            }
            if (kind == null) {
                kind = piece.kind();
            } else if (kind != piece.kind()) {
                return null;
            }
        }
        return kind;
    }

    /**
     * @return the active suit id (full set worn and definition loaded), or {@code ""}
     */
    public static String activeSuitId(LivingEntity entity, boolean clientSide) {
        SuitKind kind = wornKind(entity);
        if (kind == null) {
            return "";
        }
        String id = kind.suitId();
        return SuitDefinitions.side(clientSide).get(id).isPresent() ? id : "";
    }
}
