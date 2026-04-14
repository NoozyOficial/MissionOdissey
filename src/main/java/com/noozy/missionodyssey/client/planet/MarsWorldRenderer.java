package com.noozy.missionodyssey.client.planet;

import com.noozy.missionodyssey.MissionOdyssey;
import com.noozy.missionodyssey.registry.ModDimensions;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.LightTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import com.mojang.math.Axis;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@EventBusSubscriber(modid = MissionOdyssey.MODID, value = Dist.CLIENT)
public class MarsWorldRenderer {

    private static final double MIN_FAKE_DISTANCE = 150.0;
    private static final MarsGeoRenderer PLANET_RENDERER = new MarsGeoRenderer();

    @SubscribeEvent
    public static void onWorldRenderLast(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }

        ClientLevel world = Minecraft.getInstance().level;
        MultiBufferSource consumers = Minecraft.getInstance().renderBuffers().bufferSource();
        if (world == null || consumers == null || !world.dimension().equals(ModDimensions.SPACE_KEY)) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack matrices = event.getPoseStack();
        float partialTick = event.getPartialTick().getGameTimeDeltaTicks();
        float time = world.getGameTime() + partialTick;

        Vec3 marsWorld = new Vec3(ModDimensions.MARS_X, ModDimensions.MARS_Y, ModDimensions.MARS_Z);
        Vec3 toMars = marsWorld.subtract(cameraPos);
        double actualDist = toMars.length();

        if (actualDist < 0.1) return;

        // Lógica de Distância visual no Skybox
        Vec3 direction = toMars.normalize();
        int viewChunks = Minecraft.getInstance().options.renderDistance().get();
        double maxFakeDist = Math.max(MIN_FAKE_DISTANCE, viewChunks * 16.0 * 1.8);
        double fakeDistance = Math.max(MIN_FAKE_DISTANCE, Math.min(actualDist, maxFakeDist));

        double angularRadius = Math.atan2(ModDimensions.MARS_RADIUS_BLOCKS, actualDist);
        double fakeRadius = Math.tan(angularRadius) * fakeDistance;
        double scale = Math.min(fakeRadius / ModDimensions.MARS_MODEL_RADIUS, (fakeDistance - 0.1) / ModDimensions.MARS_MODEL_RADIUS);

        // Rotação Diária (Marte vira parecido com a Terra)
        float rotation = (float) ((time % 7500.0) / 7500.0 * 360.0);
        
        // Full bright — shading direcional vem do shader de atmosfera GL puro
        int ambientLight = 0xF000F0;

        Vector3f sunWorldDir = new Vector3f(
                (float) -ModDimensions.MARS_X,
                (float) (64.0 - ModDimensions.MARS_Y),
                (float) -ModDimensions.MARS_Z
        ).normalize();
        
        Vector3f sunDirView = new Vector3f(sunWorldDir);
        RenderSystem.getModelViewMatrix().transformDirection(sunDirView);

        // Renderização do Planeta
        Vec3 renderPos = direction.scale(fakeDistance);
        matrices.pushPose();
        matrices.mulPose(new Quaternionf(camera.rotation()).conjugate());
        matrices.translate(renderPos.x, renderPos.y, renderPos.z);

        // Inclinação axial de Marte é ~25.19 graus
        matrices.mulPose(Axis.ZP.rotationDegrees(-25.19f));
        matrices.scale((float) scale, (float) scale, (float) scale);

        // Rotação diária em torno do próprio eixo Y
        matrices.mulPose(Axis.YP.rotationDegrees(rotation));

        PLANET_RENDERER.renderPlanet(matrices, consumers, partialTick, ambientLight);
        if (consumers instanceof MultiBufferSource.BufferSource immediate) {
            immediate.endBatch();
        }

        MarsAtmosphereRenderer.render(matrices, sunDirView);

        matrices.popPose();
    }
}
