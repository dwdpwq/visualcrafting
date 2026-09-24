package com.visualcrafting.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S：请求服务端下发当前完整的运行时 Block 级禁用名单。
 * 打开 GUI 时发送一次，保证客户端缓存/按钮状态与服务器一致。
 */
public record RequestDisabledBlocksPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestDisabledBlocksPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("visualcrafting", "request_disabled_blocks"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestDisabledBlocksPacket> STREAM_CODEC =
            StreamCodec.of((buf, pkt) -> {
            }, buf -> new RequestDisabledBlocksPacket());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
