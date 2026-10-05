package com.powersnj.registry;

import com.powersnj.PowersNJ;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Particle types. Sprites: {@code assets/powersnj/particles/<name>.json} &rarr;
 * {@code textures/particle/<name>.png}.
 */
public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, PowersNJ.MOD_ID);

    public static final RegistryObject<SimpleParticleType> SPEED_SPARK = PARTICLES.register("speed_spark", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> NEGATIVE_LIGHTNING = PARTICLES.register("negative_lightning", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> SYMBIOTE_GOO = PARTICLES.register("symbiote_goo", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> SHOCKWAVE_DUST = PARTICLES.register("shockwave_dust", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> VILTRUMITE_IMPACT = PARTICLES.register("viltrumite_impact", () -> new SimpleParticleType(true));

    private ModParticles() {
    }
}
