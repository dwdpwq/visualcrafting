package com.visualcrafting.merge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
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
import java.util.List;
import java.util.Map;

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
    /** KubeJS 启动脚本输出根目录（实例根目录下 kubejs/startup_scripts，与 screen 写盘基准一致） */
    private final Path startupScriptsBase;

    private final Map<String, List<PendingOperation>> operations = new HashMap<>();
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
        this.startupScriptsBase = instanceRoot.resolve("kubejs").resolve("startup_scripts");
    }

    /**
     * 扫描暂存目录、按 (type, recipeId) 分组、深度合并并输出内容文件。
     * 调用时机：服务端启动、{@code /reload} 或手动执行。
     */
    public void mergeAll() {
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

    /** worldgen 数据包输出前确保 pack.mcmeta 存在（pack_format 57），否则数据包无法被游戏加载 */
    private void ensurePackMcmeta(Path outputFile) {
        try {
            // worldgenBase = .../datapacks/visualcrafting/data/visualcrafting
            // datapackRoot = .../datapacks/visualcrafting（向上两级）
            Path datapackRoot = worldgenBase.getParent().getParent();
            Path mcmeta = datapackRoot.resolve("pack.mcmeta");
            if (!Files.exists(mcmeta)) {
                Files.createDirectories(datapackRoot);
                Files.writeString(mcmeta,
                        "{\n  \"pack\": {\n    \"pack_format\": 57,\n    \"description\": \"VisualCrafting Ore Generation\"\n  }\n}",
                        StandardCharsets.UTF_8);
                log("[MergeManager] worldgen 数据包缺少 pack.mcmeta，已自动补全: " + mcmeta);
            }
        } catch (IOException e) {
            log("[MergeManager] 补全 pack.mcmeta 失败: " + e.getMessage());
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
