package com.powersnj.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Server-side ray casts used by abilities (the client never tells the server what it hit).
 */
public final class Targeting {

    private Targeting() {
    }

    /**
     * First living entity along the player's view within {@code reach}, not behind a block.
     */
    public static @Nullable LivingEntity livingInSight(Player player, double reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(reach));
        BlockHitResult block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDistance = block.getType() == HitResult.Type.MISS ? reach : block.getLocation().distanceTo(eye);
        Vec3 limitedEnd = eye.add(look.scale(maxDistance));
        AABB box = player.getBoundingBox().expandTowards(look.scale(maxDistance)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, limitedEnd, box,
                e -> e instanceof LivingEntity && e != player && e.isAlive() && !e.isSpectator() && e.isPickable(), maxDistance * maxDistance);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    /**
     * Block the player looks at within {@code reach}.
     */
    public static @Nullable BlockHitResult blockInSight(Entity entity, double reach) {
        Vec3 eye = entity.getEyePosition();
        Vec3 end = eye.add(entity.getViewVector(1.0F).scale(reach));
        BlockHitResult hit = entity.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    /**
     * Horizontal facing direction of an entity (never zero).
     */
    public static Vec3 horizontalLook(Entity entity) {
        Vec3 look = entity.getViewVector(1.0F);
        Vec3 horizontal = new Vec3(look.x, 0, look.z);
        return horizontal.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : horizontal.normalize();
    }
}
