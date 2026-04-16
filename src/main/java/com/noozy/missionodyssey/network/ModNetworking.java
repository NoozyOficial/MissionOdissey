package com.noozy.missionodyssey.network;

import com.noozy.missionodyssey.client.WarpEffectHandler;
import com.noozy.missionodyssey.entity.SpaceshipEntity;
import com.noozy.missionodyssey.block.entity.TitaniumBlastFurnaceBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworking {

    public static final byte BIT_FORWARD  = 0b000001;
    public static final byte BIT_BACKWARD = 0b000010;
    public static final byte BIT_LEFT     = 0b000100;
    public static final byte BIT_RIGHT    = 0b001000;
    public static final byte BIT_UP       = 0b010000;
    public static final byte BIT_DOWN     = 0b100000;

    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                ShipInputPayload.TYPE,
                ShipInputPayload.STREAM_CODEC,
                (payload, context) -> {
                    byte inputMask = payload.inputMask();

                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player && player.getVehicle() instanceof SpaceshipEntity ship) {
                            ship.inputForward  = (inputMask & BIT_FORWARD)  != 0;
                            ship.inputBackward = (inputMask & BIT_BACKWARD) != 0;
                            ship.inputLeft     = (inputMask & BIT_LEFT)     != 0;
                            ship.inputRight    = (inputMask & BIT_RIGHT)    != 0;
                            ship.inputUp       = (inputMask & BIT_UP)       != 0;
                            ship.inputDown     = (inputMask & BIT_DOWN)     != 0;
                        }
                    });
                }
        );


        registrar.playToServer(
                TemporalJumpPayload.TYPE,
                TemporalJumpPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.getVehicle() instanceof SpaceshipEntity ship) {
                        ship.requestTemporalJump();
                    }
                })
        );


        registrar.playToClient(
                WarpSyncPayload.TYPE,
                WarpSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist == Dist.CLIENT) {
                        handleWarpSyncClient(payload.cooldownTicks());
                    }
                })
        );


        registrar.playToClient(
                TitaniumBlastFurnaceStatePayload.TYPE,
                TitaniumBlastFurnaceStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist == Dist.CLIENT) {
                        handleBlastFurnaceStateClient(payload);
                    }
                })
        );
    }

    private static void handleBlastFurnaceStateClient(TitaniumBlastFurnaceStatePayload payload) {
        if (Minecraft.getInstance().level != null) {
            BlockEntity be = Minecraft.getInstance().level.getBlockEntity(payload.pos());
            if (be instanceof TitaniumBlastFurnaceBlockEntity furnace) {
                furnace.clientSetState(payload.state(), payload.serverTime());
            }
        }
    }


    private static void handleWarpSyncClient(int ticks) {
        WarpEffectHandler.setCooldown(ticks);
    }
}
