package com.visualcrafting.network;

import com.visualcrafting.compat.AE2Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * "从 ME 接口取出熔炉卡经验"网络包的服务端入口。
 * <p>
 * 本类在 AE2 缺失时同样会被加载，因此 <b>不得 import 任何 appeng 类型</b>：
 * 实际处理逻辑位于 {@code compat.ae2.Ae2ExpExtraction}，经 {@link AE2Compat} 反射转发。
 */
@EventBusSubscriber(modid = "visualcrafting", bus = EventBusSubscriber.Bus.MOD)
public class FurnaceExpRegistry {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                ExtractFurnaceExpPacket.TYPE,
                ExtractFurnaceExpPacket.STREAM_CODEC,
                FurnaceExpRegistry::handleExtractFurnaceExp);
    }

    private static void handleExtractFurnaceExp(ExtractFurnaceExpPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // AE2 为可选前置：未安装时不处理该网络包
            if (!AE2Compat.isAe2Loaded()) {
                return;
            }
            try {
                ServerPlayer player = (ServerPlayer) context.player();
                BlockPos pos = packet.pos();
                if (player == null || pos == null) {
                    return;
                }
                // 距离校验与接口校验在 Ae2ExpExtraction 内完成
                AE2Compat.handleExtractExp(player, pos);
            } catch (Throwable e) {
                System.err.println("[VC] Failed to extract furnace exp: " + e.getMessage());
            }
        });
    }
}
