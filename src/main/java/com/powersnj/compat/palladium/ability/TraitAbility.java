package com.powersnj.compat.palladium.ability;

import com.powersnj.player.PowersPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.StringProperty;

/**
 * {@code powersnj:trait} - grants a named trait while enabled. Movement controllers and systems
 * read traits ({@code wall_running}, {@code water_running}, {@code wall_crawling}...), so complex
 * mechanics are switched on purely from data: unlock with {@code powersnj:skill_unlocked}, toggle
 * with {@code palladium:toggle}, keep alive with {@code powersnj:has_energy}.
 */
public class TraitAbility extends PowersAbility {

    public static final PalladiumProperty<String> TRAIT = new StringProperty("trait").configurable("Trait granted while enabled (e.g. wall_running, water_running, wall_crawling).");

    public TraitAbility() {
        this.withProperty(TRAIT, "wall_running");
        this.withProperty(XP_REWARD, 0F);
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public boolean onActivated(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        data.traits().add(instance.getProperty(TRAIT));
        return true;
    }

    @Override
    protected void whileEnabled(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        data.traits().add(instance.getProperty(TRAIT));
    }

    @Override
    public String getDocumentationDescription() {
        return "Powers NJ: grants a movement/system trait while enabled.";
    }
}
