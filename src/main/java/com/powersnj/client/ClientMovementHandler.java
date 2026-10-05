package com.powersnj.client;

import com.powersnj.PowersNJ;
import com.powersnj.core.math.Vec3d;
import com.powersnj.core.net.MovementSnapshot;
import com.powersnj.movement.MovementControllers;
import com.powersnj.symbiote.TendrilEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side movement prediction for the local player (Minecraft movement is client driven, so
 * the motion of mechanics decided by the server is applied here):
 * <ul>
 *     <li>speedster turning control (inertia grows with speed), wall running, water running;</li>
 *     <li>symbiote wall crawling;</li>
 *     <li>tendril swing rope constraint.</li>
 * </ul>
 * Every mechanic only runs while the server-synced state says it is active.
 */
@Mod.EventBusSubscriber(modid = PowersNJ.MOD_ID, value = Dist.CLIENT)
public final class ClientMovementHandler {

    private static Vec3 previousVelocity = Vec3.ZERO;

    private ClientMovementHandler() {
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof LocalPlayer player) || player != Minecraft.getInstance().player) {
            return;
        }
        MovementSnapshot state = ClientPowerState.movement(player.getId()).orElse(null);
        if (state == null) {
            previousVelocity = player.getDeltaMovement();
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            preMovement(player, state);
        } else {
            postMovement(player, state);
        }
    }

    private static void preMovement(LocalPlayer player, MovementSnapshot state) {
        Vec3 velocity = player.getDeltaMovement();
        if (MovementControllers.SPEEDSTER.equals(state.controller()) && state.has(MovementSnapshot.FLAG_ACTIVE)) {
            if (state.has(MovementSnapshot.FLAG_WALL_RUNNING) && player.horizontalCollision) {
                player.setDeltaMovement(velocity.x, Math.max(velocity.y, 0.45D), velocity.z);
                player.fallDistance = 0F;
            }
            if (state.has(MovementSnapshot.FLAG_WATER_RUNNING)) {
                BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.1D, player.getZ());
                if (player.isInWater()) {
                    player.setDeltaMovement(velocity.x, Math.max(velocity.y, 0.25D), velocity.z);
                } else if (player.level().getFluidState(below).is(FluidTags.WATER) && velocity.y < 0D) {
                    player.setDeltaMovement(velocity.x, 0D, velocity.z);
                    player.setOnGround(true);
                }
            }
        }
        if (MovementControllers.SYMBIOTE.equals(state.controller())) {
            if (state.has(MovementSnapshot.FLAG_WALL_CRAWLING) && player.horizontalCollision) {
                double y;
                if (player.isShiftKeyDown()) {
                    y = 0D;
                } else if (player.input.jumping || player.input.forwardImpulse > 0F) {
                    y = 0.25D;
                } else {
                    y = Math.max(velocity.y, -0.08D);
                }
                player.setDeltaMovement(velocity.x, y, velocity.z);
                player.fallDistance = 0F;
            }
            if (state.has(MovementSnapshot.FLAG_SWINGING)) {
                applySwing(player);
            }
        }
    }

    private static void postMovement(LocalPlayer player, MovementSnapshot state) {
        Vec3 velocity = player.getDeltaMovement();
        if (MovementControllers.SPEEDSTER.equals(state.controller()) && state.has(MovementSnapshot.FLAG_ACTIVE) && state.speed() > 2F && player.onGround()) {
            SpeedsterVisuals.SpeedsterHelpers.Normalizer normalizer = SpeedsterVisuals.SpeedsterHelpers.normalizer(player);
            double turn = 1D - (1D - normalizer.profile().turnRateAtMax()) * normalizer.normalize(state.speed());
            Vec3d steered = Vec3d.steerHorizontal(new Vec3d(previousVelocity.x, velocity.y, previousVelocity.z),
                    new Vec3d(velocity.x, velocity.y, velocity.z), turn);
            // Keep the new speed, only limit how fast the direction can change.
            double speed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            Vec3d direction = steered.horizontal().normalize();
            if (!direction.isZero() && speed > 1.0E-3) {
                player.setDeltaMovement(direction.x() * speed, velocity.y, direction.z() * speed);
            }
        }
        previousVelocity = player.getDeltaMovement();
    }

    /**
     * Rope constraint around the anchored tendril: the player swings like a pendulum.
     */
    private static void applySwing(LocalPlayer player) {
        TendrilEntity tendril = null;
        for (Entity entity : player.level().getEntitiesOfClass(TendrilEntity.class, player.getBoundingBox().inflate(48D))) {
            if (entity instanceof TendrilEntity t && t.getOwnerId() == player.getId() && t.isSwingAnchor()) {
                tendril = t;
                break;
            }
        }
        if (tendril == null || tendril.getAnchor().isEmpty()) {
            return;
        }
        Vec3 anchor = Vec3.atCenterOf(tendril.getAnchor().get());
        Vec3 offset = player.position().add(0, player.getBbHeight() * 0.5D, 0).subtract(anchor);
        double length = Math.max(2D, tendril.getRopeLength());
        double distance = offset.length();
        Vec3 velocity = player.getDeltaMovement();
        if (distance > length && distance > 1.0E-3) {
            Vec3 radial = offset.scale(1D / distance);
            double outward = velocity.dot(radial);
            if (outward > 0) {
                velocity = velocity.subtract(radial.scale(outward));
            }
            // Pull back onto the rope length to avoid drifting.
            velocity = velocity.subtract(radial.scale((distance - length) * 0.2D));
        }
        // Small boost along the look direction so players can pump the swing.
        if (player.input.forwardImpulse > 0F) {
            Vec3 look = player.getLookAngle();
            velocity = velocity.add(look.x * 0.03D, 0, look.z * 0.03D);
        }
        player.setDeltaMovement(velocity);
        player.fallDistance = 0F;
    }
}
