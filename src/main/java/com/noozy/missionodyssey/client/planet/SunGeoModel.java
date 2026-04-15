package com.noozy.missionodyssey.client.planet;

import com.noozy.missionodyssey.MissionOdyssey;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SunGeoModel extends GeoModel<SunGeoObject> {

    private static final ResourceLocation MODEL_ID   = ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "geo/sun.geo.json");
    private static final ResourceLocation TEXTURE_ID = ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "textures/stars/sun.png");

    @Override
    public ResourceLocation getModelResource(SunGeoObject animatable) {
        return MODEL_ID;
    }

    @Override
    public ResourceLocation getTextureResource(SunGeoObject animatable) {
        return TEXTURE_ID;
    }

    @Override
    public ResourceLocation getAnimationResource(SunGeoObject animatable) {
        return ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "animations/empty.json");
    }
}
