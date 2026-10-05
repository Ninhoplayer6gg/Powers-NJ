package com.powersnj.compat.palladium;

import com.powersnj.PowersNJ;
import com.powersnj.compat.palladium.ability.CamouflageAbility;
import com.powersnj.compat.palladium.ability.ChargeAbility;
import com.powersnj.compat.palladium.ability.FlightBoostAbility;
import com.powersnj.compat.palladium.ability.GroundSlamAbility;
import com.powersnj.compat.palladium.ability.HeavyPunchAbility;
import com.powersnj.compat.palladium.ability.HighSpeedFlightAbility;
import com.powersnj.compat.palladium.ability.PhaseAbility;
import com.powersnj.compat.palladium.ability.RapidAttackAbility;
import com.powersnj.compat.palladium.ability.ScaledAttributeAbility;
import com.powersnj.compat.palladium.ability.ShockwaveAbility;
import com.powersnj.compat.palladium.ability.SpeedDashAbility;
import com.powersnj.compat.palladium.ability.SpeedLevelAbility;
import com.powersnj.compat.palladium.ability.SpeedPunchAbility;
import com.powersnj.compat.palladium.ability.SuperSpeedAbility;
import com.powersnj.compat.palladium.ability.SymbioteFormAbility;
import com.powersnj.compat.palladium.ability.SymbioteShieldAbility;
import com.powersnj.compat.palladium.ability.TendrilAbility;
import com.powersnj.compat.palladium.ability.TraitAbility;
import com.powersnj.compat.palladium.ability.VortexAbility;
import com.powersnj.compat.palladium.condition.HasEnergyCondition;
import com.powersnj.compat.palladium.condition.SkillUnlockedCondition;
import com.powersnj.compat.palladium.condition.SpeedsterSpeedCondition;
import com.powersnj.compat.palladium.condition.SuitLevelCondition;
import com.powersnj.compat.palladium.condition.SymbioteStableCondition;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.threetag.palladium.condition.ConditionSerializer;
import net.threetag.palladium.power.ability.Ability;

/**
 * Registers Powers NJ content into Palladium's public registries (ability types, condition
 * serializers, suit sets). Palladium itself is never modified: its registries are regular Forge
 * registries, so Forge {@link DeferredRegister}s keyed by
 * {@code PalladiumRegistry#getRegistryKey()} are the supported extension point.
 * <p>
 * Power sets themselves are data: {@code data/powersnj/palladium/powers/*.json} and
 * {@code data/powersnj/palladium/suit_set_powers/*.json}.
 */
public final class PalladiumCompat {

    public static final DeferredRegister<Ability> ABILITIES = DeferredRegister.create(Ability.REGISTRY.getRegistryKey(), PowersNJ.MOD_ID);
    public static final DeferredRegister<ConditionSerializer> CONDITIONS = DeferredRegister.create(ConditionSerializer.REGISTRY.getRegistryKey(), PowersNJ.MOD_ID);

    // Generic
    public static final RegistryObject<Ability> TRAIT = ABILITIES.register("trait", TraitAbility::new);
    public static final RegistryObject<Ability> SCALED_ATTRIBUTE = ABILITIES.register("scaled_attribute", ScaledAttributeAbility::new);
    // Viltrumite / flight
    public static final RegistryObject<Ability> GROUND_SLAM = ABILITIES.register("ground_slam", GroundSlamAbility::new);
    public static final RegistryObject<Ability> SHOCKWAVE = ABILITIES.register("shockwave", ShockwaveAbility::new);
    public static final RegistryObject<Ability> HEAVY_PUNCH = ABILITIES.register("heavy_punch", HeavyPunchAbility::new);
    public static final RegistryObject<Ability> CHARGE = ABILITIES.register("charge", ChargeAbility::new);
    public static final RegistryObject<Ability> FLIGHT_BOOST = ABILITIES.register("flight_boost", FlightBoostAbility::new);
    public static final RegistryObject<Ability> HIGH_SPEED_FLIGHT = ABILITIES.register("high_speed_flight", HighSpeedFlightAbility::new);
    // Symbiote
    public static final RegistryObject<Ability> TENDRIL = ABILITIES.register("tendril", TendrilAbility::new);
    public static final RegistryObject<Ability> SYMBIOTE_SHIELD = ABILITIES.register("symbiote_shield", SymbioteShieldAbility::new);
    public static final RegistryObject<Ability> SYMBIOTE_FORM = ABILITIES.register("symbiote_form", SymbioteFormAbility::new);
    public static final RegistryObject<Ability> CAMOUFLAGE = ABILITIES.register("camouflage", CamouflageAbility::new);
    // Speedster
    public static final RegistryObject<Ability> SUPER_SPEED = ABILITIES.register("super_speed", SuperSpeedAbility::new);
    public static final RegistryObject<Ability> SPEED_LEVEL = ABILITIES.register("speed_level", SpeedLevelAbility::new);
    public static final RegistryObject<Ability> SPEED_DASH = ABILITIES.register("speed_dash", SpeedDashAbility::new);
    public static final RegistryObject<Ability> SPEED_PUNCH = ABILITIES.register("speed_punch", SpeedPunchAbility::new);
    public static final RegistryObject<Ability> RAPID_ATTACK = ABILITIES.register("rapid_attack", RapidAttackAbility::new);
    public static final RegistryObject<Ability> PHASE = ABILITIES.register("phase", PhaseAbility::new);
    public static final RegistryObject<Ability> VORTEX = ABILITIES.register("vortex", VortexAbility::new);

    public static final RegistryObject<ConditionSerializer> SKILL_UNLOCKED = CONDITIONS.register("skill_unlocked", SkillUnlockedCondition.Serializer::new);
    public static final RegistryObject<ConditionSerializer> SUIT_LEVEL = CONDITIONS.register("suit_level", SuitLevelCondition.Serializer::new);
    public static final RegistryObject<ConditionSerializer> HAS_ENERGY = CONDITIONS.register("has_energy", HasEnergyCondition.Serializer::new);
    public static final RegistryObject<ConditionSerializer> SPEEDSTER_SPEED = CONDITIONS.register("speedster_speed", SpeedsterSpeedCondition.Serializer::new);
    public static final RegistryObject<ConditionSerializer> SYMBIOTE_STABLE = CONDITIONS.register("symbiote_stable", SymbioteStableCondition.Serializer::new);

    private PalladiumCompat() {
    }

    public static void register(IEventBus modBus) {
        ABILITIES.register(modBus);
        CONDITIONS.register(modBus);
        PalladiumSuitSets.SUIT_SETS.register(modBus);
    }

    public static void commonSetup() {
        PowersNJ.LOGGER.info("Powers NJ registered {} Palladium ability types and {} conditions", ABILITIES.getEntries().size(), CONDITIONS.getEntries().size());
    }
}
