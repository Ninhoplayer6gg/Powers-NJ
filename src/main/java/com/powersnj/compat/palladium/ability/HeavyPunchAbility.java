package com.powersnj.compat.palladium.ability;

import com.powersnj.combat.CombatService;
import com.powersnj.combat.Targeting;
import com.powersnj.core.destruction.DestructionTier;
import com.powersnj.destruction.DestructionEngine;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.registry.ModDamageTypes;
import com.powersnj.registry.ModEffects;
import com.powersnj.registry.ModParticles;
import com.powersnj.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

/**
 * {@code powersnj:heavy_punch} - devastating punch: huge damage and knockback to the entity in
 * sight, or a small crater when punching a wall (can break obsidian-class blocks when
 * {@code destruction_power} is {@code extreme} and the server allows it).
 */
public class HeavyPunchAbility extends PowersActionAbility {

    public static final PalladiumProperty<Float> REACH = new FloatProperty("reach").configurable("Reach in blocks.");
    public static final PalladiumProperty<Float> DAMAGE = new FloatProperty("damage").configurable("Damage (PvE value).");
    public static final PalladiumProperty<Float> KNOCKBACK = new FloatProperty("knockback").configurable("Knockback strength.");
    public static final PalladiumProperty<Float> CRATER_RADIUS = new FloatProperty("crater_radius").configurable("Crater radius when hitting blocks.");

    public HeavyPunchAbility() {
        this.withProperty(REACH, 5F);
        this.withProperty(DAMAGE, 20F);
        this.withProperty(KNOCKBACK, 3F);
        this.withProperty(CRATER_RADIUS, 1.5F);
        this.withProperty(DESTRUCTION_POWER, "extreme");
    }

    @Override
    public boolean onActivated(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        ServerLevel level = player.serverLevel();
        float reach = instance.getProperty(REACH);
        LivingEntity target = Targeting.livingInSight(player, reach);
        if (target != null) {
            if (!CombatService.dealAbilityDamage(player, target, instance.getProperty(DAMAGE), ModDamageTypes.SUPER_STRENGTH, true)) {
                return false;
            }
            Vec3 push = Targeting.horizontalLook(player).scale(instance.getProperty(KNOCKBACK));
            CombatService.launch(target, target.getDeltaMovement().add(push).add(0, 0.45D, 0));
            target.addEffect(new MobEffectInstance(ModEffects.STAGGERED.get(), 30, 0));
            level.sendParticles(ModParticles.VILTRUMITE_IMPACT.get(), target.getX(), target.getY(0.6D), target.getZ(), 12, 0.2, 0.2, 0.2, 0.2);
            level.playSound(null, target.blockPosition(), ModSounds.HEAVY_PUNCH.get(), SoundSource.PLAYERS, 1.2F, 0.9F);
            return true;
        }
        BlockHitResult block = Targeting.blockInSight(player, reach);
        DestructionTier power = destructionPower(instance);
        if (block == null || power == null) {
            return false;
        }
        int scheduled = DestructionEngine.crater(level, player, block.getBlockPos(), instance.getProperty(CRATER_RADIUS), power);
        if (scheduled <= 0) {
            return false;
        }
        Vec3 hit = block.getLocation();
        level.sendParticles(ModParticles.VILTRUMITE_IMPACT.get(), hit.x, hit.y, hit.z, 16, 0.3, 0.3, 0.3, 0.2);
        level.playSound(null, block.getBlockPos(), ModSounds.HEAVY_PUNCH.get(), SoundSource.PLAYERS, 1.2F, 0.7F);
        return true;
    }

    @Override
    public String getDocumentationDescription() {
        return "Powers NJ: heavy punch against entities or walls.";
    }
}
