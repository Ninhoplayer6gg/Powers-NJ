package com.powersnj.compat.palladium.ability;

import com.powersnj.core.config.Settings;
import com.powersnj.core.phasing.PhasePlan;
import com.powersnj.phasing.PhasingService;
import com.powersnj.player.PowersPlayerData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

/**
 * {@code powersnj:phase} - vibrate through the wall in front: server-planned, verified safe exit,
 * energy + cooldown through the gate, disabled by {@code enablePhasing=false}. Combine with the
 * Palladium {@code palladium:vibrate} ability for the visual.
 */
public class PhaseAbility extends PowersActionAbility {

    public static final PalladiumProperty<Integer> DISTANCE = new IntegerProperty("distance").configurable("Maximum phase distance (capped by the server maxPhaseDistance).");

    public PhaseAbility() {
        this.withProperty(DISTANCE, 6);
    }

    @Override
    public boolean isDisabledByConfig() {
        return !Settings.get().enablePhasing();
    }

    @Override
    public boolean onActivated(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        PhasePlan plan = PhasingService.plan(player, instance.getProperty(DISTANCE));
        if (!plan.success()) {
            player.displayClientMessage(Component.translatable(plan.outcome().translationKey()).withStyle(ChatFormatting.RED), true);
            return false;
        }
        return PhasingService.execute(player, plan);
    }

    @Override
    public String getDocumentationDescription() {
        return "Powers NJ: safe server-authoritative phasing through walls.";
    }
}
