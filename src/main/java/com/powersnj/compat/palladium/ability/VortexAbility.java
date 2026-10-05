package com.powersnj.compat.palladium.ability;

import com.powersnj.combat.CombatService;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.registry.ModParticles;
import com.powersnj.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

/**
 * {@code powersnj:vortex} - held: running in circles creates a vortex that lifts and spins
 * entities around the speedster, deflects projectiles and extinguishes fire.
 */
public class VortexAbility extends PowersAbility {

    public static final PalladiumProperty<Float> RADIUS = new FloatProperty("radius").configurable("Vortex radius.");
    public static final PalladiumProperty<Float> STRENGTH = new FloatProperty("strength").configurable("Spin strength.");

    public VortexAbility() {
        this.withProperty(RADIUS, 6F);
        this.withProperty(STRENGTH, 0.6F);
        this.withProperty(ENERGY_PER_TICK, 0.8F);
    }

    @Override
    protected boolean retriesWhileEnabled() {
        return false;
    }

    @Override
    public boolean onActivated(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        player.level().playSound(null, player.blockPosition(), ModSounds.VORTEX.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    @Override
    protected void whileEnabled(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        ServerLevel level = player.serverLevel();
        float radius = instance.getProperty(RADIUS);
        float strength = instance.getProperty(STRENGTH);
        Vec3 center = player.position();
        player.clearFire();
        for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(radius), e -> e.isAlive() && !e.isSpectator())) {
            Vec3 offset = entity.position().subtract(center);
            double distance = Math.max(0.5D, Math.sqrt(offset.x * offset.x + offset.z * offset.z));
            if (distance > radius) {
                continue;
            }
            if (entity instanceof Projectile) {
                CombatService.launch(entity, offset.normalize().scale(1.2D));
                continue;
            }
            if (entity instanceof LivingEntity living && !CombatService.canAffect(player, living)) {
                continue;
            }
            Vec3 tangent = new Vec3(-offset.z, 0, offset.x).normalize().scale(strength);
            Vec3 inward = new Vec3(-offset.x, 0, -offset.z).normalize().scale(strength * 0.3D);
            CombatService.launch(entity, tangent.add(inward).add(0, 0.12D, 0));
            entity.fallDistance = 0F;
            entity.clearFire();
        }
        if (player.tickCount % 2 == 0) {
            double angle = player.tickCount * 0.6D;
            level.sendParticles(ModParticles.SPEED_SPARK.get(), center.x + Math.cos(angle) * radius * 0.7D, center.y + 0.5D, center.z + Math.sin(angle) * radius * 0.7D, 3, 0.2, 0.6, 0.2, 0.05);
        }
    }

    @Override
    public String getDocumentationDescription() {
        return "Powers NJ: speedster vortex (held).";
    }
}
