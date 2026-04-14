package com.noozy.missionodyssey.network;

import com.noozy.missionodyssey.entity.SpaceshipEntity;
import net.minecraft.server.level.ServerPlayer;
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
    }
}
