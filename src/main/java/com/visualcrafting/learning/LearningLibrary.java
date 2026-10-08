package com.visualcrafting.learning;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * VisualCrafting 的离线学习能力库。
 *
 * <p>这里保存的是编辑器能力、Schema 和代表样本，不是 Minecraft Recipe。
 * 资源位于 data/visualcrafting/learning/，因此不会被 RecipeManager 当作正式配方加载。</p>
 */
public final class LearningLibrary {
    private static final Gson GSON = new Gson();
    private static final String ROOT = "/data/visualcrafting/learning/";
    private static final String CAPABILITIES = "editor-capabilities.json";
    private static final String SCHEMAS = "schema-library.json";
    private static final String SAMPLES = "recipe-samples.json";
    private static final String MACHINES = "machine-index.json";

    private static volatile Snapshot snapshot = Snapshot.empty();

    private LearningLibrary() {}

    public static Snapshot get() {
        return snapshot;
    }

    public static synchronized Snapshot reload() {
        snapshot = new Snapshot(
                readArray(CAPABILITIES),
                readArray(SCHEMAS),
                readArray(SAMPLES),
                readArray(MACHINES)
        );
        return snapshot;
    }

    private static JsonArray readArray(String name) {
        try (InputStream in = LearningLibrary.class.getResourceAsStream(ROOT + name)) {
            if (in == null) return new JsonArray();
            JsonElement root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            if (root.isJsonArray()) return root.getAsJsonArray().deepCopy();
            if (root.isJsonObject()) {
                JsonObject o = root.getAsJsonObject();
                for (String key : new String[]{"capabilities", "schemas", "samples", "machines"}) {
                    JsonElement value = o.get(key);
                    if (value != null && value.isJsonArray()) return value.getAsJsonArray().deepCopy();
                }
            }
        } catch (Exception ignored) {
            // 学习库损坏不能阻止 VisualCrafting 正常启动；只回退到空学习库。
        }
        return new JsonArray();
    }

    public record Snapshot(JsonArray capabilities, JsonArray schemas,
                           JsonArray samples, JsonArray machines) {
        static Snapshot empty() {
            return new Snapshot(new JsonArray(), new JsonArray(), new JsonArray(), new JsonArray());
        }

        public boolean isEmpty() {
            return capabilities.size() == 0 && schemas.size() == 0
                    && samples.size() == 0 && machines.size() == 0;
        }

        public int capabilityCount() {
            return capabilities.size();
        }

        public int schemaCount() {
            return schemas.size();
        }

        public int sampleCount() {
            return samples.size();
        }

        public int machineCount() {
            return machines.size();
        }

        /**
         * 按 recipeType / type 查询机器能力，供后续动态编辑器使用。
         */
        /**
         * 返回与 Recipe Type 匹配的全部编辑能力。支持 VAS 生成的 recipeTypes 数组，
         * 同时兼容旧版只保存 type / recipeType 的学习数据。
         */
        public List<JsonObject> capabilitiesFor(String recipeType) {
            if (recipeType == null || recipeType.isBlank()) return Collections.emptyList();
            List<JsonObject> result = new ArrayList<>();
            for (JsonElement e : capabilities) {
                if (!e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                if (matchesType(o, recipeType)) result.add(o.deepCopy());
            }
            return Collections.unmodifiableList(result);
        }

        public JsonObject bestCapabilityFor(String recipeType) {
            List<JsonObject> matches = capabilitiesFor(recipeType);
            if (matches.isEmpty()) return null;
            matches.sort((a,b) -> Integer.compare(
                    b.has("sampleCount") ? b.get("sampleCount").getAsInt() : 0,
                    a.has("sampleCount") ? a.get("sampleCount").getAsInt() : 0));
            return matches.get(0);
        }

        public List<JsonObject> schemasFor(String recipeType) {
            List<JsonObject> result = new ArrayList<>();
            for (JsonElement e : schemas) {
                if (!e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                if (matchesType(o, recipeType)) result.add(o.deepCopy());
            }
            return Collections.unmodifiableList(result);
        }

        public JsonObject machineFor(String recipeType) {
            for (JsonElement e : machines) {
                if (!e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                JsonElement t = o.get("recipeType");
                if (t != null && recipeType.equals(t.getAsString())) return o.deepCopy();
            }
            return null;
        }

        private static boolean matchesType(JsonObject o, String recipeType) {
            JsonElement types = o.get("recipeTypes");
            if (types != null && types.isJsonArray()) {
                for (JsonElement type : types.getAsJsonArray()) {
                    if (type.isJsonPrimitive() && recipeType.equals(type.getAsString())) return true;
                }
            }
            for (String key : new String[]{"recipeType", "type", "machineIdentity"}) {
                JsonElement v = o.get(key);
                if (v != null && !v.isJsonNull() && v.isJsonPrimitive() && recipeType.equals(v.getAsString())) return true;
            }
            return false;
        }
    }
}
