package com.powersnj.compat.palladium.condition;

import com.google.gson.JsonObject;
import com.powersnj.compat.palladium.PalladiumCompat;
import com.powersnj.player.PowersPlayerData;
import net.minecraft.world.entity.player.Player;
import net.threetag.palladium.condition.Condition;
import net.threetag.palladium.condition.ConditionSerializer;
import net.threetag.palladium.util.context.DataContext;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

/**
 * {@code powersnj:has_energy} - true while the active suit resource holds at least {@code amount}.
 * As an enabling condition it turns toggles off automatically when the resource runs out.
 */
public class HasEnergyCondition extends Condition {

    private final float amount;

    public HasEnergyCondition(float amount) {
        this.amount = amount;
    }

    @Override
    public boolean active(DataContext context) {
        if (!(context.getLivingEntity() instanceof Player player)) {
            return false;
        }
        return PowersPlayerData.get(player).flatMap(data -> data.activeEnergy(false)).map(pool -> pool.current() >= this.amount).orElse(false);
    }

    @Override
    public ConditionSerializer getSerializer() {
        return PalladiumCompat.HAS_ENERGY.get();
    }

    public static class Serializer extends ConditionSerializer {

        public static final PalladiumProperty<Float> AMOUNT = new FloatProperty("amount").configurable("Minimum energy.");

        public Serializer() {
            this.withProperty(AMOUNT, 1F);
        }

        @Override
        public Condition make(JsonObject json) {
            return new HasEnergyCondition(this.getProperty(json, AMOUNT));
        }

        @Override
        public String getDocumentationDescription() {
            return "Checks the energy of the worn Powers NJ suit.";
        }
    }
}
