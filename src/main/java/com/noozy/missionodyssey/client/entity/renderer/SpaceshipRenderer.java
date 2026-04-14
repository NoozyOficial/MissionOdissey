package com.noozy.missionodyssey.client.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.noozy.missionodyssey.client.entity.model.SpaceshipModel;
import com.noozy.missionodyssey.entity.SpaceshipEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class SpaceshipRenderer extends GeoEntityRenderer<SpaceshipEntity> {

    private static final String[] FLAME_BONES = {"flame_1", "flame_2", "flame_3", "flame_4"};
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    public SpaceshipRenderer(EntityRendererProvider.Context context) {
        super(context, new SpaceshipModel());
        this.shadowRadius = 0f;
    }

    @Override
    public void preRender(PoseStack poseStack, SpaceshipEntity entity, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, int packedColor) {
        super.preRender(poseStack, entity, model, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, packedColor);

        if (!isReRender) {
            float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
            poseStack.mulPose(Axis.YN.rotationDegrees(yaw));

            float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

            float roll = entity.getRoll(partialTick);
            if (Math.abs(roll) > 0.01f) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
            }
        }
    }

    @Override
    public void postRender(PoseStack poseStack, SpaceshipEntity entity, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer,
                           boolean isReRender, float partialTick, int packedLight,
                           int packedOverlay, int packedColor) {
        super.postRender(poseStack, entity, model, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, packedColor);

        if (isReRender || !entity.isEngineOn()) return;

        float throttle = entity.getThrottleAmount();
        if (throttle < 0.01f) return;

        for (String boneName : FLAME_BONES) {
            Optional<GeoBone> boneOpt = model.getBone(boneName);
            boneOpt.ifPresent(bone -> spawnThrusterParticles(entity, bone, throttle, partialTick));
        }
    }

    private void spawnThrusterParticles(SpaceshipEntity entity, GeoBone bone,
                                        float throttle, float partialTick) {
        Vector3d bonePos = bone.getWorldPosition();

        float yawRad = entity.getYRot() * DEG_TO_RAD;
        float pitchRad = entity.getXRot() * DEG_TO_RAD;
        float rollRad = entity.getVisualRoll() * DEG_TO_RAD;

        Vec3 rotated = rotatePoint(bonePos.x, bonePos.y, bonePos.z, yawRad, pitchRad, rollRad);

        double entityX = Mth.lerp(partialTick, entity.xo, entity.getX());
        double entityY = Mth.lerp(partialTick, entity.yo, entity.getY());
        double entityZ = Mth.lerp(partialTick, entity.zo, entity.getZ());

        double worldX = entityX + rotated.x;
        double worldY = entityY + rotated.y;
        double worldZ = entityZ + rotated.z;

        Vec3 backward = getBackwardVec(entity).scale(0.3 + throttle * 0.5);

        float spread = 0.02f + throttle * 0.05f;

        entity.level().addParticle(
                ParticleTypes.SOUL_FIRE_FLAME,
                worldX + randomSpread(spread),
                worldY + randomSpread(spread),
                worldZ + randomSpread(spread),
                backward.x, backward.y, backward.z
        );

        if (throttle > 0.3f) {
            entity.level().addParticle(
                    ParticleTypes.FLAME,
                    worldX + randomSpread(spread),
                    worldY + randomSpread(spread),
                    worldZ + randomSpread(spread),
                    backward.x * 0.7, backward.y * 0.7, backward.z * 0.7
            );
        }

        if (throttle > 0.6f && entity.level().getRandom().nextFloat() < throttle) {
            entity.level().addParticle(
                    ParticleTypes.SMOKE,
                    worldX + randomSpread(spread * 2),
                    worldY + randomSpread(spread * 2),
                    worldZ + randomSpread(spread * 2),
                    backward.x * 0.2, backward.y * 0.1, backward.z * 0.2
            );
        }
    }

    private Vec3 rotatePoint(double x, double y, double z,
                             float yawRad, float pitchRad, float rollRad) {
        double cosR = Math.cos(rollRad), sinR = Math.sin(rollRad);
        double rx = x * cosR - y * sinR;
        double ry = x * sinR + y * cosR;
        double rz = z;

        double cosP = Math.cos(pitchRad), sinP = Math.sin(pitchRad);
        double px = rx;
        double py = ry * cosP - rz * sinP;
        double pz = ry * sinP + rz * cosP;

        double cosY = Math.cos(-yawRad), sinY = Math.sin(-yawRad);
        double fx = px * cosY + pz * sinY;
        double fy = py;
        double fz = -px * sinY + pz * cosY;

        return new Vec3(fx, fy, fz);
    }

    private Vec3 getBackwardVec(SpaceshipEntity entity) {
        float yawRad = entity.getYRot() * DEG_TO_RAD;
        float pitchRad = entity.getXRot() * DEG_TO_RAD;
        return new Vec3(
                Mth.sin(yawRad) * Mth.cos(pitchRad),
                Mth.sin(pitchRad),
                -Mth.cos(yawRad) * Mth.cos(pitchRad)
        );
    }

    private double randomSpread(float amount) {
        return (Math.random() - 0.5) * 2.0 * amount;
    }
}
