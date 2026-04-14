package com.noozy.missionodyssey.registry;

import com.noozy.missionodyssey.MissionOdyssey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class ModDimensions {

    public static final ResourceKey<Level> SPACE_KEY = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "space")
    );

    public static final ResourceKey<Level> MARS_KEY = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "mars")
    );

    public static final double SATURN_X = 85_000.0;
    public static final double SATURN_Y = 64.0;
    public static final double SATURN_Z = 0.0;
    public static final double SATURN_RADIUS_BLOCKS = 500.0;
    public static final double SATURN_MODEL_RADIUS = 1.5;

    public static final double MARS_X = 15_000.0;
    public static final double MARS_Y = 64.0;
    public static final double MARS_Z = 0.0;
    public static final double MARS_RADIUS_BLOCKS = 200.0;
    public static final double MARS_MODEL_RADIUS = 1.5;

    public static final double EARTH_X = 0.0;
    public static final double EARTH_Y = 64.0;
    public static final double EARTH_Z = 0.0;
    public static final double EARTH_RADIUS_BLOCKS = 300.0;
    public static final double EARTH_MODEL_RADIUS = 1.5;
}
