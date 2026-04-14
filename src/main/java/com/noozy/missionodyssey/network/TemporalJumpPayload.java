package com.noozy.missionodyssey.network;

import com.noozy.missionodyssey.MissionOdyssey;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent by the client when the player presses the temporal jump key.
 * The server validates the request (in ship + cooldown ready) and queues the jump.
 */
public record TemporalJumpPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TemporalJumpPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "temporal_jump"));

    public static final StreamCodec<ByteBuf, TemporalJumpPayload> STREAM_CODEC =
            StreamCodec.of((buf, value) -> {}, buf -> new TemporalJumpPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
