package com.noozy.missionodyssey.network;

import com.noozy.missionodyssey.MissionOdyssey;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;


public record WarpSyncPayload(int cooldownTicks) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<WarpSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "warp_sync"));

    public static final StreamCodec<ByteBuf, WarpSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, WarpSyncPayload::cooldownTicks,
                    WarpSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
