package com.powersnj.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.powersnj.PowersNJ;
import com.powersnj.compat.geckolib.TendrilGeoRenderer;
import com.powersnj.symbiote.TendrilEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Tendril renderer. Draws a textured ribbon from the owner's hand to the tendril tip (placeholder
 * art: {@code textures/entity/tendril.png}). When {@code geo/entity/tendril.geo.json} is present the
 * tip is additionally rendered with GeckoLib, ready for the final animated model.
 */
public class TendrilRenderer extends EntityRenderer<TendrilEntity> {

    public static final ResourceLocation TEXTURE = PowersNJ.id("textures/entity/tendril.png");
    private static final float HALF_WIDTH = 0.06F;

    private final TendrilGeoRenderer geoRenderer;

    public TendrilRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.geoRenderer = new TendrilGeoRenderer(context);
    }

    @Override
    public boolean shouldRender(TendrilEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(TendrilEntity tendril, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Entity owner = tendril.getOwner();
        if (owner instanceof LivingEntity living) {
            Vec3 tip = tendril.getPosition(partialTick);
            float bodyYaw = Mth.lerp(partialTick, living.yBodyRotO, living.yBodyRot) * Mth.DEG_TO_RAD;
            Vec3 shoulder = living.getPosition(partialTick).add(-Mth.cos(bodyYaw) * 0.35D, living.getBbHeight() * 0.72D, -Mth.sin(bodyYaw) * 0.35D);
            Vec3 start = shoulder.subtract(tip);
            this.drawRibbon(poseStack, buffer, start, packedLight, tendril.tickCount + partialTick);
        }
        if (AssetAvailability.has(TendrilGeoRenderer.MODEL)) {
            this.geoRenderer.render(tendril, yaw, partialTick, poseStack, buffer, packedLight);
        }
        super.render(tendril, yaw, partialTick, poseStack, buffer, packedLight);
    }

    /**
     * Ribbon from the tip (local origin) to {@code start}, with a slight organic wobble.
     */
    private void drawRibbon(PoseStack poseStack, MultiBufferSource buffer, Vec3 start, int light, float time) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        int segments = 16;
        double length = start.length();
        Vec3 side = start.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / segments;
            float t1 = (float) (i + 1) / segments;
            Vec3 p0 = this.point(start, side, t0, time);
            Vec3 p1 = this.point(start, side, t1, time);
            float v0 = (float) (t0 * length);
            float v1 = (float) (t1 * length);
            this.quad(consumer, pose, normal, p0, p1, side.scale(HALF_WIDTH), v0, v1, light);
            this.quad(consumer, pose, normal, p0, p1, new Vec3(0, HALF_WIDTH, 0), v0, v1, light);
        }
    }

    private Vec3 point(Vec3 start, Vec3 side, float t, float time) {
        double wobble = Math.sin(t * Math.PI) * Math.sin(time * 0.4F + t * 8F) * 0.12D;
        return start.scale(t).add(side.scale(wobble));
    }

    private void quad(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, Vec3 a, Vec3 b, Vec3 offset, float v0, float v1, int light) {
        this.vertex(consumer, pose, normal, a.add(offset), 0F, v0, light);
        this.vertex(consumer, pose, normal, a.subtract(offset), 1F, v0, light);
        this.vertex(consumer, pose, normal, b.subtract(offset), 1F, v1, light);
        this.vertex(consumer, pose, normal, b.add(offset), 0F, v1, light);
    }

    private void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, Vec3 p, float u, float v, int light) {
        consumer.vertex(pose, (float) p.x, (float) p.y, (float) p.z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0F, 1F, 0F)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(TendrilEntity entity) {
        return TEXTURE;
    }
}
