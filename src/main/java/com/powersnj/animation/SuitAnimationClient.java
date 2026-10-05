package com.powersnj.animation;

import com.powersnj.client.ClientPowerState;
import com.powersnj.core.animation.AnimationPose;
import com.powersnj.core.animation.AnimationSet;
import com.powersnj.core.animation.AnimatorInput;
import com.powersnj.core.animation.SuitAnimator;
import com.powersnj.core.net.MovementSnapshot;
import com.powersnj.movement.MovementControllers;
import com.powersnj.suit.SuitArmorItem;
import com.powersnj.suit.SuitKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.threetag.palladium.entity.FlightHandler;
import net.threetag.palladium.entity.PalladiumPlayerExtension;

import java.util.HashMap;
import java.util.Map;

/**
 * Client side owner of the per-player {@link SuitAnimator}s. Every client animates every player it
 * renders from synced state (equipment, movement, flight, swings) plus the one-shot clips announced
 * by the server, so all players see the same animations.
 * <p>
 * A player is animated while wearing the four pieces of a suit that has an animation set.
 */
public final class SuitAnimationClient {

    private static final double SPRINT_MEMORY = 0.3D;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<Integer, Tracked> TRACKED = new HashMap<>();
    private static long tickCounter;

    private static final class Tracked {
        final SuitAnimator animator;
        final AnimationPose pose = new AnimationPose();
        double poseTime = Double.NaN;
        boolean wasOnGround = true;
        double lastY = Double.NaN;
        boolean wasSwinging;
        int lastSwingTime;
        int lastHurtTime;
        double lastSprint = Double.NEGATIVE_INFINITY;
        double lastCombat = Double.NEGATIVE_INFINITY;
        long seen;

        Tracked(AnimationSet set) {
            this.animator = new SuitAnimator(set);
        }
    }

    private SuitAnimationClient() {
    }

    /** Client animation clock in seconds (pauses with the game). */
    public static double now(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0D : (minecraft.level.getGameTime() + (double) partialTick) / 20D;
    }

    /**
     * Name of the suit worn as a complete set, or null.
     */
    public static String fullSuit(Player player) {
        SuitKind kind = null;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof SuitArmorItem item)) {
                return null;
            }
            if (kind == null) {
                kind = item.kind();
            } else if (kind != item.kind()) {
                return null;
            }
        }
        return kind == null ? null : kind.name();
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null) {
            clear();
            return;
        }
        tickCounter++;
        double now = now(0F);
        for (AbstractClientPlayer player : minecraft.level.players()) {
            AnimationSet set = SuitAnimationLibrary.INSTANCE.get(fullSuit(player));
            Tracked tracked = TRACKED.get(player.getId());
            if (set == null) {
                if (tracked != null) {
                    // keep ticking (inactive) so the suit fades back to vanilla smoothly
                    tickPlayer(player, tracked, now, false);
                }
                continue;
            }
            if (tracked == null || tracked.animator.set() != set) {
                tracked = new Tracked(set);
                TRACKED.put(player.getId(), tracked);
            }
            tickPlayer(player, tracked, now, true);
        }
        TRACKED.entrySet().removeIf(e -> e.getValue().seen != tickCounter
                || (e.getValue().animator.master() <= 0F && SuitAnimationLibrary.INSTANCE.get(fullSuitOf(minecraft, e.getKey())) == null));
    }

    private static String fullSuitOf(Minecraft minecraft, int entityId) {
        return minecraft.level != null && minecraft.level.getEntity(entityId) instanceof Player player ? fullSuit(player) : null;
    }

    private static void tickPlayer(AbstractClientPlayer player, Tracked tracked, double now, boolean wearing) {
        tracked.seen = tickCounter;
        SuitAnimator animator = tracked.animator;
        FlightHandler flight = player instanceof PalladiumPlayerExtension extension ? extension.palladium$getFlightHandler() : null;
        boolean flying = flight != null && flight.getFlightType().isNotNull();
        float boost = flight == null ? 0F : flight.getFlightAnimation(1F);
        MovementSnapshot movement = ClientPowerState.movement(player.getId()).orElse(null);
        boolean fast = movement != null && MovementControllers.FLIGHT.equals(movement.controller())
                && ("BOOST".equals(movement.mode()) || "HIGH_SPEED".equals(movement.mode()));
        boolean onGround = player.onGround();

        if (player.isSprinting()) {
            tracked.lastSprint = now;
        }
        if (player.hurtTime > tracked.lastHurtTime) {
            tracked.lastCombat = now;
        }
        tracked.lastHurtTime = player.hurtTime;

        boolean active = wearing && !player.isSleeping() && !player.isPassenger() && !player.isSwimming() && !player.isVisuallySwimming()
                && !player.isFallFlying() && !player.isAutoSpinAttack() && !player.isDeadOrDying() && !player.isSpectator();
        boolean inCombat = now - tracked.lastCombat < animator.set().combatWindow();
        animator.tick(new AnimatorInput(active, onGround, flying, boost, fast, player.isCrouching(), player.isSprinting(), inCombat), now);

        double y = player.getY();
        if (active && tracked.wasOnGround && !onGround && !flying && !Double.isNaN(tracked.lastY) && y - tracked.lastY > 0.03D) {
            animator.onJump(now);
        }
        boolean newSwing = player.swinging && (!tracked.wasSwinging || player.swingTime < tracked.lastSwingTime);
        if (active && newSwing && player.swingingArm == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty()) {
            animator.onSwing(now - tracked.lastSprint <= SPRINT_MEMORY, now);
            tracked.lastCombat = now;
        }
        tracked.wasSwinging = player.swinging;
        tracked.lastSwingTime = player.swingTime;
        tracked.wasOnGround = onGround;
        tracked.lastY = y;
    }

    /** One-shot clip or event announced by the server. */
    public static void play(int entityId, String key) {
        Tracked tracked = TRACKED.get(entityId);
        if (tracked != null) {
            tracked.animator.play(key, now(0F));
        }
    }

    /**
     * The blended pose of a player for this frame (computed once per frame), or null when the player
     * has no suit animations.
     */
    public static AnimationPose pose(Player player, float partialTick) {
        Tracked tracked = TRACKED.get(player.getId());
        if (tracked == null) {
            return null;
        }
        double now = now(partialTick);
        if (tracked.poseTime != now) {
            float limbSwing = player.walkAnimation.position(partialTick);
            float limbAmount = Math.min(1F, player.walkAnimation.speed(partialTick));
            tracked.animator.sample(now, limbSwing, limbAmount, tracked.pose);
            tracked.poseTime = now;
        }
        return tracked.pose.master() > 0F ? tracked.pose : null;
    }

    public static void clear() {
        TRACKED.clear();
    }
}
