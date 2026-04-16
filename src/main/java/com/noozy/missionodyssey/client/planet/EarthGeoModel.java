package com.noozy.missionodyssey.client.planet;

import com.noozy.missionodyssey.MissionOdyssey;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class EarthGeoModel extends GeoModel<EarthGeoObject> {

    private static final ResourceLocation MODEL_ID = ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "geo/earth.geo.json");
    private static final ResourceLocation TEXTURE_ID = ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "textures/planets/earth.png");

    @Override
    public ResourceLocation getModelResource(EarthGeoObject animatable) {
        return MODEL_ID;
    }

    @Override
    public ResourceLocation getTextureResource(EarthGeoObject animatable) {
        return TEXTURE_ID;
    }

    @Override
    public ResourceLocation getAnimationResource(EarthGeoObject animatable) {

        return ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "animations/empty.json");
    }
}
