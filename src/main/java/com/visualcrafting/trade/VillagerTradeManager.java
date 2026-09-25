package com.visualcrafting.trade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * VisualCrafting 交易编辑器的运行时后端。
 *
 * 1.21.1 仍使用 VillagerTradesEvent 收集职业交易；交易数据写入
 * world/visualcrafting/trades/<profession>/<index>.json 后，在 /reload 时重新注入。
 *
 * JSON:
 * {
 *   "level": 1,
 *   "cost1": "minecraft:emerald",
 *   "cost1Count": 2,
 *   "cost2": "",
 *   "cost2Count": 0,
 *   "result": "minecraft:bread",
 *   "resultCount": 4,
 *   "maxUses": 12,
 *   "xp": 2,
 *   "priceMultiplier": 0.05,
 *   "clearExisting": false,
 *   "disabled": false
 * }
 */
@EventBusSubscriber(modid = "visualcrafting", bus = EventBusSubscriber.Bus.GAME)
public final class VillagerTradeManager {
    private static final Gson GSON = new GsonBuilder().create();

    private VillagerTradeManager() {}

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        ResourceLocation professionId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(event.getType());
        if (professionId == null) return;

        File worldRoot = ServerLifecycleHooks.getCurrentServer() == null ? null : ServerLifecycleHooks.getCurrentServer().getWorldPath(LevelResource.ROOT).toFile();
        if (worldRoot == null) return;
        File root = new File(worldRoot, "visualcrafting/trades/" + professionId.getNamespace() + "_" + professionId.getPath());
        // 兼容 GUI 使用的旧目录命名：minecraft:farmer -> farmer
        File legacyRoot = new File(worldRoot, "visualcrafting/trades/" + professionId.getPath());
        File dir = root.isDirectory() ? root : legacyRoot;
        if (!dir.isDirectory()) return;

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) return;
        Arrays.sort(files, Comparator.comparing(VillagerTradeManager::fileIndex).thenComparing(File::getName));

        for (File file : files) {
            try {
                JsonObject json = JsonParser.parseString(Files.readString(file.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
                if (json.has("disabled") && json.get("disabled").getAsBoolean()) continue;

                int level = clamp(json.has("level") ? json.get("level").getAsInt() : 1, 1, 5);
                List<VillagerTrades.ItemListing> list = event.getTrades().get(level);
                if (list == null) continue;

                if (json.has("clearExisting") && json.get("clearExisting").getAsBoolean()) {
                    list.clear();
                }

                ItemStack cost1 = readItem(json, "cost1", "cost1Count", true);
                ItemStack cost2 = readItem(json, "cost2", "cost2Count", false);
                ItemStack result = readItem(json, "result", "resultCount", true);
                if (cost1.isEmpty() || result.isEmpty()) {
                    System.err.println("[VisualCrafting] Skip invalid villager trade: " + file);
                    continue;
                }

                int maxUses = clamp(json.has("maxUses") ? json.get("maxUses").getAsInt() : 12, 1, 9999);
                int xp = clamp(json.has("xp") ? json.get("xp").getAsInt() : 2, 0, 9999);
                float multiplier = json.has("priceMultiplier") ? json.get("priceMultiplier").getAsFloat() : 0.05f;
                if (!Float.isFinite(multiplier)) multiplier = 0.05f;
                multiplier = Math.max(0.0f, multiplier);

                VillagerTrades.ItemListing listing;
                if (cost2.isEmpty()) {
                    listing = new BasicItemListing(cost1, result, maxUses, xp, multiplier);
                } else {
                    listing = new BasicItemListing(cost1, cost2, result, maxUses, xp, multiplier);
                }
                list.add(listing);
            } catch (Exception e) {
                System.err.println("[VisualCrafting] Failed to load villager trade " + file + ": " + e.getMessage());
            }
        }
    }

    private static ItemStack readItem(JsonObject json, String idKey, String countKey, boolean required) {
        String id = json.has(idKey) ? json.get(idKey).getAsString().trim() : "";
        if (id.isEmpty()) return required ? ItemStack.EMPTY : ItemStack.EMPTY;
        try {
            ResourceLocation location = ResourceLocation.parse(id);
            Item item = BuiltInRegistries.ITEM.get(location);
            if (item == null || item == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
            int count = clamp(json.has(countKey) ? json.get(countKey).getAsInt() : 1, 1, 64);
            return new ItemStack(item, count);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private static int fileIndex(File file) {
        String name = file.getName();
        if (name.endsWith(".json")) name = name.substring(0, name.length() - 5);
        try { return Integer.parseInt(name); }
        catch (NumberFormatException e) { return Integer.MAX_VALUE; }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
