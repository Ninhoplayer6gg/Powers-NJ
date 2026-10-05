package com.powersnj.registry;

import com.powersnj.PowersNJ;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Sound events. {@code assets/powersnj/sounds.json} maps them to placeholder vanilla sounds until
 * the final {@code .ogg} files from ASSET_REQUIREMENTS.md are delivered.
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, PowersNJ.MOD_ID);

    // Suit / progression
    public static final RegistryObject<SoundEvent> SUIT_EQUIP = register("suit.equip");
    public static final RegistryObject<SoundEvent> SUIT_FABRICATE = register("suit.fabricate");
    public static final RegistryObject<SoundEvent> LEVEL_UP = register("progression.level_up");
    public static final RegistryObject<SoundEvent> SKILL_UNLOCK = register("progression.skill_unlock");
    // Viltrumite
    public static final RegistryObject<SoundEvent> GROUND_SLAM = register("thragg.ground_slam");
    public static final RegistryObject<SoundEvent> SHOCKWAVE = register("thragg.shockwave");
    public static final RegistryObject<SoundEvent> HEAVY_PUNCH = register("thragg.heavy_punch");
    public static final RegistryObject<SoundEvent> FLIGHT_BOOST = register("thragg.flight_boost");
    // Symbiote
    public static final RegistryObject<SoundEvent> TENDRIL_SHOOT = register("venom.tendril_shoot");
    public static final RegistryObject<SoundEvent> TENDRIL_RETRACT = register("venom.tendril_retract");
    public static final RegistryObject<SoundEvent> SYMBIOTE_SHIELD = register("venom.symbiote_shield");
    public static final RegistryObject<SoundEvent> SYMBIOTE_HISS = register("venom.symbiote_hiss");
    // Speedster
    public static final RegistryObject<SoundEvent> SPEED_START = register("reverse_flash.speed_start");
    public static final RegistryObject<SoundEvent> SPEED_STOP = register("reverse_flash.speed_stop");
    public static final RegistryObject<SoundEvent> SPEED_DASH = register("reverse_flash.dash");
    public static final RegistryObject<SoundEvent> PHASE = register("reverse_flash.phase");
    public static final RegistryObject<SoundEvent> VORTEX = register("reverse_flash.vortex");

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(PowersNJ.id(name)));
    }
}
