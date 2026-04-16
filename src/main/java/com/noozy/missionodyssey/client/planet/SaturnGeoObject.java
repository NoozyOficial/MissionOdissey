package com.noozy.missionodyssey.client.planet;

import net.minecraft.client.Minecraft;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;


public class SaturnGeoObject implements GeoAnimatable {

    public static final SaturnGeoObject INSTANCE = new SaturnGeoObject();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private SaturnGeoObject() {}

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object o) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getGameTime() : 0;
    }
}
