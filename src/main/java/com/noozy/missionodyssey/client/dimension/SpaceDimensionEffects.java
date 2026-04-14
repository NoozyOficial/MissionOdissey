package com.noozy.missionodyssey.client.dimension;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

public class SpaceDimensionEffects extends DimensionSpecialEffects {

    public SpaceDimensionEffects() {
        super(
            Float.NaN,
            false,
            SkyType.NONE,
            false,
            false
        );
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float sunHeight) {
        return Vec3.ZERO;
    }

    @Override
    public boolean isFoggyAt(int camX, int camZ) {
        return false;
    }
}
