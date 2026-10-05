package com.powersnj.registry;

import com.powersnj.PowersNJ;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Status effects used by power systems.
 */
public final class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, PowersNJ.MOD_ID);

    /** Symbiote weakened by heat or sound: symbiote abilities are blocked while active. */
    public static final RegistryObject<MobEffect> SYMBIOTE_DESTABILIZED = EFFECTS.register("symbiote_destabilized",
            () -> new PowersMobEffect(MobEffectCategory.HARMFUL, 0x9AE6E6));

    /** Applied by ground slams / shockwaves to mobs and players: brief stagger. */
    public static final RegistryObject<MobEffect> STAGGERED = EFFECTS.register("staggered",
            () -> new PowersMobEffect(MobEffectCategory.HARMFUL, 0x8B7355));

    private ModEffects() {
    }

    /**
     * Marker effect: behaviour is implemented by the systems that check for it.
     */
    public static final class PowersMobEffect extends MobEffect {

        public PowersMobEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
