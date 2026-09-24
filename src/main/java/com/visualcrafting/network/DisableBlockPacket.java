package com.visualcrafting.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S：请求将某个方块加入/移出运行时 Block 级禁用名单。
 * disable=true 加入（世界生成替换为空气），false 移出。
 */
public record DisableBlockPacket(BlockPos pos, ResourceLocation blockId, boolean disable) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DisableBlockPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("visualcrafting", "disable_block"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DisableBlockPacket> STREAM_CODEC =
            StreamCodec.of(DisableBlockPacket::encode, DisableBlockPacket::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buf, DisableBlockPacket packet) {
        buf.writeBlockPos(packet.pos);
        buf.writeResourceLocation(packet.blockId);
        buf.writeBoolean(packet.disable);
    }

    private static DisableBlockPacket decode(RegistryFriendlyByteBuf buf) {
        return new DisableBlockPacket(buf.readBlockPos(), buf.readResourceLocation(), buf.readBoolean());
    }
}
