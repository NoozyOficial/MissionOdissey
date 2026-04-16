package com.noozy.missionodyssey.client.planet;

import com.noozy.missionodyssey.MissionOdyssey;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;


public class SaturnGeoRenderer extends GeoObjectRenderer<SaturnGeoObject> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "textures/planets/saturn.png");

    public SaturnGeoRenderer() {
        super(new SaturnGeoModel());
    }

    @Override
    public ResourceLocation getTextureLocation(SaturnGeoObject animatable) {
        return TEXTURE;
    }


    @Override
    public void preRender(PoseStack poseStack, SaturnGeoObject animatable, BakedGeoModel model,
                          net.minecraft.client.renderer.MultiBufferSource bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          int packedColor) {
        this.objectRenderTranslations = new Matrix4f(poseStack.last().pose());

        poseStack.translate(0.0, -1.5, 0.0);
    }


    public void renderPlanet(PoseStack matrices, net.minecraft.client.renderer.MultiBufferSource consumers,
                             float partialTick, int packedLight) {
        render(matrices, SaturnGeoObject.INSTANCE, consumers,
               net.minecraft.client.renderer.RenderType.entityTranslucent(TEXTURE), null, packedLight, partialTick);
    }
}
