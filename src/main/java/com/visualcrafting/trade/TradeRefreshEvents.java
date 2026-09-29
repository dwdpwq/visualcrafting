package com.visualcrafting.trade;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 交易删除后的“静默刷新”生命周期挂载。
 *
 * 现状：删除交易（写盘）后需手动执行 /reload（数据包重载）才会重新派发
 * VillagerTradesEvent，由 VisualCraftingTradeHandler 重新注入 override/custom
 * 交易，使已删除的 override 不再注入（原版交易恢复）。
 *
 * 为避免删除后立即全服 reload 打扰在线玩家，这里采用：
 *   1. 删除成功后由 handleDeleteTrade 调用 {@link #markPending()} 置位内存标记；
 *   2. 下一位玩家登录（PlayerLoggedInEvent）时静默执行一次 /reload
 *      （withSuppressedOutput 抑制命令反馈），随后清除标记；
 *   3. 单机“再次进入游戏”= 服务端重启，VillagerTradesEvent 在启动时自然重新
 *      派发，删除已自动生效；本类仅兜底确保待生效标记被消费。
 */
@EventBusSubscriber(modid = "visualcrafting", bus = EventBusSubscriber.Bus.GAME)
public final class TradeRefreshEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(TradeRefreshEvents.class);

    /** 是否存在“已删除交易待刷新生效”的标记；volatile 保证跨线程可见（实际事件均在主线程派发）。 */
    private static volatile boolean pendingTradeRefresh = false;

    private TradeRefreshEvents() {}

    /** 删除交易成功后调用：置位待刷新标记，等待下一位玩家登录时静默应用。 */
    public static void markPending() {
        pendingTradeRefresh = true;
    }

    /**
     * 删除交易成功后调用：立即（延迟 1 tick）静默执行一次 /reload，
     * 使 deleted 过滤即时生效——新生成村民不再含被删交易，无需手动 /reload 或重进。
     * 复用与登录兜底完全一致的静默执行通道（withSuppressedOutput 抑制命令反馈）。
     * 执行前消费待刷新标记，避免下一位玩家登录时重复 reload；登录兜底逻辑保留作异常兜底。
     */
    public static void scheduleImmediateReload(MinecraftServer server) {
        if (server == null) return;
        pendingTradeRefresh = false;
        server.tell(new TickTask(server.getTickCount() + 1, () -> {
            try {
                server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack().withSuppressedOutput(), "reload");
                LOGGER.info("[VisualCrafting] Applied immediate trade refresh after delete");
            } catch (Exception e) {
                LOGGER.error("[VisualCrafting] Failed to apply immediate trade refresh: " + e.getMessage());
            }
        }));
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        if (!pendingTradeRefresh) return;
        pendingTradeRefresh = false;

        MinecraftServer server = serverPlayer.server;
        // 延迟一 tick 执行：避开玩家登录流程中的世界加载阶段，等价于静默执行 /reload
        server.tell(new TickTask(server.getTickCount() + 1, () -> {
            try {
                server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack().withSuppressedOutput(), "reload");
                LOGGER.info("[VisualCrafting] Applied pending trade refresh on login for player: "
                        + serverPlayer.getGameProfile().getName());
            } catch (Exception e) {
                LOGGER.error("[VisualCrafting] Failed to apply pending trade refresh: " + e.getMessage());
            }
        }));
    }
}
