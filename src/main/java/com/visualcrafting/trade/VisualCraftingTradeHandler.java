package com.visualcrafting.trade;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * VisualCrafting 绿宝石交易页的服务端执行器。
 *
 * GUI 保存的定义位于：
 * world/visualcrafting/trades/<profession>/<index>.json
 *
 * VillagerTradesEvent 在 /reload 时重建职业交易表，因此删除/覆盖交易也能
 * 在下一次 reload 后可靠生效。这样不再依赖“写文件但没有消费者”的旧逻辑。
 */
@EventBusSubscriber(modid = "visualcrafting")
public final class VisualCraftingTradeHandler {

    private VisualCraftingTradeHandler() {
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        String professionId = BuiltInRegistries.VILLAGER_PROFESSION
                .getKey(event.getType()) != null
                ? BuiltInRegistries.VILLAGER_PROFESSION.getKey(event.getType()).toString()
                : "";

        if (professionId.isEmpty()) {
            return;
        }

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        File worldRoot = server.getWorldPath(LevelResource.ROOT).toFile();

        String normalizedProfession = normalizeProfessionId(professionId);
        File professionDir = new File(
                new File(new File(worldRoot, "visualcrafting"), "trades"),
                normalizedProfession
        );

        if (!professionDir.isDirectory()) {
            return;
        }

        File[] files = professionDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            return;
        }

        Arrays.sort(files, Comparator
                .comparingInt(VisualCraftingTradeHandler::tradeIndex)
                .thenComparing(File::getName));

        List<TradeDefinition> definitions = new ArrayList<>();
        boolean clearExisting = false;

        for (File file : files) {
            try {
                JsonObject json = JsonParser.parseString(
                        Files.readString(file.toPath(), StandardCharsets.UTF_8)
                ).getAsJsonObject();

                TradeDefinition definition = TradeDefinition.fromJson(json);
                if (definition == null) {
                    continue;
                }

                definitions.add(definition);
                clearExisting |= definition.clearExisting;
            } catch (Exception e) {
                System.err.println("[VisualCrafting] Failed to load trade file "
                        + file.getAbsolutePath() + ": " + e.getMessage());
            }
        }

        if (definitions.isEmpty()) {
            return;
        }

        if (clearExisting) {
            for (List<VillagerTrades.ItemListing> levelTrades : event.getTrades().values()) {
                levelTrades.clear();
            }
        }

        int added = 0;
        for (TradeDefinition definition : definitions) {
            List<VillagerTrades.ItemListing> levelTrades = event.getTrades().get(definition.level);
            if (levelTrades == null) {
                continue;
            }

            ItemStack costA = itemStack(definition.cost1, definition.cost1Count);
            ItemStack costB = definition.cost2.isEmpty()
                    ? ItemStack.EMPTY
                    : itemStack(definition.cost2, definition.cost2Count);
            ItemStack result = itemStack(definition.result, definition.resultCount);

            if (costA.isEmpty() || result.isEmpty()) {
                continue;
            }

            levelTrades.add(new BasicItemListing(
                    costA,
                    costB,
                    result,
                    definition.maxUses,
                    definition.xp,
                    definition.priceMultiplier
            ));
            added++;
        }

        if (added > 0) {
            System.out.println("[VisualCrafting] Loaded " + added
                    + " custom emerald trade(s) for " + professionId);
        }
    }

    private static String normalizeProfessionId(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }

    private static int tradeIndex(File file) {
        String name = file.getName();
        if (name.endsWith(".json")) {
            name = name.substring(0, name.length() - 5);
        }
        try {
            return Integer.parseInt(name);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private static ItemStack itemStack(String id, int count) {
        if (id == null || id.isBlank()) {
            return ItemStack.EMPTY;
        }

        try {
            var optional = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id));
            if (optional.isEmpty() || optional.get() == Items.AIR) {
                return ItemStack.EMPTY;
            }

            int safeCount = Math.max(1, Math.min(64, count));
            return new ItemStack(optional.get(), safeCount);
        } catch (Exception e) {
            return ItemStack.EMPTY;
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

        private TradeDefinition(
                int level,
                String cost1,
                int cost1Count,
                String cost2,
                int cost2Count,
                String result,
                int resultCount,
                int maxUses,
                int xp,
                float priceMultiplier,
                boolean clearExisting
        ) {
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
            if (cost1.isEmpty() || result.isEmpty()) {
                return null;
            }

            int level = clamp(integer(json, "level", 1), 1, 5);
            int cost1Count = clamp(integer(json, "cost1Count", 1), 1, 64);
            int cost2Count = clamp(integer(json, "cost2Count", 0), 0, 64);
            int resultCount = clamp(integer(json, "resultCount", 1), 1, 64);
            int maxUses = clamp(integer(json, "maxUses", 12), 1, 9999);
            int xp = clamp(integer(json, "xp", 2), 0, 9999);

            float multiplier = number(json, "priceMultiplier", 0.05f);
            if (!Float.isFinite(multiplier) || multiplier < 0.0f) {
                multiplier = 0.05f;
            }

            String cost2 = string(json, "cost2");
            if (cost2.isEmpty() || cost2Count <= 0) {
                cost2 = "";
                cost2Count = 0;
            }

            return new TradeDefinition(
                    level,
                    cost1,
                    cost1Count,
                    cost2,
                    cost2Count,
                    result,
                    resultCount,
                    maxUses,
                    xp,
                    multiplier,
                    json.has("clearExisting") && json.get("clearExisting").getAsBoolean()
            );
        }

        private static String string(JsonObject json, String key) {
            return json.has(key) ? json.get(key).getAsString().trim() : "";
        }

        private static int integer(JsonObject json, String key, int fallback) {
            try {
                return json.has(key) ? json.get(key).getAsInt() : fallback;
            } catch (Exception e) {
                return fallback;
            }
        }

        private static float number(JsonObject json, String key, float fallback) {
            try {
                return json.has(key) ? json.get(key).getAsFloat() : fallback;
            } catch (Exception e) {
                return fallback;
            }
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
