package com.noozy.missionodyssey.client.planet;

import com.noozy.missionodyssey.MissionOdyssey;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

public class EarthGeoRenderer extends GeoObjectRenderer<EarthGeoObject> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "textures/planets/earth.png");

    public EarthGeoRenderer() {
        super(new EarthGeoModel());
    }

    @Override
    public ResourceLocation getTextureLocation(EarthGeoObject animatable) {
        return TEXTURE;
    }

    @Override
    public void preRender(PoseStack poseStack, EarthGeoObject animatable, BakedGeoModel model,
                          net.minecraft.client.renderer.MultiBufferSource bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          int packedColor) {
        this.objectRenderTranslations = new Matrix4f(poseStack.last().pose());

        poseStack.translate(0.0, -1.5, 0.0);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, EarthGeoObject animatable, GeoBone bone, RenderType renderType, net.minecraft.client.renderer.MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int packedColor) {


        if (bone.getName().equals("clouds")) {
            return;
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, packedColor);
    }

    public void renderPlanet(PoseStack matrices, net.minecraft.client.renderer.MultiBufferSource consumers,
                             float partialTick, int packedLight) {
        render(matrices, EarthGeoObject.INSTANCE, consumers,
               RenderType.entitySolid(TEXTURE), null, packedLight, partialTick);
    }
}
