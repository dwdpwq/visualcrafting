package com.visualcrafting.event;

import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.blockentity.misc.InterfaceBlockEntity;
import com.visualcrafting.fluid.ExperienceFluidHelper;
import com.visualcrafting.item.FurnaceCardItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Handles furnace card XP accumulation and fluid routing.
 * Periodically scans ME Interfaces with furnace cards installed,
 * captures XP from adjacent furnaces, stores in card,
 * and routes overflow to ME fluid storage.
 * Player notifications are handled by AEBaseScreenMixin when the interface GUI is opened.
 */
@EventBusSubscriber(modid = "visualcrafting")
public class FurnaceCardExpHandler {

    private static int tickCounter;
    private static final int SCAN_INTERVAL = 40;
    private static final Map<BlockPos, Component> pendingWarnings = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter % SCAN_INTERVAL != 0) return;

        retryPendingWarnings(event.getServer());

        for (ServerLevel level : event.getServer().getAllLevels()) {
            processLevel(level);
        }
    }

    private static void retryPendingWarnings(net.minecraft.server.MinecraftServer server) {
        if (pendingWarnings.isEmpty()) return;
        Iterator<Map.Entry<BlockPos, Component>> it = pendingWarnings.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Component> entry = it.next();
            BlockPos pos = entry.getKey();
            for (ServerLevel level : server.getAllLevels()) {
                BlockEntity be = level.getBlockEntity(pos);
                if (!(be instanceof InterfaceBlockEntity iface) || iface.isRemoved()) continue;
                IGrid grid = getGrid(iface);
                if (grid == null) continue;
                ServerPlayer holder = findWirelessTerminalHolder(grid, level);
                if (holder != null) {
                    holder.displayClientMessage(entry.getValue(), false);
                    it.remove();
                }
                break;
            }
        }
    }

    private static IGrid getGrid(InterfaceBlockEntity iface) {
        try {
            var node = iface.getMainNode();
            return node != null ? node.getGrid() : null;
        } catch (Exception ignored) { return null; }
    }

    private static void processLevel(ServerLevel level) {
        for (InterfaceBlockEntity iface : findLoadedInterfaces(level)) {
            IUpgradeInventory upgrades = iface.getUpgrades();
            if (upgrades == null) continue;

            boolean hasCard = false;
            for (int i = 0; i < upgrades.size() && !hasCard; i++) {
                ItemStack s = upgrades.getStackInSlot(i);
                if (!s.isEmpty() && s.getItem() instanceof FurnaceCardItem) hasCard = true;
            }
            if (!hasCard) continue;

            BlockPos pos = iface.getBlockPos();
            long totalXp = collectFurnaceXp(level, pos);
            if (totalXp <= 0) continue;

            long remaining = storeXpToUpgrades(upgrades, totalXp);
            if (remaining > 0) {
                tryRouteToNetwork(iface, remaining);
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
                        } catch (Exception ignored) {}
                    }
                    if (chunk == null) continue;
                    var getBlockEntities = chunk.getClass().getMethod("getBlockEntities");
                    var blockEntities = getBlockEntities.invoke(chunk);
                    if (blockEntities instanceof java.util.Map<?,?> map) {
                        for (Object be : map.values()) {
                            if (be instanceof InterfaceBlockEntity iface && !iface.isRemoved()) {
                                list.add(iface);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return list;
    }

    private static long storeXpToUpgrades(IUpgradeInventory upgrades, long xpMilli) {
        for (int i = 0; i < upgrades.size(); i++) {
            if (xpMilli <= 0) break;
            ItemStack stack = upgrades.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof FurnaceCardItem item)) continue;
            long cap = item.getMaxExpStorage();
            long current = FurnaceCardItem.getStoredExpMilli(stack);
            long space = cap - current;
            if (space <= 0) continue;
            long toStore = Math.min(space, xpMilli);
            FurnaceCardItem.setStoredExpMilli(stack, current + toStore);
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
                return f.getFloat(be) > 0 ? (long)(f.getFloat(be) * 1000) : 0;
            }
        } catch (Exception ignored) {}
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
        } catch (Exception ignored) {}
    }

    private static void tryRouteToNetwork(InterfaceBlockEntity iface, long milliXp) {
        try {
            appeng.api.networking.IManagedGridNode node = iface.getMainNode();
            if (node == null) { warnAndDiscard(iface, milliXp, null); return; }
            IGrid grid = node.getGrid();
            if (grid == null) { warnAndDiscard(iface, milliXp, null); return; }
            IActionSource source = IActionSource.ofMachine(iface);
            boolean success = ExperienceFluidHelper.insertExpFluidToNetwork(grid, source, milliXp);
            if (!success) {
                warnAndDiscard(iface, milliXp, grid);
            }
        } catch (Exception e) {
            warnAndDiscard(iface, milliXp, null);
        }
    }

    /**
     * Notify the holder of a wireless terminal bound to this network that
     * overflow XP was discarded because fluid injection into the AE network failed.
     * If no holder is found, silently discard.
     * If sending fails, queue for retry on next tick cycle.
     */
    private static void warnAndDiscard(InterfaceBlockEntity iface, long milliXp, IGrid grid) {
        Level level = iface.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;
        long mB = milliXp / 50L;
        Component msg = Component.literal(
                "[VC] 经验流体注入失败, " + mB + " mB 溢出经验已销毁 ("
                + iface.getBlockPos().toShortString() + ")");

        if (grid == null) return; // Silent if no network

        ServerPlayer holder = findWirelessTerminalHolder(grid, serverLevel);
        if (holder != null) {
            try {
                holder.displayClientMessage(msg, false);
            } catch (Exception e) {
                // Send failed, queue for retry
                pendingWarnings.put(iface.getBlockPos(), msg);
            }
            return;
        }
        // No wireless terminal holder — silent discard
    }

    /**
     * Finds the first online player whose inventory contains an AE2 wireless
     * terminal bound to the given grid.
     */
    private static ServerPlayer findWirelessTerminalHolder(IGrid grid, ServerLevel serverLevel) {
        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            if (holdsWirelessTerminalForGrid(player, grid)) return player;
        }
        return null;
    }

    private static boolean holdsWirelessTerminalForGrid(ServerPlayer player, IGrid grid) {
        for (ItemStack stack : player.getInventory().items) {
            if (isWirelessTerminalBoundToGrid(stack, grid)) return true;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (isWirelessTerminalBoundToGrid(stack, grid)) return true;
        }
        return false;
    }

    private static boolean isWirelessTerminalBoundToGrid(ItemStack stack, IGrid grid) {
        if (stack.isEmpty()) return false;
        String cls = stack.getItem().getClass().getName();
        boolean isWirelessTerminal = cls.equals("appeng.items.tools.powered.WirelessTerminalItem")
                || cls.equals("appeng.items.tools.powered.WirelessCraftingTerminalItem");
        if (!isWirelessTerminal) {
            for (Class<?> c = stack.getItem().getClass(); c != Object.class; c = c.getSuperclass()) {
                String parent = c.getName();
                if (parent.equals("appeng.items.tools.powered.WirelessTerminalItem")
                        || parent.equals("appeng.items.tools.powered.WirelessCraftingTerminalItem")) {
                    isWirelessTerminal = true;
                    break;
                }
            }
        }
        if (!isWirelessTerminal) return false;

        return true;
    }

    /**
     * Called by FurnaceCardTickHandler to process a single ME Interface.
     * When auto-processing is enabled, scans interface inventory for smelting/
     * blasting patterns and dispatches matching items through them.
     */
    public static void processInterface(InterfaceBlockEntity iface, ServerLevel level, long tick) {
        IUpgradeInventory upgrades = iface.getUpgrades();
        if (upgrades == null) return;

        boolean hasCard = false;
        for (int i = 0; i < upgrades.size() && !hasCard; i++) {
            ItemStack s = upgrades.getStackInSlot(i);
            if (!s.isEmpty() && s.getItem() instanceof FurnaceCardItem) hasCard = true;
        }
        if (!hasCard) return;

        // Simple XP collection from adjacent furnaces (existing logic)
        BlockPos pos = iface.getBlockPos();
        long totalXp = collectFurnaceXp(level, pos);
        if (totalXp > 0) {
            long remaining = storeXpToUpgrades(upgrades, totalXp);
            if (remaining > 0) {
                tryRouteToNetwork(iface, remaining);
            }
        }
    }
}
