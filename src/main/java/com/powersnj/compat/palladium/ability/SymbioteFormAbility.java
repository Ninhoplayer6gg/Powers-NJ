package com.powersnj.compat.palladium.ability;

import com.powersnj.movement.AttributeHelper;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.StringProperty;

import java.util.UUID;

/**
 * {@code powersnj:symbiote_form} - partial body transformation (claws, blade arm, mask...). Sets the
 * synced visual form (rendered by {@code SymbioteFormLayer} once the form textures/models exist)
 * and applies the form's combat bonuses while enabled.
 */
public class SymbioteFormAbility extends PowersAbility {

    public static final PalladiumProperty<String> FORM = new StringProperty("form").configurable("Form id (claws, blade_arm, mask...). Asset: textures/suits/venom/forms/<form>.png");
    public static final PalladiumProperty<Float> DAMAGE_BONUS = new FloatProperty("attack_damage_bonus").configurable("Extra attack damage while the form is active.");
    public static final PalladiumProperty<Float> REACH_BONUS = new FloatProperty("reach_bonus").configurable("Extra entity reach while the form is active.");

    private static final UUID DAMAGE_ID = UUID.fromString("5f0e0b8e-6b77-4d0f-9a4b-3f2f51f0e001");
    private static final UUID REACH_ID = UUID.fromString("5f0e0b8e-6b77-4d0f-9a4b-3f2f51f0e002");

    public SymbioteFormAbility() {
        this.withProperty(FORM, "claws");
        this.withProperty(DAMAGE_BONUS, 3F);
        this.withProperty(REACH_BONUS, 0F);
    }

    @Override
    public boolean onActivated(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        data.setForm(instance.getProperty(FORM));
        AttributeHelper.set(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID, "powersnj symbiote form", instance.getProperty(DAMAGE_BONUS), AttributeModifier.Operation.ADDITION, 0.01D);
        AttributeHelper.set(player, ForgeMod.ENTITY_REACH.get(), REACH_ID, "powersnj symbiote form", instance.getProperty(REACH_BONUS), AttributeModifier.Operation.ADDITION, 0.01D);
        player.level().playSound(null, player.blockPosition(), ModSounds.SYMBIOTE_HISS.get(), SoundSource.PLAYERS, 0.7F, 1.3F);
        return true;
    }

    @Override
    protected void whileEnabled(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        data.setForm(instance.getProperty(FORM));
    }

    @Override
    protected void onDisabled(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        if (instance.getProperty(FORM).equals(data.form())) {
            data.setForm("");
        }
        AttributeHelper.remove(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID);
        AttributeHelper.remove(player, ForgeMod.ENTITY_REACH.get(), REACH_ID);
    }

    @Override
    public String getDocumentationDescription() {
        return "Powers NJ: symbiote partial transformation.";
    }
}
