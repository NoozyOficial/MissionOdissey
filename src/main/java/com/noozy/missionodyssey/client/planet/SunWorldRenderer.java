package com.noozy.missionodyssey.client.planet;

import com.noozy.missionodyssey.MissionOdyssey;
import com.noozy.missionodyssey.registry.ModDimensions;
import com.mojang.blaze3d.vertex.PoseStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;


@EventBusSubscriber(modid = MissionOdyssey.MODID, value = Dist.CLIENT)
public class SunWorldRenderer {

    private static final double MIN_FAKE_DISTANCE = 150.0;

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderSun(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        ClientLevel world = Minecraft.getInstance().level;
        if (world == null || !world.dimension().equals(ModDimensions.SPACE_KEY)) return;

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack matrices = event.getPoseStack();
        float partialTick = event.getPartialTick().getGameTimeDeltaTicks();
        float time = world.getGameTime() + partialTick;


        Vec3 sunWorld = new Vec3(ModDimensions.SUN_X, ModDimensions.SUN_Y, ModDimensions.SUN_Z);
        Vec3 toSun    = sunWorld.subtract(cameraPos);
        double actualDist = toSun.length();

        if (actualDist < 0.1) return;


        Vec3 direction = toSun.normalize();
        int viewChunks = Minecraft.getInstance().options.renderDistance().get();
        double maxFakeDist = Math.max(MIN_FAKE_DISTANCE, viewChunks * 16.0 * 1.8);
        double fakeDistance = Math.max(MIN_FAKE_DISTANCE, Math.min(actualDist, maxFakeDist));

        double angularRadius = Math.atan2(ModDimensions.SUN_RADIUS_BLOCKS, actualDist);
        double fakeRadius    = Math.tan(angularRadius) * fakeDistance;
        double scale = Math.min(fakeRadius / ModDimensions.SUN_MODEL_RADIUS,
                                (fakeDistance - 0.1) / ModDimensions.SUN_MODEL_RADIUS);


        Vec3 renderPos = direction.scale(fakeDistance);
        matrices.pushPose();


        matrices.mulPose(new Quaternionf(camera.rotation()).conjugate());
        matrices.translate(renderPos.x, renderPos.y, renderPos.z);
        matrices.scale((float) scale, (float) scale, (float) scale);


        SunBodyRenderer.render(matrices);


        SunAtmosphereRenderer.render(matrices, time);

        matrices.popPose();
    }
}

