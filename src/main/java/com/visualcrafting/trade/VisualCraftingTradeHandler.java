package com.visualcrafting.trade;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
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
@EventBusSubscriber(modid = "visualcrafting")
public final class VisualCraftingTradeHandler {
    private VisualCraftingTradeHandler() {}

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        String professionId = BuiltInRegistries.VILLAGER_PROFESSION
                .getKey(event.getType()) != null
                ? BuiltInRegistries.VILLAGER_PROFESSION.getKey(event.getType()).toString()
                : "";
        if (professionId.isEmpty()) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        File root = server.getWorldPath(LevelResource.ROOT).toFile();

        applyVillagerOverrides(event.getTrades(), normalizeProfessionId(professionId), root);
        loadCustomTrades(event.getTrades(), normalizeProfessionId(professionId), root);
    }

    @SubscribeEvent
    public static void onWanderingTrades(WandererTradesEvent event) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        File root = server.getWorldPath(LevelResource.ROOT).toFile();
        applyWanderingOverrides(event.getGenericTrades(), "generic", root);
        applyWanderingOverrides(event.getRareTrades(), "rare", root);
    }

    private static void applyVillagerOverrides(
            Map<Integer, List<VillagerTrades.ItemListing>> trades,
            String professionId,
            File worldRoot) {
        File dir = new File(new File(new File(worldRoot, "visualcrafting"),
                "trade_overrides/villager"), professionId);
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

            int maxUses = optionalInt(json, "maxUses", -1);
            int xp = optionalInt(json, "xp", -1);
            float multiplier = optionalFloat(json, "priceMultiplier", -1.0F);

            if (cost1.isEmpty() && cost2.isEmpty() && result.isEmpty()
                    && maxUses < 0 && xp < 0 && multiplier < 0.0F) {
                return null;
            }
            return new OverrideDefinition(cost1, cost2, result, maxUses, xp, multiplier);
        } catch (Exception e) {
            System.err.println("[VisualCrafting] Failed to load trade override "
                    + file.getAbsolutePath() + ": " + e.getMessage());
            return null;
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
        File professionDir = new File(
                new File(new File(worldRoot, "visualcrafting"), "trades"),
                professionId);
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

            ItemStack costA = itemStack(definition.cost1, definition.cost1Count);
            ItemStack costB = definition.cost2.isEmpty()
                    ? ItemStack.EMPTY : itemStack(definition.cost2, definition.cost2Count);
            ItemStack result = itemStack(definition.result, definition.resultCount);

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

    private static String normalizeProfessionId(String id) {
        int colon = id.indexOf(':');
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record OverrideDefinition(
            ItemStack cost1,
            ItemStack cost2,
            ItemStack result,
            int maxUses,
            int xp,
            float priceMultiplier) {
        public OverrideDefinition {
            cost1 = cost1 == null ? ItemStack.EMPTY : cost1.copy();
            cost2 = cost2 == null ? ItemStack.EMPTY : cost2.copy();
            result = result == null ? ItemStack.EMPTY : result.copy();
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

        private TradeDefinition(int level, String cost1, int cost1Count,
                                String cost2, int cost2Count, String result,
                                int resultCount, int maxUses, int xp,
                                float priceMultiplier, boolean clearExisting) {
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
        }

        static TradeDefinition fromJson(JsonObject json) {
            String cost1 = string(json, "cost1");
            String result = string(json, "result");
            if (cost1.isEmpty() || result.isEmpty()) return null;

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

            return new TradeDefinition(level, cost1, cost1Count, cost2, cost2Count,
                    result, resultCount, maxUses, xp, multiplier,
                    json.has("clearExisting") && json.get("clearExisting").getAsBoolean());
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
