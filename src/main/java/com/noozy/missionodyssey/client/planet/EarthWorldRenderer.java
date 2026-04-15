package com.noozy.missionodyssey.client.planet;

import com.noozy.missionodyssey.MissionOdyssey;
import com.noozy.missionodyssey.registry.ModDimensions;
import com.noozy.missionodyssey.util.OrbitalMathHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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
public class EarthWorldRenderer {

    private static final double MIN_FAKE_DISTANCE = 150.0;
    private static final EarthGeoRenderer PLANET_RENDERER = new EarthGeoRenderer();

    @SubscribeEvent
    public static void onWorldRenderLast(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }

        ClientLevel world = net.minecraft.client.Minecraft.getInstance().level;
        net.minecraft.client.renderer.MultiBufferSource consumers = net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource();
        if (world == null || consumers == null || !world.dimension().equals(ModDimensions.SPACE_KEY)) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack matrices = event.getPoseStack();
        float partialTick = event.getPartialTick().getGameTimeDeltaTicks();
        float time = world.getGameTime() + partialTick;

        Vec3 earthWorld = OrbitalMathHelper.getEarthPosition(time);
        Vec3 toEarth = earthWorld.subtract(cameraPos);
        double actualDist = toEarth.length();

        if (actualDist < 0.1) return;

        // Lógica de Distância visual no Skybox
        Vec3 direction = toEarth.normalize();
        int viewChunks = net.minecraft.client.Minecraft.getInstance().options.renderDistance().get();
        double maxFakeDist = Math.max(MIN_FAKE_DISTANCE, viewChunks * 16.0 * 1.8);
        double fakeDistance = Math.max(MIN_FAKE_DISTANCE, Math.min(actualDist, maxFakeDist));

        double angularRadius = Math.atan2(ModDimensions.EARTH_RADIUS_BLOCKS, actualDist);
        double fakeRadius = Math.tan(angularRadius) * fakeDistance;
        double scale = Math.min(fakeRadius / ModDimensions.EARTH_MODEL_RADIUS, (fakeDistance - 0.1) / ModDimensions.EARTH_MODEL_RADIUS);

        // Rotação Diária da Terra
        float rotation = (float) ((time % 24000.0) / 24000.0 * 360.0);
        
        // Full bright — shading direcional vem do shader de atmosfera GL puro
        int ambientLight = 0xF000F0;

        // Direção do Sol calculada dinamicamente a partir da posição orbital atual
        Vector3f sunWorldDir = OrbitalMathHelper.getSunDirection(earthWorld);
        
        Vector3f sunDirView = new Vector3f(sunWorldDir);
        RenderSystem.getModelViewMatrix().transformDirection(sunDirView);

        // Renderização do Planeta
        Vec3 renderPos = direction.scale(fakeDistance);
        matrices.pushPose();
        // A PoseStack em AFTER_SKY chega sem rotação de câmera — aplicar manualmente
        // para que translações world-space sejam corretamente projetadas para view-space.
        matrices.mulPose(new Quaternionf(camera.rotation()).conjugate());
        matrices.translate(renderPos.x, renderPos.y, renderPos.z);

        // Inclinação axial da Terra: 23.5 graus
        matrices.mulPose(Axis.ZP.rotationDegrees(-23.5f));
        matrices.scale((float) scale, (float) scale, (float) scale);

        // Rotação diária em torno do próprio eixo Y
        matrices.mulPose(Axis.YP.rotationDegrees(rotation));

        PLANET_RENDERER.renderPlanet(matrices, consumers, partialTick, ambientLight);
        if (consumers instanceof net.minecraft.client.renderer.MultiBufferSource.BufferSource immediate) {
             immediate.endBatch();
        }

        // Renderiza o shader procedimental de Nuvens Pixeladas por cima do modelo do planera
        EarthCloudsRenderer.render(matrices, sunDirView, time);
        
        // Renderiza a Atmosfera por cima das nuvens e do planeta
        EarthAtmosphereRenderer.render(matrices, sunDirView);

        matrices.popPose();
    }
}
