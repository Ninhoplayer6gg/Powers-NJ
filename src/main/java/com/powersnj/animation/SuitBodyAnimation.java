package com.powersnj.animation;

import com.powersnj.core.animation.AnimationPose;
import com.powersnj.core.animation.HumanoidBones;
import com.powersnj.core.animation.RootMotion;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.client.model.animation.PalladiumAnimation;
import net.threetag.palladium.entity.FlightHandler;
import net.threetag.palladium.entity.PalladiumPlayerExtension;

/**
 * Applies the suit animation pose to the player model through Palladium's humanoid animation
 * pipeline (so it composes with every other Palladium animation and drives worn armor - including
 * the GeckoLib suit, which copies these parts).
 * <ul>
 *     <li>{@code root} becomes Palladium's whole-body transform (render pose stack);</li>
 *     <li>body, arms and legs are blended over the vanilla pose by their weight;</li>
 *     <li>the head keeps following the look direction, except in the fly states.</li>
 * </ul>
 * Arms busy with an item (bow, shield, eating, swinging a weapon...) are left to vanilla. First person
 * rendering is never touched.
 */
public class SuitBodyAnimation extends PalladiumAnimation {

    private final float[] value = new float[AnimationPose.SIZE];

    public SuitBodyAnimation(int priority) {
        super(priority);
    }

    @Override
    public void animate(Builder builder, AbstractClientPlayer player, HumanoidModel<?> model, FirstPersonContext firstPersonContext, float partialTicks) {
        if (firstPersonContext.firstPerson()) {
            return;
        }
        AnimationPose pose = SuitAnimationClient.pose(player, partialTicks);
        if (pose == null) {
            return;
        }
        this.root(builder, player, pose, partialTicks);
        this.head(builder, model.head, pose);
        boolean attacking = model.attackTime > 0F && !player.getMainHandItem().isEmpty();
        this.part(builder, PlayerModelPart.CHEST, model.body, HumanoidBones.BODY, pose, attacking ? 0F : 1F);
        this.part(builder, PlayerModelPart.RIGHT_ARM, model.rightArm, HumanoidBones.RIGHT_ARM, pose, armFreedom(player, model, HumanoidArm.RIGHT, attacking));
        this.part(builder, PlayerModelPart.LEFT_ARM, model.leftArm, HumanoidBones.LEFT_ARM, pose, armFreedom(player, model, HumanoidArm.LEFT, attacking));
        this.part(builder, PlayerModelPart.RIGHT_LEG, model.rightLeg, HumanoidBones.RIGHT_LEG, pose, 1F);
        this.part(builder, PlayerModelPart.LEFT_LEG, model.leftLeg, HumanoidBones.LEFT_LEG, pose, 1F);
    }

    private void root(Builder builder, AbstractClientPlayer player, AnimationPose pose, float partialTicks) {
        pose.accumulated(HumanoidBones.ROOT, this.value);
        float steering = pose.steering();
        float pitch = steering > 0F ? player.getViewXRot(partialTicks) : 0F;
        float bank = steering > 0F ? bank(player, partialTicks) : 0F;
        float crouch = player.isCrouching() ? pose.crouchCompensation() : 0F;
        RootMotion.Result result = RootMotion.compose(this.value, steering, pitch, bank, crouch);
        builder.get(PlayerModelPart.BODY)
                .setX(result.x()).setY(result.y()).setZ(result.z()).setY2(0F)
                .setXRot(result.xRot()).setYRot(result.yRot()).setZRot(result.zRot())
                .multiplier(pose.master());
    }

    private void head(Builder builder, ModelPart head, AnimationPose pose) {
        float weight = pose.weight(HumanoidBones.HEAD);
        if (weight <= 0F) {
            return;
        }
        pose.normalized(HumanoidBones.HEAD, this.value);
        float look = pose.lookWeight();
        PartPose rest = head.getInitialPose();
        builder.get(PlayerModelPart.HEAD)
                .setXRot(builder.getHeadPitch() * Mth.DEG_TO_RAD * look + this.value[0] * Mth.DEG_TO_RAD)
                .setYRot(builder.getNetHeadYaw() * Mth.DEG_TO_RAD * look + this.value[1] * Mth.DEG_TO_RAD)
                .setZRot(this.value[2] * Mth.DEG_TO_RAD)
                .setX(rest.x + this.value[3]).setY(rest.y - this.value[4]).setZ(rest.z + this.value[5])
                .multiplier(weight);
    }

    private void part(Builder builder, PlayerModelPart target, ModelPart part, String bone, AnimationPose pose, float freedom) {
        float weight = pose.weight(bone) * freedom;
        if (weight <= 0F) {
            return;
        }
        pose.normalized(bone, this.value);
        PartPose rest = part.getInitialPose();
        builder.get(target)
                .setXRot(rest.xRot + this.value[0] * Mth.DEG_TO_RAD)
                .setYRot(rest.yRot + this.value[1] * Mth.DEG_TO_RAD)
                .setZRot(rest.zRot + this.value[2] * Mth.DEG_TO_RAD)
                .setX(rest.x + this.value[3]).setY(rest.y - this.value[4]).setZ(rest.z + this.value[5])
                .multiplier(weight);
    }

    /**
     * 0 when vanilla must keep the arm: special arm poses (bow, crossbow, shield, spyglass...), using an
     * item with that hand, or swinging an item.
     */
    private static float armFreedom(AbstractClientPlayer player, HumanoidModel<?> model, HumanoidArm arm, boolean attacking) {
        HumanoidModel.ArmPose armPose = arm == HumanoidArm.RIGHT ? model.rightArmPose : model.leftArmPose;
        if (armPose != HumanoidModel.ArmPose.EMPTY && armPose != HumanoidModel.ArmPose.ITEM) {
            return 0F;
        }
        if (player.isUsingItem()) {
            HumanoidArm used = player.getUsedItemHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
            if (used == arm) {
                return 0F;
            }
        }
        return attacking && arm == player.getMainArm() ? 0F : 1F;
    }

    /**
     * Same banking as Palladium's own flight animation: roll into turns, scaled by speed.
     */
    private static float bank(AbstractClientPlayer player, float partialTicks) {
        if (!(player instanceof PalladiumPlayerExtension extension)) {
            return 0F;
        }
        FlightHandler flight = extension.palladium$getFlightHandler();
        Vec3 direction = flight.getFlightVector(partialTicks);
        Vec3 look = flight.getLookAngle(partialTicks);
        double angle = Math.atan2(direction.x * look.z - direction.z * look.x, direction.x * look.x + direction.z * look.z);
        double tilt = Mth.clamp(angle, -0.5D, 0.5D) * 90D * flight.getHorizontalSpeed(partialTicks);
        return (float) -tilt;
    }
}
