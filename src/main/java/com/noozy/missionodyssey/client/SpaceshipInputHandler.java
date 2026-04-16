package com.noozy.missionodyssey.client;

import com.noozy.missionodyssey.MissionOdyssey;
import com.noozy.missionodyssey.entity.SpaceshipEntity;
import com.noozy.missionodyssey.network.ModNetworking;
import com.noozy.missionodyssey.network.ShipInputPayload;
import com.noozy.missionodyssey.network.TemporalJumpPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = MissionOdyssey.MODID, value = Dist.CLIENT)
public class SpaceshipInputHandler {

    private static byte lastSentMask = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (!(client.player.getVehicle() instanceof SpaceshipEntity ship)) {
            lastSentMask = 0;
            return;
        }

        Options options = client.options;

        boolean forward  = options.keyUp.isDown();
        boolean backward = options.keyDown.isDown();
        boolean left     = options.keyLeft.isDown();
        boolean right    = options.keyRight.isDown();
        boolean up       = options.keyJump.isDown();
        boolean down     = options.keyShift.isDown();

        ship.inputForward  = forward;
        ship.inputBackward = backward;
        ship.inputLeft     = left;
        ship.inputRight    = right;
        ship.inputUp       = up;
        ship.inputDown     = down;

        byte currentMask = 0;
        if (forward)  currentMask |= ModNetworking.BIT_FORWARD;
        if (backward) currentMask |= ModNetworking.BIT_BACKWARD;
        if (left)     currentMask |= ModNetworking.BIT_LEFT;
        if (right)    currentMask |= ModNetworking.BIT_RIGHT;
        if (up)       currentMask |= ModNetworking.BIT_UP;
        if (down)     currentMask |= ModNetworking.BIT_DOWN;

        if (currentMask != lastSentMask) {
            PacketDistributor.sendToServer(new ShipInputPayload(currentMask));
            lastSentMask = currentMask;
        }


        if (ModKeybindings.TEMPORAL_JUMP.consumeClick()) {
            if (ship.isJumpReady() && !WarpEffectHandler.isActive()) {
                PacketDistributor.sendToServer(new TemporalJumpPayload());
                WarpEffectHandler.startWarp();
            }
        }
    }
}
