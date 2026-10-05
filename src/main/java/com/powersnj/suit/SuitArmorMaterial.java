package com.powersnj.suit;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Armor stats of a suit. Super-powered durability/strength comes from the Palladium power
 * (attribute modifiers), so these values only define the physical suit.
 */
public final class SuitArmorMaterial implements ArmorMaterial {

    private static final Map<ArmorItem.Type, Integer> BASE_DURABILITY = new EnumMap<>(Map.of(
            ArmorItem.Type.HELMET, 11,
            ArmorItem.Type.CHESTPLATE, 16,
            ArmorItem.Type.LEGGINGS, 15,
            ArmorItem.Type.BOOTS, 13));

    private final String name;
    private final int durabilityMultiplier;
    private final Map<ArmorItem.Type, Integer> defense;
    private final int enchantability;
    private final Supplier<SoundEvent> equipSound;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repairIngredient;

    public SuitArmorMaterial(String name, int durabilityMultiplier, int helmet, int chest, int legs, int boots, int enchantability,
                             Supplier<SoundEvent> equipSound, float toughness, float knockbackResistance, Supplier<Ingredient> repairIngredient) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.defense = new EnumMap<>(Map.of(
                ArmorItem.Type.HELMET, helmet,
                ArmorItem.Type.CHESTPLATE, chest,
                ArmorItem.Type.LEGGINGS, legs,
                ArmorItem.Type.BOOTS, boots));
        this.enchantability = enchantability;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = repairIngredient;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return BASE_DURABILITY.get(type) * this.durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return this.defense.get(type);
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantability;
    }

    @Override
    public SoundEvent getEquipSound() {
        return this.equipSound.get();
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public float getToughness() {
        return this.toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return this.knockbackResistance;
    }
}
