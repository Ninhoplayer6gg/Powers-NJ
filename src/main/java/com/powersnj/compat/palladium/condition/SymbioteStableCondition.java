package com.powersnj.compat.palladium.condition;

import com.google.gson.JsonObject;
import com.powersnj.compat.palladium.PalladiumCompat;
import com.powersnj.symbiote.SymbioteEvents;
import net.threetag.palladium.condition.Condition;
import net.threetag.palladium.condition.ConditionSerializer;
import net.threetag.palladium.util.context.DataContext;

/**
 * {@code powersnj:symbiote_stable} - false while the symbiote is destabilised by heat or sound.
 */
public class SymbioteStableCondition extends Condition {

    @Override
    public boolean active(DataContext context) {
        return context.getLivingEntity() != null && !SymbioteEvents.isDestabilized(context.getLivingEntity());
    }

    @Override
    public ConditionSerializer getSerializer() {
        return PalladiumCompat.SYMBIOTE_STABLE.get();
    }

    public static class Serializer extends ConditionSerializer {

        @Override
        public Condition make(JsonObject json) {
            return new SymbioteStableCondition();
        }

        @Override
        public String getDocumentationDescription() {
            return "Checks that the symbiote is not destabilised.";
        }
    }
}
