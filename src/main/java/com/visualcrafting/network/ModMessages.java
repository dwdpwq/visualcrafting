package com.visualcrafting.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.visualcrafting.block.VisualCraftingBlockEntity;
import com.visualcrafting.recipe.RecipeRegistrar;
import com.visualcrafting.screen.VisualCraftingMenu;
import com.visualcrafting.screen.VisualCraftingScreen;
import com.visualcrafting.trade.TradeRefreshEvents;
import com.visualcrafting.trade.VisualCraftingJobSiteHandler;
import com.visualcrafting.trade.VisualCraftingTradeHandler;
import com.visualcrafting.worldgen.BlockDisableRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.util.RandomSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModMessages {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModMessages.class);

    // Packet type IDs
    public static final ResourceLocation ADD_RECIPE_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "add_recipe");
    public static final ResourceLocation REMOVE_RECIPE_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "remove_recipe");
    public static final ResourceLocation SYNC_RECIPES_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "sync_recipes");
    public static final ResourceLocation DELETE_BY_OUTPUT_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "delete_by_output");
    public static final ResourceLocation TIER_UPDATE_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "tier_update");
    public static final ResourceLocation FORMAT_UPDATE_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "format_update");
    public static final ResourceLocation MODE_UPDATE_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "mode_update");
    public static final ResourceLocation ADD_INFUSING_RECIPE_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "add_infusing_recipe");
    public static final ResourceLocation REMOVE_INFUSING_RECIPE_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "remove_infusing_recipe");
    public static final ResourceLocation DELETE_INFUSING_BY_OUTPUT_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "delete_infusing_by_output");
    public static final ResourceLocation SYNC_INFUSING_RECIPES_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "sync_infusing_recipes");
    public static final ResourceLocation REQUEST_DIM_BIOMES_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "request_dim_biomes");
    public static final ResourceLocation SYNC_DIM_BIOMES_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "sync_dim_biomes");
    public static final ResourceLocation REQUEST_MODE4_DATA_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "request_mode4_data");
    public static final ResourceLocation SYNC_MODE4_DATA_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "sync_mode4_data");
    public static final ResourceLocation SAVE_TRADE_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "save_trade");
    public static final ResourceLocation SAVE_TRADE_RESPONSE_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "save_trade_response");
    public static final ResourceLocation DELETE_TRADE_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "delete_trade");
    public static final ResourceLocation DELETE_TRADE_RESPONSE_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "delete_trade_response");
    public static final ResourceLocation REQUEST_TRADE_LIST_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "request_trade_list");
    public static final ResourceLocation SYNC_TRADE_LIST_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "sync_trade_list");
    public static final ResourceLocation CLEAR_TRADE_LEVEL_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "clear_trade_level");
    public static final ResourceLocation CLEAR_TRADE_LEVEL_RESPONSE_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "clear_trade_level_response");
    public static final ResourceLocation DISABLE_BLOCK_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "disable_block");
    public static final ResourceLocation SYNC_DISABLED_BLOCKS_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "sync_disabled_blocks");
    public static final ResourceLocation REQUEST_DISABLED_BLOCKS_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "request_disabled_blocks");
    public static final ResourceLocation APPLY_MODE8_CHANGE_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "apply_mode8_change");

    private static DimensionBiomesData cachedDimBiomesData;

    /** 客户端缓存的运行时 Block 级禁用名单（由 SyncDisabledBlocksPacket 维护，GUI 据此判断封禁/解封）。 */
    private static Set<ResourceLocation> cachedDisabledBlocks = Set.of();

    public static Set<ResourceLocation> getCachedDisabledBlocks() {
        return cachedDisabledBlocks;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // ===== Registration =====

    public static void register(IEventBus modBus) {
        modBus.addListener(ModMessages::onRegister);
    }

    private static void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(AddRecipePacket.TYPE, AddRecipePacket.STREAM_CODEC, ModMessages::handleAddRecipe);
        registrar.playToServer(RemoveRecipePacket.TYPE, RemoveRecipePacket.STREAM_CODEC, ModMessages::handleRemoveRecipe);
        registrar.playToServer(DeleteByOutputPacket.TYPE, DeleteByOutputPacket.STREAM_CODEC, ModMessages::handleDeleteByOutput);
        registrar.playToClient(SyncRecipesPacket.TYPE, SyncRecipesPacket.STREAM_CODEC, ModMessages::handleSyncRecipes);
        registrar.playToServer(TierUpdatePacket.TYPE, TierUpdatePacket.STREAM_CODEC, ModMessages::handleTierUpdate);
        registrar.playToServer(FormatUpdatePacket.TYPE, FormatUpdatePacket.STREAM_CODEC, ModMessages::handleFormatUpdate);
        registrar.playToServer(ModeUpdatePacket.TYPE, ModeUpdatePacket.STREAM_CODEC, ModMessages::handleModeUpdate);
        registrar.playToServer(AddInfusingRecipePacket.TYPE, AddInfusingRecipePacket.STREAM_CODEC,
                ModMessages::handleAddInfusingRecipe);
        registrar.playToServer(RemoveInfusingRecipePacket.TYPE, RemoveInfusingRecipePacket.STREAM_CODEC,
                ModMessages::handleRemoveInfusingRecipe);
        registrar.playToServer(DeleteInfusingByOutputPacket.TYPE, DeleteInfusingByOutputPacket.STREAM_CODEC,
                ModMessages::handleDeleteInfusingByOutput);
        registrar.playToClient(SyncInfusingRecipesPacket.TYPE, SyncInfusingRecipesPacket.STREAM_CODEC,
                ModMessages::handleSyncInfusingRecipes);
        registrar.playToServer(RequestDimBiomesPacket.TYPE, RequestDimBiomesPacket.STREAM_CODEC,
                ModMessages::handleRequestDimBiomes);
        registrar.playToClient(SyncDimBiomesPacket.TYPE, SyncDimBiomesPacket.STREAM_CODEC,
                ModMessages::handleSyncDimBiomes);
        registrar.playToServer(RequestMode4DataPacket.TYPE, RequestMode4DataPacket.STREAM_CODEC,
                ModMessages::handleRequestMode4Data);
        registrar.playToClient(SyncMode4DataPacket.TYPE, SyncMode4DataPacket.STREAM_CODEC,
                ModMessages::handleSyncMode4Data);
        registrar.playToServer(SaveTradePacket.TYPE, SaveTradePacket.STREAM_CODEC,
                ModMessages::handleSaveTrade);
        registrar.playToClient(SaveTradeResponsePacket.TYPE, SaveTradeResponsePacket.STREAM_CODEC,
                ModMessages::handleSaveTradeResponse);
        registrar.playToServer(RequestDeleteTradePacket.TYPE, RequestDeleteTradePacket.STREAM_CODEC,
                ModMessages::handleDeleteTrade);
        registrar.playToClient(DeleteTradeResponsePacket.TYPE, DeleteTradeResponsePacket.STREAM_CODEC,
                ModMessages::handleDeleteTradeResponse);
        registrar.playToServer(RequestTradeListPacket.TYPE, RequestTradeListPacket.STREAM_CODEC,
                ModMessages::handleRequestTradeList);
        registrar.playToClient(SyncTradeListPacket.TYPE, SyncTradeListPacket.STREAM_CODEC,
                ModMessages::handleSyncTradeList);
        registrar.playToServer(ClearTradeLevelPacket.TYPE, ClearTradeLevelPacket.STREAM_CODEC,
                ModMessages::handleClearTradeLevel);
        registrar.playToClient(ClearTradeLevelResponsePacket.TYPE, ClearTradeLevelResponsePacket.STREAM_CODEC,
                ModMessages::handleClearTradeLevelResponse);
        registrar.playToServer(DisableBlockPacket.TYPE, DisableBlockPacket.STREAM_CODEC, ModMessages::handleDisableBlock);
        registrar.playToServer(RequestDisabledBlocksPacket.TYPE, RequestDisabledBlocksPacket.STREAM_CODEC,
                ModMessages::handleRequestDisabledBlocks);
        registrar.playToClient(SyncDisabledBlocksPacket.TYPE, SyncDisabledBlocksPacket.STREAM_CODEC,
                ModMessages::handleSyncDisabledBlocks);
        registrar.playToServer(ApplyMode8ChangePacket.TYPE, ApplyMode8ChangePacket.STREAM_CODEC,
                ModMessages::handleApplyMode8Change);
    }

    // ===== Utility =====

    /** Maximum interaction distance (blocks) for operating a visual crafting table. */
    private static final int MAX_INTERACTION_DISTANCE = 8;

    /**
     * Validate that the packet's block position refers to a visual crafting table
     * in the player's own world, within an allowed interaction distance.
     * Also assigns the table owner on first interaction.
     */
    private static VisualCraftingBlockEntity getAccessibleTable(ServerPlayer player, BlockPos pos) {
        if (player == null || pos == null) return null;
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) return null;
        if (!serverLevel.isLoaded(pos)) return null;
        if (!(serverLevel.getBlockEntity(pos) instanceof VisualCraftingBlockEntity vcBe)) return null;
        if (vcBe.isRemoved()) return null;
        double distSqr = player.blockPosition().distSqr(pos);
        if (distSqr > (double) MAX_INTERACTION_DISTANCE * MAX_INTERACTION_DISTANCE) {
            LOGGER.error("[VisualCrafting] Rejected packet: table out of range at " + pos);
            return null;
        }
        // 归属校验：已有主人的工作台只允许主人操作，防止越权读写他人配方
        UUID owner = vcBe.getOwnerId();
        if (owner != null && !owner.equals(player.getUUID())) {
            LOGGER.error("[VisualCrafting] Rejected packet: table at " + pos
                    + " belongs to " + owner + ", sender=" + player.getUUID());
            return null;
        }
        vcBe.ensureOwner(player.getUUID());
        RecipeRegistrar.setPlayerName(player.getUUID(), player.getGameProfile().getName());
        return vcBe;
    }

    /** Validate a player-supplied profile id to prevent path traversal. */
    private static boolean isValidProfileId(String profId) {
        if (profId == null || profId.isEmpty()) return false;
        if ("__wandering_generic__".equals(profId) || "__wandering_rare__".equals(profId)) return true;
        if (profId.contains("..") || profId.contains("/") || profId.contains("\\") || profId.contains(":")) {
            return profId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+");
        }
        return profId.matches("[A-Za-z0-9_\\-]+");
    }

    /** 保留完整 registry id；旧版仅保存 path 的配置仍可通过 legacyProfileId 读取。 */
    private static String normalizeProfileId(String profId) {
        return profId == null ? null : profId.trim();
    }

    /** 将 registry id 安全映射为目录名，例如 examplemod:alchemist -> examplemod__alchemist。 */
    private static String profileDirectoryId(String profId) {
        return profId == null ? null : profId.replace(":", "__");
    }

    /** job_sites 文件名使用去命名空间规则（与 VisualCraftingJobSiteHandler.normalize 一致），
     *  例如 minecraft:librarian -> librarian；与 trades 目录的 __ 替换规则不同。 */
    private static String jobSiteDirectoryId(String profId) {
        int colon = profId == null ? -1 : profId.indexOf(':');
        return colon >= 0 ? profId.substring(colon + 1) : profId;
    }

    /** 读取 world/visualcrafting/job_sites/<prof>.json 中的 block 字段，无配置返回 null。 */
    private static String loadJobSiteBlock(File worldDir, String profId) {
        try {
            File file = new File(new File(new File(worldDir, "visualcrafting"), "job_sites"),
                    jobSiteDirectoryId(profId) + ".json");
            if (!file.isFile()) return null;
            JsonObject json = JsonParser.parseString(
                    Files.readString(file.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
            return json.has("block") ? json.get("block").getAsString().trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** 遍历 job_sites 目录下其他职业 json，返回占用相同 block 的职业目录名；无重复返回 null。 */
    private static String findDuplicateJobSiteBlock(File jobSiteDir, String profId, String block) {
        File[] files = jobSiteDir == null ? null : jobSiteDir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return null;
        String self = jobSiteDirectoryId(profId) + ".json";
        for (File f : files) {
            if (self.equalsIgnoreCase(f.getName())) continue;
            try {
                JsonObject json = JsonParser.parseString(
                        Files.readString(f.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
                String b = json.has("block") ? json.get("block").getAsString().trim() : null;
                if (block.equals(b)) return f.getName().replace(".json", "");
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static File getTradeProfessionDirectory(File worldDir, String profId, boolean create) {
        File tradesRoot = new File(new File(worldDir, "visualcrafting"), "trades");
        File canonicalDir = new File(tradesRoot, profileDirectoryId(profId));
        if (profId != null && profId.startsWith("minecraft:")) {
            File legacyDir = new File(tradesRoot, profId.substring(profId.indexOf(':') + 1));
            // 兼容旧版目录：即使新版目录已经被创建，只要新版还没有交易文件，
            // 仍优先读取旧目录，避免“职业能选中但交易列表为空”。
            if (hasTradeJsonFiles(legacyDir) && !hasTradeJsonFiles(canonicalDir)) {
                return legacyDir;
            }
        }
        if (create) canonicalDir.mkdirs();
        return canonicalDir;
    }

    private static boolean hasTradeJsonFiles(File dir) {
        if (dir == null || !dir.isDirectory()) return false;
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        return files != null && files.length > 0;
    }

    private static void deleteTradeLevelFiles(File profDir, int level) {
        File[] files = profDir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return;
        for (File file : files) {
            try {
                JsonObject json = JsonParser.parseString(Files.readString(file.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
                int fileLevel = Math.clamp(json.has("level") ? json.get("level").getAsInt() : 1, 1, 5);
                if (fileLevel == level) file.delete();
            } catch (Exception ignored) {}
        }
    }

    /** 交易文件名（纯数字）转编号；非数字文件名排到末尾。 */
    private static int tradeFileIndex(File f) {
        String base = f.getName();
        if (base.endsWith(".json")) base = base.substring(0, base.length() - ".json".length());
        try {
            return Integer.parseInt(base);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * 自定义交易 ID 索引（阶段1 TradeIdRegistry 基础）。
     * profId -> (trade 字符串 ID -> 文件编号集合)。
     * ID 由「职业 + 交易货币 + 结果」推导（profId|buyItemId|sellItemId[|buyItem2Id]），
     * 相同交易的重复条目共享同一 ID，因此值必须支持一对多（List）。
     * 删除时按 GUI 文件编号定位 + ID 校验，精确删单条、不误删同 ID 其他条。
     * 构建：handleRequestTradeList 全量扫描目录时重建（一次遍历同时完成旧 UUID 文件 id 迁移）；
     * 增量维护：handleSaveTrade / handleDeleteTrade 写盘成功后立即 put / remove。
     */
    private static final Map<String, Map<String, List<Integer>>> CUSTOM_TRADE_ID_INDEX = new ConcurrentHashMap<>();

    /** 重置某职业的自定义交易 ID 索引（列表全量扫描前调用，避免残留过期映射）。 */
    private static void resetCustomTradeIndex(String profId) {
        if (profId != null) CUSTOM_TRADE_ID_INDEX.remove(profId);
    }

    /** 读取交易文件 json 中的 id 字段；无 id 或读取失败返回 null。 */
    private static String readTradeId(File tradeFile) {
        try {
            JsonObject json = JsonParser.parseString(
                    Files.readString(tradeFile.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
            if (!json.has("id")) return null;
            String id = json.get("id").getAsString();
            return (id == null || id.isEmpty()) ? null : id;
        } catch (Exception e) {
            return null;
        }
    }

    /** 按交易 id 扫描目录文件，返回第一个内容 id 匹配的文件；无匹配返回 null。 */
    private static File findTradeFileById(File[] tradeFiles, String id) {
        if (tradeFiles == null || id == null) return null;
        for (File f : tradeFiles) {
            if (id.equals(readTradeId(f))) return f;
        }
        return null;
    }

    /** 判断是否为旧版随机 UUID id（迁移前格式）。 */
    private static boolean isLegacyUuidId(String id) {
        if (id == null || id.isEmpty()) return false;
        try {
            UUID.fromString(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 由「职业 + 交易货币 + 结果」推导可读字符串 ID：
     * profId|buyItemId|sellItemId[|buyItem2Id]。
     * 物品使用注册名（如 minecraft:emerald），不含数量/NBT；
     * buyItem2（cost2）为空时省略。相同交易必然生成相同 ID（允许共享）。
     */
    private static String generateTradeId(String profId, JsonObject tradeJson) {
        StringBuilder sb = new StringBuilder();
        sb.append(profId == null ? "?" : profId);
        String buy = tradeJson.has("cost1") ? tradeJson.get("cost1").getAsString() : "";
        String sell = tradeJson.has("result") ? tradeJson.get("result").getAsString() : "";
        sb.append('|').append((buy == null || buy.isEmpty()) ? "?" : buy);
        sb.append('|').append((sell == null || sell.isEmpty()) ? "?" : sell);
        if (tradeJson.has("cost2")) {
            String buy2 = tradeJson.get("cost2").getAsString();
            if (buy2 != null && !buy2.isEmpty()) {
                sb.append('|').append(buy2);
            }
        }
        return sb.toString();
    }

    /**
     * 生成带重复解歧的最终 ID：先得基础 ID（profId|buy|sell[|buy2]），
     * 再检测同 profId 下已存在的相同基础 ID 家族数量：
     * - 家族内无任何条目：返回 base（第一条无后缀）；
     * - 已有 N 条：从后缀 N 起，若 base|N 已被占用则顺延（避免删除中间项后撞号）。
     * 后缀为自然数（从 1 开始），保存/创建时一次性定死并写入文件，不做位置重算。
     */
    private static String generateTradeIdWithSuffix(String profId, JsonObject tradeJson) {
        String base = generateTradeId(profId, tradeJson);
        if (profId == null) return base;
        Map<String, List<Integer>> idx = CUSTOM_TRADE_ID_INDEX.get(profId);
        if (idx == null || idx.isEmpty()) return base;
        int count = 0;
        for (String key : idx.keySet()) {
            if (key.equals(base) || key.startsWith(base + "|")) count++;
        }
        if (count == 0) return base;
        int suffix = count;
        while (idx.containsKey(base + "|" + suffix)) suffix++;
        return base + "|" + suffix;
    }

    /** 提取交易三元组（cost1/cost2/result 注册名）用于编辑前后比较；返回 key 或 null。 */
    private static String tradeTripleKey(JsonObject json) {
        if (json == null) return null;
        StringBuilder sb = new StringBuilder();
        sb.append(json.has("cost1") ? json.get("cost1").getAsString() : "");
        sb.append('|');
        sb.append(json.has("cost2") ? json.get("cost2").getAsString() : "");
        sb.append('|');
        sb.append(json.has("result") ? json.get("result").getAsString() : "");
        return sb.toString();
    }

    /** 增量维护索引：保存/迁移后追加（id -> 文件编号，去重）。 */
    private static void putCustomTradeIndex(String profId, String id, int fileIndex) {
        if (profId == null || id == null) return;
        List<Integer> list = CUSTOM_TRADE_ID_INDEX.computeIfAbsent(profId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(id, k -> java.util.Collections.synchronizedList(new ArrayList<>()));
        if (!list.contains(fileIndex)) list.add(fileIndex);
    }

    /** 增量维护索引：删除成功后按编号移除单条；该 ID 无剩余编号时移除整个键。 */
    private static void removeCustomTradeIndex(String profId, String id, int fileIndex) {
        if (profId == null || id == null) return;
        Map<String, List<Integer>> idx = CUSTOM_TRADE_ID_INDEX.get(profId);
        if (idx == null) return;
        List<Integer> list = idx.get(id);
        if (list == null) return;
        list.remove((Integer) fileIndex);
        if (list.isEmpty()) idx.remove(id);
    }

    /** 查询索引：返回 id 对应的全部文件编号；未命中返回 null。 */
    private static List<Integer> getCustomTradeIndex(String profId, String id) {
        if (profId == null || id == null) return null;
        Map<String, List<Integer>> idx = CUSTOM_TRADE_ID_INDEX.get(profId);
        return idx == null ? null : idx.get(id);
    }

    /**
     * 将玩家配方编辑写入暂存目录（pending），供 MergeManager 磁盘合并。
     * 文件名规则：{玩家UUID}_visualcrafting_{产出物归属mod}.json
     * 写失败仅打印日志，不中断主流程。
     */
    /** 合并/数据包通道保留备查：当前所有编辑动作统一走 RecipeRegistrar 脚本输出，未再调用。 */
    @SuppressWarnings("unused")
    private static void writePending(ServerPlayer player, String recipeId, JsonObject content) {
        try {
            Path pendingDir = player.server.getWorldPath(LevelResource.ROOT)
                    .resolve("visualcrafting").resolve("pending");
            Files.createDirectories(pendingDir);
            String mod = recipeId.contains(":") ? recipeId.split(":", 2)[0] : "minecraft";
            // 文件名带上配方 id：同一玩家同一 mod 的不同配方不再互相覆盖
            String safeRecipe = recipeId.replaceAll("[^A-Za-z0-9._-]", "_");
            if (safeRecipe.length() > 80) safeRecipe = safeRecipe.substring(0, 80);
            String fileName = player.getUUID() + "_visualcrafting_" + mod + "_" + safeRecipe + ".json";
            JsonObject root = new JsonObject();
            root.addProperty("player", player.getUUID().toString());
            root.addProperty("recipeId", recipeId);
            root.addProperty("timestamp", System.currentTimeMillis());
            root.add("content", content);
            Files.writeString(pendingDir.resolve(fileName), GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error("[VisualCrafting] 写入暂存配方失败: " + e.getMessage());
        }
    }

    /**
     * 构建合成配方 JSON（数据包格式），供 writePending 写入 content。
     * shaped 且 ingredients 为完美方阵（side>=3）时生成 crafting_shaped，
     * 否则生成 crafting_shapeless；pattern/key 算法与 RecipeRegistrar.generateShaped 一致。
     */
    private static JsonObject buildRecipeJson(VisualCraftingBlockEntity.SavedRecipe r, boolean shaped,
                                              boolean saveNbt, net.minecraft.core.RegistryAccess registries) {
        String outputId = BuiltInRegistries.ITEM.getKey(r.result.getItem()).toString();
        int count = r.result.getCount();
        JsonObject root = new JsonObject();

        int side = (int) Math.sqrt(r.ingredients.size());
        boolean perfectGrid = side * side == r.ingredients.size() && side >= 3;

        if (shaped && perfectGrid) {
            root.addProperty("type", "minecraft:crafting_shaped");

            int minRow = side, maxRow = -1, minCol = side, maxCol = -1;
            for (int rr = 0; rr < side; rr++) {
                for (int cc = 0; cc < side; cc++) {
                    int idx = rr * side + cc;
                    if (idx >= r.ingredients.size() || r.ingredients.get(idx).isEmpty()) continue;
                    minRow = Math.min(minRow, rr);
                    maxRow = Math.max(maxRow, rr);
                    minCol = Math.min(minCol, cc);
                    maxCol = Math.max(maxCol, cc);
                }
            }

            if (minRow > maxRow) {
                // 全空网格 → 退化为 shapeless（正常流程不会出现）
                root.addProperty("type", "minecraft:crafting_shapeless");
                JsonArray ingredients = new JsonArray();
                for (ItemStack s : r.ingredients) {
                    if (!s.isEmpty()) {
                        JsonObject itemObj = new JsonObject();
                        itemObj.addProperty("item", BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
                        ingredients.add(itemObj);
                    }
                }
                root.add("ingredients", ingredients);
            } else {
                JsonArray pattern = new JsonArray();
                LinkedHashMap<String, String> keyMap = new LinkedHashMap<>();
                char nextChar = 'A';
                for (int rr = minRow; rr <= maxRow; rr++) {
                    StringBuilder rowStr = new StringBuilder();
                    for (int cc = minCol; cc <= maxCol; cc++) {
                        int idx = rr * side + cc;
                        if (idx < r.ingredients.size() && !r.ingredients.get(idx).isEmpty()) {
                            String itemId = BuiltInRegistries.ITEM.getKey(r.ingredients.get(idx).getItem()).toString();
                            String key = null;
                            for (Map.Entry<String, String> e : keyMap.entrySet()) {
                                if (e.getValue().equals(itemId)) {
                                    key = e.getKey();
                                    break;
                                }
                            }
                            if (key == null) {
                                key = String.valueOf(nextChar++);
                                keyMap.put(key, itemId);
                            }
                            rowStr.append(key);
                        } else {
                            rowStr.append(' ');
                        }
                    }
                    pattern.add(rowStr.toString());
                }
                root.add("pattern", pattern);

                JsonObject keyObj = new JsonObject();
                for (Map.Entry<String, String> e : keyMap.entrySet()) {
                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("item", e.getValue());
                    keyObj.add(e.getKey(), itemObj);
                }
                root.add("key", keyObj);
            }
        } else {
            root.addProperty("type", "minecraft:crafting_shapeless");
            JsonArray ingredients = new JsonArray();
            for (ItemStack s : r.ingredients) {
                if (!s.isEmpty()) {
                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("item", BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
                    ingredients.add(itemObj);
                }
            }
            root.add("ingredients", ingredients);
        }

        JsonObject result = new JsonObject();
        if (saveNbt && registries != null) {
            try {
                var ops = net.minecraft.resources.RegistryOps.create(
                        com.mojang.serialization.JsonOps.INSTANCE, registries);
                var encoded = ItemStack.CODEC.encodeStart(ops, r.result);
                if (encoded.result().isPresent() && encoded.result().get() instanceof JsonObject encodedObj) {
                    root.add("result", encodedObj);
                    return root;
                }
            } catch (Throwable ignored) {
                // 编码失败时回退到仅 id/count
            }
        }
        result.addProperty("id", outputId);
        result.addProperty("count", count);
        root.add("result", result);
        return root;
    }

    /**
     * 构建灌注配方 JSON（mekanism:metallurgic_infusing），供 writePending 写入 content。
     * inputA 优先取 CUSTOM_DATA 的 chemicalId，空则用物品 ID（与 RecipeRegistrar 一致）；
     * inputB 为空用 minecraft:air。
     */
    private static JsonObject buildInfusingRecipeJson(VisualCraftingBlockEntity.InfusingRecipe r) {
        String inputAStr;
        CustomData customData = r.inputA.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag != null && !tag.isEmpty()) {
                inputAStr = tag.getString("chemicalId");
            } else {
                inputAStr = "";
            }
        } else {
            inputAStr = "";
        }
        if (inputAStr.isEmpty()) {
            inputAStr = r.inputA.isEmpty() ? "minecraft:air"
                    : BuiltInRegistries.ITEM.getKey(r.inputA.getItem()).toString();
        }
        String inputBStr = r.inputB.isEmpty() ? "minecraft:air"
                : BuiltInRegistries.ITEM.getKey(r.inputB.getItem()).toString();
        String outputId = BuiltInRegistries.ITEM.getKey(r.output.getItem()).toString();

        JsonObject root = new JsonObject();
        root.addProperty("type", "mekanism:metallurgic_infusing");

        JsonObject chemicalInput = new JsonObject();
        chemicalInput.addProperty("amount", r.infusionAmount);
        chemicalInput.addProperty("chemical", inputAStr);
        root.add("chemical_input", chemicalInput);

        JsonObject itemInput = new JsonObject();
        itemInput.addProperty("item", inputBStr);
        root.add("item_input", itemInput);

        JsonObject output = new JsonObject();
        output.addProperty("id", outputId);
        root.add("output", output);
        return root;
    }

    private static void syncToWatching(Level level, BlockPos pos, VisualCraftingBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, new ChunkPos(pos),
                    new SyncRecipesPacket(pos, new ArrayList<>(be.getRecipes())));
        }
    }

    private static void syncInfusingToWatching(Level level, BlockPos pos, VisualCraftingBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, new ChunkPos(pos),
                    new SyncInfusingRecipesPacket(pos, new ArrayList<>(be.getInfusingRecipes())));
        }
    }

    public static DimensionBiomesData getCachedDimBiomesData() {
        return cachedDimBiomesData;
    }

    public static void setCachedDimBiomesData(DimensionBiomesData data) {
        cachedDimBiomesData = data;
    }

    // ===== Crafting recipe handlers =====

    private static void handleAddRecipe(AddRecipePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (!(player instanceof ServerPlayer serverPlayer)) return;

            VisualCraftingBlockEntity vcBe = getAccessibleTable(serverPlayer, packet.pos);
            if (vcBe == null) return;

            vcBe.addRecipe(new VisualCraftingBlockEntity.SavedRecipe(
                    packet.shaped, packet.result, packet.ingredients, packet.saveNbt()));

            String outputId = BuiltInRegistries.ITEM.getKey(packet.result.getItem()).toString();
            List<VisualCraftingBlockEntity.SavedRecipe> recipes = vcBe.getRecipes();
            for (int i = recipes.size() - 1; i >= 0; i--) {
                VisualCraftingBlockEntity.SavedRecipe r = recipes.get(i);
                if (!r.banned) continue;
                String rId = BuiltInRegistries.ITEM.getKey(r.result.getItem()).toString();
                if (rId.equals(outputId)) {
                    vcBe.removeRecipe(i);
                }
            }

            RecipeRegistrar.updateTableRecipes(serverPlayer.getUUID(), packet.pos, vcBe.getRecipes(), vcBe.getFormat());
            RecipeRegistrar.regenerateScript(vcBe.getRecipes(), vcBe.getTier(), vcBe.getFormat());
            syncToWatching(serverPlayer.level(), packet.pos, vcBe);

            serverPlayer.displayClientMessage(Component.translatable(
                    packet.shaped ? "gui.visualcrafting.chat.add_shaped" : "gui.visualcrafting.chat.add_shapeless",
                    packet.result.getHoverName().getString()), false);
            // 单一写盘通道：配方由 RecipeRegistrar 直接生成脚本，不再重复写 pending（避免数据包侧重复注册）
            // 自动 reload 已移除：脚本已写入，需手动执行 /reload 后生效
        });
    }

    private static void handleRemoveRecipe(RemoveRecipePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (!(player instanceof ServerPlayer serverPlayer)) return;

            VisualCraftingBlockEntity vcBe = getAccessibleTable(serverPlayer, packet.pos);
            if (vcBe == null) return;

            vcBe.removeRecipe(packet.index);
            RecipeRegistrar.updateTableRecipes(serverPlayer.getUUID(), packet.pos, vcBe.getRecipes(), vcBe.getFormat());
            syncToWatching(serverPlayer.level(), packet.pos, vcBe);

            serverPlayer.displayClientMessage(
                    Component.translatable("gui.visualcrafting.chat.delete_saved_crafting"), false);
        });
    }

    private static void handleDeleteByOutput(DeleteByOutputPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (!(player instanceof ServerPlayer serverPlayer)) return;

            VisualCraftingBlockEntity vcBe = getAccessibleTable(serverPlayer, packet.pos);
            if (vcBe == null) return;

            String outputId = BuiltInRegistries.ITEM.getKey(packet.output.getItem()).toString();
            List<VisualCraftingBlockEntity.SavedRecipe> recipes = vcBe.getRecipes();
            for (int i = recipes.size() - 1; i >= 0; i--) {
                VisualCraftingBlockEntity.SavedRecipe r = recipes.get(i);
                if (r.banned) continue;
                String rId = BuiltInRegistries.ITEM.getKey(r.result.getItem()).toString();
                if (rId.equals(outputId)) {
                    vcBe.removeRecipe(i);
                }
            }

            RecipeRegistrar.updateTableRecipes(serverPlayer.getUUID(), packet.pos, vcBe.getRecipes(), vcBe.getFormat());
            RecipeRegistrar.banOutput(outputId, vcBe.getRecipes(), vcBe.getTier(), vcBe.getFormat());
            syncToWatching(serverPlayer.level(), packet.pos, vcBe);

            serverPlayer.displayClientMessage(
                    Component.translatable("gui.visualcrafting.chat.delete_crafting", packet.output.getHoverName().getString()), false);
        });
    }

    private static void handleSyncRecipes(SyncRecipesPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Screen screen = Minecraft.getInstance().screen;
            if (screen instanceof VisualCraftingScreen vcScreen) {
                vcScreen.updateRecipes(packet.recipes);
            }
        });
    }

    // ===== Tier / Format / Mode handlers =====

    private static void handleTierUpdate(TierUpdatePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            VisualCraftingBlockEntity vcBe = getAccessibleTable(serverPlayer, packet.pos);
            if (vcBe != null) {
                int tier = Math.clamp(packet.tier, 0, 3);
                vcBe.setTier(tier);

                // 3x3 模式不保留隐藏的 4~9 阶输入，避免再次切到 CRT 时旧物品“复活”。
                if (tier == 0 && serverPlayer.containerMenu instanceof VisualCraftingMenu vcMenu
                        && vcMenu.blockPos.equals(packet.pos)) {
                    for (int i = 9; i < VisualCraftingMenu.MAX_GRID; i++) {
                        vcMenu.craftSlots.setItem(i, ItemStack.EMPTY);
                    }
                    vcMenu.broadcastChanges();
                }
            }
        });
    }

    private static void handleFormatUpdate(FormatUpdatePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            VisualCraftingBlockEntity vcBe = getAccessibleTable(serverPlayer, packet.pos);
            if (vcBe == null) return;

            vcBe.setFormat(packet.format);

            // KubeJS 与 CRT 格式切换统一回到 3x3，避免 CRT 终极网格残留。
            vcBe.setTier(0);
            if (serverPlayer.containerMenu instanceof VisualCraftingMenu vcMenu
                    && vcMenu.blockPos.equals(packet.pos)) {
                vcMenu.setTier(0);
                for (int i = 9; i < VisualCraftingMenu.MAX_GRID; i++) {
                    vcMenu.craftSlots.setItem(i, ItemStack.EMPTY);
                }
                vcMenu.broadcastChanges();
            }

            RecipeRegistrar.updateTableRecipes(serverPlayer.getUUID(), packet.pos, vcBe.getRecipes(), vcBe.getFormat());
            RecipeRegistrar.regenerateScript(vcBe.getRecipes(), vcBe.getTier(), vcBe.getFormat());

            serverPlayer.displayClientMessage(Component.literal(
                    packet.format == 0 ? "Switched to KubeJS mode" : "Switched to CraftTweaker mode"), false);
        });
    }

    private static void handleModeUpdate(ModeUpdatePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            VisualCraftingBlockEntity vcBe = getAccessibleTable(serverPlayer, packet.pos);
            if (vcBe != null) {
                vcBe.setMode(packet.mode);
                vcBe.setTier(0);

                if (serverPlayer.containerMenu instanceof VisualCraftingMenu vcMenu
                        && vcMenu.blockPos.equals(packet.pos)) {
                    vcMenu.setTier(0);
                    for (int i = 9; i < VisualCraftingMenu.MAX_GRID; i++) {
                        vcMenu.craftSlots.setItem(i, ItemStack.EMPTY);
                    }
                    vcMenu.broadcastChanges();
                }
            }
        });
    }

    /**
     * Mode 8 is a client-side editor, but the final ItemStack is server-authoritative.
     * Only the components exposed by Mode 8 are accepted from the client.
     */
    private static void handleApplyMode8Change(ApplyMode8ChangePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (!(player instanceof ServerPlayer serverPlayer)) return;

            VisualCraftingBlockEntity table = getAccessibleTable(serverPlayer, packet.pos());
            if (table == null) return;
            if (!(serverPlayer.containerMenu instanceof VisualCraftingMenu vcMenu)
                    || !vcMenu.blockPos.equals(packet.pos())) return;

            Slot slot = vcMenu.slots.get(VisualCraftingMenu.OUTPUT_SLOT);
            ItemStack current = slot.getItem();
            ItemStack requested = packet.stack();

            if (slot == null || current.isEmpty() || requested.isEmpty()) return;
            if (current.getItem() != requested.getItem() || current.getCount() != requested.getCount()) {
                LOGGER.error("[VisualCrafting] Rejected Mode 8 packet: item identity/count changed");
                return;
            }

            ItemStack sanitized = current.copy();

            if (requested.has(DataComponents.MAX_DAMAGE)) {
                int maxDamage = requested.getOrDefault(DataComponents.MAX_DAMAGE, 0);
                if (maxDamage < -1) return;
                sanitized.set(DataComponents.MAX_DAMAGE, maxDamage);
            }

            if (requested.has(DataComponents.TOOL)) {
                Tool requestedTool = requested.get(DataComponents.TOOL);
                if (!isValidMode8MiningTool(current, requestedTool)) return;
                sanitized.set(DataComponents.TOOL, requestedTool);
            }

            if (requested.has(DataComponents.ATTRIBUTE_MODIFIERS)) {
                ItemAttributeModifiers requestedMods =
                        requested.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
                if (!isValidMode8Attributes(current, requestedMods)) return;
                sanitized.set(DataComponents.ATTRIBUTE_MODIFIERS, requestedMods);
            }

            if (requested.has(DataComponents.ENCHANTMENTS)) {
                ItemEnchantments requestedEnchants =
                        requested.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                if (!isValidMode8Enchantments(serverPlayer, current, requestedEnchants)) return;
                sanitized.set(DataComponents.ENCHANTMENTS, requestedEnchants);
            }

            slot.set(sanitized);
            slot.setChanged();
            vcMenu.broadcastChanges();
        });
    }

    private static boolean isValidMode8MiningTool(ItemStack current, Tool requested) {
        Tool original = current.get(DataComponents.TOOL);
        if (original == null || requested == null) return false;
        if (!Float.isFinite(requested.defaultMiningSpeed()) || requested.defaultMiningSpeed() < 0.0F || requested.defaultMiningSpeed() > 1000.0F) return false;
        if (requested.damagePerBlock() != original.damagePerBlock()) return false;
        if (requested.rules().size() != 2) return false;
        boolean hasPickaxeRule = false;
        boolean hasTierRule = false;
        for (Tool.Rule rule : requested.rules()) {
            Optional<TagKey<Block>> key = rule.blocks().unwrapKey();
            if (key.isEmpty() || !key.get().location().getNamespace().equals("minecraft")) return false;
            String path = key.get().location().getPath();
            if (path.equals("mineable/pickaxe")) {
                if (!rule.correctForDrops().orElse(false)) return false;
                if (rule.speed().isEmpty() || !Float.isFinite(rule.speed().get()) || Math.abs(rule.speed().get() - requested.defaultMiningSpeed()) > 0.0001F) return false;
                hasPickaxeRule = true;
            } else if (path.startsWith("incorrect_for_") && path.endsWith("_tool")) {
                String tier = path.substring("incorrect_for_".length(), path.length() - "_tool".length());
                if (!Set.of("wood", "stone", "iron", "gold", "diamond", "netherite").contains(tier)) return false;
                if (rule.correctForDrops().orElse(true) || rule.speed().isPresent()) return false;
                hasTierRule = true;
            } else return false;
        }
        return hasPickaxeRule && hasTierRule;
    }

    private static boolean isValidMode8Attributes(ItemStack current, ItemAttributeModifiers requested) {
        ItemAttributeModifiers original =
                current.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        Map<ResourceLocation, ItemAttributeModifiers.Entry> originalById = new HashMap<>();
        for (ItemAttributeModifiers.Entry entry : original.modifiers()) {
            originalById.put(entry.modifier().id(), entry);
        }

        for (ItemAttributeModifiers.Entry entry : requested.modifiers()) {
            ResourceLocation id = entry.modifier().id();
            ItemAttributeModifiers.Entry old = originalById.get(id);

            if (old != null && !id.getNamespace().equals("visualcrafting")
                    && !sameAttributeEntry(old, entry)) return false;
            if (old == null && !id.getNamespace().equals("visualcrafting")) return false;

            double amount = entry.modifier().amount();
            if (!Double.isFinite(amount) || Math.abs(amount) > 1.0E9) return false;
        }
        return true;
    }

    private static boolean sameAttributeEntry(ItemAttributeModifiers.Entry a, ItemAttributeModifiers.Entry b) {
        return a.attribute().equals(b.attribute())
                && a.modifier().equals(b.modifier())
                && a.slot().equals(b.slot());
    }

    private static boolean isValidMode8Enchantments(ServerPlayer player, ItemStack current,
                                                     ItemEnchantments requested) {
        Optional<Registry<Enchantment>> registry =
                player.level().registryAccess().registry(Registries.ENCHANTMENT);
        if (registry.isEmpty()) return false;

        ItemEnchantments original =
                current.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

        for (Holder<Enchantment> holder : requested.keySet()) {
            int level = requested.getLevel(holder);
            if (level <= 0 || level > holder.value().getMaxLevel()) return false;

            int oldLevel = original.getLevel(holder);
            if (oldLevel == 0 && !current.supportsEnchantment(holder)) return false;
        }

        // Mode 8 must not remove existing enchantments as a side effect.
        for (Holder<Enchantment> holder : original.keySet()) {
            if (original.getLevel(holder) > 0 && requested.getLevel(holder) <= 0) return false;
        }
        return true;
    }

    // ===== Infusing recipe handlers =====

    private static void handleAddInfusingRecipe(AddInfusingRecipePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (!(player instanceof ServerPlayer serverPlayer)) return;

            VisualCraftingBlockEntity vcBe = getAccessibleTable(serverPlayer, packet.pos);
            if (vcBe == null) return;

            vcBe.addInfusingRecipe(new VisualCraftingBlockEntity.InfusingRecipe(
                    packet.inputA, packet.inputB, packet.output, packet.infusionAmount));

            String outputId = BuiltInRegistries.ITEM.getKey(packet.output.getItem()).toString();
            List<VisualCraftingBlockEntity.InfusingRecipe> recipes = vcBe.getInfusingRecipes();
            for (int i = recipes.size() - 1; i >= 0; i--) {
                VisualCraftingBlockEntity.InfusingRecipe r = recipes.get(i);
                if (!r.banned) continue;
                String rId = BuiltInRegistries.ITEM.getKey(r.output.getItem()).toString();
                if (rId.equals(outputId)) {
                    vcBe.removeInfusingRecipe(i);
                }
            }

            RecipeRegistrar.updateInfusingTableRecipes(serverPlayer.getUUID(), packet.pos, vcBe.getInfusingRecipes(), vcBe.getFormat());
            RecipeRegistrar.regenerateInfusingScript(vcBe.getInfusingRecipes(), vcBe.getFormat());
            syncInfusingToWatching(serverPlayer.level(), packet.pos, vcBe);

            serverPlayer.displayClientMessage(
                    Component.translatable("gui.visualcrafting.chat.add_infusing", packet.output.getHoverName().getString()), false);

        });
    }

    private static void handleRemoveInfusingRecipe(RemoveInfusingRecipePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (!(player instanceof ServerPlayer serverPlayer)) return;

            VisualCraftingBlockEntity vcBe = getAccessibleTable(serverPlayer, packet.pos);
            if (vcBe == null) return;

            vcBe.removeInfusingRecipe(packet.index);
            RecipeRegistrar.updateInfusingTableRecipes(serverPlayer.getUUID(), packet.pos, vcBe.getInfusingRecipes(), vcBe.getFormat());
            syncInfusingToWatching(serverPlayer.level(), packet.pos, vcBe);

            serverPlayer.displayClientMessage(
                    Component.translatable("gui.visualcrafting.chat.delete_saved_infusing"), false);
        });
    }

    private static void handleDeleteInfusingByOutput(DeleteInfusingByOutputPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (!(player instanceof ServerPlayer serverPlayer)) return;

            VisualCraftingBlockEntity vcBe = getAccessibleTable(serverPlayer, packet.pos);
            if (vcBe == null) return;

            String outputId = BuiltInRegistries.ITEM.getKey(packet.output.getItem()).toString();

            // Create a banned entry from the output item
            vcBe.addInfusingRecipe(new VisualCraftingBlockEntity.InfusingRecipe(packet.output.copy()));

            List<VisualCraftingBlockEntity.InfusingRecipe> recipes = vcBe.getInfusingRecipes();
            for (int i = recipes.size() - 1; i >= 0; i--) {
                VisualCraftingBlockEntity.InfusingRecipe r = recipes.get(i);
                if (r.banned) continue;
                String rId = BuiltInRegistries.ITEM.getKey(r.output.getItem()).toString();
                if (rId.equals(outputId)) {
                    vcBe.removeInfusingRecipe(i);
                }
            }

            RecipeRegistrar.updateInfusingTableRecipes(serverPlayer.getUUID(), packet.pos, vcBe.getInfusingRecipes(), vcBe.getFormat());
            RecipeRegistrar.banInfusingOutput(outputId, vcBe.getInfusingRecipes(), vcBe.getFormat());
            syncInfusingToWatching(serverPlayer.level(), packet.pos, vcBe);

            serverPlayer.displayClientMessage(
                    Component.translatable("gui.visualcrafting.chat.delete_infusing", packet.output.getHoverName().getString()), false);
        });
    }

    private static void handleSyncInfusingRecipes(SyncInfusingRecipesPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Screen screen = Minecraft.getInstance().screen;
            if (screen instanceof VisualCraftingScreen vcScreen) {
                vcScreen.updateInfusingRecipes(packet.recipes);
            }
        });
    }


    // ===== Dimension / Biomes handlers =====

    private static void handleRequestDimBiomes(RequestDimBiomesPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (!(player instanceof ServerPlayer serverPlayer)) return;

            List<String> dimIds = new ArrayList<>();
            LinkedHashMap<String, List<String>> biomesByDim = new LinkedHashMap<>();
            List<String> allBiomes = new ArrayList<>();

            try {
                var registryAccess = serverPlayer.server.registryAccess();
                var dims = registryAccess.registryOrThrow(Registries.LEVEL_STEM);
                var biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);
                TagKey<Biome> tagOverworld = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", "is_overworld"));
                TagKey<Biome> tagNether = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", "is_nether"));
                TagKey<Biome> tagEnd = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", "is_end"));

                for (Map.Entry<ResourceKey<LevelStem>, LevelStem> entry : dims.entrySet()) {
                    String dimId = entry.getKey().location().toString();
                    dimIds.add(dimId);

                    try {
                        List<String> biomeList = new ArrayList<>();
                        TagKey<Biome> categoryTag = null;
                        if (dimId.equals("minecraft:overworld")) {
                            categoryTag = tagOverworld;
                        } else if (dimId.equals("minecraft:the_nether")) {
                            categoryTag = tagNether;
                        } else if (dimId.equals("minecraft:the_end")) {
                            categoryTag = tagEnd;
                        }

                        if (categoryTag != null) {
                            // 主世界/下界/末地：原版标签群系 ∪ biomeSource.possibleBiomes()
                            // 标签保证单群系/虚空世界分类完整；possibleBiomes 纳入 TerraBlender/BOP 等模组动态注入的群系
                            final TagKey<Biome> resolvedTag = categoryTag;
                            biomeRegistry.holders().forEach(holder -> {
                                if (holder.is(resolvedTag)) {
                                    String id = holder.key().location().toString();
                                    biomeList.add(id);
                                    if (!allBiomes.contains(id)) {
                                        allBiomes.add(id);
                                    }
                                }
                            });
                            // 合并 biomeSource 实际可能的群系（含模组动态注入，如 TerraBlender/BOP）
                            var biomeSource = entry.getValue().generator().getBiomeSource();
                            Set<Holder<Biome>> possibleBiomes = biomeSource.possibleBiomes();
                            for (Holder<?> holder : possibleBiomes) {
                                holder.unwrapKey().ifPresent(key -> {
                                    String id = key.location().toString();
                                    if (!biomeList.contains(id)) {
                                        biomeList.add(id);
                                    }
                                    if (!allBiomes.contains(id)) {
                                        allBiomes.add(id);
                                    }
                                });
                            }
                        } else {
                            var biomeSource = entry.getValue().generator().getBiomeSource();
                            Set<Holder<Biome>> biomes = biomeSource.possibleBiomes();
                            for (Holder<?> holder : biomes) {
                                holder.unwrapKey().ifPresent(key -> {
                                    String id = key.location().toString();
                                    biomeList.add(id);
                                    if (!allBiomes.contains(id)) {
                                        allBiomes.add(id);
                                    }
                                });
                            }
                        }
                        biomesByDim.put(dimId, biomeList);
                    } catch (Exception ignored) {
                        biomesByDim.put(dimId, new ArrayList<>());
                    }
                }

                // Include all registered biomes not already covered
                for (var biomeEntry : biomeRegistry.entrySet()) {
                    String id = biomeEntry.getKey().location().toString();
                    if (!allBiomes.contains(id)) {
                        allBiomes.add(id);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[VisualCrafting] Failed to load dim/biome data on server: " + e.getMessage());
            }

            DimensionBiomesData data = new DimensionBiomesData(dimIds, biomesByDim, allBiomes);

            // Save index on server
            try {
                File worldDir = serverPlayer.server.getWorldPath(LevelResource.ROOT).toFile();
                File vcDir = new File(worldDir, "visualcrafting");
                vcDir.mkdirs();
                File indexFile = new File(vcDir, "dim_biomes_index.json");
                Files.writeString(indexFile.toPath(), data.toJson(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                LOGGER.error("[VisualCrafting] Failed to save dim/biome index: " + e.getMessage());
            }

            PacketDistributor.sendToPlayer(serverPlayer, new SyncDimBiomesPacket(data));
        });
    }

    private static void handleSyncDimBiomes(SyncDimBiomesPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // 无论 GUI 是否打开，都更新内存缓存，保证后续打开时命中最新数据
            cachedDimBiomesData = packet.data;
            Screen screen = Minecraft.getInstance().screen;
            if (screen instanceof VisualCraftingScreen vcScreen) {
                vcScreen.applyDimBiomesData(packet.data);
            }

            try {
                File vcDir = new File(Minecraft.getInstance().gameDirectory, "visualcrafting");
                vcDir.mkdirs();
                File cacheFile = new File(vcDir, "dim_biomes_cache.json");
                DimensionBiomesData cacheData = packet.data;
                if (cacheData.sourceVersion == null || cacheData.sourceVersion.isEmpty()) {
                    cacheData.sourceVersion = Minecraft.getInstance().getLaunchedVersion();
                }
                Files.writeString(cacheFile.toPath(), cacheData.toJson(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                LOGGER.error("[VisualCrafting] Failed to save dim/biome client cache: " + e.getMessage());
            }
        });
    }

    // ===== Mode 4 / Trade handlers =====

    private static void handleRequestMode4Data(RequestMode4DataPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (!(player instanceof ServerPlayer serverPlayer)) return;

            List<String> profNames = new ArrayList<>();
            List<String> profIds = new ArrayList<>();
            List<String> mgmtProfNames = new ArrayList<>();
            List<String> mgmtProfIds = new ArrayList<>();
            List<String> mgmtTradeLabels = new ArrayList<>();
            List<Boolean> mgmtTradeDisabled = new ArrayList<>();

            try {
                File worldDir = serverPlayer.server.getWorldPath(LevelResource.ROOT).toFile();
                File vcDir = new File(worldDir, "visualcrafting");
                File mgmtDir = new File(vcDir, "mgmt");
                File[] mgmtFiles = mgmtDir.listFiles((dir, name) -> name.endsWith(".json"));
                if (mgmtFiles != null) {
                    Arrays.sort(mgmtFiles, Comparator.comparing(File::getName));
                    for (File f : mgmtFiles) {
                        String name = f.getName();
                        String label = name.substring(0, name.length() - 5);
                        mgmtProfIds.add(label);
                        mgmtProfNames.add(label);
                        String content = Files.readString(f.toPath(), StandardCharsets.UTF_8);
                        JsonObject obj = JsonParser.parseString(content).getAsJsonObject();
                        boolean disabled = obj.has("disabled") && obj.get("disabled").getAsBoolean();
                        mgmtTradeDisabled.add(disabled);
                        mgmtTradeLabels.add(obj.has("label") ? obj.get("label").getAsString() : label);
                    }
                }

                // 1.21.1 原生 VillagerTradesEvent 可以直接按注册表职业注入交易。
                // GUI 因此始终列出当前运行时所有职业，包括其他模组注册的职业。
                for (ResourceLocation id : BuiltInRegistries.VILLAGER_PROFESSION.keySet()) {
                    String key = id.toString();
                    String normalized = key;
                    if (!profIds.contains(normalized)) {
                        profIds.add(normalized);
                        profNames.add(key);
                    }
                }

                // 兼容历史自定义目录：即使职业当前不在注册表，也不要让已有配置在 GUI 中消失。
                File tradesDir = new File(vcDir, "trades");
                File[] tradeDirs = tradesDir.listFiles(File::isDirectory);
                if (tradeDirs != null) {
                    for (File dir : tradeDirs) {
                        String dirName = dir.getName();
                        // 新版命名空间目录：examplemod__alchemist -> examplemod:alchemist。
                        if (dirName.contains("__")) {
                            String decodedId = dirName.replaceFirst("__", ":");
                            if (!profIds.contains(decodedId)) {
                                profIds.add(decodedId);
                                profNames.add(decodedId);
                            }
                            continue;
                        }
                        // Vanilla 旧版目录（farmer/librarian/...）已由注册表返回，不重复显示。
                        if (profIds.contains("minecraft:" + dirName)) continue;
                        if (!profIds.contains(dirName)) {
                            profIds.add(dirName);
                            profNames.add(dirName);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[VisualCrafting] Failed to load mode4 data: " + e.getMessage());
            }

            PacketDistributor.sendToPlayer(serverPlayer, new SyncMode4DataPacket(
                    profNames, profIds, mgmtProfNames, mgmtProfIds, mgmtTradeLabels, mgmtTradeDisabled));
        });
    }

    private static void handleSyncMode4Data(SyncMode4DataPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Screen screen = Minecraft.getInstance().screen;
            if (screen instanceof VisualCraftingScreen vcScreen) {
                try {
                    java.lang.reflect.Method method = vcScreen.getClass()
                            .getDeclaredMethod("updateMode4Data",
                                    List.class, List.class, List.class, List.class, List.class, List.class);
                    method.setAccessible(true);
                    method.invoke(vcScreen,
                            packet.profNames, packet.profIds, packet.mgmtProfNames, packet.mgmtProfIds,
                            packet.mgmtTradeLabels, packet.mgmtTradeDisabled);
                } catch (Exception e) {
                    LOGGER.error("[VisualCrafting] Client reflection handler failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        });
    }

    private static void handleSaveTrade(SaveTradePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            String profId = normalizeProfileId(packet.profId);
            if (!isValidProfileId(profId)) {
                LOGGER.error("[VisualCrafting] Rejected trade save with invalid profile id: " + packet.profId);
                return;
            }

            try {
                File worldDir = serverPlayer.server.getWorldPath(LevelResource.ROOT).toFile();
                JsonObject tradeJson = JsonParser.parseString(packet.tradeJson).getAsJsonObject();

                // 职业方块槽位：非空写 job_sites/<prof>.json（{"block":"minecraft:lectern"}），
                // 显式清除（jobSiteCleared）删除该文件；槽位为空且未清除则不修改配置，
                // 避免普通交易保存时误删已配置的职业方块。
                // 文件名使用去命名空间规则，与 VisualCraftingJobSiteHandler.normalize 一致。
                File jobSiteDir = new File(new File(worldDir, "visualcrafting"), "job_sites");
                File jobSiteFile = new File(jobSiteDir, jobSiteDirectoryId(profId) + ".json");
                if (packet.jobSiteCleared) {
                    // 修复3：显式清除分支日志增强——区分 cleared / not-found / delete-failed。
                    if (jobSiteFile.isFile()) {
                        if (jobSiteFile.delete()) {
                            LOGGER.info("[VisualCrafting] job-site cleared for " + profId
                                    + " (deleted " + jobSiteFile.getAbsolutePath() + ")");
                            VisualCraftingJobSiteHandler.refreshCache(serverPlayer.server);
                        } else {
                            LOGGER.error("[VisualCrafting] job-site delete-failed for " + profId
                                    + " at " + jobSiteFile.getAbsolutePath());
                        }
                    } else {
                        LOGGER.info("[VisualCrafting] job-site clear requested but not-found for " + profId
                                + " (" + jobSiteFile.getAbsolutePath() + ")");
                    }
                    PacketDistributor.sendToPlayer(serverPlayer, new SaveTradeResponsePacket());
                    return;
                }
                if (packet.jobSite != null && !packet.jobSite.isEmpty()) {
                    // 重复职业方块校验：同一方块不能被多个职业同时占用（当前职业自身更新允许）。
                    String dupProf = findDuplicateJobSiteBlock(jobSiteDir, profId, packet.jobSite);
                    if (dupProf != null) {
                        serverPlayer.displayClientMessage(
                                Component.literal("该职业方块已被职业 " + dupProf + " 使用，职业方块未更新"),
                                false);
                    } else {
                        // 修复3：set 分支日志增强——写盘成功/失败明确区分。
                        jobSiteDir.mkdirs();
                        JsonObject jobJson = new JsonObject();
                        jobJson.addProperty("block", packet.jobSite);
                        try {
                            Files.writeString(jobSiteFile.toPath(), GSON.toJson(jobJson), StandardCharsets.UTF_8);
                            LOGGER.info("[VisualCrafting] job-site set " + packet.jobSite + " for " + profId
                                    + " (" + jobSiteFile.getAbsolutePath() + ")");
                            VisualCraftingJobSiteHandler.refreshCache(serverPlayer.server);
                        } catch (Exception we) {
                            LOGGER.error("[VisualCrafting] job-site write-failed for " + profId
                                    + " block=" + packet.jobSite + ": " + we.getMessage());
                        }
                    }
                } else {
                    // 槽位为空且未显式清除：保留服务端现有配置。
                    LOGGER.info("[VisualCrafting] job-site untouched for " + profId);
                }

                // 纯职业方块操作包（tradeJson 为 "{}"，不含任何交易字段）：job-site 已处理，
                // 直接返回，避免把空对象当作交易保存到 trades 目录。
                if (!tradeJson.has("cost1") && !tradeJson.has("result")
                        && !tradeJson.has("override") && !tradeJson.has("editIndex")) {
                    PacketDistributor.sendToPlayer(serverPlayer, new SaveTradeResponsePacket());
                    return;
                }

                boolean override = tradeJson.has("override") && tradeJson.get("override").getAsBoolean();

                if (override) {
                    int index = Math.max(0, tradeJson.has("overrideIndex")
                            ? tradeJson.get("overrideIndex").getAsInt() : 0);
                    int level = Math.clamp(tradeJson.has("level")
                            ? tradeJson.get("level").getAsInt() : 1, 1, 5);

                    File overrideFile;
                    if ("__wandering_generic__".equals(profId) || "__wandering_rare__".equals(profId)
                            || "minecraft:wandering_trader".equals(profId)) {
                        String pool = "__wandering_generic__".equals(profId)
                                ? "generic"
                                : ("__wandering_rare__".equals(profId) ? "rare" : (level == 2 ? "rare" : "generic"));
                        File dir = new File(new File(new File(worldDir, "visualcrafting"),
                                "trade_overrides"), "wandering");
                        dir.mkdirs();
                        overrideFile = new File(dir, pool + "-" + index + ".json");
                    } else {
                        File dir = new File(new File(new File(worldDir, "visualcrafting"),
                                "trade_overrides/villager"), profileDirectoryId(profId));
                        dir.mkdirs();
                        overrideFile = new File(dir, level + "-" + index + ".json");
                    }

                    tradeJson.remove("override");
                    tradeJson.remove("overrideIndex");
                    Files.writeString(overrideFile.toPath(), GSON.toJson(tradeJson), StandardCharsets.UTF_8);
                    // 用户重新保存覆盖记录 = 撤销该条目的删除标记，避免 reload 后交易仍被过滤
                    clearRuntimeDeleteMark(worldDir, profId, level, index);
                    serverPlayer.displayClientMessage(
                            Component.literal("已修改交易 #" + (index + 1)), false);
                    PacketDistributor.sendToPlayer(serverPlayer, new SaveTradeResponsePacket());
                    return;
                }

                File profDir = getTradeProfessionDirectory(worldDir, profId, true);
                profDir.mkdirs();

                // 序号取现有最大编号 +1：删除中间的条目后不会重号、不会覆盖既有交易
                int nextIndex = 0;
                File[] existing = profDir.listFiles((d, name) -> name.endsWith(".json"));
                if (existing != null) {
                    for (File f : existing) {
                        String base = f.getName();
                        if (base.endsWith(".json")) {
                            base = base.substring(0, base.length() - ".json".length());
                        }
                        try {
                            nextIndex = Math.max(nextIndex, Integer.parseInt(base) + 1);
                        } catch (NumberFormatException ignored) {
                            // 非数字文件名不参与编号
                        }
                    }
                }
                // 选中已有自定义交易后保存：直接覆盖原文件，真正实现“修改”，而不是每次新增一条重复交易。
                if (tradeJson.has("editIndex")) {
                    int editIndex = tradeJson.get("editIndex").getAsInt();
                    if (editIndex >= 0) {
                        File editFile = new File(profDir, editIndex + ".json");
                        if (editFile.isFile()) {
                            // 阶段1：编辑时比较三元组（cost1/cost2/result）——
                            // 内容变化则按新内容重新生成 ID（含重复解歧后缀）；
                            // 内容未变则保留原 id（旧文件无 id / 旧 UUID 时按当前内容生成）
                            JsonObject oldJson = null;
                            try {
                                oldJson = JsonParser.parseString(
                                        Files.readString(editFile.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
                            } catch (Exception ignored) {
                            }
                            String oldId = readTradeId(editFile);
                            String newTriple = tradeTripleKey(tradeJson);
                            String existingId;
                            if (oldJson == null || !newTriple.equals(tradeTripleKey(oldJson))) {
                                // 三元组变化（或旧文件不可读）：按新内容重新生成
                                existingId = generateTradeIdWithSuffix(profId, tradeJson);
                            } else {
                                // 三元组未变：保留原 id；无 id / 旧 UUID 时按新内容生成
                                existingId = oldId;
                                if (existingId == null || existingId.isEmpty() || isLegacyUuidId(existingId)) {
                                    existingId = generateTradeIdWithSuffix(profId, tradeJson);
                                }
                            }
                            tradeJson.addProperty("id", existingId);
                            tradeJson.remove("editIndex");
                            Files.writeString(editFile.toPath(), GSON.toJson(tradeJson), StandardCharsets.UTF_8);
                            if (oldId != null && !oldId.isEmpty() && !oldId.equals(existingId)) {
                                removeCustomTradeIndex(profId, oldId, editIndex);
                            }
                            putCustomTradeIndex(profId, existingId, editIndex);
                            serverPlayer.displayClientMessage(
                                    Component.literal("已修改交易 #" + (editIndex + 1)), false);
                            PacketDistributor.sendToPlayer(serverPlayer, new SaveTradeResponsePacket());
                            return;
                        }
                    }
                    tradeJson.remove("editIndex");
                }

                // 阶段1：新增自定义交易一次性定死可读字符串 ID（profId|buyItemId|sellItemId[|buyItem2Id]）。
                // 同 profId 下相同基础 ID 已有条目时追加自然数后缀解歧（第2条起 |1、|2...）
                String tradeId = generateTradeIdWithSuffix(profId, tradeJson);
                tradeJson.addProperty("id", tradeId);

                File tradeFile = new File(profDir, nextIndex + ".json");
                Files.writeString(tradeFile.toPath(), GSON.toJson(tradeJson), StandardCharsets.UTF_8);
                putCustomTradeIndex(profId, tradeId, nextIndex);

                // 自动 reload 已移除：脚本已写入，需手动执行 /reload 后生效
                int tradeLevel = Math.clamp(tradeJson.has("level") ? tradeJson.get("level").getAsInt() : 1, 1, 5);
                StringBuilder addedMsg = new StringBuilder("已添加交易 [Lv").append(tradeLevel).append("] ");
                if (tradeJson.has("result")) {
                    int rCount = tradeJson.has("resultCount") ? tradeJson.get("resultCount").getAsInt() : 1;
                    addedMsg.append(rCount).append("x ").append(shortItemId(tradeJson.get("result").getAsString()));
                } else {
                    addedMsg.append("自定义交易");
                }
                serverPlayer.displayClientMessage(Component.literal(addedMsg.toString()), false);
                PacketDistributor.sendToPlayer(serverPlayer, new SaveTradeResponsePacket());
            } catch (Exception e) {
                LOGGER.error("[VisualCrafting] Failed to save trade: " + e.getMessage());
                serverPlayer.displayClientMessage(
                        Component.literal("交易保存失败：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())), false);
            }
        });
    }

    private static void handleSaveTradeResponse(SaveTradeResponsePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Screen screen = Minecraft.getInstance().screen;
            if (screen instanceof VisualCraftingScreen vcScreen) {
                try {
                    java.lang.reflect.Method method = vcScreen.getClass()
                            .getDeclaredMethod("onSaveTradeResponse");
                    method.setAccessible(true);
                    method.invoke(vcScreen);
                } catch (Exception e) {
                    LOGGER.error("[VisualCrafting] Client reflection handler failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        });
    }

    private static void handleDeleteTrade(RequestDeleteTradePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            String profId = normalizeProfileId(packet.profId);
            if (!isValidProfileId(profId)) {
                LOGGER.error("[VisualCrafting] Rejected trade delete with invalid profile id: " + packet.profId);
                return;
            }

            try {
                File worldDir = serverPlayer.server.getWorldPath(LevelResource.ROOT).toFile();
                // 交易实际存放在 world/visualcrafting/trades/<profId>/（与保存路径一致）
                // 运行时原版/Mod 交易使用负索引；删除对应 override 即恢复原始交易。
                if (packet.tradeIndex < 0) {
                    int runtimeIndex = -packet.tradeIndex - 1;
                    int level = Math.clamp(packet.level, 1, 5);
                    boolean removedOverride = false;
                    File overrideRoot = new File(new File(new File(worldDir, "visualcrafting"),
                            "trade_overrides"), "villager");
                    if ("minecraft:wandering_trader".equals(profId)) {
                        File wanderingDir = new File(new File(worldDir, "visualcrafting"),
                                "trade_overrides/wandering");
                        for (String pool : new String[]{"generic", "rare"}) {
                            File f = new File(wanderingDir, pool + "-" + runtimeIndex + ".json");
                            removedOverride |= f.isFile() && f.delete();
                        }
                    } else {
                        File dir = new File(overrideRoot, profileDirectoryId(profId));
                        File f = new File(dir, level + "-" + runtimeIndex + ".json");
                        removedOverride = f.isFile() && f.delete();
                    }
                    if (removedOverride) {
                        // 缓存保持“override 后完整列表”（原始索引），不做本地移除：
                        // GUI 重拉时该条显示原版内容（override 已删 = 恢复原版）；
                        // 若在此移除缓存会使剩余条目索引左移，与 override/deleted 文件
                        // 按原始索引命名的体系错位，导致后续保存/删除定位到错误条目。
                        serverPlayer.displayClientMessage(
                                Component.literal("已删除交易 #" + (-packet.tradeIndex)
                                        + "，交易列表已同步更新（重进后原版交易将恢复）"), false);
                        // 修复1：删除 override 后同样立即静默 /reload，恢复原版交易即时生效。
                        TradeRefreshEvents.scheduleImmediateReload(serverPlayer.server);
                    } else {
                        // 无 override 覆盖记录：这是原版/Mod 运行时交易，不再提示“不能直接删除”，
                        // 改为写入 deleted 标记（独立目录，与 override 区分），事件注入时按 ID 过滤。
                        String markId = writeRuntimeDeleteMark(serverPlayer, worldDir, profId, level,
                                runtimeIndex, packet.cost1, packet.cost2, packet.result);
                        if (markId != null) {
                            // 缓存保持完整列表（原始索引），不做本地移除：
                            // GUI 重拉时 handleRequestTradeList 按 deleted 标记跳过显示，
                            // 索引仍为原版事件原始位置，后续保存/删除定位不左移，
                            // 不会因 clearRuntimeDeleteMark 撤销相邻条目的删除标记而“恢复已删交易”。
                            serverPlayer.displayClientMessage(
                                    Component.literal("已删除交易 #" + (-packet.tradeIndex)
                                            + "（原版/Mod），交易列表已同步更新"), false);
                            // 修复1：删除成功后立即静默 /reload 重建交易表，过滤即时生效，
                            // 新生成村民不再含被删交易（无需手动 /reload 或重进）。
                            TradeRefreshEvents.scheduleImmediateReload(serverPlayer.server);
                        } else {
                            serverPlayer.displayClientMessage(
                                    Component.literal("未找到交易 #" + (-packet.tradeIndex)
                                            + "（无法识别该原版/Mod 交易的三元组，删除失败）"), false);
                        }
                    }
                    PacketDistributor.sendToPlayer(serverPlayer, new DeleteTradeResponsePacket());
                    return;
                }

                File profDir = getTradeProfessionDirectory(worldDir, profId, true);

                boolean deleted = false;
                File[] tradeFiles = profDir.listFiles((d, name) -> name.endsWith(".json"));
                if (tradeFiles != null) {
                    // 按文件名数字编号精确匹配删除（GUI 下发的是文件编号，编号可能不连续，
                    // 不能按排序后数组下标删除，否则编号有 gap 时会删错/删不到文件）。
                    File target = null;
                    for (File f : tradeFiles) {
                        if (tradeFileIndex(f) == packet.tradeIndex) {
                            target = f;
                            break;
                        }
                    }
                    if (target != null) {
                        // 阶段1：按 GUI 文件编号定位 + ID 校验（CUSTOM_TRADE_ID_INDEX），
                        // 重复项共享同一 ID 时精确删单条、不误删同 ID 其他条；
                        // 旧文件无 id / 旧 UUID（未迁移）退化为编号匹配（兼容路径）。
                        String targetId = readTradeId(target);
                        if (targetId == null || targetId.isEmpty() || isLegacyUuidId(targetId)) {
                            // 旧文件无 id / 旧 UUID：按编号删除（与旧行为一致）
                            deleted = target.delete();
                        } else {
                            File matched = null;
                            // 路径1：缓存命中且编号列表包含 GUI 下发编号（ID 校验通过），
                            // 删除对象就是编号定位的 target 自身，同 ID 其他条不受影响
                            List<Integer> cached = getCustomTradeIndex(profId, targetId);
                            if (cached != null && cached.contains(packet.tradeIndex)) {
                                matched = target;
                            }
                            // 路径2：缓存未命中/不一致，回退扫描目录按 id + 编号双匹配兜底
                            if (matched == null) {
                                File byId = findTradeFileById(tradeFiles, targetId);
                                if (byId != null && tradeFileIndex(byId) == packet.tradeIndex) {
                                    matched = byId;
                                }
                            }
                            if (matched != null) {
                                deleted = matched.delete();
                                if (deleted) {
                                    removeCustomTradeIndex(profId, targetId, packet.tradeIndex);
                                }
                            }
                        }
                    }
                }
                if (!deleted) {
                    LOGGER.error("[VisualCrafting] Trade delete removed nothing under "
                            + profDir.getAbsolutePath() + " (index=" + packet.tradeIndex + ")");
                    serverPlayer.displayClientMessage(
                            Component.literal("未找到交易 #" + packet.tradeIndex), false);
                } else {
                    serverPlayer.displayClientMessage(
                            Component.literal("已删除交易 #" + packet.tradeIndex
                                    + "，交易已删除，正在刷新交易表即时生效"), false);
                    // 修复1：删除成功后立即静默 /reload 重建交易表，过滤即时生效。
                    TradeRefreshEvents.scheduleImmediateReload(serverPlayer.server);
                }

                PacketDistributor.sendToPlayer(serverPlayer, new DeleteTradeResponsePacket());
            } catch (Exception e) {
                LOGGER.error("[VisualCrafting] Failed to delete trade: " + e.getMessage());
                serverPlayer.displayClientMessage(
                        Component.literal("交易删除失败：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())), false);
            }
        });
    }

    /**
     * 删除成功后同步从运行时缓存移除对应条目，使客户端重拉列表立即生效。
     * profId 为流浪商人（minecraft:wandering_trader）或历史池入口（__wandering_generic__/__wandering_rare__）
     * 时按池映射移除 wandering 缓存；其余按职业+等级移除 villager 缓存。
     */
    private static void removeRuntimeFromCache(String profId, int level, int runtimeIndex) {
        try {
            if ("minecraft:wandering_trader".equals(profId)) {
                VisualCraftingTradeHandler.removeRuntimeWanderingTrade("generic", runtimeIndex);
                VisualCraftingTradeHandler.removeRuntimeWanderingTrade("rare", runtimeIndex);
            } else if ("__wandering_generic__".equals(profId)) {
                VisualCraftingTradeHandler.removeRuntimeWanderingTrade("generic", runtimeIndex);
            } else if ("__wandering_rare__".equals(profId)) {
                VisualCraftingTradeHandler.removeRuntimeWanderingTrade("rare", runtimeIndex);
            } else {
                VisualCraftingTradeHandler.removeRuntimeVillagerTrade(profId, level, runtimeIndex);
            }
        } catch (Exception e) {
            LOGGER.error("[VisualCrafting] Failed to remove runtime trade from cache: "
                    + profId + " #" + runtimeIndex + ": " + e.getMessage());
        }
    }

    /**
     * 为原版/Mod 运行时交易写入 deleted 标记（无 override 覆盖时调用）。
     * 落盘目录与 override 完全区分：
     *   world/visualcrafting/trade_deleted/villager/<profDir>/<level>-<index>.json
     *   world/visualcrafting/trade_deleted/wandering/<pool>-<index>.json
     * 标记 ID 按现 ID 体系生成 profId|cost1|result[|cost2]；同职业同三元组多条运行时交易
     * 共享基础 ID，事件过滤按 ID 匹配整组移除；标记文件间按基础 ID 家族计数追加自然数后缀，
     * 保证每个标记文件 ID 唯一（与 generateTradeIdWithSuffix 语义一致）。
     * 优先使用客户端包携带的三元组；缺失时从运行时缓存重建预览 offer 兜底。
     * @return 写入的标记 id；识别/写入失败返回 null
     */
    private static String writeRuntimeDeleteMark(ServerPlayer serverPlayer, File worldDir, String profId,
                                                 int level, int runtimeIndex,
                                                 String cost1, String cost2, String result) {
        try {
            String c1 = cost1 == null ? "" : cost1.trim();
            String c2 = cost2 == null ? "" : cost2.trim();
            String rs = result == null ? "" : result.trim();
            if (c1.isEmpty() || rs.isEmpty()) {
                // 兜底：从运行时缓存重建该条目的三元组（与列表请求同源逻辑）
                boolean wandering = "minecraft:wandering_trader".equals(profId);
                List<VillagerTrades.ItemListing> listings;
                if (wandering) {
                    listings = VisualCraftingTradeHandler.getRuntimeWanderingTrades(level == 2 ? "rare" : "generic");
                } else {
                    listings = VisualCraftingTradeHandler.getRuntimeVillagerTrades(profId, level);
                }
                if (runtimeIndex >= 0 && runtimeIndex < listings.size()) {
                    MerchantOffer offer = createPreviewOffer(serverPlayer, profId, level, listings.get(runtimeIndex));
                    if (offer != null) {
                        JsonObject json = merchantOfferToTradeJson(offer);
                        c1 = json.has("cost1") ? json.get("cost1").getAsString() : "";
                        c2 = json.has("cost2") ? json.get("cost2").getAsString() : "";
                        rs = json.has("result") ? json.get("result").getAsString() : "";
                    }
                }
                if (c1.isEmpty() || rs.isEmpty()) return null;
            }

            JsonObject triple = new JsonObject();
            triple.addProperty("cost1", c1);
            if (!c2.isEmpty()) triple.addProperty("cost2", c2);
            triple.addProperty("result", rs);
            String base = generateTradeId(profId, triple);

            File deletedRoot = new File(new File(worldDir, "visualcrafting"), "trade_deleted");
            File deletedDir;
            String fileName;
            if ("minecraft:wandering_trader".equals(profId)) {
                String pool = level == 2 ? "rare" : "generic";
                deletedDir = new File(deletedRoot, "wandering");
                fileName = pool + "-" + runtimeIndex + ".json";
            } else {
                deletedDir = new File(new File(deletedRoot, "villager"), profileDirectoryId(profId));
                fileName = level + "-" + runtimeIndex + ".json";
            }
            deletedDir.mkdirs();
            File markFile = new File(deletedDir, fileName);

            // 同一 runtimeIndex 重复删除：沿用已有标记 ID（幂等）；新条目按家族计数解歧后缀
            String id = base;
            if (markFile.isFile()) {
                try {
                    JsonObject old = JsonParser.parseString(
                            Files.readString(markFile.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
                    if (old.has("id")) id = old.get("id").getAsString();
                } catch (Exception ignored) { }
            }
            if (id.equals(base)) {
                int count = countDeletedFamily(deletedDir, base);
                if (count > 0) {
                    int suffix = count;
                    while (deletedFamilyContains(deletedDir, base + "|" + suffix)) suffix++;
                    id = base + "|" + suffix;
                }
            }

            JsonObject mark = new JsonObject();
            mark.addProperty("id", id);
            mark.addProperty("profId", profId);
            mark.addProperty("level", level);
            mark.addProperty("runtimeIndex", runtimeIndex);
            mark.addProperty("cost1", c1);
            mark.addProperty("cost2", c2);
            mark.addProperty("result", rs);
            mark.addProperty("deletedAt", System.currentTimeMillis());
            Files.writeString(markFile.toPath(), GSON.toJson(mark), StandardCharsets.UTF_8);
            LOGGER.info("[VisualCrafting] Deleted runtime trade {} #{} id={} -> {}",
                    profId, runtimeIndex, id, markFile.getAbsolutePath());
            return id;
        } catch (Exception e) {
            LOGGER.error("[VisualCrafting] Failed to write deleted mark for "
                    + profId + " #" + runtimeIndex + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * 用户重新保存 override 覆盖记录 = 撤销该条目的删除标记，
     * 避免 reload 后该交易仍被 deleted 过滤（恢复“保存修改 → 交易重新出现”）。
     */
    private static void clearRuntimeDeleteMark(File worldDir, String profId, int level, int runtimeIndex) {
        try {
            File deletedRoot = new File(new File(worldDir, "visualcrafting"), "trade_deleted");
            File deletedDir;
            String fileName;
            if ("minecraft:wandering_trader".equals(profId)) {
                String pool = level == 2 ? "rare" : "generic";
                deletedDir = new File(deletedRoot, "wandering");
                fileName = pool + "-" + runtimeIndex + ".json";
            } else {
                deletedDir = new File(new File(deletedRoot, "villager"), profileDirectoryId(profId));
                fileName = level + "-" + runtimeIndex + ".json";
            }
            File mark = new File(deletedDir, fileName);
            if (mark.isFile()) {
                mark.delete();
                LOGGER.info("[VisualCrafting] Cleared deleted mark " + mark.getAbsolutePath());
            }
        } catch (Exception e) {
            LOGGER.error("[VisualCrafting] Failed to clear deleted mark: " + e.getMessage());
        }
    }

    /** 统计 deleted 目录中同基础 ID 家族的标记数量（用于后缀解歧）。 */
    private static int countDeletedFamily(File deletedDir, String base) {
        int count = 0;
        File[] files = deletedDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return 0;
        for (File f : files) {
            try {
                JsonObject json = JsonParser.parseString(
                        Files.readString(f.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
                String id = json.has("id") ? json.get("id").getAsString() : "";
                if (id.equals(base) || id.startsWith(base + "|")) count++;
            } catch (Exception ignored) { }
        }
        return count;
    }

    private static boolean deletedFamilyContains(File deletedDir, String candidate) {
        File[] files = deletedDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return false;
        for (File f : files) {
            try {
                JsonObject json = JsonParser.parseString(
                        Files.readString(f.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
                if (candidate.equals(json.has("id") ? json.get("id").getAsString() : "")) return true;
            } catch (Exception ignored) { }
        }
        return false;
    }

    private static void handleDeleteTradeResponse(DeleteTradeResponsePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Screen screen = Minecraft.getInstance().screen;
            if (screen instanceof VisualCraftingScreen vcScreen) {
                try {
                    java.lang.reflect.Method method = vcScreen.getClass()
                            .getDeclaredMethod("onDeleteTradeResponse");
                    method.setAccessible(true);
                    method.invoke(vcScreen);
                } catch (Exception e) {
                    LOGGER.error("[VisualCrafting] Client reflection handler failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        });
    }

    private static void handleRequestTradeList(RequestTradeListPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (!(player instanceof ServerPlayer serverPlayer)) return;

            String profId = normalizeProfileId(packet.profId());
            if (!isValidProfileId(profId)) return;

            int level = Math.clamp(packet.level(), 1, 5);
            List<String> labels = new ArrayList<>();
            List<Integer> indices = new ArrayList<>();
            List<String> tradeJsons = new ArrayList<>();
            File worldDir = null;
            String jobSite = null;

            try {
                File worldDirTmp = serverPlayer.server.getWorldPath(LevelResource.ROOT).toFile();
                worldDir = worldDirTmp;
                // 职业方块槽位：读取 job_sites/<prof>.json 的 block 配置随列表回传客户端回填
                jobSite = loadJobSiteBlock(worldDir, profId);

                // ① 先列出当前运行时的原版/其他 Mod 交易。
                // 负数索引表示运行时交易：-(原始 index + 1)，用于后续写入 trade_overrides。
                List<VillagerTrades.ItemListing> runtimeListings;
                boolean wandering = "minecraft:wandering_trader".equals(profId);
                if (wandering) {
                    runtimeListings = VisualCraftingTradeHandler.getRuntimeWanderingTrades(
                            level == 2 ? "rare" : "generic");
                } else {
                    runtimeListings = VisualCraftingTradeHandler.getRuntimeVillagerTrades(profId, level);
                }
                for (int runtimeIndex = 0; runtimeIndex < runtimeListings.size(); runtimeIndex++) {
                    // 方案B：运行时交易全部列出（供预览/修改），是否已有 override 用 runtimeHasOverride 标记；
                    // 未覆盖条目删除时由客户端提示保护，避免误删/触发“未找到交易”。
                    File overrideFile;
                    if (wandering) {
                        String pool = (level == 2 ? "rare" : "generic");
                        overrideFile = new File(new File(new File(worldDir, "visualcrafting"),
                                "trade_overrides/wandering"), pool + "-" + runtimeIndex + ".json");
                    } else {
                        overrideFile = new File(new File(new File(new File(worldDir, "visualcrafting"),
                                "trade_overrides/villager"), profileDirectoryId(profId)),
                                level + "-" + runtimeIndex + ".json");
                    }
                    boolean hasOverride = overrideFile.isFile();

                    MerchantOffer offer = createPreviewOffer(serverPlayer, profId, level, runtimeListings.get(runtimeIndex));
                    if (offer == null) continue;

                    JsonObject json = merchantOfferToTradeJson(offer);
                    // 删除过滤：已写 deleted 标记的运行时交易不再出现在 GUI 列表，
                    // 但索引仍保持原版事件原始位置（不左移），保证后续保存/删除按原始索引
                    // 定位 override/deleted 文件，不会撤销相邻条目的删除标记导致“恢复已删交易”。
                    if (wandering) {
                        String pool = (level == 2 ? "rare" : "generic");
                        if (VisualCraftingTradeHandler.isWanderingRuntimeTradeDeleted(
                                worldDir, pool,
                                json.has("cost1") ? json.get("cost1").getAsString() : "",
                                json.has("cost2") ? json.get("cost2").getAsString() : "",
                                json.has("result") ? json.get("result").getAsString() : "")) {
                            continue;
                        }
                    } else {
                        if (VisualCraftingTradeHandler.isVillagerRuntimeTradeDeleted(
                                worldDir, profId, level,
                                json.has("cost1") ? json.get("cost1").getAsString() : "",
                                json.has("cost2") ? json.get("cost2").getAsString() : "",
                                json.has("result") ? json.get("result").getAsString() : "")) {
                            continue;
                        }
                    }
                    json.addProperty("runtime", true);
                    json.addProperty("runtimeIndex", runtimeIndex);
                    json.addProperty("runtimeHasOverride", hasOverride);
                    json.addProperty("level", level);

                    labels.add(tradeLabel(offer.getCostA(), offer.getCostB(), offer.getResult()));
                    indices.add(-(runtimeIndex + 1));
                    tradeJsons.add(GSON.toJson(json));
                }

                // ② 再列出 GUI 自己保存的自定义交易。
                File profDir = getTradeProfessionDirectory(worldDir, profId, false);
                File[] files = profDir.listFiles((d, name) -> name.endsWith(".json"));
                if (files != null) {
                    // 阶段1：全量扫描前重建该职业的 ID 索引（一次遍历同时完成旧文件 id 迁移与建索引）
                    resetCustomTradeIndex(profId);
                    Arrays.sort(files, Comparator.comparingInt(ModMessages::tradeFileIndex)
                            .thenComparing(File::getName));
                    for (File file : files) {
                        int index = tradeFileIndex(file);
                        if (index == Integer.MAX_VALUE) continue;
                        try {
                            JsonObject json = JsonParser.parseString(
                                    Files.readString(file.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
                            int tradeLevel = Math.clamp(json.has("level") ? json.get("level").getAsInt() : 1, 1, 5);
                            // 取消按请求等级过滤：GUI 需要一眼看到该职业全部等级的自定义交易，
                            // 标签以 [LvN] 前缀标注等级；删除/编辑仍以文件名编号为准。
                            // clear-<level>.json 只是“清空本级”标记，不是可编辑交易，不能出现在交易下拉框。
                            if (json.has("clearExisting") && json.get("clearExisting").getAsBoolean()) continue;
                            // 阶段1：旧文件无 id / 旧 UUID 时迁移为可读字符串 ID 并回写文件，
                            // 列表回传 json 携带 id；相同交易共享同一 ID（一对多索引）
                            String tradeId = json.has("id") ? json.get("id").getAsString() : "";
                            if (tradeId == null || tradeId.isEmpty() || isLegacyUuidId(tradeId)) {
                                // 旧 UUID / 无 id 迁移为可读 base ID；若同 profId 已有相同 base 则追加自然数后缀
                                tradeId = generateTradeIdWithSuffix(profId, json);
                                json.addProperty("id", tradeId);
                                Files.writeString(file.toPath(), GSON.toJson(json), StandardCharsets.UTF_8);
                            }
                            if (tradeId != null && !tradeId.isEmpty()) {
                                putCustomTradeIndex(profId, tradeId, index);
                            }
                            String cost1 = json.has("cost1") ? json.get("cost1").getAsString() : "";
                            String cost2 = json.has("cost2") ? json.get("cost2").getAsString() : "";
                            String result = json.has("result") ? json.get("result").getAsString() : "";
                            int cost1Count = json.has("cost1Count") ? json.get("cost1Count").getAsInt() : 1;
                            int cost2Count = json.has("cost2Count") ? json.get("cost2Count").getAsInt() : 0;
                            int resultCount = json.has("resultCount") ? json.get("resultCount").getAsInt() : 1;

                            // 显示名统一为“货币+结果”（cost1[+cost2]→result），不带数量与来源/序号前缀；
                            // 等级标注此前随“自定义 N.”前缀一并移除，跨等级重名条目仅按下拉顺序区分。
                            StringBuilder label = new StringBuilder();
                            label.append(shortItemId(cost1));
                            if (!cost2.isEmpty() && cost2Count > 0) {
                                label.append("+").append(shortItemId(cost2));
                            }
                            label.append("→").append(shortItemId(result));

                            labels.add(label.toString());
                            indices.add(index);
                            tradeJsons.add(json.toString());
                        } catch (Exception ignored) {
                            // 单个坏文件不应影响整个交易列表。
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[VisualCrafting] Failed to load trade list: " + e.getMessage());
            }

            PacketDistributor.sendToPlayer(serverPlayer,
                    new SyncTradeListPacket(profId, level, labels, indices, tradeJsons, jobSite));
        });
    }

    /**
     * 生成一个不加入世界的临时村民/流浪商人，仅用于调用 ItemListing#getOffer。
     * 这样 GUI 可以看到原版和其他 Mod 的真实交易，而不需要玩家附近必须存在对应村民。
     */
    private static MerchantOffer createPreviewOffer(ServerPlayer player, String profId, int level,
                                                    VillagerTrades.ItemListing listing) {
        try {
            if ("minecraft:wandering_trader".equals(profId)) {
                WanderingTrader trader = new WanderingTrader(EntityType.WANDERING_TRADER, player.serverLevel());
                return listing.getOffer(trader, RandomSource.create());
            }

            Optional<VillagerProfession> profession = BuiltInRegistries.VILLAGER_PROFESSION
                    .getOptional(ResourceLocation.parse(profId));
            if (profession.isEmpty()) return null;

            Villager villager = new Villager(EntityType.VILLAGER, player.serverLevel());
            VillagerData data = villager.getVillagerData()
                    .setProfession(profession.get())
                    .setLevel(Math.clamp(level, 1, 5));
            villager.setVillagerData(data);
            return listing.getOffer(villager, RandomSource.create());
        } catch (Exception e) {
            LOGGER.error("[VisualCrafting] Failed to create preview offer for "
                    + profId + " level " + level + ": " + e.getMessage());
            return null;
        }
    }

    private static JsonObject merchantOfferToTradeJson(MerchantOffer offer) {
        JsonObject json = new JsonObject();
        ItemStack costA = offer.getCostA();
        ItemStack costB = offer.getCostB();
        ItemStack result = offer.getResult();

        json.addProperty("cost1", BuiltInRegistries.ITEM.getKey(costA.getItem()).toString());
        json.addProperty("cost1Count", Math.max(1, costA.getCount()));
        if (!costB.isEmpty()) {
            json.addProperty("cost2", BuiltInRegistries.ITEM.getKey(costB.getItem()).toString());
            json.addProperty("cost2Count", Math.max(1, costB.getCount()));
        }
        json.addProperty("result", BuiltInRegistries.ITEM.getKey(result.getItem()).toString());
        json.addProperty("resultCount", Math.max(1, result.getCount()));
        json.addProperty("maxUses", offer.getMaxUses());
        json.addProperty("xp", offer.getXp());
        json.addProperty("priceMultiplier", offer.getPriceMultiplier());
        return json;
    }

    /** 缩短物品 ID 便于 GUI 展示：去掉 minecraft: 前缀，保留其他命名空间以便区分。 */
    private static String shortItemId(String itemId) {
        if (itemId == null) return "";
        return itemId.startsWith("minecraft:") ? itemId.substring("minecraft:".length()) : itemId;
    }

    /**
     * 生成“货币+结果”组合名（展示层）：cost1[+cost2]→result，均为物品注册名短名；
     * 不带数量、不带来源/序号前缀。仅用于 GUI 下拉框展示，不参与删除/筛选（删除按索引与三元组）。
     */
    private static String tradeLabel(ItemStack costA, ItemStack costB, ItemStack result) {
        StringBuilder label = new StringBuilder();
        label.append(shortItemId(BuiltInRegistries.ITEM.getKey(costA.getItem()).toString()));
        if (costB != null && !costB.isEmpty()) {
            label.append("+").append(shortItemId(BuiltInRegistries.ITEM.getKey(costB.getItem()).toString()));
        }
        label.append("→").append(shortItemId(BuiltInRegistries.ITEM.getKey(result.getItem()).toString()));
        return label.toString();
    }

    private static void handleSyncTradeList(SyncTradeListPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Screen screen = Minecraft.getInstance().screen;
            if (screen instanceof VisualCraftingScreen vcScreen) {
                try {
                    java.lang.reflect.Method method = vcScreen.getClass()
                            .getDeclaredMethod("updateMode3TradeList",
                                    String.class, int.class, List.class, List.class, List.class, String.class);
                    method.setAccessible(true);
                    method.invoke(vcScreen, packet.profId, packet.level,
                            packet.labels, packet.indices, packet.tradeJsons, packet.jobSite);
                } catch (Exception e) {
                    LOGGER.error("[VisualCrafting] Client trade list sync failed: "
                            + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        });
    }

    private static void handleClearTradeLevel(ClearTradeLevelPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (!(player instanceof ServerPlayer serverPlayer)) return;

            String profId = normalizeProfileId(packet.profId());
            if (!isValidProfileId(profId)) return;
            int level = Math.clamp(packet.level(), 1, 5);

            try {
                File worldDir = serverPlayer.server.getWorldPath(LevelResource.ROOT).toFile();
                File profDir = getTradeProfessionDirectory(worldDir, profId, true);
                profDir.mkdirs();

                File[] files = profDir.listFiles((d, name) -> name.endsWith(".json"));
                if (files != null) {
                    for (File file : files) {
                        try {
                            JsonObject json = JsonParser.parseString(
                                    Files.readString(file.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
                            int fileLevel = Math.clamp(json.has("level") ? json.get("level").getAsInt() : 1, 1, 5);
                            if (fileLevel == level) file.delete();
                        } catch (Exception ignored) {
                        }
                    }
                }

                // 保留一个“本级清空”标记，使 /reload 时同时清空原版/其他模组该职业的本级交易。
                JsonObject clear = new JsonObject();
                clear.addProperty("level", level);
                clear.addProperty("clearExisting", true);
                Files.writeString(new File(profDir, "clear-" + level + ".json").toPath(),
                        GSON.toJson(clear), StandardCharsets.UTF_8);

                PacketDistributor.sendToPlayer(serverPlayer, new ClearTradeLevelResponsePacket());
            } catch (Exception e) {
                LOGGER.error("[VisualCrafting] Failed to clear trade level: " + e.getMessage());
            }
        });
    }

    private static void handleClearTradeLevelResponse(ClearTradeLevelResponsePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Screen screen = Minecraft.getInstance().screen;
            if (screen instanceof VisualCraftingScreen vcScreen) {
                try {
                    java.lang.reflect.Method method = vcScreen.getClass()
                            .getDeclaredMethod("onClearTradeLevelResponse");
                    method.invoke(vcScreen);
                } catch (Exception e) {
                    LOGGER.error("[VisualCrafting] Client clear trade response failed: "
                            + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        });
    }

    // ========================================================================
    // Packet records
    // ========================================================================

    public record AddRecipePacket(BlockPos pos, boolean shaped, ItemStack result,
                                  List<ItemStack> ingredients,
                                  boolean saveNbt) implements CustomPacketPayload {
        public static final Type<AddRecipePacket> TYPE = new Type<>(ADD_RECIPE_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, AddRecipePacket> STREAM_CODEC =
                StreamCodec.of(AddRecipePacket::encode, AddRecipePacket::decode);

        @Override
        public Type<AddRecipePacket> type() { return TYPE; }

        private static void encode(RegistryFriendlyByteBuf buf, AddRecipePacket pkt) {
            buf.writeBlockPos(pkt.pos);
            buf.writeBoolean(pkt.shaped);
            ItemStack.STREAM_CODEC.encode(buf, pkt.result);
            buf.writeVarInt(pkt.ingredients.size());
            for (ItemStack stack : pkt.ingredients) {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
            }
            buf.writeBoolean(pkt.saveNbt);
        }

        private static AddRecipePacket decode(RegistryFriendlyByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            boolean shaped = buf.readBoolean();
            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
            int count = buf.readVarInt();
            List<ItemStack> ingredients = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                ingredients.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
            }
            boolean saveNbt = buf.readBoolean();
            return new AddRecipePacket(pos, shaped, result, ingredients, saveNbt);
        }
    }

    public record RemoveRecipePacket(BlockPos pos, int index) implements CustomPacketPayload {
        public static final Type<RemoveRecipePacket> TYPE = new Type<>(REMOVE_RECIPE_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, RemoveRecipePacket> STREAM_CODEC =
                StreamCodec.of(RemoveRecipePacket::encode, RemoveRecipePacket::decode);

        @Override
        public Type<RemoveRecipePacket> type() { return TYPE; }

        private static void encode(RegistryFriendlyByteBuf buf, RemoveRecipePacket pkt) {
            buf.writeBlockPos(pkt.pos);
            buf.writeVarInt(pkt.index);
        }

        private static RemoveRecipePacket decode(RegistryFriendlyByteBuf buf) {
            return new RemoveRecipePacket(buf.readBlockPos(), buf.readVarInt());
        }
    }

    public record DeleteByOutputPacket(BlockPos pos, ItemStack output) implements CustomPacketPayload {
        public static final Type<DeleteByOutputPacket> TYPE = new Type<>(DELETE_BY_OUTPUT_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, DeleteByOutputPacket> STREAM_CODEC =
                StreamCodec.of(DeleteByOutputPacket::encode, DeleteByOutputPacket::decode);

        @Override
        public Type<DeleteByOutputPacket> type() { return TYPE; }

        private static void encode(RegistryFriendlyByteBuf buf, DeleteByOutputPacket pkt) {
            buf.writeBlockPos(pkt.pos);
            ItemStack.STREAM_CODEC.encode(buf, pkt.output);
        }

        private static DeleteByOutputPacket decode(RegistryFriendlyByteBuf buf) {
            return new DeleteByOutputPacket(buf.readBlockPos(), ItemStack.STREAM_CODEC.decode(buf));
        }
    }

    public record SyncRecipesPacket(BlockPos pos,
                                    List<VisualCraftingBlockEntity.SavedRecipe> recipes)
            implements CustomPacketPayload {
        public static final Type<SyncRecipesPacket> TYPE = new Type<>(SYNC_RECIPES_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncRecipesPacket> STREAM_CODEC =
                StreamCodec.of(SyncRecipesPacket::encode, SyncRecipesPacket::decode);

        @Override
        public Type<SyncRecipesPacket> type() { return TYPE; }

        private static void encode(RegistryFriendlyByteBuf buf, SyncRecipesPacket pkt) {
            buf.writeBlockPos(pkt.pos);
            buf.writeVarInt(pkt.recipes.size());
            for (VisualCraftingBlockEntity.SavedRecipe r : pkt.recipes) {
                buf.writeBoolean(r.shaped);
                buf.writeBoolean(r.banned);
                buf.writeBoolean(r.saveNbt);
                ItemStack.STREAM_CODEC.encode(buf, r.result);
                buf.writeVarInt(r.ingredients.size());
                for (ItemStack stack : r.ingredients) {
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
                }
            }
        }

        private static SyncRecipesPacket decode(RegistryFriendlyByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            int total = buf.readVarInt();
            List<VisualCraftingBlockEntity.SavedRecipe> recipes = new ArrayList<>();
            for (int i = 0; i < total; i++) {
                boolean shaped = buf.readBoolean();
                boolean banned = buf.readBoolean();
                boolean saveNbt = buf.readBoolean();
                ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
                int ingCount = buf.readVarInt();
                List<ItemStack> ingredients = new ArrayList<>();
                for (int j = 0; j < ingCount; j++) {
                    ingredients.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                }
                recipes.add(new VisualCraftingBlockEntity.SavedRecipe(shaped, banned, result, ingredients, saveNbt));
            }
            return new SyncRecipesPacket(pos, recipes);
        }
    }

    public record TierUpdatePacket(BlockPos pos, int tier) implements CustomPacketPayload {
        public static final Type<TierUpdatePacket> TYPE = new Type<>(TIER_UPDATE_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, TierUpdatePacket> STREAM_CODEC =
                StreamCodec.of(TierUpdatePacket::encode, TierUpdatePacket::decode);

        @Override
        public Type<TierUpdatePacket> type() { return TYPE; }

        private static void encode(RegistryFriendlyByteBuf buf, TierUpdatePacket pkt) {
            buf.writeBlockPos(pkt.pos);
            buf.writeVarInt(pkt.tier);
        }

        private static TierUpdatePacket decode(RegistryFriendlyByteBuf buf) {
            return new TierUpdatePacket(buf.readBlockPos(), buf.readVarInt());
        }
    }

    public record FormatUpdatePacket(BlockPos pos, int format) implements CustomPacketPayload {
        public static final Type<FormatUpdatePacket> TYPE = new Type<>(FORMAT_UPDATE_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, FormatUpdatePacket> STREAM_CODEC =
                StreamCodec.of(FormatUpdatePacket::encode, FormatUpdatePacket::decode);

        @Override
        public Type<FormatUpdatePacket> type() { return TYPE; }

        private static void encode(RegistryFriendlyByteBuf buf, FormatUpdatePacket pkt) {
            buf.writeBlockPos(pkt.pos);
            buf.writeVarInt(pkt.format);
        }

        private static FormatUpdatePacket decode(RegistryFriendlyByteBuf buf) {
            return new FormatUpdatePacket(buf.readBlockPos(), buf.readVarInt());
        }
    }

    public record ModeUpdatePacket(BlockPos pos, int mode) implements CustomPacketPayload {
        public static final Type<ModeUpdatePacket> TYPE = new Type<>(MODE_UPDATE_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, ModeUpdatePacket> STREAM_CODEC =
                StreamCodec.of(ModeUpdatePacket::encode, ModeUpdatePacket::decode);

        @Override
        public Type<ModeUpdatePacket> type() { return TYPE; }

        private static void encode(RegistryFriendlyByteBuf buf, ModeUpdatePacket pkt) {
            buf.writeBlockPos(pkt.pos);
            buf.writeVarInt(pkt.mode);
        }

        private static ModeUpdatePacket decode(RegistryFriendlyByteBuf buf) {
            return new ModeUpdatePacket(buf.readBlockPos(), buf.readVarInt());
        }
    }

    public record ApplyMode8ChangePacket(BlockPos pos, ItemStack stack) implements CustomPacketPayload {
        public static final Type<ApplyMode8ChangePacket> TYPE = new Type<>(APPLY_MODE8_CHANGE_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, ApplyMode8ChangePacket> STREAM_CODEC =
                StreamCodec.of(ApplyMode8ChangePacket::encode, ApplyMode8ChangePacket::decode);

        @Override
        public Type<ApplyMode8ChangePacket> type() { return TYPE; }

        private static void encode(RegistryFriendlyByteBuf buf, ApplyMode8ChangePacket pkt) {
            buf.writeBlockPos(pkt.pos);
            ItemStack.STREAM_CODEC.encode(buf, pkt.stack);
        }

        private static ApplyMode8ChangePacket decode(RegistryFriendlyByteBuf buf) {
            return new ApplyMode8ChangePacket(buf.readBlockPos(), ItemStack.STREAM_CODEC.decode(buf));
        }
    }

    public record AddInfusingRecipePacket(BlockPos pos, ItemStack inputA, ItemStack inputB,
                                          ItemStack output, int infusionAmount) implements CustomPacketPayload {
        public static final Type<AddInfusingRecipePacket> TYPE = new Type<>(ADD_INFUSING_RECIPE_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, AddInfusingRecipePacket> STREAM_CODEC =
                StreamCodec.of(AddInfusingRecipePacket::encode, AddInfusingRecipePacket::decode);

        @Override
        public Type<AddInfusingRecipePacket> type() { return TYPE; }

        private static void encode(RegistryFriendlyByteBuf buf, AddInfusingRecipePacket pkt) {
            buf.writeBlockPos(pkt.pos);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, pkt.inputA);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, pkt.inputB);
            ItemStack.STREAM_CODEC.encode(buf, pkt.output);
            buf.writeVarInt(pkt.infusionAmount);
        }

        private static AddInfusingRecipePacket decode(RegistryFriendlyByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            ItemStack inputA = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            ItemStack inputB = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            ItemStack output = ItemStack.STREAM_CODEC.decode(buf);
            int amount = buf.readVarInt();
            return new AddInfusingRecipePacket(pos, inputA, inputB, output, amount);
        }
    }

    public record RemoveInfusingRecipePacket(BlockPos pos, int index) implements CustomPacketPayload {
        public static final Type<RemoveInfusingRecipePacket> TYPE = new Type<>(REMOVE_INFUSING_RECIPE_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, RemoveInfusingRecipePacket> STREAM_CODEC =
                StreamCodec.of(RemoveInfusingRecipePacket::encode, RemoveInfusingRecipePacket::decode);

        @Override
        public Type<RemoveInfusingRecipePacket> type() { return TYPE; }

        private static void encode(RegistryFriendlyByteBuf buf, RemoveInfusingRecipePacket pkt) {
            buf.writeBlockPos(pkt.pos);
            buf.writeVarInt(pkt.index);
        }

        private static RemoveInfusingRecipePacket decode(RegistryFriendlyByteBuf buf) {
            return new RemoveInfusingRecipePacket(buf.readBlockPos(), buf.readVarInt());
        }
    }

    public record DeleteInfusingByOutputPacket(BlockPos pos, ItemStack output) implements CustomPacketPayload {
        public static final Type<DeleteInfusingByOutputPacket> TYPE = new Type<>(DELETE_INFUSING_BY_OUTPUT_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, DeleteInfusingByOutputPacket> STREAM_CODEC =
                StreamCodec.of(DeleteInfusingByOutputPacket::encode, DeleteInfusingByOutputPacket::decode);

        @Override
        public Type<DeleteInfusingByOutputPacket> type() { return TYPE; }

        private static void encode(RegistryFriendlyByteBuf buf, DeleteInfusingByOutputPacket pkt) {
            buf.writeBlockPos(pkt.pos);
            ItemStack.STREAM_CODEC.encode(buf, pkt.output);
        }

        private static DeleteInfusingByOutputPacket decode(RegistryFriendlyByteBuf buf) {
            return new DeleteInfusingByOutputPacket(buf.readBlockPos(), ItemStack.STREAM_CODEC.decode(buf));
        }
    }

    public record SyncInfusingRecipesPacket(BlockPos pos,
                                            List<VisualCraftingBlockEntity.InfusingRecipe> recipes)
            implements CustomPacketPayload {
        public static final Type<SyncInfusingRecipesPacket> TYPE = new Type<>(SYNC_INFUSING_RECIPES_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncInfusingRecipesPacket> STREAM_CODEC =
                StreamCodec.of(SyncInfusingRecipesPacket::encode, SyncInfusingRecipesPacket::decode);

        @Override
        public Type<SyncInfusingRecipesPacket> type() { return TYPE; }

        private static void encode(RegistryFriendlyByteBuf buf, SyncInfusingRecipesPacket pkt) {
            buf.writeBlockPos(pkt.pos);
            buf.writeVarInt(pkt.recipes.size());
            for (VisualCraftingBlockEntity.InfusingRecipe r : pkt.recipes) {
                buf.writeBoolean(r.banned);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, r.inputA);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, r.inputB);
                ItemStack.STREAM_CODEC.encode(buf, r.output);
                buf.writeVarInt(r.infusionAmount);
            }
        }

        private static SyncInfusingRecipesPacket decode(RegistryFriendlyByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            int total = buf.readVarInt();
            List<VisualCraftingBlockEntity.InfusingRecipe> recipes = new ArrayList<>();
            for (int i = 0; i < total; i++) {
                boolean banned = buf.readBoolean();
                ItemStack inputA = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                ItemStack inputB = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                ItemStack output = ItemStack.STREAM_CODEC.decode(buf);
                int amount = buf.readVarInt();
                if (banned) {
                    VisualCraftingBlockEntity.InfusingRecipe r =
                            new VisualCraftingBlockEntity.InfusingRecipe(output);
                    r.infusionAmount = amount;
                    recipes.add(r);
                } else {
                    recipes.add(new VisualCraftingBlockEntity.InfusingRecipe(
                            inputA, inputB, output, amount));
                }
            }
            return new SyncInfusingRecipesPacket(pos, recipes);
        }
    }

    public record RequestDimBiomesPacket() implements CustomPacketPayload {
        public static final Type<RequestDimBiomesPacket> TYPE = new Type<>(REQUEST_DIM_BIOMES_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestDimBiomesPacket> STREAM_CODEC =
                StreamCodec.of((buf, pkt) -> {}, buf -> new RequestDimBiomesPacket());

        @Override
        public Type<RequestDimBiomesPacket> type() { return TYPE; }
    }

    public record SyncDimBiomesPacket(DimensionBiomesData data) implements CustomPacketPayload {
        public static final Type<SyncDimBiomesPacket> TYPE = new Type<>(SYNC_DIM_BIOMES_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncDimBiomesPacket> STREAM_CODEC =
                StreamCodec.of(SyncDimBiomesPacket::encode, SyncDimBiomesPacket::decode);

        @Override
        public Type<SyncDimBiomesPacket> type() { return TYPE; }

        private static void encode(RegistryFriendlyByteBuf buf, SyncDimBiomesPacket pkt) {
            DimensionBiomesData data = pkt.data;
            buf.writeVarInt(data.dimIds.size());
            for (String dimId : data.dimIds) {
                buf.writeUtf(dimId);
            }
            buf.writeVarInt(data.biomesByDim.size());
            for (Map.Entry<String, List<String>> entry : data.biomesByDim.entrySet()) {
                buf.writeUtf(entry.getKey());
                List<String> biomes = entry.getValue();
                buf.writeVarInt(biomes.size());
                for (String biome : biomes) {
                    buf.writeUtf(biome);
                }
            }
            buf.writeVarInt(data.allBiomes.size());
            for (String biome : data.allBiomes) {
                buf.writeUtf(biome);
            }
        }

        private static SyncDimBiomesPacket decode(RegistryFriendlyByteBuf buf) {
            int dimCount = buf.readVarInt();
            List<String> dimIds = new ArrayList<>();
            for (int i = 0; i < dimCount; i++) {
                dimIds.add(buf.readUtf());
            }

            int mapSize = buf.readVarInt();
            LinkedHashMap<String, List<String>> biomesByDim = new LinkedHashMap<>();
            for (int i = 0; i < mapSize; i++) {
                String dimId = buf.readUtf();
                int biomeCount = buf.readVarInt();
                List<String> biomes = new ArrayList<>();
                for (int j = 0; j < biomeCount; j++) {
                    biomes.add(buf.readUtf());
                }
                biomesByDim.put(dimId, biomes);
            }

            int allCount = buf.readVarInt();
            List<String> allBiomes = new ArrayList<>();
            for (int i = 0; i < allCount; i++) {
                allBiomes.add(buf.readUtf());
            }

            return new SyncDimBiomesPacket(new DimensionBiomesData(dimIds, biomesByDim, allBiomes));
        }
    }

    public record RequestMode4DataPacket() implements CustomPacketPayload {
        public static final Type<RequestMode4DataPacket> TYPE = new Type<>(REQUEST_MODE4_DATA_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestMode4DataPacket> STREAM_CODEC =
                StreamCodec.of((buf, pkt) -> {}, buf -> new RequestMode4DataPacket());

        @Override
        public Type<RequestMode4DataPacket> type() { return TYPE; }
    }

    public record SyncMode4DataPacket(
            List<String> profNames, List<String> profIds,
            List<String> mgmtProfNames, List<String> mgmtProfIds,
            List<String> mgmtTradeLabels, List<Boolean> mgmtTradeDisabled)
            implements CustomPacketPayload {
        public static final Type<SyncMode4DataPacket> TYPE = new Type<>(SYNC_MODE4_DATA_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncMode4DataPacket> STREAM_CODEC =
                StreamCodec.of(SyncMode4DataPacket::encode, SyncMode4DataPacket::decode);

        @Override
        public Type<SyncMode4DataPacket> type() { return TYPE; }

        private static void encode(RegistryFriendlyByteBuf buf, SyncMode4DataPacket pkt) {
            buf.writeVarInt(pkt.profNames.size());
            for (String s : pkt.profNames) buf.writeUtf(s);
            buf.writeVarInt(pkt.profIds.size());
            for (String s : pkt.profIds) buf.writeUtf(s);
            buf.writeVarInt(pkt.mgmtProfNames.size());
            for (String s : pkt.mgmtProfNames) buf.writeUtf(s);
            buf.writeVarInt(pkt.mgmtProfIds.size());
            for (String s : pkt.mgmtProfIds) buf.writeUtf(s);
            buf.writeVarInt(pkt.mgmtTradeLabels.size());
            for (String s : pkt.mgmtTradeLabels) buf.writeUtf(s);
            buf.writeVarInt(pkt.mgmtTradeDisabled.size());
            for (Boolean b : pkt.mgmtTradeDisabled) buf.writeBoolean(b);
        }

        private static SyncMode4DataPacket decode(RegistryFriendlyByteBuf buf) {
            List<String> profNames = new ArrayList<>();
            int size = buf.readVarInt();
            for (int i = 0; i < size; i++) profNames.add(buf.readUtf());
            List<String> profIds = new ArrayList<>();
            size = buf.readVarInt();
            for (int i = 0; i < size; i++) profIds.add(buf.readUtf());
            List<String> mgmtProfNames = new ArrayList<>();
            size = buf.readVarInt();
            for (int i = 0; i < size; i++) mgmtProfNames.add(buf.readUtf());
            List<String> mgmtProfIds = new ArrayList<>();
            size = buf.readVarInt();
            for (int i = 0; i < size; i++) mgmtProfIds.add(buf.readUtf());
            List<String> mgmtTradeLabels = new ArrayList<>();
            size = buf.readVarInt();
            for (int i = 0; i < size; i++) mgmtTradeLabels.add(buf.readUtf());
            List<Boolean> mgmtTradeDisabled = new ArrayList<>();
            size = buf.readVarInt();
            for (int i = 0; i < size; i++) mgmtTradeDisabled.add(buf.readBoolean());
            return new SyncMode4DataPacket(profNames, profIds, mgmtProfNames, mgmtProfIds,
                    mgmtTradeLabels, mgmtTradeDisabled);
        }
    }

    public record RequestTradeListPacket(String profId, int level) implements CustomPacketPayload {
        public static final Type<RequestTradeListPacket> TYPE = new Type<>(REQUEST_TRADE_LIST_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestTradeListPacket> STREAM_CODEC =
                StreamCodec.of(RequestTradeListPacket::encode, RequestTradeListPacket::decode);

        @Override
        public Type<RequestTradeListPacket> type() { return TYPE; }

        private static void encode(RegistryFriendlyByteBuf buf, RequestTradeListPacket pkt) {
            buf.writeUtf(pkt.profId);
            buf.writeVarInt(pkt.level);
        }

        private static RequestTradeListPacket decode(RegistryFriendlyByteBuf buf) {
            return new RequestTradeListPacket(buf.readUtf(), buf.readVarInt());
        }
    }

    public record SyncTradeListPacket(String profId, int level, List<String> labels,
                                           List<Integer> indices, List<String> tradeJsons, String jobSite)
            implements CustomPacketPayload {
        public static final Type<SyncTradeListPacket> TYPE = new Type<>(SYNC_TRADE_LIST_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncTradeListPacket> STREAM_CODEC =
                StreamCodec.of(SyncTradeListPacket::encode, SyncTradeListPacket::decode);

        @Override
        public Type<SyncTradeListPacket> type() { return TYPE; }

        private static void encode(RegistryFriendlyByteBuf buf, SyncTradeListPacket pkt) {
            buf.writeUtf(pkt.profId);
            buf.writeVarInt(pkt.level);

            buf.writeVarInt(pkt.labels.size());
            for (String label : pkt.labels) buf.writeUtf(label);

            buf.writeVarInt(pkt.indices.size());
            for (Integer index : pkt.indices) buf.writeVarInt(index);

            buf.writeVarInt(pkt.tradeJsons.size());
            for (String json : pkt.tradeJsons) buf.writeUtf(json);

            buf.writeBoolean(pkt.jobSite != null);
            if (pkt.jobSite != null) buf.writeUtf(pkt.jobSite);
        }

        private static SyncTradeListPacket decode(RegistryFriendlyByteBuf buf) {
            String profId = buf.readUtf();
            int level = buf.readVarInt();

            int labelCount = buf.readVarInt();
            List<String> labels = new ArrayList<>();
            for (int i = 0; i < labelCount; i++) labels.add(buf.readUtf());

            int indexCount = buf.readVarInt();
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < indexCount; i++) indices.add(buf.readVarInt());

            int jsonCount = buf.readVarInt();
            List<String> tradeJsons = new ArrayList<>();
            for (int i = 0; i < jsonCount; i++) tradeJsons.add(buf.readUtf());

            String jobSite = buf.readBoolean() ? buf.readUtf() : null;
            return new SyncTradeListPacket(profId, level, labels, indices, tradeJsons, jobSite);
        }
    }

    public record ClearTradeLevelPacket(String profId, int level) implements CustomPacketPayload {
        public static final Type<ClearTradeLevelPacket> TYPE = new Type<>(CLEAR_TRADE_LEVEL_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, ClearTradeLevelPacket> STREAM_CODEC =
                StreamCodec.of(ClearTradeLevelPacket::encode, ClearTradeLevelPacket::decode);

        @Override
        public Type<ClearTradeLevelPacket> type() { return TYPE; }

        private static void encode(RegistryFriendlyByteBuf buf, ClearTradeLevelPacket pkt) {
            buf.writeUtf(pkt.profId);
            buf.writeVarInt(pkt.level);
        }

        private static ClearTradeLevelPacket decode(RegistryFriendlyByteBuf buf) {
            return new ClearTradeLevelPacket(buf.readUtf(), buf.readVarInt());
        }
    }

    public record ClearTradeLevelResponsePacket() implements CustomPacketPayload {
        public static final Type<ClearTradeLevelResponsePacket> TYPE = new Type<>(CLEAR_TRADE_LEVEL_RESPONSE_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, ClearTradeLevelResponsePacket> STREAM_CODEC =
                StreamCodec.of((buf, pkt) -> {}, buf -> new ClearTradeLevelResponsePacket());

        @Override
        public Type<ClearTradeLevelResponsePacket> type() { return TYPE; }
    }

    public record SaveTradePacket(String profId, String tradeJson, String jobSite, boolean jobSiteCleared) implements CustomPacketPayload {
        public static final Type<SaveTradePacket> TYPE = new Type<>(SAVE_TRADE_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, SaveTradePacket> STREAM_CODEC =
                StreamCodec.of(SaveTradePacket::encode, SaveTradePacket::decode);

        @Override
        public Type<SaveTradePacket> type() { return TYPE; }

        private static void encode(RegistryFriendlyByteBuf buf, SaveTradePacket pkt) {
            buf.writeUtf(pkt.profId);
            buf.writeUtf(pkt.tradeJson);
            buf.writeBoolean(pkt.jobSite != null);
            if (pkt.jobSite != null) buf.writeUtf(pkt.jobSite);
            buf.writeBoolean(pkt.jobSiteCleared);
        }

        private static SaveTradePacket decode(RegistryFriendlyByteBuf buf) {
            String profId = buf.readUtf();
            String tradeJson = buf.readUtf();
            String jobSite = buf.readBoolean() ? buf.readUtf() : null;
            boolean jobSiteCleared = buf.readBoolean();
            return new SaveTradePacket(profId, tradeJson, jobSite, jobSiteCleared);
        }
    }

    public record SaveTradeResponsePacket() implements CustomPacketPayload {
        public static final Type<SaveTradeResponsePacket> TYPE = new Type<>(SAVE_TRADE_RESPONSE_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, SaveTradeResponsePacket> STREAM_CODEC =
                StreamCodec.of((buf, pkt) -> {}, buf -> new SaveTradeResponsePacket());

        @Override
        public Type<SaveTradeResponsePacket> type() { return TYPE; }
    }

    public record RequestDeleteTradePacket(String profId, int tradeIndex, int level,
                                           String cost1, String cost2, String result) implements CustomPacketPayload {
        public static final Type<RequestDeleteTradePacket> TYPE = new Type<>(DELETE_TRADE_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestDeleteTradePacket> STREAM_CODEC =
                StreamCodec.of(RequestDeleteTradePacket::encode, RequestDeleteTradePacket::decode);

        @Override
        public Type<RequestDeleteTradePacket> type() { return TYPE; }

        private static void encode(RegistryFriendlyByteBuf buf, RequestDeleteTradePacket pkt) {
            buf.writeUtf(pkt.profId);
            buf.writeVarInt(pkt.tradeIndex);
            buf.writeVarInt(pkt.level);
            // 追加三元组（cost1/cost2/result）：供服务端对无 override 的原版/Mod 运行时交易
            // 生成 deleted 标记 ID。字段写在末尾，decode 按剩余可读字节判断——
            // 旧客户端包不携带这三个字段时不读取，保持向后兼容。
            buf.writeUtf(pkt.cost1 == null ? "" : pkt.cost1);
            buf.writeUtf(pkt.cost2 == null ? "" : pkt.cost2);
            buf.writeUtf(pkt.result == null ? "" : pkt.result);
        }

        private static RequestDeleteTradePacket decode(RegistryFriendlyByteBuf buf) {
            String profId = buf.readUtf();
            int tradeIndex = buf.readVarInt();
            int level = buf.readVarInt();
            String cost1 = "", cost2 = "", result = "";
            if (buf.readableBytes() > 0) {
                cost1 = buf.readUtf();
                cost2 = buf.readUtf();
                result = buf.readUtf();
            }
            return new RequestDeleteTradePacket(profId, tradeIndex, level, cost1, cost2, result);
        }
    }

    public record DeleteTradeResponsePacket() implements CustomPacketPayload {
        public static final Type<DeleteTradeResponsePacket> TYPE = new Type<>(DELETE_TRADE_RESPONSE_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, DeleteTradeResponsePacket> STREAM_CODEC =
                StreamCodec.of((buf, pkt) -> {}, buf -> new DeleteTradeResponsePacket());

        @Override
        public Type<DeleteTradeResponsePacket> type() { return TYPE; }
    }

    // ===== Block disable (runtime) handlers =====

    private static void handleDisableBlock(DisableBlockPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (!(player instanceof ServerPlayer serverPlayer)) return;

            // 权限校验：必须对着已加载且可达的合成桌操作
            if (getAccessibleTable(serverPlayer, packet.pos()) == null) return;

            Block block = BuiltInRegistries.BLOCK.get(packet.blockId());
            if (block == null || block == Blocks.AIR) return;
            // 核心地形方块硬保护：封禁会挖空地表/破坏流体，服务端直接拒绝
            if (packet.disable() && BlockDisableRegistry.isProtected(block)) return;

            boolean changed = packet.disable()
                    ? BlockDisableRegistry.add(block)
                    : BlockDisableRegistry.remove(block);
            if (changed) {
                BlockDisableRegistry.save(serverPlayer.server.getWorldPath(LevelResource.ROOT));
            }
            // 广播全量（同时充当操作回执：客户端收到即代表服务端已采纳）
            PacketDistributor.sendToAllPlayers(new SyncDisabledBlocksPacket(BlockDisableRegistry.snapshotIds()));
        });
    }

    private static void handleRequestDisabledBlocks(RequestDisabledBlocksPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            PacketDistributor.sendToPlayer(serverPlayer, new SyncDisabledBlocksPacket(BlockDisableRegistry.snapshotIds()));
        });
    }

    private static void handleSyncDisabledBlocks(SyncDisabledBlocksPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            cachedDisabledBlocks = Set.copyOf(packet.disabledBlocks());
            Screen screen = Minecraft.getInstance().screen;
            if (screen instanceof VisualCraftingScreen vcScreen) {
                vcScreen.applyDisabledBlocks();
            }
        });
    }
}
