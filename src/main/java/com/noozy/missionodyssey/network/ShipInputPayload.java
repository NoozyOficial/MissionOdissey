package com.noozy.missionodyssey.network;

import com.noozy.missionodyssey.MissionOdyssey;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ShipInputPayload(byte inputMask) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ShipInputPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "ship_input"));

    public static final StreamCodec<ByteBuf, ShipInputPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, value) -> buf.writeByte(value.inputMask()),
                    buf -> new ShipInputPayload(buf.readByte())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
