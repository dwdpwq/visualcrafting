package com.visualcrafting.trade;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

        // 先缓存运行时原版/Mod 交易，再追加 GUI 自定义交易；避免 GUI 把自定义交易重复显示为运行时交易。
        // 缓存只依赖 event.getTrades()，需在 server 检查之前完成，否则集成服务器尚未就绪时 RUNTIME 为空。
        cacheVillagerRuntimeTrades(professionId, event.getTrades());

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        File root = server.getWorldPath(LevelResource.ROOT).toFile();

        applyVillagerOverrides(event.getTrades(), professionId, root);
        loadCustomTrades(event.getTrades(), professionId, root);
    }

    @SubscribeEvent
    public static void onWanderingTrades(WandererTradesEvent event) {
        // 运行时缓存只依赖事件数据，需在 server 检查之前完成，否则集成服务器尚未就绪时 RUNTIME 为空。
        RUNTIME_WANDERING_TRADES.put("generic", List.copyOf(event.getGenericTrades()));
        RUNTIME_WANDERING_TRADES.put("rare", List.copyOf(event.getRareTrades()));

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        File root = server.getWorldPath(LevelResource.ROOT).toFile();
        applyWanderingOverrides(event.getGenericTrades(), "generic", root);
        applyWanderingOverrides(event.getRareTrades(), "rare", root);

        // 将流浪商人作为一个独立编辑项接入 Mode 3：
        // 等级 1 对应普通交易池，等级 2 对应稀有交易池。
        Map<Integer, List<VillagerTrades.ItemListing>> customTrades = new HashMap<>();
        customTrades.put(1, event.getGenericTrades());
        customTrades.put(2, event.getRareTrades());
        loadCustomTrades(customTrades, "minecraft:wandering_trader", root);
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
