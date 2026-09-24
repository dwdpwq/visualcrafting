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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModMessages {

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
            System.err.println("[VisualCrafting] Rejected packet: table out of range at " + pos);
            return null;
        }
        // 归属校验：已有主人的工作台只允许主人操作，防止越权读写他人配方
        UUID owner = vcBe.getOwnerId();
        if (owner != null && !owner.equals(player.getUUID())) {
            System.err.println("[VisualCrafting] Rejected packet: table at " + pos
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
        if (profId.contains("..") || profId.contains("/") || profId.contains("\\") || profId.contains(":")) {
            return false;
        }
        return profId.matches("[A-Za-z0-9_\\-]+");
    }

    /** 兼容 GUI 传来的 "命名空间:id" 形式：统一剥掉命名空间，避免同一档案被写成两种目录。 */
    private static String normalizeProfileId(String profId) {
        if (profId == null) return null;
        int sep = profId.indexOf(':');
        return sep >= 0 ? profId.substring(sep + 1) : profId;
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
            System.err.println("[VisualCrafting] 写入暂存配方失败: " + e.getMessage());
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
                    packet.shaped, packet.result, packet.ingredients));

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
                vcBe.setTier(packet.tier);
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
                System.err.println("[VisualCrafting] Rejected Mode 8 packet: item identity/count changed");
                return;
            }

            ItemStack sanitized = current.copy();

            if (requested.has(DataComponents.MAX_DAMAGE)) {
                int maxDamage = requested.getOrDefault(DataComponents.MAX_DAMAGE, 0);
                if (maxDamage < 0) return;
                sanitized.set(DataComponents.MAX_DAMAGE, maxDamage);
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

    private static boolean isValidMode8Attributes(ItemStack current, ItemAttributeModifiers requested) {
        ItemAttributeModifiers original =
                current.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        Map<UUID, ItemAttributeModifiers.Entry> originalById = new HashMap<>();
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
                System.err.println("[VisualCrafting] Failed to load dim/biome data on server: " + e.getMessage());
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
                System.err.println("[VisualCrafting] Failed to save dim/biome index: " + e.getMessage());
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
                System.err.println("[VisualCrafting] Failed to save dim/biome client cache: " + e.getMessage());
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

                File tradesDir = new File(vcDir, "trades");
                File[] tradeDirs = tradesDir.listFiles(File::isDirectory);
                if (tradeDirs != null) {
                    for (File dir : tradeDirs) {
                        File[] tradeFiles = dir.listFiles((d, name) -> name.endsWith(".json"));
                        if (tradeFiles != null && tradeFiles.length > 0) {
                            profIds.add(dir.getName());
                            profNames.add(dir.getName());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[VisualCrafting] Failed to load mode4 data: " + e.getMessage());
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
                    method.invoke(vcScreen,
                            packet.profNames, packet.profIds, packet.mgmtProfNames, packet.mgmtProfIds,
                            packet.mgmtTradeLabels, packet.mgmtTradeDisabled);
                } catch (Exception e) {
                    System.err.println("[VisualCrafting] Client reflection handler failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
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
                System.err.println("[VisualCrafting] Rejected trade save with invalid profile id: " + packet.profId);
                return;
            }

            try {
                File worldDir = serverPlayer.server.getWorldPath(LevelResource.ROOT).toFile();
                File tradesDir = new File(new File(worldDir, "visualcrafting"), "trades");
                File profDir = new File(tradesDir, profId);
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
                File tradeFile = new File(profDir, nextIndex + ".json");
                Files.writeString(tradeFile.toPath(), packet.tradeJson, StandardCharsets.UTF_8);

                // 自动 reload 已移除：脚本已写入，需手动执行 /reload 后生效
                PacketDistributor.sendToPlayer(serverPlayer, new SaveTradeResponsePacket());
            } catch (Exception e) {
                System.err.println("[VisualCrafting] Failed to save trade: " + e.getMessage());
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
                    method.invoke(vcScreen);
                } catch (Exception e) {
                    System.err.println("[VisualCrafting] Client reflection handler failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
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
                System.err.println("[VisualCrafting] Rejected trade delete with invalid profile id: " + packet.profId);
                return;
            }

            try {
                File worldDir = serverPlayer.server.getWorldPath(LevelResource.ROOT).toFile();
                // 交易实际存放在 world/visualcrafting/trades/<profId>/（与保存路径一致）
                File profDir = new File(new File(new File(worldDir, "visualcrafting"), "trades"), profId);

                boolean deleted = false;
                File[] tradeFiles = profDir.listFiles((d, name) -> name.endsWith(".json"));
                if (tradeFiles != null && tradeFiles.length > 0) {
                    // 按数字编号升序，与 GUI 列表顺序一致
                    Arrays.sort(tradeFiles, Comparator.comparingInt(ModMessages::tradeFileIndex)
                            .thenComparing(File::getName));
                    if (packet.tradeIndex >= 0 && packet.tradeIndex < tradeFiles.length) {
                        deleted = tradeFiles[packet.tradeIndex].delete();
                    } else {
                        System.err.println("[VisualCrafting] Trade delete index out of range: index="
                                + packet.tradeIndex + ", files=" + tradeFiles.length + ", profId=" + profId);
                    }
                }
                if (!deleted) {
                    System.err.println("[VisualCrafting] Trade delete removed nothing under "
                            + profDir.getAbsolutePath() + " (index=" + packet.tradeIndex + ")");
                }

                PacketDistributor.sendToPlayer(serverPlayer, new DeleteTradeResponsePacket());
            } catch (Exception e) {
                System.err.println("[VisualCrafting] Failed to delete trade: " + e.getMessage());
            }
        });
    }

    private static void handleDeleteTradeResponse(DeleteTradeResponsePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Screen screen = Minecraft.getInstance().screen;
            if (screen instanceof VisualCraftingScreen vcScreen) {
                try {
                    java.lang.reflect.Method method = vcScreen.getClass()
                            .getDeclaredMethod("onDeleteTradeResponse");
                    method.invoke(vcScreen);
                } catch (Exception e) {
                    System.err.println("[VisualCrafting] Client reflection handler failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
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
                ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
                int ingCount = buf.readVarInt();
                List<ItemStack> ingredients = new ArrayList<>();
                for (int j = 0; j < ingCount; j++) {
                    ingredients.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                }
                recipes.add(new VisualCraftingBlockEntity.SavedRecipe(shaped, banned, result, ingredients));
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

    public record SaveTradePacket(String profId, String tradeJson) implements CustomPacketPayload {
        public static final Type<SaveTradePacket> TYPE = new Type<>(SAVE_TRADE_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, SaveTradePacket> STREAM_CODEC =
                StreamCodec.of(SaveTradePacket::encode, SaveTradePacket::decode);

        @Override
        public Type<SaveTradePacket> type() { return TYPE; }

        private static void encode(RegistryFriendlyByteBuf buf, SaveTradePacket pkt) {
            buf.writeUtf(pkt.profId);
            buf.writeUtf(pkt.tradeJson);
        }

        private static SaveTradePacket decode(RegistryFriendlyByteBuf buf) {
            return new SaveTradePacket(buf.readUtf(), buf.readUtf());
        }
    }

    public record SaveTradeResponsePacket() implements CustomPacketPayload {
        public static final Type<SaveTradeResponsePacket> TYPE = new Type<>(SAVE_TRADE_RESPONSE_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, SaveTradeResponsePacket> STREAM_CODEC =
                StreamCodec.of((buf, pkt) -> {}, buf -> new SaveTradeResponsePacket());

        @Override
        public Type<SaveTradeResponsePacket> type() { return TYPE; }
    }

    public record RequestDeleteTradePacket(String profId, int tradeIndex) implements CustomPacketPayload {
        public static final Type<RequestDeleteTradePacket> TYPE = new Type<>(DELETE_TRADE_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestDeleteTradePacket> STREAM_CODEC =
                StreamCodec.of(RequestDeleteTradePacket::encode, RequestDeleteTradePacket::decode);

        @Override
        public Type<RequestDeleteTradePacket> type() { return TYPE; }

        private static void encode(RegistryFriendlyByteBuf buf, RequestDeleteTradePacket pkt) {
            buf.writeUtf(pkt.profId);
            buf.writeVarInt(pkt.tradeIndex);
        }

        private static RequestDeleteTradePacket decode(RegistryFriendlyByteBuf buf) {
            return new RequestDeleteTradePacket(buf.readUtf(), buf.readVarInt());
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
