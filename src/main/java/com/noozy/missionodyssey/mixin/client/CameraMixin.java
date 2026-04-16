package com.noozy.missionodyssey.mixin.client;

import com.noozy.missionodyssey.entity.SpaceshipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;


@Mixin(Camera.class)
public class CameraMixin {


    @Unique
    private static final float SHIP_DISTANCE = 14.0f;
    @Unique
    private static final float DEFAULT_DISTANCE = 4.0f;
    @Unique
    private static final float ZOOM_OUT_SPEED = 0.035f;
    @Unique
    private static final float ZOOM_IN_SPEED = 0.06f;


    @Unique
    private static float currentDistance = DEFAULT_DISTANCE;


    @ModifyArg(
            method = "setup",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"
            )
    )
    private float missaoodisseia$modifyCameraDistance(float original) {
        Minecraft client = Minecraft.getInstance();

        boolean onShip = client.player != null
                && client.player.getVehicle() instanceof SpaceshipEntity;

        float target = onShip ? SHIP_DISTANCE : DEFAULT_DISTANCE;


        float speed = (target > currentDistance) ? ZOOM_OUT_SPEED : ZOOM_IN_SPEED;
        currentDistance = Mth.lerp(speed, currentDistance, target);


        if (Math.abs(currentDistance - target) < 0.01f) {
            currentDistance = target;
        }

        return currentDistance;
    }
}
