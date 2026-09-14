package com.visualcrafting.recipe;

import com.visualcrafting.block.VisualCraftingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class RecipeRegistrar {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeRegistrar.class);

    private static final Map<String, List<VisualCraftingBlockEntity.SavedRecipe>> ALL_TABLE_RECIPES =
            new ConcurrentHashMap<>();
    private static final Map<String, Integer> TABLE_FORMATS = new ConcurrentHashMap<>();
    /** playerId (UUID) -> 游戏内玩家名，用于脚本文件玩家维度命名。 */
    private static final Map<UUID, String> PLAYER_NAMES = new ConcurrentHashMap<>();
    private static final Map<String, List<VisualCraftingBlockEntity.InfusingRecipe>> ALL_INFUSING_TABLE_RECIPES =
            new ConcurrentHashMap<>();
    private static final Map<String, Integer> INFUSING_TABLE_FORMATS = new ConcurrentHashMap<>();

    private static final Path KUBEJS_OUTPUTS = Path.of("kubejs/server_scripts/visualcrafting_outputs.txt");
    private static final Path KUBEJS_BANNED = Path.of("kubejs/server_scripts/visualcrafting_banned.txt");
    private static final Path CRT_OUTPUTS = Path.of("scripts/visualcrafting_outputs.txt");
    private static final Path CRT_BANNED = Path.of("scripts/visualcrafting_banned.txt");
    private static final Path KUBEJS_INFUSING_OUTPUTS =
            Path.of("kubejs/server_scripts/visualcrafting_infusing_outputs.txt");
    private static final Path KUBEJS_INFUSING_BANNED =
            Path.of("kubejs/server_scripts/visualcrafting_infusing_banned.txt");
    private static final Path CRT_INFUSING_OUTPUTS = Path.of("scripts/visualcrafting_infusing_outputs.txt");
    private static final Path CRT_INFUSING_BANNED = Path.of("scripts/visualcrafting_infusing_banned.txt");

    // ---- Public update methods ----

    /** 记录某玩家的游戏内名字（服务端收到交互时由 network handler 调用）。 */
    public static void setPlayerName(UUID playerId, String playerName) {
        if (playerId == null || playerName == null || playerName.isBlank()) {
            return;
        }
        PLAYER_NAMES.put(playerId, playerName);
    }

    public static void updateTableRecipes(UUID playerId, BlockPos pos,
                                          List<VisualCraftingBlockEntity.SavedRecipe> recipes, int format) {
        String key = tableKey(playerId, pos);
        ALL_TABLE_RECIPES.put(key, new CopyOnWriteArrayList<>(recipes));
        TABLE_FORMATS.put(key, format);
    }

    public static void updateInfusingTableRecipes(UUID playerId, BlockPos pos,
                                                  List<VisualCraftingBlockEntity.InfusingRecipe> recipes,
                                                  int format) {
        String key = tableKey(playerId, pos);
        ALL_INFUSING_TABLE_RECIPES.put(key, new CopyOnWriteArrayList<>(recipes));
        INFUSING_TABLE_FORMATS.put(key, format);
    }

    /**
     * Remove a specific player's entries for a table position.
     * Called when a table is broken.
     */
    public static void removeTable(UUID playerId, BlockPos pos) {
        String key = tableKey(playerId, pos);
        ALL_TABLE_RECIPES.remove(key);
        TABLE_FORMATS.remove(key);
        ALL_INFUSING_TABLE_RECIPES.remove(key);
        INFUSING_TABLE_FORMATS.remove(key);
    }

    /**
     * Remove every player's entries for a table position.
     * Fallback used when the table owner is unknown (legacy blocks).
     */
    public static void removeTableByPos(BlockPos pos) {
        String suffix = "_" + pos;
        ALL_TABLE_RECIPES.keySet().removeIf(key -> key.endsWith(suffix));
        TABLE_FORMATS.keySet().removeIf(key -> key.endsWith(suffix));
        ALL_INFUSING_TABLE_RECIPES.keySet().removeIf(key -> key.endsWith(suffix));
        INFUSING_TABLE_FORMATS.keySet().removeIf(key -> key.endsWith(suffix));
    }

    private static String tableKey(UUID playerId, BlockPos pos) {
        return playerId + "_" + pos;
    }

    // ---- Collect all recipes for given format ----

    private record OwnedRecipe(UUID ownerId, VisualCraftingBlockEntity.SavedRecipe recipe) {
        String ownerName() {
            return ownerDisplay(ownerId);
        }
    }

    private static List<OwnedRecipe> collectAllRecipes(int format) {
        List<OwnedRecipe> all = new ArrayList<>();
        for (Map.Entry<String, List<VisualCraftingBlockEntity.SavedRecipe>> entry : ALL_TABLE_RECIPES.entrySet()) {
            Integer fmt = TABLE_FORMATS.get(entry.getKey());
            if (fmt != null && fmt == format) {
                UUID ownerId = parseOwnerUuid(entry.getKey());
                for (VisualCraftingBlockEntity.SavedRecipe r : entry.getValue()) {
                    all.add(new OwnedRecipe(ownerId, r));
                }
            }
        }
        return all;
    }

    /** tableKey = {uuid}_{pos}，uuid 不含 '_'，取第一个 '_' 前为玩家 uuid。 */
    private static UUID parseOwnerUuid(String tableKey) {
        int idx = tableKey.indexOf('_');
        if (idx <= 0) return null;
        try {
            return UUID.fromString(tableKey.substring(0, idx));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 玩家显示名：优先游戏内名字，未知时用 uuid 前 8 位，保证文件名稳定且不含非法字符。 */
    private static String ownerDisplay(UUID playerId) {
        if (playerId == null) return "unknown";
        String name = PLAYER_NAMES.get(playerId);
        if (name == null || name.isBlank()) {
            name = "p_" + playerId.toString().substring(0, 8);
        }
        return sanitizeFileNamePart(name);
    }

    private static String sanitizeFileNamePart(String raw) {
        String s = raw == null ? "" : raw.replaceAll("[^A-Za-z0-9._-]", "_");
        if (s.isEmpty() || s.equals(".") || s.equals("..")) {
            s = "player";
        }
        return s;
    }

    private static List<VisualCraftingBlockEntity.InfusingRecipe> collectAllInfusingRecipes(int format) {
        List<VisualCraftingBlockEntity.InfusingRecipe> all = new ArrayList<>();
        for (Map.Entry<String, List<VisualCraftingBlockEntity.InfusingRecipe>> entry :
                ALL_INFUSING_TABLE_RECIPES.entrySet()) {
            Integer fmt = INFUSING_TABLE_FORMATS.get(entry.getKey());
            if (fmt != null && fmt == format) {
                all.addAll(entry.getValue());
            }
        }
        return all;
    }

    // ---- Ban / unban ----

    public static void banOutput(String outputId, List<VisualCraftingBlockEntity.SavedRecipe> recipes,
                                 int tier, int format) {
        Path path = format == 1 ? CRT_BANNED : KUBEJS_BANNED;
        Set<String> banned = loadSet(path);
        if (banned.add(outputId)) {
            saveSet(path, banned);
        }
        regenerateScript(recipes, tier, format);
    }

    public static void banInfusingOutput(String outputId,
                                         List<VisualCraftingBlockEntity.InfusingRecipe> recipes, int format) {
        Path path = format == 1 ? CRT_INFUSING_BANNED : KUBEJS_INFUSING_BANNED;
        Set<String> banned = loadSet(path);
        if (banned.add(outputId)) {
            saveSet(path, banned);
        }
        regenerateInfusingScript(recipes, format);
    }

    public static void unbanOutput(String outputId, int format) {
        Path path = format == 1 ? CRT_BANNED : KUBEJS_BANNED;
        Set<String> banned = loadSet(path);
        if (banned.remove(outputId)) {
            saveSet(path, banned);
        }
    }

    public static void unbanInfusingOutput(String outputId, int format) {
        Path path = format == 1 ? CRT_INFUSING_BANNED : KUBEJS_INFUSING_BANNED;
        Set<String> banned = loadSet(path);
        if (banned.remove(outputId)) {
            saveSet(path, banned);
        }
    }

    // ---- Script regeneration: crafting table ----

    public static void regenerateScript(List<VisualCraftingBlockEntity.SavedRecipe> callerRecipes,
                                        int tier, int format) {
        LOGGER.info("[VisualCrafting] regenerateScript: format={} ({}), tier={}, callerRecipes={}",
                format, format == 0 ? "KubeJS" : "CRT", tier, callerRecipes.size());

        List<OwnedRecipe> collected = collectAllRecipes(format);
        List<OwnedRecipe> allRecipes = collected;
        if (allRecipes.isEmpty()) {
            allRecipes = new ArrayList<>();
            for (VisualCraftingBlockEntity.SavedRecipe r : callerRecipes) {
                allRecipes.add(new OwnedRecipe(null, r));
            }
            LOGGER.info("[VisualCrafting] ALL_TABLE_RECIPES empty — falling back to caller list ({} recipes)",
                    callerRecipes.size());
        }
        LOGGER.info("[VisualCrafting] Total allRecipes (after fallback): {}", allRecipes.size());

        Path bannedPath = format == 1 ? CRT_BANNED : KUBEJS_BANNED;
        Path outputsPath = format == 1 ? CRT_OUTPUTS : KUBEJS_OUTPUTS;
        Set<String> banned = loadSet(bannedPath);
        String ext = format == 1 ? ".zs" : ".js";
        String dirPrefix = format == 1 ? "scripts/" : "kubejs/server_scripts/";
        boolean isCRT = format == 1;

        // ---- 0. 清理历史生成脚本（旧 mod 命名 / 旧玩家文件），避免改名后残留文件仍被脚本加载
        cleanupGeneratedScripts(Path.of(dirPrefix));

        // ---- 1. 按创建时间升序（早的在先），为“相同项只保留最早”做稳定输入
        List<OwnedRecipe> sorted = new ArrayList<>(allRecipes);
        sorted.sort(java.util.Comparator.comparingLong((OwnedRecipe t) -> t.recipe().createdAt)
                .thenComparing(t -> t.ownerId() == null ? "" : t.ownerId().toString()));

        // ---- 2. 自动合并：配方一致且产出一致 -> 只保留创建时间最早的；不同时间/不同配方均保留
        LinkedHashMap<String, OwnedRecipe> unique3x3 = new LinkedHashMap<>();
        LinkedHashMap<String, OwnedRecipe> uniqueExtended = new LinkedHashMap<>();
        LinkedHashSet<String> allOutputIds = new LinkedHashSet<>();
        for (OwnedRecipe or : sorted) {
            VisualCraftingBlockEntity.SavedRecipe r = or.recipe();
            String outputId = BuiltInRegistries.ITEM.getKey(r.result.getItem()).toString();
            allOutputIds.add(outputId);
            if (isExtendedGrid(r)) {
                uniqueExtended.putIfAbsent(fingerprint(r), or);
            } else {
                unique3x3.putIfAbsent(fingerprint(r), or);
            }
        }
        saveSet(outputsPath, allOutputIds);

        // ---- 3. banned（全局删除输出）按产出 mod 归组
        LinkedHashMap<String, List<String>> bannedByNs = new LinkedHashMap<>();
        for (String bannedId : banned) {
            String ns = bannedId.split(":")[0];
            bannedByNs.computeIfAbsent(ns, k -> new ArrayList<>()).add(bannedId);
        }

        LOGGER.info("[VisualCrafting] unique3x3={}, uniqueExtended={}, bannedByNs={}",
                unique3x3.size(), uniqueExtended.size(), bannedByNs.keySet());

        // ---- 4. 普通 3x3 配方：按 (玩家名, 产出mod) 分组写文件 {玩家}.visualcrafting.{mod}{ext}
        java.util.TreeMap<String, List<OwnedRecipe>> groups = new java.util.TreeMap<>();
        for (OwnedRecipe or : unique3x3.values()) {
            if (isExtendedGrid(or.recipe())) continue;
            String outputId = BuiltInRegistries.ITEM.getKey(or.recipe().result.getItem()).toString();
            String mod = outputId.split(":")[0];
            groups.computeIfAbsent(or.ownerName() + "\u0001" + mod, k -> new ArrayList<>()).add(or);
        }

        Map<String, Integer> crtNameCounter = new HashMap<>();
        for (Map.Entry<String, List<OwnedRecipe>> groupEntry : groups.entrySet()) {
            String owner = groupEntry.getKey().split("\u0001", 2)[0];
            String mod = groupEntry.getKey().split("\u0001", 2)[1];
            List<OwnedRecipe> groupRecipes = groupEntry.getValue();

            String fileName = owner + ".visualcrafting." + mod + ext;
            Path outputPath = Path.of(dirPrefix).resolve(fileName);
            StringBuilder sb = new StringBuilder();

            if (isCRT) {
                sb.append("// VisualCrafting auto-generated - player:").append(owner)
                        .append(" output:").append(mod).append("\n");
                sb.append("// /reload to apply\n\n");
            } else {
                sb.append("ServerEvents.recipes(event => {\n");
            }

            List<String> nsBanned = bannedByNs.get(mod);
            if (nsBanned != null && !nsBanned.isEmpty()) {
                for (String bannedId : nsBanned) {
                    String name = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(bannedId)))
                            .getHoverName().getString();
                    if (isCRT) {
                        sb.append("craftingTable.remove(<item:").append(bannedId).append(">);//删除\"")
                                .append(name).append("\"配方\n");
                    } else {
                        sb.append("  event.remove({ output: '").append(bannedId).append("' });//删除\"")
                                .append(name).append("\"配方\n");
                    }
                }
            }

            for (OwnedRecipe or : groupRecipes) {
                VisualCraftingBlockEntity.SavedRecipe r = or.recipe();
                String outputId = BuiltInRegistries.ITEM.getKey(r.result.getItem()).toString();
                int count = r.result.getCount();
                if (isCRT) {
                    sb.append("\n// Recipes\n");
                    String recipeName = crtRecipeName(crtNameCounter, outputId);
                    if (r.shaped) {
                        generateShapedCRT(sb, r, outputId, count, recipeName);
                    } else {
                        generateShapelessCRT(sb, r, outputId, count, recipeName);
                    }
                } else {
                    if (r.shaped) {
                        generateShaped(sb, r, outputId, count);
                    } else {
                        generateShapeless(sb, r, outputId, count);
                    }
                }
                sb.append("\n");
            }

            if (!isCRT) {
                sb.append("});\n");
            }
            writeScript(outputPath, sb, fileName);
        }

        // ---- 5. banned-only 的 mod（无任何玩家配方文件）补一个仅含删除声明的文件
        for (String ns : bannedByNs.keySet()) {
            boolean hasGroup = false;
            for (String gk : groups.keySet()) {
                if (gk.endsWith("\u0001" + ns)) {
                    hasGroup = true;
                    break;
                }
            }
            if (hasGroup) continue;
            String fileName = "visualcrafting_" + ns + ext;
            Path outputPath = Path.of(dirPrefix).resolve(fileName);
            StringBuilder sb = new StringBuilder();
            if (isCRT) {
                sb.append("// VisualCrafting auto-generated - banned ").append(ns).append("\n");
            } else {
                sb.append("ServerEvents.recipes(event => {\n");
            }
            for (String bannedId : bannedByNs.get(ns)) {
                String name = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(bannedId)))
                        .getHoverName().getString();
                if (isCRT) {
                    sb.append("craftingTable.remove(<item:").append(bannedId).append(">);//删除\"")
                            .append(name).append("\"配方\n");
                } else {
                    sb.append("  event.remove({ output: '").append(bannedId).append("' });//删除\"")
                            .append(name).append("\"配方\n");
                }
            }
            if (!isCRT) {
                sb.append("});\n");
            }
            writeScript(outputPath, sb, fileName);
        }

        // ---- 6. Extended crafting (4x4+)：独立文件 + 同配方按时间去重
        if (!uniqueExtended.isEmpty()) {
            String extCraftFile = "visualcrafting~Extended_Crafting" + ext;
            Path extCraftPath = Path.of(dirPrefix).resolve(extCraftFile);
            StringBuilder extSb = new StringBuilder();

            if (isCRT) {
                extSb.append("// VisualCrafting auto-generated - Extended Crafting\n");
                extSb.append("// /reload to apply\n\n");
                extSb.append("// Recipes\n");
            } else {
                extSb.append("ServerEvents.recipes(event => {\n");
            }

            for (OwnedRecipe or : uniqueExtended.values()) {
                VisualCraftingBlockEntity.SavedRecipe r = or.recipe();
                String outputId = BuiltInRegistries.ITEM.getKey(r.result.getItem()).toString();
                int count = r.result.getCount();
                if (isCRT) {
                    String recipeName = crtRecipeName(crtNameCounter, outputId);
                    if (r.shaped) {
                        generateShapedCRT(extSb, r, outputId, count, recipeName);
                    } else {
                        generateShapelessCRT(extSb, r, outputId, count, recipeName);
                    }
                } else {
                    if (r.shaped) {
                        generateShaped(extSb, r, outputId, count);
                    } else {
                        generateShapeless(extSb, r, outputId, count);
                    }
                }
                extSb.append("\n");
            }

            if (!isCRT) {
                extSb.append("});\n");
            }
            writeScript(extCraftPath, extSb, extCraftFile);
        }
    }

    /** CRT 配方名：path 首次直接用，重复时追加 _1/_2...（跨文件全局递增，保证不冲突）。 */
    private static String crtRecipeName(Map<String, Integer> counter, String outputId) {
        String pathName = ResourceLocation.parse(outputId).getPath();
        int idx = counter.merge(pathName, 1, Integer::sum) - 1;
        return idx == 0 ? pathName : pathName + "_" + idx;
    }

    /** 是否为 >3x3 的大网格（4x4 / 5x5 / ...）→ Extended_Crafting 文件。 */
    private static boolean isExtendedGrid(VisualCraftingBlockEntity.SavedRecipe r) {
        int side = (int) Math.sqrt(r.ingredients.size());
        return side * side == r.ingredients.size() && side > 3;
    }

    /** 生成“配方+产出”指纹：配方一致（shaped 按裁剪后的逐格物品、shapeless 按无序物品集）且产出一致。 */
    private static String fingerprint(VisualCraftingBlockEntity.SavedRecipe r) {
        String outId = BuiltInRegistries.ITEM.getKey(r.result.getItem()).toString();
        StringBuilder sb = new StringBuilder(outId).append('|').append(r.result.getCount()).append('|');
        if (r.shaped) {
            sb.append("S|");
            int side = (int) Math.sqrt(r.ingredients.size());
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
            if (minRow <= maxRow) {
                for (int rr = minRow; rr <= maxRow; rr++) {
                    if (rr > minRow) sb.append(';');
                    for (int cc = minCol; cc <= maxCol; cc++) {
                        int idx = rr * side + cc;
                        if (idx < r.ingredients.size() && !r.ingredients.get(idx).isEmpty()) {
                            sb.append(BuiltInRegistries.ITEM.getKey(r.ingredients.get(idx).getItem()));
                        } else {
                            sb.append('.');
                        }
                        if (cc < maxCol) sb.append(' ');
                    }
                }
            }
        } else {
            sb.append("L|");
            List<String> ids = new ArrayList<>();
            for (ItemStack s : r.ingredients) {
                if (!s.isEmpty()) ids.add(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
            }
            Collections.sort(ids);
            sb.append(String.join(",", ids));
        }
        return sb.toString();
    }

    /** 清理本 mod 在脚本目录中生成的旧命名脚本（旧 visualcrafting_*.js/.zs 或玩家文件），避免残留旧配方。 */
    private static void cleanupGeneratedScripts(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) return;
        try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile).forEach(p -> {
                String name = p.getFileName().toString();
                if (!name.endsWith(".js") && !name.endsWith(".zs")) return;
                // 保留灌注脚本（另有专用输出名）
                if (name.startsWith("visualcrafting_metallurgic_infusing")) return;
                boolean managed = name.startsWith("visualcrafting_")
                        || name.startsWith("visualcrafting~")
                        || name.contains(".visualcrafting.");
                if (!managed) return;
                try {
                    Files.deleteIfExists(p);
                    LOGGER.info("[VisualCrafting] Cleaned stale script {}", name);
                } catch (IOException e) {
                    LOGGER.warn("[VisualCrafting] Failed to clean stale script {}: {}", name, e.getMessage());
                }
            });
        } catch (IOException e) {
            LOGGER.warn("[VisualCrafting] Failed to list script dir {}: {}", dir, e.getMessage());
        }
    }

    private static void writeScript(Path outputPath, StringBuilder sb, String fileName) {
        try {
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, sb.toString());
            LOGGER.info("[VisualCrafting] Wrote {} ({} chars) → {}", fileName, sb.length(),
                    outputPath.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("[VisualCrafting] Write failed: {} — {}", outputPath.toAbsolutePath(), e.getMessage());
        }
    }

    // ---- Script regeneration: metallurgic infusing ----

    public static void regenerateInfusingScript(List<VisualCraftingBlockEntity.InfusingRecipe> callerRecipes,
                                                int format) {
        LOGGER.info("[VisualCrafting] regenerateInfusingScript: format={} ({}), callerRecipes={}",
                format, format == 0 ? "KubeJS" : "CRT", callerRecipes.size());

        List<VisualCraftingBlockEntity.InfusingRecipe> allRecipes = collectAllInfusingRecipes(format);
        if (allRecipes.isEmpty()) {
            allRecipes = callerRecipes;
            LOGGER.info("[VisualCrafting] ALL_INFUSING_TABLE_RECIPES empty — falling back to caller list ({} recipes)",
                    callerRecipes.size());
        }
        LOGGER.info("[VisualCrafting] Total infusing allRecipes: {}", allRecipes.size());

        Path bannedPath = format == 1 ? CRT_INFUSING_BANNED : KUBEJS_INFUSING_BANNED;
        Path outputsPath = format == 1 ? CRT_INFUSING_OUTPUTS : KUBEJS_INFUSING_OUTPUTS;
        Set<String> banned = loadSet(bannedPath);

        String ext = format == 1 ? ".zs" : ".js";
        String dirPrefix = format == 1 ? "scripts/" : "kubejs/server_scripts/";
        String fileName = "visualcrafting_metallurgic_infusing" + ext;
        Path outputPath = Path.of(dirPrefix).resolve(fileName);
        boolean isCRT = format == 1;

        StringBuilder sb = new StringBuilder();
        if (isCRT) {
            sb.append("// VisualCrafting auto-generated - Metallurgic Infusing\n");
            sb.append("// /reload to apply\n\n");
        } else {
            sb.append("ServerEvents.recipes(event => {\n");
        }

        if (!banned.isEmpty()) {
            if (isCRT) {
                sb.append("// Banned infusing recipes\n");
            }
            for (String bannedId : banned) {
                String name = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(bannedId)))
                        .getHoverName().getString();
                if (isCRT) {
                    sb.append("// Banned: ").append(bannedId).append(" // \"").append(name).append("\"\n");
                } else {
                    sb.append("  event.remove({ output: '").append(bannedId).append("' });//删除灌注配方\"")
                            .append(name).append("\"\n");
                }
            }
        }

        List<VisualCraftingBlockEntity.InfusingRecipe> active = new ArrayList<>();
        LinkedHashSet<String> outputIds = new LinkedHashSet<>();
        for (VisualCraftingBlockEntity.InfusingRecipe r : allRecipes) {
            if (r.banned) continue;
            String id = BuiltInRegistries.ITEM.getKey(r.output.getItem()).toString();
            outputIds.add(id);
            active.add(r);
        }
        saveSet(outputsPath, outputIds);

        if (!active.isEmpty()) {
            if (isCRT) {
                sb.append("\n// Recipes\n\n");
            }
            for (VisualCraftingBlockEntity.InfusingRecipe r : active) {
                String outputId = BuiltInRegistries.ITEM.getKey(r.output.getItem()).toString();

                // Resolve inputA: may be chemical (has CUSTOM_DATA with chemicalId) or item
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

                if (isCRT) {
                    sb.append("mods.mekanism.metallurgic_infusing.recipeBuilder()\n");
                    sb.append("    .itemInput(<item:").append(inputBStr).append(">)\n");
                    sb.append("    .infusionInput(<item:").append(inputAStr).append(">, ")
                            .append(r.infusionAmount).append(")\n");
                    sb.append("    .output(<item:").append(outputId).append(">)\n");
                    sb.append("    .build();//").append(r.output.getHoverName().getString()).append("\n\n");
                } else {
                    sb.append("  event.remove({ output: \"").append(outputId).append("\" });\n");
                    sb.append("  event.recipes.mekanism.metallurgic_infusing(\n");
                    sb.append("    '").append(outputId).append("',  // 输出\n");
                    sb.append("    '").append(inputBStr).append("',  // 输入物品\n");
                    sb.append("    '").append(r.infusionAmount).append("x ").append(inputAStr).append("'  // 化学品 + 数量\n");
                    sb.append("  );\n");
                }
            }
        }

        if (!isCRT) {
            sb.append("});\n");
        }

        try {
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, sb.toString());
            LOGGER.info("[VisualCrafting] Wrote {} ({} chars) → {}", fileName, sb.length(),
                    outputPath.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("[VisualCrafting] Write failed: {} — {}", outputPath.toAbsolutePath(), e.getMessage());
        }
    }

    // ---- KubeJS shaped/shapeless generators ----

    private static void generateShaped(StringBuilder sb, VisualCraftingBlockEntity.SavedRecipe recipe,
                                       String outputId, int count) {
        int side = (int) Math.sqrt(recipe.ingredients.size());
        if (side * side != recipe.ingredients.size() || side < 3 || side > 9) return;

        int minRow = side, maxRow = -1, minCol = side, maxCol = -1;
        for (int r = 0; r < side; r++) {
            for (int c = 0; c < side; c++) {
                int idx = r * side + c;
                if (idx >= recipe.ingredients.size() || recipe.ingredients.get(idx).isEmpty()) continue;
                minRow = Math.min(minRow, r);
                maxRow = Math.max(maxRow, r);
                minCol = Math.min(minCol, c);
                maxCol = Math.max(maxCol, c);
            }
        }
        if (minRow > maxRow) return;

        LinkedHashMap<String, String> keyMap = new LinkedHashMap<>();
        char nextChar = 'A';

        sb.append("  event.shaped(\n");
        sb.append("    Item.of('").append(outputId).append("'");
        if (count > 1) sb.append(", ").append(count);
        sb.append("),\n");
        sb.append("    [\n");

        for (int r = minRow; r <= maxRow; r++) {
            StringBuilder rowStr = new StringBuilder();
            for (int c = minCol; c <= maxCol; c++) {
                int idx = r * side + c;
                if (idx < recipe.ingredients.size() && !recipe.ingredients.get(idx).isEmpty()) {
                    String itemId = BuiltInRegistries.ITEM.getKey(recipe.ingredients.get(idx).getItem()).toString();
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
            sb.append("      '").append(rowStr).append("'");
            if (r < maxRow) sb.append(",");
            sb.append("\n");
        }

        sb.append("    ],\n");
        sb.append("    {\n");
        int ki = 0;
        for (Map.Entry<String, String> e : keyMap.entrySet()) {
            sb.append("      ").append(e.getKey()).append(": '").append(e.getValue()).append("'");
            if (++ki < keyMap.size()) sb.append(",");
            sb.append("\n");
        }
        sb.append("    }\n");
        sb.append("  );//添加有序合成\"").append(recipe.result.getHoverName().getString()).append("\"配方");
    }

    private static void generateShapeless(StringBuilder sb, VisualCraftingBlockEntity.SavedRecipe recipe,
                                          String outputId, int count) {
        sb.append("  event.shapeless(\n");
        sb.append("    Item.of('").append(outputId).append("'");
        if (count > 1) sb.append(", ").append(count);
        sb.append("),\n");
        sb.append("    [\n");

        List<ItemStack> nonEmpty = new ArrayList<>();
        for (ItemStack s : recipe.ingredients) {
            if (!s.isEmpty()) nonEmpty.add(s);
        }
        for (int i = 0; i < nonEmpty.size(); i++) {
            String itemId = BuiltInRegistries.ITEM.getKey(nonEmpty.get(i).getItem()).toString();
            sb.append("      '").append(itemId).append("'");
            if (i < nonEmpty.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("    ]\n");
        sb.append("  );//添加无序合成\"").append(recipe.result.getHoverName().getString()).append("\"配方");
    }

    // ---- CRT shaped/shapeless generators ----

    private static void generateShapedCRT(StringBuilder sb, VisualCraftingBlockEntity.SavedRecipe recipe,
                                          String outputId, int count, String recipeName) {
        int side = (int) Math.sqrt(recipe.ingredients.size());
        if (side * side != recipe.ingredients.size() || side < 3 || side > 9) return;

        boolean hasAny = false;
        for (int i = 0; i < side; i++) {
            for (int j = 0; j < side; j++) {
                int idx = i * side + j;
                if (idx < recipe.ingredients.size() && !recipe.ingredients.get(idx).isEmpty()) {
                    hasAny = true;
                    break;
                }
            }
            if (hasAny) break;
        }
        if (!hasAny) return;

        boolean extended = side > 3;
        if (extended) {
            int tier = side == 5 ? 2 : (side == 7 ? 3 : 4);
            sb.append("<recipetype:extendedcrafting:table>.addShaped(\"");
            sb.append(recipeName).append("\", ");
            sb.append(tier);
            sb.append(", <item:").append(outputId).append(">");
            if (count > 1) sb.append(" * ").append(count);
            sb.append(", [\n");
        } else {
            sb.append("craftingTable.addShaped(\"");
            sb.append(recipeName).append("\", ");
            sb.append("<item:").append(outputId).append(">");
            if (count > 1) sb.append(" * ").append(count);
            sb.append(", [\n");
        }

        for (int r = 0; r < side; r++) {
            sb.append("  [");
            for (int c = 0; c < side; c++) {
                int idx = r * side + c;
                if (idx < recipe.ingredients.size() && !recipe.ingredients.get(idx).isEmpty()) {
                    String itemId = BuiltInRegistries.ITEM.getKey(recipe.ingredients.get(idx).getItem()).toString();
                    sb.append("<item:").append(itemId).append(">");
                } else {
                    sb.append("<item:minecraft:air>");
                }
                if (c < side - 1) sb.append(", ");
            }
            sb.append("]");
            if (r < side - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]);//添加有序合成\"").append(recipe.result.getHoverName().getString()).append("\"配方");
    }

    private static void generateShapelessCRT(StringBuilder sb, VisualCraftingBlockEntity.SavedRecipe recipe,
                                             String outputId, int count, String recipeName) {
        List<ItemStack> nonEmpty = new ArrayList<>();
        for (ItemStack s : recipe.ingredients) {
            if (!s.isEmpty()) nonEmpty.add(s);
        }

        int side = (int) Math.sqrt(recipe.ingredients.size());
        boolean extended = side > 3;

        if (extended) {
            int tier = side == 5 ? 2 : (side == 7 ? 3 : 4);
            sb.append("<recipetype:extendedcrafting:table>.addShapeless(\"");
            sb.append(recipeName).append("\", ");
            sb.append(tier);
            sb.append(", <item:").append(outputId).append(">");
            if (count > 1) sb.append(" * ").append(count);
            sb.append(", [");
        } else {
            sb.append("craftingTable.addShapeless(\"");
            sb.append(recipeName).append("\", ");
            sb.append("<item:").append(outputId).append(">");
            if (count > 1) sb.append(" * ").append(count);
            sb.append(", [");
        }

        for (int i = 0; i < nonEmpty.size(); i++) {
            String itemId = BuiltInRegistries.ITEM.getKey(nonEmpty.get(i).getItem()).toString();
            sb.append("<item:").append(itemId).append(">");
            if (i < nonEmpty.size() - 1) sb.append(", ");
        }
        sb.append("]);//添加无序合成\"").append(recipe.result.getHoverName().getString()).append("\"配方");
    }

    // ---- File helpers ----

    private static Set<String> loadSet(Path path) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        try {
            if (Files.exists(path)) {
                set.addAll(Files.readAllLines(path));
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to load set file", e);
        }
        return set;
    }

    private static void saveSet(Path path, Set<String> set) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, new ArrayList<>(set));
        } catch (IOException e) {
            System.err.println("[VisualCrafting] 写入文件失败: " + path + " - " + e.getMessage());
        }
    }
}
