package com.visualcrafting.learning;

import com.google.gson.*;
import java.util.*;

/**
 * 通用学习配方草稿模型。
 *
 * <p>不把未知 Recipe Type 强行转换成原版 Recipe 类，而是依据学习库保存的
 * Schema/Capability 对 JSON 字段进行编辑，并原样保留未知字段。</p>
 */
public final class LearnedRecipeDraft {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final JsonObject json;

    public LearnedRecipeDraft(JsonObject source) {
        this.json = source == null ? new JsonObject() : source.deepCopy();
    }

    public JsonObject json() {
        return json.deepCopy();
    }

    public String type() {
        JsonElement e = json.get("type");
        return e != null && e.isJsonPrimitive() ? e.getAsString() : "";
    }

    public LearnedRecipeDraft set(String path, JsonElement value) {
        setPath(json, path, value == null ? JsonNull.INSTANCE : value.deepCopy());
        return this;
    }

    public JsonElement get(String path) {
        return getPath(json, path);
    }

    public boolean has(String path) {
        return get(path) != null;
    }

    /**
     * 生成安全的 KubeJS event.custom 脚本。
     * 这是通用 Recipe Type 输出，不注册 Minecraft 内置 RecipeSerializer。
     */
    public String toKubeJs(String recipeId) {
        JsonObject body = json.deepCopy();
        String type = body.has("type") && body.get("type").isJsonPrimitive()
                ? body.get("type").getAsString() : "";
        if (type.isBlank()) throw new IllegalStateException("学习配方缺少 type");
        if (recipeId == null || recipeId.isBlank()) throw new IllegalArgumentException("recipeId 不能为空");

        return "ServerEvents.recipes(event => {\n"
                + "  event.custom(" + GSON.toJson(body) + ")\n"
                + "    .id(" + GSON.toJson(recipeId) + ");\n"
                + "});\n";
    }

    private static void setPath(JsonObject root, String path, JsonElement value) {
        String[] parts = path.split("\\.");
        JsonObject current = root;
        for (int i=0;i<parts.length-1;i++) {
            JsonElement next=current.get(parts[i]);
            if(next==null || !next.isJsonObject()){
                JsonObject created=new JsonObject();
                current.add(parts[i],created);
                current=created;
            }else current=next.getAsJsonObject();
        }
        current.add(parts[parts.length-1],value);
    }

    private static JsonElement getPath(JsonObject root, String path) {
        String[] parts=path.split("\\.");
        JsonElement current=root;
        for(String part:parts){
            if(current==null || !current.isJsonObject()) return null;
            current=current.getAsJsonObject().get(part);
        }
        return current;
    }
}
