package com.visualcrafting.merge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.visualcrafting.worldgen.OreDisableRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 玩家配方/内容编辑暂存合并管理器。
 * <p>
 * 规则：
 * <ol>
 *   <li>玩家编辑的内容写入 {@code ./visualcrafting/pending/}，
 *       文件名为 {@code {玩家}_{类型}_{归属}.json}；</li>
 *   <li>服务端启动或执行 {@code /reload} 时调用 {@link #mergeAll()}：</li>
 *   <li>按 (type, recipeId) 分组，对同一 key 的多个玩家文件执行深度合并：
 *       <ul>
 *         <li>仅一个玩家有该字段 → 直接采用；</li>
 *         <li>同字段冲突 → 比较 timestamp，最后写入者获胜；</li>
 *         <li>数组（pattern / ingredients / features 等）→ 时间戳最新的完整数组覆盖；</li>
 *       </ul>
 *   </li>
 *   <li>按类型路由输出：
 *       <ul>
 *         <li>{@code recipe} → {@code kubejs/data/[命名空间]/recipes/}；</li>
 *         <li>{@code worldgen} → {@code datapacks/visualcrafting/data/visualcrafting/[路径]}（自动补全 pack.mcmeta）；</li>
 *         <li>{@code startup_scripts} → {@code kubejs/startup_scripts/[文件名]}；</li>
 *       </ul>
 *   </li>
 *   <li>存在 {@code operation: "delete"} 的条目 → 优先级最高，跳过合并并从对应输出目录删除；</li>
 *   <li>合并完成后 pending/ 清空或归档到 backup/。</li>
 * </ol>
 */
public class MergeManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String DELETE_MARK = "delete";

    /** 内容类型常量 */
    public static final String TYPE_RECIPE = "recipe";
    public static final String TYPE_WORLDGEN = "worldgen";
    public static final String TYPE_STARTUP_SCRIPTS = "startup_scripts";

    /** 暂存目录（世界目录下 visualcrafting/pending） */
    private final Path pendingDir;
    /** 归档目录（世界目录下 visualcrafting/backup） */
    private final Path backupDir;
    /** KubeJS 配方输出根目录（世界目录下 kubejs/data） */
    private final Path outputBase;
    /** worldgen 数据包输出根目录（世界目录下 datapacks/visualcrafting/data/visualcrafting） */
    private final Path worldgenBase;
    /** worldgen 数据包根目录（世界目录下 datapacks/visualcrafting），放置 pack.mcmeta 与禁用清单 */
    private final Path datapackRoot;
    /** KubeJS 启动脚本输出根目录（实例根目录下 kubejs/startup_scripts，与 screen 写盘基准一致） */
    private final Path startupScriptsBase;

    private final Map<String, List<PendingOperation>> operations = new HashMap<>();
    /** 本次合并读入的被禁矿物短名集合（来自数据包根目录的禁用清单） */
    private Set<String> disabledShortNames = new LinkedHashSet<>();
    private boolean disabledOresLoaded = false;
    private int mergedCount = 0;
    private int deletedCount = 0;

    public MergeManager(MinecraftServer server) {
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        Path instanceRoot = server.getServerDirectory();
        this.pendingDir = worldRoot.resolve("visualcrafting").resolve("pending");
        this.backupDir = worldRoot.resolve("visualcrafting").resolve("backup");
        this.outputBase = worldRoot.resolve("kubejs").resolve("data");
        this.worldgenBase = worldRoot.resolve("datapacks").resolve("visualcrafting")
                .resolve("data").resolve("visualcrafting");
        this.datapackRoot = this.worldgenBase.getParent().getParent();
        this.startupScriptsBase = instanceRoot.resolve("kubejs").resolve("startup_scripts");
    }

    /**
     * 扫描暂存目录、按 (type, recipeId) 分组、深度合并并输出内容文件。
     * 调用时机：服务端启动、{@code /reload} 或手动执行。
     */
    public void mergeAll() {
        loadDisabledOres();
        if (!Files.isDirectory(pendingDir)) {
            log("[MergeManager] 暂存目录不存在: " + pendingDir.toAbsolutePath() + "，跳过合并");
            return;
        }

        loadPendingOperations();
        if (operations.isEmpty()) {
            log("[MergeManager] pending 目录下没有待合并的内容文件");
            return;
        }

        for (Map.Entry<String, List<PendingOperation>> entry : operations.entrySet()) {
            String key = entry.getKey();
            List<PendingOperation> ops = entry.getValue();

            if (hasDeleteMark(ops)) {
                handleDelete(key, ops);
                continue;
            }
            mergeEntry(key, ops);
        }

        archiveOrClearPending();
        log(String.format("[MergeManager] 合并完成：共合并 %d 个条目，删除 %d 个条目",
                mergedCount, deletedCount));
    }

    /** 扫描 pending/ 下所有 .json 文件并按 type|recipeId 分组 */
    private void loadPendingOperations() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pendingDir, "*.json")) {
            for (Path file : stream) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    PendingOperation op = GSON.fromJson(root, PendingOperation.class);
                    if (op.getRecipeId() == null || op.getContent() == null) {
                        log("[MergeManager] 跳过无效暂存文件: " + file.getFileName());
                        continue;
                    }
                    String key = groupKey(op.getType(), op.getRecipeId());
                    operations.computeIfAbsent(key, k -> new ArrayList<>()).add(op);
                    log(String.format("[MergeManager] 读取暂存文件 %s（类型=%s, 玩家=%s, key=%s, 时间戳=%d）",
                            file.getFileName(), op.getType(), op.getPlayer(), op.getRecipeId(), op.getTimestamp()));
                } catch (Exception e) {
                    log("[MergeManager] 解析失败，跳过文件 " + file.getFileName() + "：" + e.getMessage());
                }
            }
        } catch (IOException e) {
            log("[MergeManager] 扫描暂存目录失败: " + e.getMessage());
        }
    }

    /** 分组 key：type|recipeId，避免不同类型同 id 冲突 */
    private static String groupKey(String type, String recipeId) {
        return type + "|" + recipeId;
    }

    /** 该条目是否存在 delete 标记（优先级最高） */
    private boolean hasDeleteMark(List<PendingOperation> ops) {
        for (PendingOperation op : ops) {
            if (op.isDeleteMark()) {
                return true;
            }
        }
        return false;
    }

    /** 删除标记：跳过合并，并从对应输出目录移除文件 */
    private void handleDelete(String key, List<PendingOperation> ops) {
        PendingOperation latest = ops.get(ops.size() - 1);
        String type = latest.getType();
        String recipeId = latest.getRecipeId();
        Path output = resolveOutputPath(type, recipeId);
        try {
            if (Files.exists(output)) {
                Files.deleteIfExists(output);
                log("[MergeManager] 条目 " + key + " 存在 delete 标记，已移除输出文件: " + output);
            } else {
                log("[MergeManager] 条目 " + key + " 存在 delete 标记，输出文件不存在，无需删除");
            }
            deletedCount++;
        } catch (IOException e) {
            log("[MergeManager] 删除输出文件失败 " + output + ": " + e.getMessage());
        }
    }

    /** 对同一 key 的多玩家文件执行深度合并并写出 */
    private void mergeEntry(String key, List<PendingOperation> ops) {
        // 时间戳升序；时间戳完全相同时按玩家名字母顺序决定优先级，保证结果确定性
        ops.sort((a, b) -> {
            int ts = Long.compare(a.getTimestamp(), b.getTimestamp());
            if (ts != 0) return ts;
            return a.getPlayer().compareTo(b.getPlayer());
        });
        PendingOperation latest = ops.get(ops.size() - 1);
        String type = latest.getType();
        String recipeId = latest.getRecipeId();

        // 以时间戳最新的 content 为基础，将其他玩家的字段并入
        JsonObject merged = latest.getContent().deepCopy();
        for (int i = 0; i < ops.size() - 1; i++) {
            PendingOperation op = ops.get(i);
            deepMerge(merged, op.getContent(), op.getPlayer(), latest.getPlayer(), key);
        }

        Path output = resolveOutputPath(type, recipeId);
        try {
            Files.createDirectories(output.getParent());
            if (TYPE_WORLDGEN.equals(type)) {
                ensurePackMcmeta(output);
                qualifyWorldgenReferences(recipeId, merged);
                enforceOreDisable(recipeId, key, merged);
                enforceRemoveFeatures(recipeId, key, merged);
            }
            if (TYPE_STARTUP_SCRIPTS.equals(type)) {
                // 启动脚本为纯文本文件，content 中 script 字段保存完整文本
                String script = merged.has("script") ? merged.get("script").getAsString() : merged.toString();
                Files.writeString(output, script, StandardCharsets.UTF_8);
            } else {
                try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
                    GSON.toJson(merged, writer);
                }
            }
            log("[MergeManager] 条目 " + key + " 合并完成，输出: " + output);
            if (TYPE_WORLDGEN.equals(type) && isDisabledPlacedFeature(recipeId)) {
                verifyDisabledOverride(key, output);
            }
            mergedCount++;
        } catch (IOException e) {
            log("[MergeManager] 写出条目失败 " + output + ": " + e.getMessage());
        }
    }

    /**
     * 深度合并：将 source 中尚未存在（或时间戳更旧）的字段并入 target。
     * 冲突时以 target（时间戳最新玩家）为准，即最后写入者获胜。
     */
    private void deepMerge(JsonObject target, JsonObject source, String sourcePlayer,
                           String targetPlayer, String key) {
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            String field = entry.getKey();
            JsonElement srcVal = entry.getValue();
            JsonElement tgtVal = target.get(field);

            if (tgtVal == null) {
                // 仅一个玩家有该字段 → 直接添加
                target.add(field, srcVal.deepCopy());
                log(String.format("[MergeManager] 条目%s：新增字段 %s，采用玩家 %s 的版本",
                        key, field, sourcePlayer));
                continue;
            }

            if (tgtVal.isJsonObject() && srcVal.isJsonObject()) {
                // 双方都是对象 → 递归深度合并
                deepMerge(tgtVal.getAsJsonObject(), srcVal.getAsJsonObject(),
                        sourcePlayer, targetPlayer, key);
            } else if (tgtVal.isJsonArray() && srcVal.isJsonArray()) {
                // 数组（pattern / ingredients / features 等）：时间戳最新的完整数组直接覆盖旧数组
                // target 为最新玩家，保留 target 数组即可，这里仅打印覆盖说明
                log(String.format("[MergeManager] 条目%s：数组字段 %s 采用玩家 %s 的完整数组（覆盖玩家 %s 的旧数组）",
                        key, field, targetPlayer, sourcePlayer));
            } else {
                // 同字段冲突（标量）→ 时间戳大者获胜，target 即为最新玩家版本
                log(String.format("[MergeManager] 条目%s：合并冲突，字段 %s，采用玩家 %s 的版本（时间戳较新）",
                        key, field, targetPlayer));
            }
        }
    }

    /**
     * worldgen 数据包写出前的命名空间规范化（兜底防线）。
     * <p>历史遗留的暂存/归档 JSON（pending、backup）与旧版本 mod 生成的模板可能存在裸名资源引用，
     * 重放写入数据包时会被 Minecraft 补成默认命名空间 minecraft:，导致注册表未绑定
     * （Unbound values in registry）世界加载失败。此处对写出的 worldgen JSON 统一补全
     * visualcrafting: 前缀，确保最终落盘的数据包资源引用一律自带命名空间。</p>
     */
    private void qualifyWorldgenReferences(String recipeId, JsonObject content) {
        if (recipeId.startsWith("worldgen/placed_feature/")) {
            JsonElement feature = content.get("feature");
            if (feature != null && feature.isJsonPrimitive()) {
                String featureId = feature.getAsString();
                if (!featureId.contains(":")) {
                    content.addProperty("feature", "visualcrafting:" + featureId);
                    log("[MergeManager] placed_feature " + recipeId + " 的 feature 字段补全命名空间: visualcrafting:" + featureId);
                }
            }
        } else if (recipeId.startsWith("neoforge/biome_modifier/")) {
            JsonElement features = content.get("features");
            if (features != null && features.isJsonArray()) {
                JsonArray array = features.getAsJsonArray();
                for (int i = 0; i < array.size(); i++) {
                    JsonElement element = array.get(i);
                    if (element != null && element.isJsonPrimitive()) {
                        String featureId = element.getAsString();
                        if (!featureId.contains(":")) {
                            array.set(i, new JsonPrimitive("visualcrafting:" + featureId));
                            log("[MergeManager] biome_modifier " + recipeId + " 的 features[" + i + "] 补全命名空间: visualcrafting:" + featureId);
                        }
                    }
                }
            }
        }
    }

    /** 按类型路由输出路径 */
    private Path resolveOutputPath(String type, String recipeId) {
        if (TYPE_WORLDGEN.equals(type)) {
            // recipeId 为相对 data/visualcrafting 的路径（不含 .json 后缀）
            return worldgenBase.resolve(recipeId + ".json");
        }
        if (TYPE_STARTUP_SCRIPTS.equals(type)) {
            // recipeId 为输出文件名（如 visualcrafting_food.js）
            return startupScriptsBase.resolve(recipeId);
        }
        // 默认 recipe：kubejs/data/[命名空间]/recipes/[路径].json
        String id = recipeId;
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        String[] parts = id.split(":", 2);
        return outputBase.resolve(parts[0]).resolve("recipes").resolve(parts[1] + ".json");
    }

    /** worldgen 数据包输出前确保 pack.mcmeta 存在且 pack_format 与当前 MC 版本一致（1.21/1.21.1 = 48） */
    private void ensurePackMcmeta(Path outputFile) {
        try {
            if (OreDisableRegistry.ensurePackMcmeta(this.datapackRoot.toFile())) {
                log("[MergeManager] worldgen 数据包 pack.mcmeta 已写入/修正（pack_format="
                        + OreDisableRegistry.DATAPACK_FORMAT + "）: " + this.datapackRoot.resolve("pack.mcmeta"));
            }
        } catch (Exception e) {
            log("[MergeManager] 补全 pack.mcmeta 失败: " + e.getMessage());
        }
    }

    /** 读入数据包根目录的矿物禁用清单（合并前统一刷新，避免使用过期缓存） */
    private void loadDisabledOres() {
        this.disabledShortNames = OreDisableRegistry.disabledShortNames(this.datapackRoot.toFile());
        this.disabledOresLoaded = true;
        if (!this.disabledShortNames.isEmpty()) {
            log("[MergeManager] 已读入被禁矿物清单 " + this.disabledShortNames + "（来源: "
                    + this.datapackRoot.resolve(OreDisableRegistry.FILE_NAME) + "）");
        }
    }

    /** 该 worldgen 条目是否为被禁矿物的 placed_feature（禁用清单以矿物短名为维度） */
    private boolean isDisabledPlacedFeature(String recipeId) {
        if (recipeId == null || !recipeId.startsWith(OreDisableRegistry.PLACED_FEATURE_DIR)) {
            return false;
        }
        if (!this.disabledOresLoaded) {
            loadDisabledOres();
        }
        String placedId = recipeId.substring(OreDisableRegistry.PLACED_FEATURE_DIR.length());
        for (String shortName : this.disabledShortNames) {
            if (placedId.equals(OreDisableRegistry.mineralPlacedId(shortName))
                    || placedId.equals(OreDisableRegistry.byproductPlacedId(shortName))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 写出侧兜底防线：被禁矿物的 placed_feature 一律强制把生成数量置 0。
     * <p>玩家之后再次「生成/更新矿物」（pending 中携带正常生成数量）、或历史遗留 JSON 被重放时，
     * 只要矿物仍在禁用清单中，落盘结果的数量仍为 0，禁用不会复发。</p>
     */
    private void enforceOreDisable(String recipeId, String key, JsonObject content) {
        if (!isDisabledPlacedFeature(recipeId)) {
            return;
        }
        boolean changed = OreDisableRegistry.forceZeroCount(content);
        log("[MergeManager] 条目 " + key + " 命中矿物禁用清单，已强制将生成数量置 0"
                + (changed ? "（本次修正了 count 字段）" : "（数量原本已为 0）"));
    }

    /**
     * 写出侧兜底防线（通用单方块禁用）：remove_features biome modifier 的 features 一律强制
     * 与禁用清单中反查记录的 placed_feature id 列表保持一致。
     * <p>玩家之后再次生成/更新、或历史遗留 JSON 被重放时，只要矿物仍在禁用清单中，
     * 落盘结果的 features 仍指向被禁的 placed_feature，外部生成不会复发。</p>
     */
    private void enforceRemoveFeatures(String recipeId, String key, JsonObject content) {
        String shortName = OreDisableRegistry.extractShortNameFromRemoveModifier(recipeId);
        if (shortName == null) {
            return;
        }
        if (!this.disabledOresLoaded) {
            loadDisabledOres();
        }
        if (!this.disabledShortNames.contains(shortName)) {
            return;
        }

        List<String> placedIds = OreDisableRegistry.loadPlacedFeatures(this.datapackRoot.toFile(), shortName);
        if (placedIds.isEmpty()) {
            return;
        }

        JsonArray features = new JsonArray();
        for (String id : placedIds) {
            features.add(id);
        }
        content.add("features", features);
        if (!content.has("type") || !content.get("type").isJsonPrimitive()
                || !"neoforge:remove_features".equals(content.get("type").getAsString())) {
            content.addProperty("type", "neoforge:remove_features");
        }
        log("[MergeManager] 条目 " + key + " 命中通用方块禁用清单，已强制 features = " + placedIds);
    }

    /**
     * 禁用覆盖落盘后的生效校验：读回文件确认所有 minecraft:count 均为 0、feature 引用自带命名空间；
     * 不通过时重写一次并再次校验，仍失败则打印错误日志便于排查。
     */
    private void verifyDisabledOverride(String key, Path output) {
        try {
            JsonObject onDisk = JsonParser.parseString(Files.readString(output, StandardCharsets.UTF_8)).getAsJsonObject();
            if (OreDisableRegistry.verifyZeroCount(onDisk) && OreDisableRegistry.hasQualifiedFeature(onDisk)) {
                log("[MergeManager] 禁用生效校验通过：条目 " + key + " 生成数量为 0 → " + output);
                return;
            }
            log("[MergeManager][ERROR] 禁用生效校验失败，尝试重写：条目 " + key + " → " + output);
            OreDisableRegistry.forceZeroCount(onDisk);
            Files.writeString(output, GSON.toJson(onDisk), StandardCharsets.UTF_8);
            JsonObject again = JsonParser.parseString(Files.readString(output, StandardCharsets.UTF_8)).getAsJsonObject();
            boolean ok = OreDisableRegistry.verifyZeroCount(again) && OreDisableRegistry.hasQualifiedFeature(again);
            log("[MergeManager] 重写后禁用校验：" + (ok ? "通过" : "仍失败（请检查禁用清单与数据包写入权限）"));
        } catch (Exception e) {
            log("[MergeManager][ERROR] 禁用生效校验异常：条目 " + key + " → " + e.getMessage());
        }
    }

    /** 合并完成后：清空 pending/ 或归档到 backup/ */
    private void archiveOrClearPending() {
        try {
            Files.createDirectories(backupDir);
            int archived = 0;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(pendingDir, "*.json")) {
                for (Path file : stream) {
                    String fileName = System.currentTimeMillis() + "_" + file.getFileName();
                    Files.move(file, backupDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                    archived++;
                }
            }
            log("[MergeManager] 已归档 " + archived + " 个暂存文件到 " + backupDir.toAbsolutePath());
        } catch (IOException e) {
            log("[MergeManager] 归档失败: " + e.getMessage());
        }
    }

    private void log(String message) {
        System.out.println(message);
    }
}
