package com.powersnj.compat.geckolib;

import com.powersnj.animation.SuitAnimationClient;
import com.powersnj.animation.SuitAnimations;
import com.powersnj.core.animation.AnimationPose;
import com.powersnj.core.animation.HumanoidBones;
import com.powersnj.render.AssetAvailability;
import com.powersnj.render.SuitAssets;
import com.powersnj.suit.SuitArmorItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.state.BoneSnapshot;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for every suit: resources follow the {@link SuitAssets} conventions.
 * <p>
 * The armor bones follow the player model (GeoArmorRenderer). The suit bones below them (cape,
 * forearms, shins...) are posed here from the wearer's suit animation, so they stay in sync with
 * the body animation applied by {@code SuitBodyAnimation}. Without a wearer (suit stand) they rest.
 */
public class SuitGeoModel extends GeoModel<SuitArmorItem> {

    private final float[] value = new float[AnimationPose.SIZE];

    @Override
    public ResourceLocation getModelResource(SuitArmorItem item) {
        return SuitAssets.geoModel(item.kind());
    }

    @Override
    public ResourceLocation getTextureResource(SuitArmorItem item) {
        return SuitAssets.geoTexture(item.kind());
    }

    @Override
    public ResourceLocation getAnimationResource(SuitArmorItem item) {
        ResourceLocation animation = SuitAssets.geoAnimation(item.kind());
        return AssetAvailability.has(animation) ? animation : SuitAnimations.EMPTY_ANIMATIONS;
    }

    @Override
    public void setCustomAnimations(SuitArmorItem item, long instanceId, AnimationState<SuitArmorItem> state) {
        if (!(state.getData(DataTickets.ENTITY) instanceof Player player)) {
            return;
        }
        AnimationPose pose = SuitAnimationClient.pose(player, state.getPartialTick());
        if (pose == null) {
            return;
        }
        for (String name : pose.bones()) {
            if (HumanoidBones.isPlayerBone(name) || !pose.has(name)) {
                continue;
            }
            CoreGeoBone bone = this.getAnimationProcessor().getBone(name);
            if (bone == null) {
                continue;
            }
            pose.accumulated(name, this.value);
            BoneSnapshot rest = bone.getInitialSnapshot();
            // Bedrock values -> GeckoLib bone space (same conversion as GeckoLib's own keyframes)
            bone.setRotX(rest.getRotX() - this.value[0] * Mth.DEG_TO_RAD);
            bone.setRotY(rest.getRotY() - this.value[1] * Mth.DEG_TO_RAD);
            bone.setRotZ(rest.getRotZ() + this.value[2] * Mth.DEG_TO_RAD);
            bone.setPosX(rest.getOffsetX() + this.value[3]);
            bone.setPosY(rest.getOffsetY() + this.value[4]);
            bone.setPosZ(rest.getOffsetZ() + this.value[5]);
        }
    }
}
