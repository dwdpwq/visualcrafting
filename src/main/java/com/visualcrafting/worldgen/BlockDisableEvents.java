package com.visualcrafting.worldgen;

import com.visualcrafting.network.SyncDisabledBlocksPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 运行时 Block 级禁用名单的生命周期挂载：
 * <ul>
 *   <li>ServerAboutToStartEvent：加载名单（早于 Chunky 等预生成任务启动）；</li>
 *   <li>ServerStartedEvent：启动完成后向在线玩家广播一次全量；</li>
 *   <li>PlayerLoggedInEvent：玩家登录时再发一次全量，保证客户端缓存不落后。</li>
 * </ul>
 */
@EventBusSubscriber(modid = "visualcrafting", bus = EventBusSubscriber.Bus.GAME)
public class BlockDisableEvents {

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        BlockDisableRegistry.load(server.getWorldPath(LevelResource.ROOT));
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, new SyncDisabledBlocksPacket(BlockDisableRegistry.snapshotIds()));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncDisabledBlocksPacket(BlockDisableRegistry.snapshotIds()));
        }
    }
}
