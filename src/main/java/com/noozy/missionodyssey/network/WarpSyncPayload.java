package com.noozy.missionodyssey.network;

import com.noozy.missionodyssey.MissionOdyssey;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent by the server to the client after a temporal jump fires.
 * Carries the remaining cooldown so the client HUD stays in sync.
 */
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
