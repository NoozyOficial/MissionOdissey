package com.noozy.missionodyssey.server;

import com.noozy.missionodyssey.MissionOdyssey;
import com.noozy.missionodyssey.registry.ModDimensions;
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

        for (ServerPlayer player : new ArrayList<>(world.players())) {
            if (isOverworld) {
                if (player.getY() > 320.0) {
                    ServerLevel spaceWorld = world.getServer().getLevel(ModDimensions.SPACE_KEY);
                    if (spaceWorld != null) {
                        teleportWithShip(player, spaceWorld,
                                ModDimensions.EARTH_X - 450.0, ModDimensions.EARTH_Y, ModDimensions.EARTH_Z);
                    }
                }
            } else if (isSpace) {
                Vec3 marsCenter = new Vec3(ModDimensions.MARS_X, ModDimensions.MARS_Y, ModDimensions.MARS_Z);
                double distMars = player.position().distanceTo(marsCenter);

                if (distMars < ModDimensions.MARS_RADIUS_BLOCKS + 10.0) {
                    ServerLevel marsWorld = world.getServer().getLevel(ModDimensions.MARS_KEY);
                    if (marsWorld != null) {
                        teleportWithShip(player, marsWorld, player.getX(), 250.0, player.getZ());
                    }
                }

                Vec3 earthCenter = new Vec3(ModDimensions.EARTH_X, ModDimensions.EARTH_Y, ModDimensions.EARTH_Z);
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
                        teleportWithShip(player, spaceWorld,
                                ModDimensions.MARS_X - ModDimensions.MARS_RADIUS_BLOCKS - 150.0,
                                ModDimensions.MARS_Y, ModDimensions.MARS_Z);
                    }
                }
            }
        }
    }

    /**
     * Teleports a player (and their spaceship vehicle, if any) to {@code destLevel}
     * at the given coordinates, preserving the ship's velocity and orientation.
     *
     * <p>Order of operations:
     * <ol>
     *   <li>If the player is riding a vehicle, save its velocity + rotation.</li>
     *   <li>Dismount the player so the vehicle becomes an independent entity.</li>
     *   <li>Send the vehicle to the destination via {@link Entity#changeDimension},
     *       restoring the saved velocity immediately after arrival.</li>
     *   <li>Teleport the player to the same position.</li>
     *   <li>Re-mount the player onto the vehicle.</li>
     * </ol>
     */
    private static void teleportWithShip(ServerPlayer player, ServerLevel destLevel,
                                         double destX, double destY, double destZ) {
        Entity vehicle = player.getVehicle();

        if (vehicle == null) {
            // No ship — plain player teleport.
            player.teleportTo(destLevel, destX, destY, destZ, player.getYRot(), player.getXRot());
            return;
        }

        // ── 1. Snapshot ship state before anything changes ───────────────────
        Vec3  savedVelocity = vehicle.getDeltaMovement();
        float savedYRot     = vehicle.getYRot();
        float savedXRot     = vehicle.getXRot();

        // ── 2. Dismount so the ship is no longer a passenger container ────────
        player.stopRiding();

        // ── 3. Move the ship to the destination dimension + position ──────────
        //      DimensionTransition carries position, velocity and rotation into
        //      the new level; this is the correct 1.21.1 cross-dimension API.
        DimensionTransition shipTransition = new DimensionTransition(
                destLevel,
                new Vec3(destX, destY, destZ),
                savedVelocity,
                savedYRot,
                savedXRot,
                DimensionTransition.DO_NOTHING
        );
        Entity arrivedVehicle = vehicle.changeDimension(shipTransition);

        // ── 4. Teleport the player to the same spot ───────────────────────────
        player.teleportTo(destLevel, destX, destY, destZ, savedYRot, savedXRot);

        // ── 5. Re-mount the player on the ship that arrived ───────────────────
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
