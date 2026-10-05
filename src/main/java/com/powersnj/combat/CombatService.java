package com.powersnj.combat;

import com.powersnj.core.combat.CombatRules;
import com.powersnj.core.combat.TargetKind;
import com.powersnj.core.config.Settings;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.Nullable;

/**
 * Single entry point for power damage. Applies PvP/PvE/boss balancing ({@link CombatRules}),
 * the PvP switch, team checks and knockback scaling. Every ability uses it, so balancing is
 * consistent and server-side only.
 */
public final class CombatService {

    private CombatService() {
    }

    public static TargetKind kindOf(LivingEntity entity) {
        if (entity instanceof Player) {
            return TargetKind.PLAYER;
        }
        return entity.getType().is(Tags.EntityTypes.BOSSES) ? TargetKind.BOSS : TargetKind.MOB;
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> type, @Nullable Entity direct, @Nullable Entity attacker) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), direct, attacker);
    }

    /**
     * Whether {@code attacker} may affect {@code target} with powers at all.
     */
    public static boolean canAffect(LivingEntity attacker, LivingEntity target) {
        if (target == attacker || !target.isAlive() || target.isSpectator()) {
            return false;
        }
        if (attacker.isAlliedTo(target)) {
            return false;
        }
        if (target instanceof Player targetPlayer && (targetPlayer.isCreative() || targetPlayer.isSpectator())) {
            return false;
        }
        return CombatRules.canHit(kindOf(target), attacker instanceof Player, Settings.get());
    }

    /**
     * Deals balanced power damage.
     *
     * @param baseDamage           authored (PvE) damage
     * @param bypassInvulnerability reset hurt cooldown first (rapid multi-hit abilities)
     * @return whether damage was applied
     */
    public static boolean dealAbilityDamage(LivingEntity attacker, LivingEntity target, float baseDamage, ResourceKey<DamageType> type, boolean bypassInvulnerability) {
        if (!canAffect(attacker, target)) {
            return false;
        }
        float damage = CombatRules.scaleDamage(baseDamage, kindOf(target), attacker instanceof Player, Settings.get());
        if (damage <= 0F) {
            return false;
        }
        if (bypassInvulnerability) {
            target.invulnerableTime = 0;
        }
        return target.hurt(source(attacker.level(), type, attacker, attacker), damage);
    }

    /**
     * Pushes {@code target} away from {@code origin} (horizontal) with an upward component.
     */
    public static void knockbackFrom(LivingEntity target, Vec3 origin, double strength, double upward) {
        double scaled = CombatRules.scaleKnockback(strength, kindOf(target)) * (1.0D - target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE));
        if (scaled <= 0) {
            return;
        }
        Vec3 away = target.position().subtract(origin);
        Vec3 horizontal = new Vec3(away.x, 0, away.z);
        horizontal = horizontal.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 0) : horizontal.normalize();
        launch(target, target.getDeltaMovement().add(horizontal.scale(scaled)).add(0, upward * (scaled > 0 ? 1 : 0), 0));
    }

    /**
     * Sets the velocity of an entity and makes sure players receive it.
     */
    public static void launch(Entity target, Vec3 velocity) {
        target.setDeltaMovement(velocity);
        target.hurtMarked = true;
        target.hasImpulse = true;
    }
}
