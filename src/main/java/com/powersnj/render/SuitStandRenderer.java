package com.powersnj.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.powersnj.block.SuitStandBlock;
import com.powersnj.block.SuitStandBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Displays the stored suit standing on the stand. The pieces are rendered through an invisible
 * client-side armor stand, i.e. through the regular armor pipeline: placeholder armor textures today,
 * GeckoLib suit models automatically once their assets exist - for every character.
 */
public class SuitStandRenderer implements BlockEntityRenderer<SuitStandBlockEntity> {

    private final Map<SuitStandBlockEntity, ArmorStand> mannequins = new WeakHashMap<>();

    public SuitStandRenderer(BlockEntityRendererProvider.Context context) {
    }

    private ArmorStand mannequin(SuitStandBlockEntity stand, Level level) {
        ArmorStand mannequin = this.mannequins.get(stand);
        if (mannequin == null || mannequin.level() != level) {
            mannequin = new ArmorStand(EntityType.ARMOR_STAND, level);
            mannequin.setInvisible(true);
            this.mannequins.put(stand, mannequin);
        }
        return mannequin;
    }

    @Override
    public void render(SuitStandBlockEntity stand, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Level level = stand.getLevel();
        if (level == null || stand.isEmpty()) {
            return;
        }
        ArmorStand mannequin = this.mannequin(stand, level);
        for (EquipmentSlot slot : SuitStandBlockEntity.SLOTS) {
            mannequin.setItemSlot(slot, stand.getItem(slot));
        }
        Direction facing = stand.getBlockState().hasProperty(SuitStandBlock.FACING) ? stand.getBlockState().getValue(SuitStandBlock.FACING) : Direction.NORTH;
        float yaw = facing.toYRot();
        mannequin.setYRot(yaw);
        mannequin.yRotO = yaw;
        mannequin.yBodyRot = yaw;
        mannequin.yBodyRotO = yaw;
        mannequin.yHeadRot = yaw;
        mannequin.yHeadRotO = yaw;

        poseStack.pushPose();
        Minecraft.getInstance().getEntityRenderDispatcher().render(mannequin, 0.5D, 2.0D / 16.0D, 0.5D, yaw, partialTick, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(SuitStandBlockEntity stand) {
        return true;
    }
}
