package com.powersnj.compat.palladium.condition;

import com.google.gson.JsonObject;
import com.powersnj.compat.palladium.PalladiumCompat;
import com.powersnj.player.PowersPlayerData;
import net.minecraft.world.entity.player.Player;
import net.threetag.palladium.condition.Condition;
import net.threetag.palladium.condition.ConditionSerializer;
import net.threetag.palladium.util.context.DataContext;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

/**
 * {@code powersnj:suit_level} - true when the active suit level is at least {@code min_level}.
 */
public class SuitLevelCondition extends Condition {

    private final int minLevel;

    public SuitLevelCondition(int minLevel) {
        this.minLevel = minLevel;
    }

    @Override
    public boolean active(DataContext context) {
        if (!(context.getLivingEntity() instanceof Player player)) {
            return false;
        }
        return PowersPlayerData.get(player).map(data -> data.hasActiveSuit() && data.activeLevel() >= this.minLevel).orElse(false);
    }

    @Override
    public ConditionSerializer getSerializer() {
        return PalladiumCompat.SUIT_LEVEL.get();
    }

    public static class Serializer extends ConditionSerializer {

        public static final PalladiumProperty<Integer> MIN_LEVEL = new IntegerProperty("min_level").configurable("Minimum suit level.");

        public Serializer() {
            this.withProperty(MIN_LEVEL, 1);
        }

        @Override
        public Condition make(JsonObject json) {
            return new SuitLevelCondition(this.getProperty(json, MIN_LEVEL));
        }

        @Override
        public String getDocumentationDescription() {
            return "Checks the level of the worn Powers NJ suit.";
        }
    }
}
