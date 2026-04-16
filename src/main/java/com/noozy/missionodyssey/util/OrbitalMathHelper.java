package com.noozy.missionodyssey.util;

import com.noozy.missionodyssey.registry.ModDimensions;
import net.minecraft.world.phys.Vec3;


public final class OrbitalMathHelper {




    public static final double EARTH_SPEED_MULTIPLIER = 1.0;

    public static final double MARS_SPEED_MULTIPLIER = Math.pow(
            ModDimensions.EARTH_ORBIT_RADIUS / ModDimensions.MARS_ORBIT_RADIUS, 1.5
    );

    public static final double SATURN_SPEED_MULTIPLIER = Math.pow(
            ModDimensions.EARTH_ORBIT_RADIUS / ModDimensions.SATURN_ORBIT_RADIUS, 1.5
    );

    private OrbitalMathHelper() {}




    public static double getCurrentAngle(double gameTime, double startAngle, double speedMultiplier) {
        return startAngle + (gameTime * ModDimensions.GLOBAL_ORBIT_SPEED * speedMultiplier);
    }


    public static Vec3 getOrbitalPosition(double gameTime, double orbitRadius,
                                           double startAngle, double y,
                                           double speedMultiplier) {
        double angle = getCurrentAngle(gameTime, startAngle, speedMultiplier);
        double x = ModDimensions.SUN_X + (orbitRadius * Math.cos(angle));
        double z = ModDimensions.SUN_Z + (orbitRadius * Math.sin(angle));
        return new Vec3(x, y, z);
    }




    public static Vec3 getEarthPosition(double gameTime) {
        return getOrbitalPosition(
                gameTime,
                ModDimensions.EARTH_ORBIT_RADIUS,
                ModDimensions.EARTH_START_ANGLE,
                ModDimensions.EARTH_Y,
                EARTH_SPEED_MULTIPLIER
        );
    }


    public static Vec3 getMarsPosition(double gameTime) {
        return getOrbitalPosition(
                gameTime,
                ModDimensions.MARS_ORBIT_RADIUS,
                ModDimensions.MARS_START_ANGLE,
                ModDimensions.MARS_Y,
                MARS_SPEED_MULTIPLIER
        );
    }


    public static Vec3 getSaturnPosition(double gameTime) {
        return getOrbitalPosition(
                gameTime,
                ModDimensions.SATURN_ORBIT_RADIUS,
                ModDimensions.SATURN_START_ANGLE,
                ModDimensions.SATURN_Y,
                SATURN_SPEED_MULTIPLIER
        );
    }


    public static org.joml.Vector3f getSunDirection(Vec3 planetPos) {
        return new org.joml.Vector3f(
                (float) (ModDimensions.SUN_X - planetPos.x),
                (float) (ModDimensions.SUN_Y - planetPos.y),
                (float) (ModDimensions.SUN_Z - planetPos.z)
        ).normalize();
    }
}
