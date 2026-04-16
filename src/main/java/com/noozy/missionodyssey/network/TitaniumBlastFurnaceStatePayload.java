package com.noozy.missionodyssey.network;

import com.noozy.missionodyssey.MissionOdyssey;
import com.noozy.missionodyssey.block.entity.TitaniumBlastFurnaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TitaniumBlastFurnaceStatePayload(BlockPos pos, TitaniumBlastFurnaceBlockEntity.State state, long serverTime) implements CustomPacketPayload {
    public static final Type<TitaniumBlastFurnaceStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "titanium_blast_furnace_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TitaniumBlastFurnaceStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos());
                buf.writeEnum(payload.state());
                buf.writeLong(payload.serverTime());
            },
            buf -> new TitaniumBlastFurnaceStatePayload(buf.readBlockPos(), buf.readEnum(TitaniumBlastFurnaceBlockEntity.State.class), buf.readLong())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
