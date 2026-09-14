package com.visualcrafting.merge;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * 暂存配方操作实体类。
 * <p>
 * 对应 {@code ./visualcrafting/pending/} 目录下的一个 JSON 文件，
 * 文件名为 {@code {玩家UUID或名字}_{配方命名空间ID}.json}，
 * 例如 {@code Steve_minecraft_stone.json}。
 * <p>
 * 根 JSON 结构：
 * <pre>
 * {
 *   "type": "recipe | worldgen | startup_scripts",
 *   "player": "Steve",
 *   "recipeId": "minecraft:stone",
 *   "timestamp": 1780000000000,
 *   "content": { ... 完整配方 JSON ... }
 * }
 * </pre>
 */
public class PendingOperation {

    /** 内容类型：recipe（合成配方，默认）/ worldgen（世界生成数据包）/ startup_scripts（KubeJS 启动脚本） */
    private String type;

    /** 编辑该配方的玩家名 / UUID */
    private String player;

    /** 配方 ID，格式 {@code 命名空间:路径}，例如 {@code minecraft:stone}；worldgen 为相对 data/visualcrafting 的路径 */
    private String recipeId;

    /** 编辑时的时间戳（毫秒长整型），用于冲突时最后写入者获胜 */
    private long timestamp;

    /** 该玩家编辑的完整配方 JSON 对象 */
    private JsonObject content;

    public PendingOperation() {
    }

    public PendingOperation(String player, String recipeId, long timestamp, JsonObject content) {
        this.player = player;
        this.recipeId = recipeId;
        this.timestamp = timestamp;
        this.content = content;
    }

    /** 内容类型，缺省为 recipe（兼容旧暂存文件） */
    public String getType() {
        return type == null || type.isEmpty() ? "recipe" : type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPlayer() {
        return player;
    }

    public void setPlayer(String player) {
        this.player = player;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public JsonObject getContent() {
        return content;
    }

    public void setContent(JsonObject content) {
        this.content = content;
    }

    /** 是否带删除标记（operation = "delete"），删除标记优先级最高 */
    public boolean isDeleteMark() {
        if (content == null) {
            return false;
        }
        JsonElement op = content.get("operation");
        return op != null && op.isJsonPrimitive()
                && "delete".equals(op.getAsString());
    }
}
