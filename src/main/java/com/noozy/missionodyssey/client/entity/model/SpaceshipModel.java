package com.noozy.missionodyssey.client.entity.model;

import com.noozy.missionodyssey.MissionOdyssey;
import com.noozy.missionodyssey.entity.SpaceshipEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SpaceshipModel extends GeoModel<SpaceshipEntity> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "geo/dasher.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "textures/entity/dasher.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "animations/dasher.animation.json");

    @Override
    public ResourceLocation getModelResource(SpaceshipEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SpaceshipEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SpaceshipEntity entity) {
        return ANIMATION;
    }
}
