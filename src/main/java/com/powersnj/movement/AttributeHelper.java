package com.powersnj.movement;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

/**
 * Transient attribute modifiers driven by power engines. Changes below {@code epsilon} are skipped
 * so syncable attributes are not re-sent to clients every tick.
 */
public final class AttributeHelper {

    private AttributeHelper() {
    }

    /**
     * @return true when the modifier changed
     */
    public static boolean set(LivingEntity entity, Attribute attribute, UUID id, String name, double amount, AttributeModifier.Operation operation, double epsilon) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return false;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (Math.abs(amount) < 1.0E-6) {
            if (existing != null) {
                instance.removeModifier(id);
                return true;
            }
            return false;
        }
        if (existing != null) {
            if (existing.getOperation() == operation && Math.abs(existing.getAmount() - amount) < epsilon) {
                return false;
            }
            instance.removeModifier(id);
        }
        instance.addTransientModifier(new AttributeModifier(id, name, amount, operation));
        return true;
    }

    public static void remove(LivingEntity entity, Attribute attribute, UUID id) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null && instance.getModifier(id) != null) {
            instance.removeModifier(id);
        }
    }
}
