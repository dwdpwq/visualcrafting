package com.visualcrafting.compat.ae2;

import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.blockentity.misc.InterfaceBlockEntity;
import com.visualcrafting.item.FurnaceCardData;
import com.visualcrafting.item.IFurnaceCard;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 熔炉卡经验收集（AE2 侧）。
 * <p>
 * Handles furnace card XP accumulation: periodically scans ME Interfaces with
 * furnace cards installed, captures XP from adjacent furnaces and stores it in
 * the card. Overflow XP is injected into the AE network as experience fluid;
 * if injection is not possible, the overflow is discarded.
 * <p>
 * 由 {@link Ae2FurnaceTickHandler} 在每个服务端 tick 统一驱动。
 */
public final class Ae2FurnaceExpCollector {

    private static final int SCAN_INTERVAL = 40;
    private static final Logger LOGGER = LoggerFactory.getLogger(Ae2FurnaceExpCollector.class);

    /** 反射失败原因只告警一次：既不刷屏，又留下排查线索。 */
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private Ae2FurnaceExpCollector() {
    }

    private static void warnOnce(String key, Throwable t) {
        if (WARNED.add(key)) {
            LOGGER.warn("[VC:FurnaceCard] {} failed (logged once): {}", key, t.toString());
        }
    }

    public static void processAllLevels(MinecraftServer server, long tick) {
        if (tick % SCAN_INTERVAL != 0L) return;

        for (ServerLevel level : server.getAllLevels()) {
            processLevel(level);
        }
    }

    private static void processLevel(ServerLevel level) {
        for (InterfaceBlockEntity iface : findLoadedInterfaces(level)) {
            IUpgradeInventory upgrades = iface.getUpgrades();
            if (upgrades == null) continue;

            boolean hasCard = false;
            for (int i = 0; i < upgrades.size() && !hasCard; i++) {
                ItemStack s = upgrades.getStackInSlot(i);
                if (!s.isEmpty() && s.getItem() instanceof IFurnaceCard) hasCard = true;
            }
            if (!hasCard) continue;

            BlockPos pos = iface.getBlockPos();
            long totalXp = collectFurnaceXp(level, pos);
            if (totalXp <= 0) continue;

            long remaining = storeXpToUpgrades(upgrades, totalXp);
            if (remaining > 0) {
                discardOverflow(iface, remaining);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<InterfaceBlockEntity> findLoadedInterfaces(ServerLevel level) {
        List<InterfaceBlockEntity> list = new ArrayList<>();
        try {
            var chunkSource = level.getChunkSource();
            var chunkMapField = chunkSource.getClass().getDeclaredField("chunkMap");
            chunkMapField.setAccessible(true);
            var chunkMap = chunkMapField.get(chunkSource);
            var getChunksMethod = chunkMap.getClass().getDeclaredMethod("getChunks");
            getChunksMethod.setAccessible(true);
            var chunks = (Iterable<?>) getChunksMethod.invoke(chunkMap);

            for (var holder : chunks) {
                try {
                    Object chunk = null;
                    try {
                        var m = holder.getClass().getMethod("getTickingChunk");
                        chunk = m.invoke(holder);
                    } catch (Exception e1) {
                        try {
                            var m = holder.getClass().getMethod("getFullChunk");
                            chunk = m.invoke(holder);
                        } catch (Throwable ignored) {
                        }
                    }
                    if (chunk == null) continue;
                    var getBlockEntities = chunk.getClass().getMethod("getBlockEntities");
                    var blockEntities = getBlockEntities.invoke(chunk);
                    if (blockEntities instanceof java.util.Map<?, ?> map) {
                        for (Object be : map.values()) {
                            if (be instanceof InterfaceBlockEntity iface && !iface.isRemoved()) {
                                list.add(iface);
                            }
                        }
                    }
                } catch (Throwable ignored) {
                    // 单区块读取失败属正常回退路径，保持静默
                }
            }
        } catch (Throwable t) {
            warnOnce("findLoadedInterfaces", t);
        }
        return list;
    }

    private static long storeXpToUpgrades(IUpgradeInventory upgrades, long xpMilli) {
        for (int i = 0; i < upgrades.size(); i++) {
            if (xpMilli <= 0) break;
            ItemStack stack = upgrades.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof IFurnaceCard item)) continue;
            long cap = item.getMaxExpStorage();
            long current = FurnaceCardData.getStoredExpMilli(stack);
            long space = cap - current;
            if (space <= 0) continue;
            long toStore = Math.min(space, xpMilli);
            FurnaceCardData.setStoredExpMilli(stack, current + toStore);
            xpMilli -= toStore;
            // 写回升级槽并通知客户端同步(否则ME接口UI"取出经验"读不到新值)
            upgrades.setItemDirect(i, stack.copy());
            upgrades.sendChangeNotification(i);
        }
        return xpMilli;
    }

    private static long collectFurnaceXp(ServerLevel level, BlockPos center) {
        long total = 0;
        for (BlockPos fp : findAdjacentFurnaces(level, center)) {
            total += getFurnaceXp(level, fp);
            setFurnaceXp(level, fp, 0);
        }
        return total;
    }

    private static List<BlockPos> findAdjacentFurnaces(Level level, BlockPos center) {
        List<BlockPos> furnaces = new ArrayList<>();
        for (BlockPos offset : BlockPos.betweenClosed(
                center.offset(-2, -1, -2),
                center.offset(2, 1, 2))) {
            if (offset.equals(center)) continue;
            BlockEntity be = level.getBlockEntity(offset);
            if (be instanceof AbstractFurnaceBlockEntity) {
                furnaces.add(offset.immutable());
            }
        }
        return furnaces;
    }

    private static long getFurnaceXp(Level level, BlockPos pos) {
        try {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AbstractFurnaceBlockEntity) {
                java.lang.reflect.Field f = AbstractFurnaceBlockEntity.class.getDeclaredField("recipeExperience");
                f.setAccessible(true);
                return f.getFloat(be) > 0 ? (long) (f.getFloat(be) * 1000) : 0;
            }
        } catch (Throwable t) {
            warnOnce("getFurnaceXp(recipeExperience)", t);
        }
        return 0;
    }

    private static void setFurnaceXp(Level level, BlockPos pos, float value) {
        try {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AbstractFurnaceBlockEntity) {
                java.lang.reflect.Field f = AbstractFurnaceBlockEntity.class.getDeclaredField("recipeExperience");
                f.setAccessible(true);
                f.setFloat(be, value);
                be.setChanged();
            }
        } catch (Throwable t) {
            warnOnce("setFurnaceXp(recipeExperience)", t);
        }
    }

    private static void discardOverflow(InterfaceBlockEntity iface, long milliXp) {
        // 优先将溢出经验注入 AE 网络流体；无法注入时才丢弃。
        if (tryInjectToNetwork(iface, milliXp)) return;

        Level level = iface.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;
        long points = milliXp / 1000L;
        if (points <= 0L) return;
        serverLevel.getServer().getPlayerList().getPlayers().forEach(p ->
                p.displayClientMessage(
                        Component.translatable("gui.visualcrafting.chat.card_overflow_discard", points,
                                iface.getBlockPos().toShortString()),
                        false));
    }

    /**
     * Try to inject overflow XP into the AE network as experience fluid.
     * Returns true if at least part of the XP was injected successfully.
     */
    private static boolean tryInjectToNetwork(InterfaceBlockEntity iface, long milliXp) {
        try {
            appeng.api.networking.IManagedGridNode node = iface.getMainNode();
            if (node == null) return false;
            IGrid grid = node.getGrid();
            if (grid == null) return false;
            IActionSource source = IActionSource.ofMachine(iface);
            return Ae2ExperienceFluid.insertExpFluidToNetwork(grid, source, milliXp);
        } catch (Throwable t) {
            warnOnce("injectExpFluidToNetwork", t);
            return false;
        }
    }
}
