package com.visualcrafting.learning;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 学习配方编辑模型。
 *
 * <p>把 VAS 学到的字段路径/语义角色转换成可供 GUI 使用的字段绑定。
 * 它不注册 Minecraft Recipe，也不依赖固定槽位坐标；GUI 只需要根据角色
 * 把槽位值写回对应 JSON path。</p>
 */
public final class LearnedRecipeEditorModel {
    public enum Role {
        INPUT, CATALYST, OUTPUT, FLUID, ENERGY, DURATION, CHANCE, METADATA
    }

    public record FieldBinding(
            String path,
            Role role,
            boolean editable,
            boolean numeric,
            boolean array,
            String observedType
    ) {}

    private final String recipeType;
    private final List<FieldBinding> fields;

    private LearnedRecipeEditorModel(String recipeType, List<FieldBinding> fields) {
        this.recipeType = recipeType == null ? "" : recipeType;
        this.fields = Collections.unmodifiableList(fields);
    }

    public static LearnedRecipeEditorModel load(String recipeType) {
        List<FieldBinding> result = new ArrayList<>();
        for (JsonObject field : LearningLibrary.get().fieldsFor(recipeType)) {
            String path = string(field, "path");
            if (path.isBlank()) continue;
            String roleText = string(field, "role").toUpperCase(Locale.ROOT);
            Role role;
            try {
                role = Role.valueOf(roleText);
            } catch (IllegalArgumentException e) {
                role = Role.METADATA;
            }
            boolean array = path.endsWith("[]") || "array".equals(string(field, "observedTypes"));
            String observedType = firstObservedType(field);
            result.add(new FieldBinding(
                    path,
                    role,
                    !field.has("editable") || field.get("editable").getAsBoolean(),
                    field.has("numeric") && field.get("numeric").getAsBoolean(),
                    array,
                    observedType
            ));
        }
        return new LearnedRecipeEditorModel(recipeType, result);
    }

    public String recipeType() {
        return recipeType;
    }

    public List<FieldBinding> fields() {
        return fields;
    }

    public List<FieldBinding> fields(Role role) {
        List<FieldBinding> result = new ArrayList<>();
        for (FieldBinding field : fields) {
            if (field.role() == role) result.add(field);
        }
        return Collections.unmodifiableList(result);
    }

    public String roleFor(String path) {
        for (FieldBinding field : fields) {
            if (field.path().equals(path)) return field.role().name();
        }
        return "";
    }

    /**
     * 根据字段绑定构建一份编辑草稿。未知字段会完整保留。
     */
    public JsonObject applyValues(JsonObject source, Map<String, JsonElement> values) {
        JsonObject draft = source == null ? new JsonObject() : source.deepCopy();
        if (values == null) return draft;
        for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
            if (!isKnownPath(entry.getKey())) continue;
            setPath(draft, entry.getKey(), entry.getValue() == null ? JsonNull.INSTANCE : entry.getValue());
        }
        return draft;
    }

    /**
     * 把一组输入物品绑定到第一个学习到的 INPUT 数组字段。
     * 这里严格依据学习到的字段类型生成对象/字符串，不再通过槽位位置猜 catalyst。
     */
    public JsonObject applyInputItems(JsonObject source, List<String> itemIds) {
        FieldBinding target = firstArrayField(Role.INPUT);
        if (target == null) target = firstField(Role.INPUT);
        if (target == null) return source == null ? new JsonObject() : source.deepCopy();

        JsonElement value;
        if (target.array()) {
            JsonArray array = new JsonArray();
            if (itemIds != null) {
                for (String id : itemIds) {
                    if (id == null || id.isBlank()) continue;
                    array.add(itemValue(id, target.observedType()));
                }
            }
            value = array;
        } else {
            String id = itemIds == null || itemIds.isEmpty() ? "" : itemIds.get(0);
            value = itemValue(id, target.observedType());
        }
        return applyValues(source, Map.of(target.path(), value));
    }

    public JsonObject applyCatalyst(JsonObject source, String itemId) {
        FieldBinding target = firstField(Role.CATALYST);
        if (target == null || itemId == null || itemId.isBlank()) return source == null ? new JsonObject() : source.deepCopy();
        return applyValues(source, Map.of(target.path(), itemValue(itemId, target.observedType())));
    }

    public JsonObject applyOutput(JsonObject source, String itemId) {
        FieldBinding target = firstField(Role.OUTPUT);
        if (target == null || itemId == null || itemId.isBlank()) return source == null ? new JsonObject() : source.deepCopy();
        return applyValues(source, Map.of(target.path(), itemValue(itemId, target.observedType())));
    }

    public Map<String, Role> slotRoles() {
        Map<String, Role> result = new LinkedHashMap<>();
        for (FieldBinding field : fields) {
            if (field.role() == Role.INPUT || field.role() == Role.CATALYST || field.role() == Role.OUTPUT) {
                result.put(field.path(), field.role());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private boolean isKnownPath(String path) {
        for (FieldBinding field : fields) if (field.path().equals(path)) return true;
        return false;
    }

    private FieldBinding firstField(Role role) {
        for (FieldBinding field : fields) if (field.role() == role) return field;
        return null;
    }

    private FieldBinding firstArrayField(Role role) {
        for (FieldBinding field : fields) {
            if (field.role() == role && field.array()) return field;
        }
        return null;
    }

    private static JsonElement itemValue(String itemId, String observedType) {
        if ("string".equals(observedType)) return JsonParser.parseString(JsonParser.parseString(""" + escape(itemId) + """).getAsString());
        JsonObject object = new JsonObject();
        object.addProperty("item", itemId);
        return object;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace(""", "\\"");
    }

    private static String firstObservedType(JsonObject field) {
        JsonElement types = field.get("observedTypes");
        if (types != null && types.isJsonArray() && !types.getAsJsonArray().isEmpty()) {
            return types.getAsJsonArray().get(0).getAsString();
        }
        return "";
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
    }

    private static void setPath(JsonObject root, String path, JsonElement value) {
        String clean = path.endsWith("[]") ? path.substring(0, path.length() - 2) : path;
        String[] parts = clean.split("\\.");
        JsonObject current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            JsonElement next = current.get(parts[i]);
            if (next == null || !next.isJsonObject()) {
                JsonObject created = new JsonObject();
                current.add(parts[i], created);
                current = created;
            } else {
                current = next.getAsJsonObject();
            }
        }
        if (parts.length > 0) current.add(parts[parts.length - 1], value);
    }
}
