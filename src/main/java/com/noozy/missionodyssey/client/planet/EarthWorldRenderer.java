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


        Vec3 direction = toEarth.normalize();
        int viewChunks = net.minecraft.client.Minecraft.getInstance().options.renderDistance().get();
        double maxFakeDist = Math.max(MIN_FAKE_DISTANCE, viewChunks * 16.0 * 1.8);
        double fakeDistance = Math.max(MIN_FAKE_DISTANCE, Math.min(actualDist, maxFakeDist));

        double angularRadius = Math.atan2(ModDimensions.EARTH_RADIUS_BLOCKS, actualDist);
        double fakeRadius = Math.tan(angularRadius) * fakeDistance;
        double scale = Math.min(fakeRadius / ModDimensions.EARTH_MODEL_RADIUS, (fakeDistance - 0.1) / ModDimensions.EARTH_MODEL_RADIUS);


        float rotation = (float) ((time % 24000.0) / 24000.0 * 360.0);


        int ambientLight = 0xF000F0;


        Vector3f sunWorldDir = OrbitalMathHelper.getSunDirection(earthWorld);

        Vector3f sunDirView = new Vector3f(sunWorldDir);
        RenderSystem.getModelViewMatrix().transformDirection(sunDirView);


        Vec3 renderPos = direction.scale(fakeDistance);
        matrices.pushPose();


        matrices.mulPose(new Quaternionf(camera.rotation()).conjugate());
        matrices.translate(renderPos.x, renderPos.y, renderPos.z);


        matrices.mulPose(Axis.ZP.rotationDegrees(-23.5f));
        matrices.scale((float) scale, (float) scale, (float) scale);


        matrices.mulPose(Axis.YP.rotationDegrees(rotation));

        PLANET_RENDERER.renderPlanet(matrices, consumers, partialTick, ambientLight);
        if (consumers instanceof net.minecraft.client.renderer.MultiBufferSource.BufferSource immediate) {
             immediate.endBatch();
        }


        EarthCloudsRenderer.render(matrices, sunDirView, time);


        EarthAtmosphereRenderer.render(matrices, sunDirView);

        matrices.popPose();
    }
}
