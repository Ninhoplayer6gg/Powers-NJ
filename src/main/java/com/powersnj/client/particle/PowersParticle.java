package com.powersnj.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/**
 * Generic animated particle used by every Powers NJ particle type (sprites from
 * {@code assets/powersnj/particles/<type>.json}).
 */
public class PowersParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    protected PowersParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites, boolean glowing) {
        super(level, x, y, z, dx, dy, dz);
        this.sprites = sprites;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.lifetime = 8 + this.random.nextInt(8);
        this.quadSize *= 1.2F;
        this.hasPhysics = false;
        this.friction = 0.9F;
        if (glowing) {
            this.alpha = 0.9F;
        }
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
        this.alpha = Math.max(0F, 1F - (float) this.age / this.lifetime);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new PowersParticle(level, x, y, z, dx, dy, dz, this.sprites, true);
        }
    }
}
