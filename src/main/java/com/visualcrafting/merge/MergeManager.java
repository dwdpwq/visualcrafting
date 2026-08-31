package com.visualcrafting.merge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家配方编辑暂存合并管理器。
 * <p>
 * 规则：
 * <ol>
 *   <li>玩家编辑的配方写入 {@code ./visualcrafting/pending/}，
 *       文件名为 {@code {玩家}_{配方ID}.json}；</li>
 *   <li>服务端启动或执行 {@code /reload} 时调用 {@link #mergeAll()}：</li>
 *   <li>按 recipeId 分组，对同一配方的多个玩家文件执行深度合并：
 *       <ul>
 *         <li>仅一个玩家有该字段 → 直接采用；</li>
 *         <li>同字段冲突 → 比较 timestamp，最后写入者获胜；</li>
 *         <li>数组（pattern / ingredients 等）→ 时间戳最新的完整数组覆盖；</li>
 *       </ul>
 *   </li>
 *   <li>合并结果输出到 {@code kubejs/data/[命名空间]/recipes/}；</li>
 *   <li>存在 {@code operation: "delete"} 的配方 → 优先级最高，跳过合并并从输出目录删除；</li>
 *   <li>合并完成后 pending/ 清空或归档到 backup/。</li>
 * </ol>
 */
public class MergeManager {

    /** 暂存目录 */
    private static final Path PENDING_DIR = Paths.get("visualcrafting", "pending");
    /** 归档目录 */
    private static final Path BACKUP_DIR = Paths.get("visualcrafting", "backup");
    /** KubeJS 配方输出根目录 */
    private static final Path OUTPUT_BASE = Paths.get("kubejs", "data");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String DELETE_MARK = "delete";

    private final Map<String, List<PendingOperation>> operations = new HashMap<>();
    private int mergedCount = 0;
    private int deletedCount = 0;

    public static void main(String[] args) {
        new MergeManager().mergeAll();
    }

    /**
     * 扫描暂存目录、分组、深度合并并输出配方文件。
     * 调用时机：服务端启动、{@code /reload} 或手动执行。
     */
    public void mergeAll() {
        if (!Files.isDirectory(PENDING_DIR)) {
            log("[MergeManager] 暂存目录不存在: " + PENDING_DIR.toAbsolutePath() + "，跳过合并");
            return;
        }

        loadPendingOperations();
        if (operations.isEmpty()) {
            log("[MergeManager] pending 目录下没有待合并的配方文件");
            return;
        }

        for (Map.Entry<String, List<PendingOperation>> entry : operations.entrySet()) {
            String recipeId = entry.getKey();
            List<PendingOperation> ops = entry.getValue();

            if (hasDeleteMark(ops)) {
                handleDelete(recipeId);
                continue;
            }
            mergeRecipe(recipeId, ops);
        }

        archiveOrClearPending();
        log(String.format("[MergeManager] 合并完成：共合并 %d 个配方，删除 %d 个配方",
                mergedCount, deletedCount));
    }

    /** 扫描 pending/ 下所有 .json 文件并按 recipeId 分组 */
    private void loadPendingOperations() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(PENDING_DIR, "*.json")) {
            for (Path file : stream) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    PendingOperation op = GSON.fromJson(root, PendingOperation.class);
                    if (op.getRecipeId() == null || op.getContent() == null) {
                        log("[MergeManager] 跳过无效暂存文件: " + file.getFileName());
                        continue;
                    }
                    operations.computeIfAbsent(op.getRecipeId(), k -> new ArrayList<>()).add(op);
                    log(String.format("[MergeManager] 读取暂存文件 %s（玩家=%s, 配方=%s, 时间戳=%d）",
                            file.getFileName(), op.getPlayer(), op.getRecipeId(), op.getTimestamp()));
                } catch (Exception e) {
                    log("[MergeManager] 解析失败，跳过文件 " + file.getFileName() + "：" + e.getMessage());
                }
            }
        } catch (IOException e) {
            log("[MergeManager] 扫描暂存目录失败: " + e.getMessage());
        }
    }

    /** 该配方是否存在 delete 标记（优先级最高） */
    private boolean hasDeleteMark(List<PendingOperation> ops) {
        for (PendingOperation op : ops) {
            if (op.isDeleteMark()) {
                return true;
            }
        }
        return false;
    }

    /** 删除标记：跳过合并，并从输出目录移除对应配方文件 */
    private void handleDelete(String recipeId) {
        Path output = resolveOutputPath(recipeId);
        try {
            if (Files.exists(output)) {
                Files.deleteIfExists(output);
                log("[MergeManager] 配方 " + recipeId + " 存在 delete 标记，已移除输出文件: " + output);
            } else {
                log("[MergeManager] 配方 " + recipeId + " 存在 delete 标记，输出文件不存在，无需删除");
            }
            deletedCount++;
        } catch (IOException e) {
            log("[MergeManager] 删除输出文件失败 " + output + ": " + e.getMessage());
        }
    }

    /** 对同一 recipeId 的多玩家文件执行深度合并并写出 */
    private void mergeRecipe(String recipeId, List<PendingOperation> ops) {
        // 时间戳升序；时间戳完全相同时按玩家名字母顺序决定优先级，保证结果确定性
        ops.sort((a, b) -> {
            int ts = Long.compare(a.getTimestamp(), b.getTimestamp());
            if (ts != 0) return ts;
            return a.getPlayer().compareTo(b.getPlayer());
        });
        PendingOperation latest = ops.get(ops.size() - 1);

        // 以时间戳最新的 content 为基础，将其他玩家的字段并入
        JsonObject merged = latest.getContent().deepCopy();
        for (int i = 0; i < ops.size() - 1; i++) {
            PendingOperation op = ops.get(i);
            deepMerge(merged, op.getContent(), op.getPlayer(), latest.getPlayer(), recipeId);
        }

        Path output = resolveOutputPath(recipeId);
        try {
            Files.createDirectories(output.getParent());
            try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
                GSON.toJson(merged, writer);
            }
            log("[MergeManager] 配方 " + recipeId + " 合并完成，输出: " + output);
            mergedCount++;
        } catch (IOException e) {
            log("[MergeManager] 写出配方失败 " + output + ": " + e.getMessage());
        }
    }

    /**
     * 深度合并：将 source 中尚未存在（或时间戳更旧）的字段并入 target。
     * 冲突时以 target（时间戳最新玩家）为准，即最后写入者获胜。
     */
    private void deepMerge(JsonObject target, JsonObject source, String sourcePlayer,
                           String targetPlayer, String recipeId) {
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            String key = entry.getKey();
            JsonElement srcVal = entry.getValue();
            JsonElement tgtVal = target.get(key);

            if (tgtVal == null) {
                // 仅一个玩家有该字段 → 直接添加
                target.add(key, srcVal.deepCopy());
                log(String.format("[MergeManager] 配方%s：新增字段 %s，采用玩家 %s 的版本",
                        recipeId, key, sourcePlayer));
                continue;
            }

            if (tgtVal.isJsonObject() && srcVal.isJsonObject()) {
                // 双方都是对象 → 递归深度合并
                deepMerge(tgtVal.getAsJsonObject(), srcVal.getAsJsonObject(),
                        sourcePlayer, targetPlayer, recipeId);
            } else if (tgtVal.isJsonArray() && srcVal.isJsonArray()) {
                // 数组（pattern / ingredients 等）：时间戳最新的完整数组直接覆盖旧数组
                // target 为最新玩家，保留 target 数组即可，这里仅打印覆盖说明
                log(String.format("[MergeManager] 配方%s：数组字段 %s 采用玩家 %s 的完整数组（覆盖玩家 %s 的旧数组）",
                        recipeId, key, targetPlayer, sourcePlayer));
            } else {
                // 同字段冲突（标量）→ 时间戳大者获胜，target 即为最新玩家版本
                log(String.format("[MergeManager] 配方%s：合并冲突，字段 %s，采用玩家 %s 的版本（时间戳较新）",
                        recipeId, key, targetPlayer));
            }
        }
    }

    /** 输出目录: kubejs/data/[命名空间]/recipes/[路径].json */
    private Path resolveOutputPath(String recipeId) {
        String id = recipeId;
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        String[] parts = id.split(":", 2);
        return OUTPUT_BASE.resolve(parts[0]).resolve("recipes").resolve(parts[1] + ".json");
    }

    /** 合并完成后：清空 pending/ 或归档到 backup/ */
    private void archiveOrClearPending() {
        try {
            Files.createDirectories(BACKUP_DIR);
            int archived = 0;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(PENDING_DIR, "*.json")) {
                for (Path file : stream) {
                    String fileName = System.currentTimeMillis() + "_" + file.getFileName();
                    Files.move(file, BACKUP_DIR.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                    archived++;
                }
            }
            log("[MergeManager] 已归档 " + archived + " 个暂存文件到 " + BACKUP_DIR.toAbsolutePath());
        } catch (IOException e) {
            log("[MergeManager] 归档失败: " + e.getMessage());
        }
    }

    private void log(String message) {
        System.out.println(message);
    }
}
