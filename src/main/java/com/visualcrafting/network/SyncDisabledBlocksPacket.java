package com.visualcrafting.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C：向客户端同步完整的运行时 Block 级禁用名单（全量）。
 * 时机：玩家登录、名单变化后广播、打开 GUI 时请求下发。
 * 同时充当 DisableBlockPacket 的处理回执，客户端收到即代表服务端已采纳。
 */
public record SyncDisabledBlocksPacket(List<ResourceLocation> disabledBlocks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncDisabledBlocksPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("visualcrafting", "sync_disabled_blocks"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncDisabledBlocksPacket> STREAM_CODEC =
            StreamCodec.of(SyncDisabledBlocksPacket::encode, SyncDisabledBlocksPacket::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buf, SyncDisabledBlocksPacket packet) {
        buf.writeVarInt(packet.disabledBlocks.size());
        for (ResourceLocation id : packet.disabledBlocks) {
            buf.writeResourceLocation(id);
        }
    }

    private static SyncDisabledBlocksPacket decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<ResourceLocation> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ids.add(buf.readResourceLocation());
        }
        return new SyncDisabledBlocksPacket(ids);
    }
}
