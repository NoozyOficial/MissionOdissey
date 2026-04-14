package com.noozy.missionodyssey.client.planet;

import com.noozy.missionodyssey.MissionOdyssey;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MarsGeoModel extends GeoModel<MarsGeoObject> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "geo/mars.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "textures/planets/mars.png");

    @Override
    public ResourceLocation getModelResource(MarsGeoObject obj) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MarsGeoObject obj) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(MarsGeoObject obj) {
        return null;
    }
}
