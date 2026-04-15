package com.noozy.missionodyssey.client.planet;

import com.noozy.missionodyssey.MissionOdyssey;
import com.noozy.missionodyssey.registry.ModDimensions;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;

//@EventBusSubscriber(modid = MissionOdyssey.MODID, value = Dist.CLIENT)
public class BlackHoleWorldRenderer {

    private static final double MIN_FAKE_DISTANCE = 150.0;

    /**
     * O disco de acreção real é quase perpendicular ao eixo de spin do buraco negro.
     * Para garantir que seja sempre parcialmente visível, inclinamos o plano do disco
     * ~18° em relação ao plano de visualização (como o M87* fotografado pelo EHT).
     *
     * A inclinação é fixa em relação ao mundo (não billboarded), então o observador
     * vê um elipse do disco ao se mover.
     */
    private static final float DISK_TILT_X = 18.0f;   // inclinação no eixo X
    private static final float DISK_TILT_Z =  5.0f;   // leve torção no eixo Z

//    @SubscribeEvent
    public static void onRenderBlackHole(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        ClientLevel world = Minecraft.getInstance().level;
        if (world == null || !world.dimension().equals(ModDimensions.SPACE_KEY)) return;

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack matrices = event.getPoseStack();
        float partialTick = event.getPartialTick().getGameTimeDeltaTicks();
        float time = world.getGameTime() + partialTick;

        // Posição do buraco negro no espaço do mod
        Vec3 bhWorld = new Vec3(
                ModDimensions.BLACK_HOLE_X,
                ModDimensions.BLACK_HOLE_Y,
                ModDimensions.BLACK_HOLE_Z
        );
        Vec3 toBH = bhWorld.subtract(cameraPos);
        double actualDist = toBH.length();
        if (actualDist < 0.1) return;

        // ── Sistema de distância visual (fake distance) ──────────────────────
        Vec3 direction = toBH.normalize();
        int viewChunks = Minecraft.getInstance().options.renderDistance().get();
        double maxFakeDist = Math.max(MIN_FAKE_DISTANCE, viewChunks * 16.0 * 1.8);
        double fakeDistance = Math.max(MIN_FAKE_DISTANCE, Math.min(actualDist, maxFakeDist));

        double angularRadius = Math.atan2(ModDimensions.BLACK_HOLE_RADIUS_BLOCKS, actualDist);
        double fakeRadius    = Math.tan(angularRadius) * fakeDistance;
        double scale = Math.min(
                fakeRadius / ModDimensions.BLACK_HOLE_MODEL_RADIUS,
                (fakeDistance - 0.1) / ModDimensions.BLACK_HOLE_MODEL_RADIUS
        );

        // ── Rotação do disco ao longo do tempo ──────────────────────────────
        // O buraco negro não precisa de rotação "planetária" visível no corpo,
        // mas o disco de acreção gira via shader. Para que a inclinação do disco
        // pareça dinâmica, aplicamos uma rotação lenta no eixo Y (spin do BH).
        float diskSpin = (float) ((time % 72000.0) / 72000.0 * 360.0);

        // ── Pose do buraco negro ─────────────────────────────────────────────
        Vec3 renderPos = direction.scale(fakeDistance);
        matrices.pushPose();

        // Remove rotação de câmera (AFTER_SKY não tem rotação de câmera aplicada)
        matrices.mulPose(new Quaternionf(camera.rotation()).conjugate());
        matrices.translate(renderPos.x, renderPos.y, renderPos.z);

        // Inclina o disco de acreção — esta rotação afeta TUDO (horizon + halo + disco).
        // O event horizon é esférico, então a inclinação não afeta sua silhueta.
        // O disco fica inclinado ~18° em relação ao plano horizontal.
        matrices.mulPose(Axis.XP.rotationDegrees(DISK_TILT_X));
        matrices.mulPose(Axis.ZP.rotationDegrees(DISK_TILT_Z));

        matrices.scale((float) scale, (float) scale, (float) scale);

        // Spin lento adicional (só no eixo Y — o disco vira no plano inclinado)
        matrices.mulPose(Axis.YP.rotationDegrees(diskSpin));

        // ── Renderização: 3 passes via BlackHoleRenderer ─────────────────────
        BlackHoleRenderer.render(matrices, time);

        matrices.popPose();
    }
}
