package com.powersnj.compat.palladium.ability;

import com.powersnj.combat.CombatService;
import com.powersnj.core.destruction.DestructionTier;
import com.powersnj.destruction.DestructionEngine;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.registry.ModDamageTypes;
import com.powersnj.registry.ModEffects;
import com.powersnj.registry.ModParticles;
import com.powersnj.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

/**
 * {@code powersnj:ground_slam} - smashes the ground: area damage, knockback, stagger and a crater
 * through the DestructionEngine. Airborne users first dive to the ground.
 */
public class GroundSlamAbility extends PowersActionAbility {

    public static final PalladiumProperty<Float> RADIUS = new FloatProperty("radius").configurable("Damage radius in blocks.");
    public static final PalladiumProperty<Float> DAMAGE = new FloatProperty("damage").configurable("Damage at the centre (PvE value).");
    public static final PalladiumProperty<Float> KNOCKBACK = new FloatProperty("knockback").configurable("Horizontal knockback strength.");
    public static final PalladiumProperty<Float> CRATER_RADIUS = new FloatProperty("crater_radius").configurable("Radius of the destruction crater (0 = none).");

    public GroundSlamAbility() {
        this.withProperty(RADIUS, 6F);
        this.withProperty(DAMAGE, 14F);
        this.withProperty(KNOCKBACK, 1.6F);
        this.withProperty(CRATER_RADIUS, 2.5F);
        this.withProperty(DESTRUCTION_POWER, "hard");
    }

    @Override
    public boolean onActivated(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        ServerLevel level = player.serverLevel();
        BlockPos ground = player.blockPosition().below();
        if (!player.onGround()) {
            // Dive: find the ground below (max 24 blocks) and slam there.
            BlockPos.MutableBlockPos cursor = player.blockPosition().mutable();
            int depth = 0;
            while (depth++ < 24 && level.getBlockState(cursor.below()).getCollisionShape(level, cursor.below()).isEmpty()) {
                cursor.move(0, -1, 0);
            }
            if (depth >= 24) {
                return false;
            }
            player.teleportTo(player.getX(), cursor.getY(), player.getZ());
            ground = cursor.below().immutable();
        }
        Vec3 center = Vec3.atBottomCenterOf(ground.above());
        float radius = instance.getProperty(RADIUS);
        float damage = instance.getProperty(DAMAGE);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius, 2, radius), e -> e != player)) {
            double distance = target.position().distanceTo(center);
            if (distance > radius) {
                continue;
            }
            float falloff = (float) (1D - 0.6D * distance / radius);
            if (CombatService.dealAbilityDamage(player, target, damage * falloff, ModDamageTypes.SHOCKWAVE, false)) {
                CombatService.knockbackFrom(target, center, instance.getProperty(KNOCKBACK) * falloff, 0.5D);
                target.addEffect(new MobEffectInstance(ModEffects.STAGGERED.get(), 40, 0));
            }
        }
        DestructionTier power = destructionPower(instance);
        float crater = instance.getProperty(CRATER_RADIUS);
        if (power != null && crater > 0F) {
            DestructionEngine.crater(level, player, ground, crater, power);
        }
        level.playSound(null, ground, ModSounds.GROUND_SLAM.get(), SoundSource.PLAYERS, 1.5F, 0.8F);
        level.sendParticles(ModParticles.SHOCKWAVE_DUST.get(), center.x, center.y + 0.1D, center.z, 60, radius / 2D, 0.2D, radius / 2D, 0.05D);
        player.fallDistance = 0F;
        return true;
    }

    @Override
    public String getDocumentationDescription() {
        return "Powers NJ: area slam with crater (DestructionEngine).";
    }
}
