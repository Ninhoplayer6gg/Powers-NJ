package com.powersnj.compat.palladium.ability;

import com.powersnj.core.destruction.DestructionTier;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.StringProperty;

/**
 * One-shot ability (used with {@code palladium:action} / {@code palladium:ability_wheel}
 * enabling conditions). Never retried: one key press = one activation attempt.
 */
public abstract class PowersActionAbility extends PowersAbility {

    public static final PalladiumProperty<String> DESTRUCTION_POWER = new StringProperty("destruction_power")
            .configurable("Strongest block tier destroyed: none, fragile, normal, hard, extreme (obsidian class, also needs allowObsidianDestruction).");

    @Override
    protected boolean retriesWhileEnabled() {
        return false;
    }

    protected static DestructionTier destructionPower(AbilityInstance instance) {
        String value = instance.getProperty(DESTRUCTION_POWER);
        if (value == null || value.isEmpty() || value.equalsIgnoreCase("none")) {
            return null;
        }
        return DestructionTier.byName(value).filter(t -> t != DestructionTier.PROTECTED).orElse(null);
    }
}
