package com.powersnj.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.powersnj.PowersNJ;
import com.powersnj.client.ClientPowerState;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the active symbiote partial transformation (claws, blade arm, mask...) as an overlay on
 * the player model. Synced per entity ({@code FormPacket}); draws nothing until the form texture
 * {@code textures/suits/venom/forms/<form>.png} exists. Replaceable by GeckoLib form models later.
 */
public class SymbioteFormLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public SymbioteFormLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    public static ResourceLocation formTexture(String form) {
        return PowersNJ.id("textures/suits/venom/forms/" + form + ".png");
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        String form = ClientPowerState.form(player.getId());
        if (form.isEmpty() || player.isInvisible()) {
            return;
        }
        ResourceLocation texture = formTexture(form);
        if (!AssetAvailability.has(texture)) {
            return;
        }
        this.getParentModel().renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityTranslucent(texture)), packedLight,
                LivingEntityRenderer.getOverlayCoords(player, 0F), 1F, 1F, 1F, 1F);
    }
}
