package com.powersnj.compat.palladium.condition;

import com.google.gson.JsonObject;
import com.powersnj.compat.palladium.PalladiumCompat;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.speedster.SpeedsterMovementController;
import net.minecraft.world.entity.player.Player;
import net.threetag.palladium.condition.Condition;
import net.threetag.palladium.condition.ConditionSerializer;
import net.threetag.palladium.util.context.DataContext;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

/**
 * {@code powersnj:speedster_speed} - true while super speed is on and the current multiplier is at
 * least {@code min_multiplier}. Drives speed-dependent visuals/abilities (trails, vibration...).
 */
public class SpeedsterSpeedCondition extends Condition {

    private final float minMultiplier;

    public SpeedsterSpeedCondition(float minMultiplier) {
        this.minMultiplier = minMultiplier;
    }

    @Override
    public boolean active(DataContext context) {
        if (!(context.getLivingEntity() instanceof Player player)) {
            return false;
        }
        return PowersPlayerData.get(player).map(data -> data.movement() instanceof SpeedsterMovementController speedster
                && speedster.engine().isActive() && speedster.engine().currentMultiplier() >= this.minMultiplier).orElse(false);
    }

    @Override
    public ConditionSerializer getSerializer() {
        return PalladiumCompat.SPEEDSTER_SPEED.get();
    }

    public static class Serializer extends ConditionSerializer {

        public static final PalladiumProperty<Float> MIN_MULTIPLIER = new FloatProperty("min_multiplier").configurable("Minimum speed multiplier.");

        public Serializer() {
            this.withProperty(MIN_MULTIPLIER, 1F);
        }

        @Override
        public Condition make(JsonObject json) {
            return new SpeedsterSpeedCondition(this.getProperty(json, MIN_MULTIPLIER));
        }

        @Override
        public String getDocumentationDescription() {
            return "Checks the speedster engine speed.";
        }
    }
}
