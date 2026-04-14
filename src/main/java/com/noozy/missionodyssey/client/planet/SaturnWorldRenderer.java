package com.noozy.missionodyssey.client.planet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.noozy.missionodyssey.MissionOdyssey;
import com.noozy.missionodyssey.registry.ModDimensions;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import com.mojang.math.Axis;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import foundry.veil.api.client.render.light.data.DirectionalLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import foundry.veil.api.client.render.VeilRenderSystem;

@EventBusSubscriber(modid = MissionOdyssey.MODID, value = Dist.CLIENT)
public class SaturnWorldRenderer {

    private static final double MIN_FAKE_DISTANCE = 200.0;
    private static final SaturnGeoRenderer PLANET_RENDERER = new SaturnGeoRenderer();
    private static LightRenderHandle<DirectionalLightData> sunLightHandle;

    /**
     * HIGHEST priority: skybox sempre roda antes de qualquer planeta.
     * Earth/Mars/Saturn usam NORMAL priority, então o skybox nunca
     * sobrescreve os planetas já desenhados.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderSkybox(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        ClientLevel world = Minecraft.getInstance().level;
        if (world == null || !world.dimension().equals(ModDimensions.SPACE_KEY)) return;

        float time = world.getGameTime() + event.getPartialTick().getGameTimeDeltaTicks();
        SpaceSkyboxRenderer.render(time, event.getCamera());
    }

    /** Planeta Saturno — roda em prioridade NORMAL, depois do skybox. */
    @SubscribeEvent
    public static void onRenderSaturn(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        ClientLevel world = Minecraft.getInstance().level;
        MultiBufferSource consumers = Minecraft.getInstance().renderBuffers().bufferSource();

        if (world == null || !world.dimension().equals(ModDimensions.SPACE_KEY)) {
            if (sunLightHandle != null) {
                sunLightHandle.free();
                sunLightHandle = null;
            }
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack matrices = event.getPoseStack();
        float partialTick = event.getPartialTick().getGameTimeDeltaTicks();
        float time = world.getGameTime() + partialTick;

        Vec3 saturnWorld = new Vec3(ModDimensions.SATURN_X, ModDimensions.SATURN_Y, ModDimensions.SATURN_Z);
        Vec3 toSaturn = saturnWorld.subtract(cameraPos);
        double actualDist = toSaturn.length();
        if (actualDist < 0.1) return;

        Vec3 direction = toSaturn.normalize();
        int viewChunks = Minecraft.getInstance().options.renderDistance().get();
        double maxFakeDist = Math.max(MIN_FAKE_DISTANCE, viewChunks * 16.0 * 1.8);
        double fakeDistance = Math.max(MIN_FAKE_DISTANCE, Math.min(actualDist, maxFakeDist));

        double angularRadius = Math.atan2(ModDimensions.SATURN_RADIUS_BLOCKS, actualDist);
        double fakeRadius = Math.tan(angularRadius) * fakeDistance;
        double scale = Math.min(fakeRadius / ModDimensions.SATURN_MODEL_RADIUS, (fakeDistance - 0.1) / ModDimensions.SATURN_MODEL_RADIUS);

        float rotation = (float) ((time % 8750.0) / 8750.0 * 360.0);
        // Full bright — o shading direcional vem da atmosfera (shader GL puro)
        int packedLight = 0xF000F0;
        int maxLight    = 0xF000F0;

        Vector3f sunWorldDir = new Vector3f(
                (float) -ModDimensions.SATURN_X,
                (float) (64.0 - ModDimensions.SATURN_Y),
                (float) -ModDimensions.SATURN_Z
        ).normalize();

        Vector3f sunDirView = new Vector3f(sunWorldDir);
        RenderSystem.getModelViewMatrix().transformDirection(sunDirView);

        if (sunLightHandle == null) {
            sunLightHandle = VeilRenderSystem.renderer().getLightRenderer().addLight(new DirectionalLightData());
            sunLightHandle.getLightData().setColor(1.0f, 0.88f, 0.50f).setBrightness(1.25f);
        }
        sunLightHandle.getLightData().setDirection(sunDirView);

        float closeThreshold = (float) SaturnRingRenderer.CLOSE_THRESHOLD;
        float fadeRange = 400f;
        float ringAlpha;
        if (actualDist >= closeThreshold) {
            ringAlpha = 1.0f;
        } else if (actualDist <= closeThreshold - fadeRange) {
            ringAlpha = 0.0f;
        } else {
            float t = (float) ((actualDist - (closeThreshold - fadeRange)) / fadeRange);
            ringAlpha = t * t * (3f - 2f * t);
        }

        Vec3 renderPos = direction.scale(fakeDistance);
        matrices.pushPose();
        matrices.mulPose(new Quaternionf(camera.rotation()).conjugate());
        matrices.translate(renderPos.x, renderPos.y, renderPos.z);

        matrices.mulPose(Axis.ZP.rotationDegrees(-26.73f));
        matrices.scale((float) scale, (float) scale, (float) scale);

        if (ringAlpha > 0.001f) {
            SaturnRingRenderer.renderFarRings(matrices, consumers, maxLight, ringAlpha, time, sunWorldDir);
        }

        matrices.mulPose(Axis.YP.rotationDegrees(rotation));

        PLANET_RENDERER.renderPlanet(matrices, consumers, partialTick, packedLight);
        if (consumers instanceof MultiBufferSource.BufferSource immediate) {
            immediate.endBatch();
        }

        SaturnAtmosphereRenderer.render(matrices, sunDirView);
        matrices.popPose();

        if (actualDist < SaturnRingRenderer.CLOSE_THRESHOLD) {
            SaturnRingRenderer.renderCloseRings(matrices, consumers, saturnWorld, cameraPos, time, maxLight, sunWorldDir);
        }
    }
}
