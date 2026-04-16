package com.noozy.missionodyssey.server;

import com.noozy.missionodyssey.MissionOdyssey;
import com.noozy.missionodyssey.registry.ModDimensions;
import com.noozy.missionodyssey.util.OrbitalMathHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;

@EventBusSubscriber(modid = MissionOdyssey.MODID)
public class TeleportHandler {

    private static final ResourceLocation GRAVITY_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "mars_gravity");

    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel world)) return;

        boolean isSpace = world.dimension().equals(ModDimensions.SPACE_KEY);
        boolean isMars = world.dimension().equals(ModDimensions.MARS_KEY);
        boolean isOverworld = world.dimension().equals(Level.OVERWORLD);

        if (!isSpace && !isMars && !isOverworld) return;


        long gameTime = world.getGameTime();

        for (ServerPlayer player : new ArrayList<>(world.players())) {
            if (isOverworld) {
                if (player.getY() > 320.0) {
                    ServerLevel spaceWorld = world.getServer().getLevel(ModDimensions.SPACE_KEY);
                    if (spaceWorld != null) {
                        Vec3 earthPos = OrbitalMathHelper.getEarthPosition(gameTime);
                        teleportWithShip(player, spaceWorld,
                                earthPos.x - 450.0, earthPos.y, earthPos.z);
                    }
                }
            } else if (isSpace) {
                Vec3 marsCenter = OrbitalMathHelper.getMarsPosition(gameTime);
                double distMars = player.position().distanceTo(marsCenter);

                if (distMars < ModDimensions.MARS_RADIUS_BLOCKS + 10.0) {
                    ServerLevel marsWorld = world.getServer().getLevel(ModDimensions.MARS_KEY);
                    if (marsWorld != null) {
                        teleportWithShip(player, marsWorld, player.getX(), 250.0, player.getZ());
                    }
                }

                Vec3 earthCenter = OrbitalMathHelper.getEarthPosition(gameTime);
                double distEarth = player.position().distanceTo(earthCenter);

                if (distEarth < ModDimensions.EARTH_RADIUS_BLOCKS + 15.0) {
                    ServerLevel overworld = world.getServer().getLevel(Level.OVERWORLD);
                    if (overworld != null) {
                        teleportWithShip(player, overworld, player.getX(), 300.0, player.getZ());
                    }
                }
            } else if (isMars) {
                applyMarsGravity(player);

                if (player.getY() > 280.0) {
                    ServerLevel spaceWorld = world.getServer().getLevel(ModDimensions.SPACE_KEY);
                    if (spaceWorld != null) {
                        removeMarsGravity(player);
                        Vec3 marsPos = OrbitalMathHelper.getMarsPosition(gameTime);
                        teleportWithShip(player, spaceWorld,
                                marsPos.x - ModDimensions.MARS_RADIUS_BLOCKS - 150.0,
                                marsPos.y, marsPos.z);
                    }
                }
            }
        }
    }


    private static void teleportWithShip(ServerPlayer player, ServerLevel destLevel,
                                         double destX, double destY, double destZ) {
        Entity vehicle = player.getVehicle();

        if (vehicle == null) {

            player.teleportTo(destLevel, destX, destY, destZ, player.getYRot(), player.getXRot());
            return;
        }


        Vec3  savedVelocity = vehicle.getDeltaMovement();
        float savedYRot     = vehicle.getYRot();
        float savedXRot     = vehicle.getXRot();


        player.stopRiding();




        DimensionTransition shipTransition = new DimensionTransition(
                destLevel,
                new Vec3(destX, destY, destZ),
                savedVelocity,
                savedYRot,
                savedXRot,
                DimensionTransition.DO_NOTHING
        );
        Entity arrivedVehicle = vehicle.changeDimension(shipTransition);


        player.teleportTo(destLevel, destX, destY, destZ, savedYRot, savedXRot);


        if (arrivedVehicle != null && !arrivedVehicle.isRemoved()) {
            player.startRiding(arrivedVehicle, true);
        }
    }

    private static void applyMarsGravity(ServerPlayer player) {
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity != null) {
            if (!gravity.hasModifier(GRAVITY_MODIFIER_ID)) {
                gravity.addPermanentModifier(new AttributeModifier(
                        GRAVITY_MODIFIER_ID, -0.62, AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ));
            }
        }
    }

    private static void removeMarsGravity(ServerPlayer player) {
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity != null) {
            gravity.removeModifier(GRAVITY_MODIFIER_ID);
        }
    }
}
