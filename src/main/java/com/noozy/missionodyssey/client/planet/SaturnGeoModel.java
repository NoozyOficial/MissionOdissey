package com.noozy.missionodyssey.client.planet;

import com.noozy.missionodyssey.MissionOdyssey;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SaturnGeoModel extends GeoModel<SaturnGeoObject> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "geo/saturn.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "textures/planets/saturn.png");

    @Override
    public ResourceLocation getModelResource(SaturnGeoObject obj) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SaturnGeoObject obj) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SaturnGeoObject obj) {
        return null; // Sem animações GeckoLib — rotação aplicada no renderer
    }
}
