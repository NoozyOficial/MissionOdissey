package com.noozy.missionodyssey.client.planet;

import com.noozy.missionodyssey.MissionOdyssey;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

public class SunGeoRenderer extends GeoObjectRenderer<SunGeoObject> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "textures/stars/sun.png");

    public SunGeoRenderer() {
        super(new SunGeoModel());
    }

    @Override
    public ResourceLocation getTextureLocation(SunGeoObject animatable) {
        return TEXTURE;
    }

    @Override
    public void preRender(PoseStack poseStack, SunGeoObject animatable, BakedGeoModel model,
                          net.minecraft.client.renderer.MultiBufferSource bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          int packedColor) {
        this.objectRenderTranslations = new Matrix4f(poseStack.last().pose());
        // Centralizar ligeiramente (mesmo offset do Earth)
        poseStack.translate(0.0, -1.5, 0.0);
    }

    public void renderSun(PoseStack matrices, net.minecraft.client.renderer.MultiBufferSource consumers,
                          float partialTick, int packedLight) {
        // Renderiza com entityCutout para que a textura seja opaca e ignorar "sky lighting"
        render(matrices, SunGeoObject.INSTANCE, consumers,
               RenderType.entityCutout(TEXTURE), null, packedLight, partialTick);
    }
}
