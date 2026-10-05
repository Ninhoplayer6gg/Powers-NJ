package com.powersnj.compat.palladium.condition;

import com.google.gson.JsonObject;
import com.powersnj.compat.palladium.PalladiumCompat;
import com.powersnj.player.PowersPlayerData;
import net.minecraft.world.entity.player.Player;
import net.threetag.palladium.condition.Condition;
import net.threetag.palladium.condition.ConditionSerializer;
import net.threetag.palladium.util.context.DataContext;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.StringProperty;

/**
 * {@code powersnj:skill_unlocked} - true when the skill is unlocked in the skill tree of the active
 * suit. Used as an unlocking condition, it links the Powers NJ skill tree to Palladium's power
 * screen and ability bar (locked abilities show as locked).
 */
public class SkillUnlockedCondition extends Condition {

    private final String skill;

    public SkillUnlockedCondition(String skill) {
        this.skill = skill;
    }

    @Override
    public boolean active(DataContext context) {
        if (!(context.getLivingEntity() instanceof Player player)) {
            return false;
        }
        return PowersPlayerData.get(player).map(data -> data.hasActiveSuit() && data.ledger().isSkillUnlocked(data.activeSuit(), this.skill)).orElse(false);
    }

    @Override
    public ConditionSerializer getSerializer() {
        return PalladiumCompat.SKILL_UNLOCKED.get();
    }

    public static class Serializer extends ConditionSerializer {

        public static final PalladiumProperty<String> SKILL = new StringProperty("skill").configurable("Skill id from the suit skill tree.");

        public Serializer() {
            this.withProperty(SKILL, "");
        }

        @Override
        public Condition make(JsonObject json) {
            return new SkillUnlockedCondition(this.getProperty(json, SKILL));
        }

        @Override
        public String getDocumentationDescription() {
            return "Checks if a skill of the worn Powers NJ suit is unlocked.";
        }
    }
}
