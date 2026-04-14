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

/**
 * Renderer GeckoLib de Saturno.
 * Estende GeoObjectRenderer para renderização standalone (sem entidade).
 * Sobrescreve preRender para remover o offset de bloco (0.5, 0.51, 0.5)
 * e centralizar o modelo corretamente na posição do planeta.
 */
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

    /**
     * Sobrescreve o preRender do GeoObjectRenderer para:
     * 1. Remover o offset (0.5, 0.51, 0.5) feito para block entities
     * 2. Centralizar o modelo: o cubo vai de Y=0 a Y=3 blocos,
     *    então translacionamos -1.5 para centralizar em Y
     */
    @Override
    public void preRender(PoseStack poseStack, SaturnGeoObject animatable, BakedGeoModel model,
                          net.minecraft.client.renderer.MultiBufferSource bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          int packedColor) {
        this.objectRenderTranslations = new Matrix4f(poseStack.last().pose());
        // Centralizar o modelo: cubo de 3 blocos de altura, pivot no centro em Y
        poseStack.translate(0.0, -1.5, 0.0);
    }

    /**
     * Ponto de entrada para renderizar Saturno.
     * As transformações de posição, escala e rotação devem ser aplicadas
     * no PoseStack ANTES de chamar este método.
     */
    public void renderPlanet(PoseStack matrices, net.minecraft.client.renderer.MultiBufferSource consumers,
                             float partialTick, int packedLight) {
        render(matrices, SaturnGeoObject.INSTANCE, consumers,
               net.minecraft.client.renderer.RenderType.entityTranslucent(TEXTURE), null, packedLight, partialTick);
    }
}
