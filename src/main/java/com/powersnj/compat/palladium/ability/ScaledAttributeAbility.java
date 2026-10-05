package com.powersnj.compat.palladium.ability;

import com.powersnj.movement.AttributeHelper;
import com.powersnj.player.PowersPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.AttributeProperty;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.UUIDProperty;

import java.util.UUID;

/**
 * {@code powersnj:scaled_attribute} - attribute modifier that grows with the suit level
 * (linear from {@code amount_at_level_1} to {@code amount_at_max_level}). This is how passives such
 * as super strength and enhanced durability progress with the suit.
 */
public class ScaledAttributeAbility extends PowersAbility {

    public static final PalladiumProperty<Attribute> ATTRIBUTE = new AttributeProperty("attribute").configurable("Attribute to modify.");
    public static final PalladiumProperty<Float> AMOUNT_MIN = new FloatProperty("amount_at_level_1").configurable("Modifier amount at suit level 1.");
    public static final PalladiumProperty<Float> AMOUNT_MAX = new FloatProperty("amount_at_max_level").configurable("Modifier amount at the suit's max level.");
    public static final PalladiumProperty<Integer> OPERATION = new IntegerProperty("operation").configurable("0 = add, 1 = multiply base, 2 = multiply total.");
    public static final PalladiumProperty<UUID> UUID_PROPERTY = new UUIDProperty("uuid").configurable("Unique modifier UUID.");

    public ScaledAttributeAbility() {
        this.withProperty(ATTRIBUTE, Attributes.ATTACK_DAMAGE);
        this.withProperty(AMOUNT_MIN, 1F);
        this.withProperty(AMOUNT_MAX, 4F);
        this.withProperty(OPERATION, 0);
        this.withProperty(UUID_PROPERTY, UUID.fromString("5f0e0b8e-6b77-4d0f-9a4b-3f2f51f0c000"));
        this.withProperty(XP_REWARD, 0F);
        this.withProperty(HIDDEN_IN_BAR, true);
    }

    private static double amount(AbilityInstance instance, PowersPlayerData data) {
        int max = data.activeDefinition(false).map(def -> def.maxLevel()).orElse(20);
        float t = max <= 1 ? 1F : (Math.max(1, Math.min(max, data.activeLevel())) - 1) / (float) (max - 1);
        return instance.getProperty(AMOUNT_MIN) + (instance.getProperty(AMOUNT_MAX) - instance.getProperty(AMOUNT_MIN)) * t;
    }

    private static AttributeModifier.Operation operation(AbilityInstance instance) {
        int op = instance.getProperty(OPERATION);
        return AttributeModifier.Operation.fromValue(Math.max(0, Math.min(2, op)));
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public boolean onActivated(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        this.whileEnabled(player, instance, data);
        return true;
    }

    @Override
    protected void whileEnabled(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        Attribute attribute = instance.getProperty(ATTRIBUTE);
        if (attribute == null) {
            return;
        }
        AttributeInstance attributeInstance = player.getAttribute(attribute);
        if (attributeInstance == null) {
            return;
        }
        boolean missing = attributeInstance.getModifier(instance.getProperty(UUID_PROPERTY)) == null;
        if (missing || player.tickCount % 20 == 0) {
            AttributeHelper.set(player, attribute, instance.getProperty(UUID_PROPERTY), "powersnj scaled attribute", amount(instance, data), operation(instance), 0.001D);
        }
    }

    @Override
    protected void onDisabled(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        Attribute attribute = instance.getProperty(ATTRIBUTE);
        if (attribute != null) {
            AttributeHelper.remove(player, attribute, instance.getProperty(UUID_PROPERTY));
        }
    }

    @Override
    public String getDocumentationDescription() {
        return "Powers NJ: attribute modifier scaled by the suit level.";
    }
}
