package com.visualcrafting.trade;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side trade editor.
 *
 * Existing GUI-created trades remain in:
 * world/visualcrafting/trades/<profession>/<index>.json
 *
 * Existing vanilla/modded listings can be transformed without replacing their
 * ItemListing implementation. This preserves dynamic/random/NBT/component-rich
 * offers created by other mods.
 *
 * Override files:
 * world/visualcrafting/trade_overrides/villager/<profession>/<level>-<index>.json
 * world/visualcrafting/trade_overrides/wandering/generic-<index>.json
 * world/visualcrafting/trade_overrides/wandering/rare-<index>.json
 *
 * Override fields are optional:
 *   cost1, cost1Count, cost2, cost2Count, result, resultCount,
 *   maxUses, xp, priceMultiplier
 *
 * Omitted fields keep the original offer value.
 */
@EventBusSubscriber(modid = "visualcrafting", bus = EventBusSubscriber.Bus.GAME)
public final class VisualCraftingTradeHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(VisualCraftingTradeHandler.class);
    /**
     * 当前服务器最近一次 VillagerTradesEvent 生成的“原版/其他 Mod”运行时交易。
     * GUI 查询时从这里读取，避免只扫描 world/visualcrafting/trades 导致原版交易永远为空。
     */
    private static final Map<String, Map<Integer, List<VillagerTrades.ItemListing>>> RUNTIME_VILLAGER_TRADES =
            new ConcurrentHashMap<>();
    private static final Map<String, List<VillagerTrades.ItemListing>> RUNTIME_WANDERING_TRADES =
            new ConcurrentHashMap<>();

    private VisualCraftingTradeHandler() {}

    public static List<VillagerTrades.ItemListing> getRuntimeVillagerTrades(String professionId, int level) {
        Map<Integer, List<VillagerTrades.ItemListing>> levels = RUNTIME_VILLAGER_TRADES.get(professionId);
        if (levels == null) return List.of();
        List<VillagerTrades.ItemListing> listings = levels.get(level);
        return listings == null ? List.of() : List.copyOf(listings);
    }

    public static List<VillagerTrades.ItemListing> getRuntimeWanderingTrades(String pool) {
        List<VillagerTrades.ItemListing> listings = RUNTIME_WANDERING_TRADES.get(pool);
        return listings == null ? List.of() : List.copyOf(listings);
    }

    /**
     * 删除成功后从运行时缓存移除单条，使 GUI 重拉列表立即看不到该条（无需 /reload）。
     * 与 writeRuntimeDeleteMark 配套：deleted 标记照写，reload/重进后事件注入过滤保持一致。
     * @return 找到并移除返回 true；池不存在或索引越界返回 false
     */
    public static boolean removeRuntimeWanderingTrade(String pool, int index) {
        List<VillagerTrades.ItemListing> listings = RUNTIME_WANDERING_TRADES.get(pool);
        if (listings == null || index < 0 || index >= listings.size()) return false;
        List<VillagerTrades.ItemListing> copy = new ArrayList<>(listings);
        copy.remove(index);
        RUNTIME_WANDERING_TRADES.put(pool, List.copyOf(copy));
        return true;
    }

    /**
     * 删除成功后从运行时缓存移除单条（村民职业版本）。
     * @return 找到并移除返回 true；否则返回 false
     */
    public static boolean removeRuntimeVillagerTrade(String professionId, int level, int index) {
        Map<Integer, List<VillagerTrades.ItemListing>> levels = RUNTIME_VILLAGER_TRADES.get(professionId);
        if (levels == null) return false;
        List<VillagerTrades.ItemListing> listings = levels.get(level);
        if (listings == null || index < 0 || index >= listings.size()) return false;
        Map<Integer, List<VillagerTrades.ItemListing>> copyLevels = new HashMap<>(levels);
        List<VillagerTrades.ItemListing> copy = new ArrayList<>(listings);
        copy.remove(index);
        copyLevels.put(level, List.copyOf(copy));
        RUNTIME_VILLAGER_TRADES.put(professionId, copyLevels);
        return true;
    }

    private static void cacheVillagerRuntimeTrades(String professionId,
                                                   Map<Integer, List<VillagerTrades.ItemListing>> trades) {
        Map<Integer, List<VillagerTrades.ItemListing>> copy = new HashMap<>();
        for (Map.Entry<Integer, List<VillagerTrades.ItemListing>> entry : trades.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        RUNTIME_VILLAGER_TRADES.put(professionId, copy);
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        String professionId = BuiltInRegistries.VILLAGER_PROFESSION
                .getKey(event.getType()) != null
                ? BuiltInRegistries.VILLAGER_PROFESSION.getKey(event.getType()).toString()
                : "";
        if (professionId.isEmpty()) return;

        // 顺序（保证删除后索引稳定，不出现“删除交易后又恢复/覆盖错条目”）：
        // 1) 先按原版事件原始索引应用 override（trade_overrides/<level>-<index>.json）；
        // 2) 再缓存“override 后完整列表”（含已写 deleted 标记的条目），缓存索引 = 原版事件原始位置；
        // 3) deleted 过滤只作用于注入列表，不左移缓存索引。
        // 这样 GUI 重拉列表时被删条目按 deleted 标记跳过显示但索引不重排，
        // 后续保存/删除仍按原始索引定位 override/deleted 文件，不会撤销相邻条目的删除标记。
        // 缓存只依赖 event.getTrades()，需在 server 检查之前完成，否则集成服务器尚未就绪时 RUNTIME 为空；
        // override/deleted 依赖存档目录，server 就绪后执行。
        var server = ServerLifecycleHooks.getCurrentServer();
        File root = server == null ? null : server.getWorldPath(LevelResource.ROOT).toFile();
        if (root != null) {
            applyVillagerOverrides(event.getTrades(), professionId, root);
        }
        cacheVillagerRuntimeTrades(professionId, event.getTrades());

        if (server == null) return;

        if (root != null) {
            applyVillagerDeletedFilters(event.getTrades(), professionId, root);
        }
        loadCustomTrades(event.getTrades(), professionId, root);
    }

    @SubscribeEvent
    public static void onWanderingTrades(WandererTradesEvent event) {
        // 与 onVillagerTrades 同序：先按原始索引应用 override → 缓存完整列表 → 再过滤 deleted 注入。
        var server = ServerLifecycleHooks.getCurrentServer();
        File root = server == null ? null : server.getWorldPath(LevelResource.ROOT).toFile();
        if (root != null) {
            applyWanderingOverrides(event.getGenericTrades(), "generic", root);
            applyWanderingOverrides(event.getRareTrades(), "rare", root);
        }
        // 运行时缓存只依赖事件数据，需在 server 检查之前完成，否则集成服务器尚未就绪时 RUNTIME 为空。
        RUNTIME_WANDERING_TRADES.put("generic", List.copyOf(event.getGenericTrades()));
        RUNTIME_WANDERING_TRADES.put("rare", List.copyOf(event.getRareTrades()));

        if (server == null) return;

        if (root != null) {
            applyWanderingDeletedFilters(event.getGenericTrades(), "generic", root);
            applyWanderingDeletedFilters(event.getRareTrades(), "rare", root);
        }

        // 将流浪商人作为一个独立编辑项接入 Mode 3：
        // 等级 1 对应普通交易池，等级 2 对应稀有交易池。
        Map<Integer, List<VillagerTrades.ItemListing>> customTrades = new HashMap<>();
        customTrades.put(1, event.getGenericTrades());
        customTrades.put(2, event.getRareTrades());
        loadCustomTrades(customTrades, "minecraft:wandering_trader", root);
    }

    // ===== deleted 标记过滤（原版/Mod 运行时交易直接删除） =====
    // 标记由服务端 handleDeleteTrade 在无 override 时写入：
    //   world/visualcrafting/trade_deleted/villager/<profDir>/<level>-<index>.json
    //   world/visualcrafting/trade_deleted/wandering/<pool>-<index>.json
    // 标记文件内 id 按 profId|cost1|result[|cost2]（同家族重复项带自然数后缀）。

    /** 仅加载文件名以 <levelOrPool>- 前缀开头的标记 ID。 */
    private static Set<String> loadDeletedIdsByLevel(File deletedDir, String levelOrPool) {
        Set<String> ids = new HashSet<>();
        String prefix = levelOrPool + "-";
        File[] files = deletedDir.listFiles((d, n) -> n.endsWith(".json") && n.startsWith(prefix));
        if (files == null) return ids;
        for (File f : files) {
            try {
                JsonObject json = JsonParser.parseString(
                        Files.readString(f.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
                String id = json.has("id") ? json.get("id").getAsString() : "";
                if (!id.isEmpty()) ids.add(id);
            } catch (Exception ignored) { }
        }
        return ids;
    }

    /** 基础 ID 命中判定：标记 ID 等于 base 或以 base|N 形式存在（共享 ID 整组过滤）。 */
    private static boolean isDeletedId(Set<String> deletedIds, String base) {
        if (deletedIds.contains(base)) return true;
        for (String id : deletedIds) {
            if (id.startsWith(base + "|")) return true;
        }
        return false;
    }

    /**
     * 将运行时 ItemListing 转为三元组 JSON（与 ModMessages.merchantOfferToTradeJson 同源规则）：
     * 用临时村民/流浪商人种子化调用 getOffer，取 cost1/cost2/result 的注册名。
     * 生成失败返回 null（保持该交易不过滤，避免误删）。
     */
    private static JsonObject listingToTripleJson(VillagerTrades.ItemListing listing,
                                                  String profId, int level) {
        try {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return null;
            MerchantOffer offer;
            if ("minecraft:wandering_trader".equals(profId)) {
                WanderingTrader trader = new WanderingTrader(EntityType.WANDERING_TRADER, server.overworld());
                offer = listing.getOffer(trader, RandomSource.create());
            } else {
                Optional<VillagerProfession> profession = BuiltInRegistries.VILLAGER_PROFESSION
                        .getOptional(ResourceLocation.parse(profId));
                if (profession.isEmpty()) return null;
                Villager villager = new Villager(EntityType.VILLAGER, server.overworld());
                VillagerData data = villager.getVillagerData()
                        .setProfession(profession.get())
                        .setLevel(Math.clamp(level, 1, 5));
                villager.setVillagerData(data);
                offer = listing.getOffer(villager, RandomSource.create());
            }
            if (offer == null) return null;
            JsonObject json = new JsonObject();
            json.addProperty("cost1", BuiltInRegistries.ITEM.getKey(offer.getCostA().getItem()).toString());
            if (!offer.getCostB().isEmpty()) {
                json.addProperty("cost2", BuiltInRegistries.ITEM.getKey(offer.getCostB().getItem()).toString());
            }
            json.addProperty("result", BuiltInRegistries.ITEM.getKey(offer.getResult().getItem()).toString());
            return json;
        } catch (Exception e) {
            LOGGER.warn("[VisualCrafting] Failed to build triple for deleted filter "
                    + profId + " level " + level + ": " + e.getMessage());
            return null;
        }
    }

    /** 按现 ID 体系生成基础 ID：profId|cost1|result[|cost2]（与 ModMessages.generateTradeId 同规则）。 */
    private static String deletedBaseId(String profId, JsonObject tripleJson) {
        StringBuilder sb = new StringBuilder();
        sb.append(profId == null ? "?" : profId);
        String buy = tripleJson.has("cost1") ? tripleJson.get("cost1").getAsString() : "";
        String sell = tripleJson.has("result") ? tripleJson.get("result").getAsString() : "";
        sb.append('|').append((buy == null || buy.isEmpty()) ? "?" : buy);
        sb.append('|').append((sell == null || sell.isEmpty()) ? "?" : sell);
        if (tripleJson.has("cost2")) {
            String buy2 = tripleJson.get("cost2").getAsString();
            if (buy2 != null && !buy2.isEmpty()) sb.append('|').append(buy2);
        }
        return sb.toString();
    }

    /** 村民：按职业目录加载 deleted 标记，过滤事件注入列表。 */
    private static void applyVillagerDeletedFilters(
            Map<Integer, List<VillagerTrades.ItemListing>> trades,
            String professionId,
            File worldRoot) {
        File deletedRoot = new File(new File(worldRoot, "visualcrafting"), "trade_deleted/villager");
        File dir = new File(deletedRoot, profileDirectoryId(professionId));
        if (!dir.isDirectory() && professionId.startsWith("minecraft:")) {
            dir = new File(deletedRoot, legacyProfessionId(professionId));
        }
        if (!dir.isDirectory()) return;
        for (int level = 1; level <= 5; level++) {
            List<VillagerTrades.ItemListing> listings = trades.get(level);
            if (listings == null || listings.isEmpty()) continue;
            Set<String> deletedIds = loadDeletedIdsByLevel(dir, String.valueOf(level));
            if (deletedIds.isEmpty()) continue;
            final int lvl = level;
            listings.removeIf(listing -> {
                JsonObject triple = listingToTripleJson(listing, professionId, lvl);
                if (triple == null) return false;
                return isDeletedId(deletedIds, deletedBaseId(professionId, triple));
            });
        }
    }

    /** 流浪商人：按 pool（generic/rare）加载 deleted 标记，过滤对应交易池。 */
    private static void applyWanderingDeletedFilters(
            List<VillagerTrades.ItemListing> listings,
            String pool,
            File worldRoot) {
        if (listings == null || listings.isEmpty()) return;
        File dir = new File(new File(worldRoot, "visualcrafting"), "trade_deleted/wandering");
        if (!dir.isDirectory()) return;
        Set<String> deletedIds = loadDeletedIdsByLevel(dir, pool);
        if (deletedIds.isEmpty()) return;
        listings.removeIf(listing -> {
            JsonObject triple = listingToTripleJson(listing, "minecraft:wandering_trader",
                    "rare".equals(pool) ? 2 : 1);
            if (triple == null) return false;
            return isDeletedId(deletedIds, deletedBaseId("minecraft:wandering_trader", triple));
        });
    }

    /**
     * 查询某条村民运行时交易是否已被删除（deleted 标记按 ID 匹配）。
     * 供 GUI 列表组装复用：被删条目跳过显示、但索引保持原版事件原始位置，
     * 保证删除后列表真实反映删除结果且后续保存/删除定位不左移。
     * cost1/result 缺失时返回 false（不误过滤）。
     */
    public static boolean isVillagerRuntimeTradeDeleted(File worldRoot, String professionId,
                                                       int level, String cost1, String cost2, String result) {
        try {
            if (worldRoot == null || cost1 == null || cost1.isEmpty()
                    || result == null || result.isEmpty()) return false;
            File deletedRoot = new File(new File(worldRoot, "visualcrafting"), "trade_deleted/villager");
            File dir = new File(deletedRoot, profileDirectoryId(professionId));
            if (!dir.isDirectory() && professionId.startsWith("minecraft:")) {
                dir = new File(deletedRoot, legacyProfessionId(professionId));
            }
            if (!dir.isDirectory()) return false;
            Set<String> deletedIds = loadDeletedIdsByLevel(dir, String.valueOf(level));
            if (deletedIds.isEmpty()) return false;
            JsonObject triple = new JsonObject();
            triple.addProperty("cost1", cost1);
            if (cost2 != null && !cost2.isEmpty()) triple.addProperty("cost2", cost2);
            triple.addProperty("result", result);
            return isDeletedId(deletedIds, deletedBaseId(professionId, triple));
        } catch (Exception e) {
            return false;
        }
    }

    /** 流浪商人版本（pool: generic/rare），规则同村民。 */
    public static boolean isWanderingRuntimeTradeDeleted(File worldRoot, String pool,
                                                         String cost1, String cost2, String result) {
        try {
            if (worldRoot == null || cost1 == null || cost1.isEmpty()
                    || result == null || result.isEmpty()) return false;
            File dir = new File(new File(worldRoot, "visualcrafting"), "trade_deleted/wandering");
            if (!dir.isDirectory()) return false;
            Set<String> deletedIds = loadDeletedIdsByLevel(dir, pool);
            if (deletedIds.isEmpty()) return false;
            JsonObject triple = new JsonObject();
            triple.addProperty("cost1", cost1);
            if (cost2 != null && !cost2.isEmpty()) triple.addProperty("cost2", cost2);
            triple.addProperty("result", result);
            return isDeletedId(deletedIds, deletedBaseId("minecraft:wandering_trader", triple));
        } catch (Exception e) {
            return false;
        }
    }

    private static void applyVillagerOverrides(
            Map<Integer, List<VillagerTrades.ItemListing>> trades,
            String professionId,
            File worldRoot) {
        File overrideRoot = new File(new File(worldRoot, "visualcrafting"), "trade_overrides/villager");
        File dir = new File(overrideRoot, profileDirectoryId(professionId));
        if (!dir.isDirectory() && professionId.startsWith("minecraft:")) {
            dir = new File(overrideRoot, legacyProfessionId(professionId));
        }
        if (!dir.isDirectory()) return;

        for (int level = 1; level <= 5; level++) {
            List<VillagerTrades.ItemListing> listings = trades.get(level);
            if (listings == null || listings.isEmpty()) continue;

            for (int index = 0; index < listings.size(); index++) {
                File file = new File(dir, level + "-" + index + ".json");
                OverrideDefinition def = readOverride(file);
                if (def != null) {
                    listings.set(index, new VisualCraftingTradeOverride(listings.get(index), def));
                }
            }
        }
    }

    private static void applyWanderingOverrides(
            List<VillagerTrades.ItemListing> listings,
            String pool,
            File worldRoot) {
        File dir = new File(new File(worldRoot, "visualcrafting"),
                "trade_overrides/wandering");
        if (!dir.isDirectory()) return;

        for (int index = 0; index < listings.size(); index++) {
            File file = new File(dir, pool + "-" + index + ".json");
            OverrideDefinition def = readOverride(file);
            if (def != null) {
                listings.set(index, new VisualCraftingTradeOverride(listings.get(index), def));
            }
        }
    }

    private static OverrideDefinition readOverride(File file) {
        if (!file.isFile()) return null;
        try {
            JsonObject json = JsonParser.parseString(
                    Files.readString(file.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();

            ItemStack cost1 = readOptionalStack(json, "cost1", "cost1Count");
            ItemStack cost2 = readOptionalStack(json, "cost2", "cost2Count");
            ItemStack result = readOptionalStack(json, "result", "resultCount");
            ItemStack nbtStackCost1 = readNbtStack(json, "nbtMatchCost1", "nbtDataCost1", cost1.getCount());
            ItemStack nbtStackCost2 = readNbtStack(json, "nbtMatchCost2", "nbtDataCost2", cost2.getCount());
            ItemStack nbtStackResult = readNbtStack(json, "nbtMatchResult", "nbtDataResult", result.getCount());

            int maxUses = optionalInt(json, "maxUses", -1);
            int xp = optionalInt(json, "xp", -1);
            float multiplier = optionalFloat(json, "priceMultiplier", -1.0F);

            if (cost1.isEmpty() && cost2.isEmpty() && result.isEmpty()
                    && nbtStackCost1.isEmpty() && nbtStackCost2.isEmpty() && nbtStackResult.isEmpty()
                    && maxUses < 0 && xp < 0 && multiplier < 0.0F) {
                return null;
            }
            return new OverrideDefinition(cost1, cost2, result,
                    nbtStackCost1, nbtStackCost2, nbtStackResult,
                    maxUses, xp, multiplier);
        } catch (Exception e) {
            System.err.println("[VisualCrafting] Failed to load trade override "
                    + file.getAbsolutePath() + ": " + e.getMessage());
            return null;
        }
    }

    /** 解析 NBT 匹配开关对应的完整物品（含组件）；未启用或解析失败返回空栈。 */
    private static ItemStack readNbtStack(JsonObject json, String matchKey, String dataKey, int count) {
        if (!json.has(matchKey) || !json.get(matchKey).getAsBoolean()) return ItemStack.EMPTY;
        if (!json.has(dataKey)) return ItemStack.EMPTY;
        try {
            String snbt = json.get(dataKey).getAsString();
            if (snbt == null || snbt.isEmpty()) return ItemStack.EMPTY;
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return ItemStack.EMPTY;
            CompoundTag tag = NbtUtils.snbtToStructure(snbt);
            ItemStack stack = ItemStack.parseOptional(server.registryAccess(), tag);
            if (!stack.isEmpty()) stack.setCount(clamp(count, 1, 64));
            return stack;
        } catch (Exception e) {
            System.err.println("[VisualCrafting] Failed to parse NBT data in override: " + e.getMessage());
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack readOptionalStack(JsonObject json, String idKey, String countKey) {
        if (!json.has(idKey)) return ItemStack.EMPTY;
        String id;
        try {
            id = json.get(idKey).getAsString().trim();
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
        if (id.isEmpty()) return ItemStack.EMPTY;

        try {
            var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id));
            if (item.isEmpty() || item.get() == Items.AIR) return ItemStack.EMPTY;
            int count = clamp(optionalInt(json, countKey, 1), 1, 64);
            return new ItemStack(item.get(), count);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private static int optionalInt(JsonObject json, String key, int fallback) {
        try {
            return json.has(key) ? json.get(key).getAsInt() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static float optionalFloat(JsonObject json, String key, float fallback) {
        try {
            return json.has(key) ? json.get(key).getAsFloat() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static void loadCustomTrades(
            Map<Integer, List<VillagerTrades.ItemListing>> trades,
            String professionId,
            File worldRoot) {
        File tradesRoot = new File(new File(worldRoot, "visualcrafting"), "trades");
        File professionDir = new File(tradesRoot, profileDirectoryId(professionId));
        if (!professionDir.isDirectory() && professionId.startsWith("minecraft:")) {
            professionDir = new File(tradesRoot, legacyProfessionId(professionId));
        }
        if (!professionDir.isDirectory()) return;

        File[] files = professionDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) return;

        Arrays.sort(files, Comparator
                .comparingInt(VisualCraftingTradeHandler::tradeIndex)
                .thenComparing(File::getName));

        List<TradeDefinition> definitions = new ArrayList<>();
        Set<Integer> clearLevels = new HashSet<>();

        for (File file : files) {
            try {
                JsonObject json = JsonParser.parseString(
                        Files.readString(file.toPath(), StandardCharsets.UTF_8)
                ).getAsJsonObject();
                TradeDefinition definition = TradeDefinition.fromJson(json);
                if (definition == null) continue;
                definitions.add(definition);
                if (definition.clearExisting) clearLevels.add(definition.level);
            } catch (Exception e) {
                System.err.println("[VisualCrafting] Failed to load trade file "
                        + file.getAbsolutePath() + ": " + e.getMessage());
            }
        }

        for (Integer level : clearLevels) {
            List<VillagerTrades.ItemListing> levelTrades = trades.get(level);
            if (levelTrades != null) levelTrades.clear();
        }

        int added = 0;
        for (TradeDefinition definition : definitions) {
            List<VillagerTrades.ItemListing> levelTrades = trades.get(definition.level);
            if (levelTrades == null) continue;

            ItemStack costA = buildTradeStack(definition.cost1, definition.cost1Count,
                    definition.nbtMatchCost1, definition.nbtDataCost1);
            ItemStack costB = definition.cost2.isEmpty()
                    ? ItemStack.EMPTY : buildTradeStack(definition.cost2, definition.cost2Count,
                            definition.nbtMatchCost2, definition.nbtDataCost2);
            ItemStack result = buildTradeStack(definition.result, definition.resultCount,
                    definition.nbtMatchResult, definition.nbtDataResult);

            if (costA.isEmpty() || result.isEmpty()) continue;

            levelTrades.add(new BasicItemListing(
                    costA, costB, result,
                    definition.maxUses, definition.xp, definition.priceMultiplier));
            added++;
        }

        if (added > 0) {
            System.out.println("[VisualCrafting] Loaded " + added
                    + " custom trade(s) for " + professionId);
        }
    }

    private static String profileDirectoryId(String id) {
        return id == null ? null : id.replace(":", "__");
    }

    private static String legacyProfessionId(String id) {
        int colon = id == null ? -1 : id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }

    private static int tradeIndex(File file) {
        String name = file.getName();
        if (name.endsWith(".json")) name = name.substring(0, name.length() - 5);
        try {
            return Integer.parseInt(name);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private static ItemStack itemStack(String id, int count) {
        if (id == null || id.isBlank()) return ItemStack.EMPTY;
        try {
            var optional = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id));
            if (optional.isEmpty() || optional.get() == Items.AIR) return ItemStack.EMPTY;
            return new ItemStack(optional.get(), Math.max(1, Math.min(64, count)));
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    /** 按基础 id/count 构建物品；勾选 NBT 匹配时用保存的 SNBT 还原完整物品（含组件），解析失败回退基础物品。 */
    private static ItemStack buildTradeStack(String id, int count, boolean nbtMatch, String nbtData) {
        ItemStack stack = itemStack(id, count);
        if (!nbtMatch || nbtData == null || nbtData.isEmpty()) return stack;
        try {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return stack;
            CompoundTag tag = NbtUtils.snbtToStructure(nbtData);
            ItemStack parsed = ItemStack.parseOptional(server.registryAccess(), tag);
            if (!parsed.isEmpty()) {
                parsed.setCount(Math.max(1, Math.min(64, count)));
                return parsed;
            }
        } catch (Exception e) {
            System.err.println("[VisualCrafting] Failed to parse NBT data for trade item "
                    + id + ": " + e.getMessage());
        }
        return stack;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record OverrideDefinition(
            ItemStack cost1,
            ItemStack cost2,
            ItemStack result,
            ItemStack nbtStackCost1,
            ItemStack nbtStackCost2,
            ItemStack nbtStackResult,
            int maxUses,
            int xp,
            float priceMultiplier) {
        public OverrideDefinition {
            cost1 = cost1 == null ? ItemStack.EMPTY : cost1.copy();
            cost2 = cost2 == null ? ItemStack.EMPTY : cost2.copy();
            result = result == null ? ItemStack.EMPTY : result.copy();
            nbtStackCost1 = nbtStackCost1 == null ? ItemStack.EMPTY : nbtStackCost1.copy();
            nbtStackCost2 = nbtStackCost2 == null ? ItemStack.EMPTY : nbtStackCost2.copy();
            nbtStackResult = nbtStackResult == null ? ItemStack.EMPTY : nbtStackResult.copy();
        }
    }

    private static final class TradeDefinition {
        final int level;
        final String cost1;
        final int cost1Count;
        final String cost2;
        final int cost2Count;
        final String result;
        final int resultCount;
        final int maxUses;
        final int xp;
        final float priceMultiplier;
        final boolean clearExisting;
        final boolean nbtMatchCost1;
        final String nbtDataCost1;
        final boolean nbtMatchCost2;
        final String nbtDataCost2;
        final boolean nbtMatchResult;
        final String nbtDataResult;
        final boolean nbtMatchResult2;
        final String nbtDataResult2;

        private TradeDefinition(int level, String cost1, int cost1Count,
                                String cost2, int cost2Count, String result,
                                int resultCount, int maxUses, int xp,
                                float priceMultiplier, boolean clearExisting,
                                boolean nbtMatchCost1, String nbtDataCost1,
                                boolean nbtMatchCost2, String nbtDataCost2,
                                boolean nbtMatchResult, String nbtDataResult,
                                boolean nbtMatchResult2, String nbtDataResult2) {
            this.level = level;
            this.cost1 = cost1;
            this.cost1Count = cost1Count;
            this.cost2 = cost2;
            this.cost2Count = cost2Count;
            this.result = result;
            this.resultCount = resultCount;
            this.maxUses = maxUses;
            this.xp = xp;
            this.priceMultiplier = priceMultiplier;
            this.clearExisting = clearExisting;
            this.nbtMatchCost1 = nbtMatchCost1;
            this.nbtDataCost1 = nbtDataCost1 == null ? "" : nbtDataCost1;
            this.nbtMatchCost2 = nbtMatchCost2;
            this.nbtDataCost2 = nbtDataCost2 == null ? "" : nbtDataCost2;
            this.nbtMatchResult = nbtMatchResult;
            this.nbtDataResult = nbtDataResult == null ? "" : nbtDataResult;
            this.nbtMatchResult2 = nbtMatchResult2;
            this.nbtDataResult2 = nbtDataResult2 == null ? "" : nbtDataResult2;
        }

        static TradeDefinition fromJson(JsonObject json) {
            String cost1 = string(json, "cost1");
            String result = string(json, "result");
            boolean clearExisting = json.has("clearExisting") && json.get("clearExisting").getAsBoolean();

            // “clear-<level>.json” 是仅用于清空原有本级交易的持久化标记，
            // 不包含 cost/result 时仍然必须被加载并参与 clearLevels。
            if (cost1.isEmpty() || result.isEmpty()) {
                if (!clearExisting) return null;
                int level = clamp(integer(json, "level", 1), 1, 5);
                return new TradeDefinition(level, "", 0, "", 0, "", 0,
                        1, 0, 0.05f, true,
                        false, "", false, "", false, "", false, "");
            }

            int level = clamp(integer(json, "level", 1), 1, 5);
            int cost1Count = clamp(integer(json, "cost1Count", 1), 1, 64);
            int cost2Count = clamp(integer(json, "cost2Count", 0), 0, 64);
            int resultCount = clamp(integer(json, "resultCount", 1), 1, 64);
            int maxUses = clamp(integer(json, "maxUses", 12), 1, 9999);
            int xp = clamp(integer(json, "xp", 2), 0, 9999);
            float multiplier = number(json, "priceMultiplier", 0.05f);
            if (!Float.isFinite(multiplier) || multiplier < 0.0f) multiplier = 0.05f;

            String cost2 = string(json, "cost2");
            if (cost2.isEmpty() || cost2Count <= 0) {
                cost2 = "";
                cost2Count = 0;
            }

            // NBT 精准匹配（旧文件无字段按未勾选处理）
            boolean nbtMatchCost1 = json.has("nbtMatchCost1") && json.get("nbtMatchCost1").getAsBoolean();
            String nbtDataCost1 = json.has("nbtDataCost1") ? json.get("nbtDataCost1").getAsString() : "";
            boolean nbtMatchCost2 = json.has("nbtMatchCost2") && json.get("nbtMatchCost2").getAsBoolean();
            String nbtDataCost2 = json.has("nbtDataCost2") ? json.get("nbtDataCost2").getAsString() : "";
            boolean nbtMatchResult = json.has("nbtMatchResult") && json.get("nbtMatchResult").getAsBoolean();
            String nbtDataResult = json.has("nbtDataResult") ? json.get("nbtDataResult").getAsString() : "";
            boolean nbtMatchResult2 = json.has("nbtMatchResult2") && json.get("nbtMatchResult2").getAsBoolean();
            String nbtDataResult2 = json.has("nbtDataResult2") ? json.get("nbtDataResult2").getAsString() : "";

            return new TradeDefinition(level, cost1, cost1Count, cost2, cost2Count,
                    result, resultCount, maxUses, xp, multiplier, clearExisting,
                    nbtMatchCost1, nbtDataCost1, nbtMatchCost2, nbtDataCost2,
                    nbtMatchResult, nbtDataResult, nbtMatchResult2, nbtDataResult2);
        }

        private static String string(JsonObject json, String key) {
            return json.has(key) ? json.get(key).getAsString().trim() : "";
        }

        private static int integer(JsonObject json, String key, int fallback) {
            try { return json.has(key) ? json.get(key).getAsInt() : fallback; }
            catch (Exception e) { return fallback; }
        }

        private static float number(JsonObject json, String key, float fallback) {
            try { return json.has(key) ? json.get(key).getAsFloat() : fallback; }
            catch (Exception e) { return fallback; }
        }
    }
}
