package com.visualcrafting.recipe.universal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Lossless, UI-friendly view of an arbitrary recipe JSON value.
 *
 * The node tree deliberately does not assign semantics to unknown fields.
 * That allows recipes from new or third-party mods to be displayed without
 * adding a hard-coded adapter first.
 */
public final class UniversalRecipeNode {
    public enum Kind {
        OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL
    }

    private final String key;
    private final Kind kind;
    private String scalarValue;
    private final List<UniversalRecipeNode> children;
    private boolean visible = true;
    private boolean editable = true;

    private UniversalRecipeNode(String key, Kind kind, String scalarValue,
                                List<UniversalRecipeNode> children) {
        this.key = key;
        this.kind = kind;
        this.scalarValue = scalarValue;
        this.children = children;
    }

    public static UniversalRecipeNode fromJson(String key, JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return new UniversalRecipeNode(key, Kind.NULL, null, List.of());
        }
        if (element.isJsonObject()) {
            List<UniversalRecipeNode> children = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                children.add(fromJson(entry.getKey(), entry.getValue()));
            }
            return new UniversalRecipeNode(key, Kind.OBJECT, null, children);
        }
        if (element.isJsonArray()) {
            List<UniversalRecipeNode> children = new ArrayList<>();
            int index = 0;
            for (JsonElement child : element.getAsJsonArray()) {
                children.add(fromJson("[" + index++ + "]", child));
            }
            return new UniversalRecipeNode(key, Kind.ARRAY, null, children);
        }

        JsonPrimitive primitive = element.getAsJsonPrimitive();
        Kind kind = primitive.isBoolean() ? Kind.BOOLEAN
                : primitive.isNumber() ? Kind.NUMBER
                : Kind.STRING;
        return new UniversalRecipeNode(key, kind, primitive.getAsString(), List.of());
    }

    public String key() { return key; }
    public Kind kind() { return kind; }
    public String scalarValue() { return scalarValue; }
    public List<UniversalRecipeNode> children() {
        return Collections.unmodifiableList(children);
    }
    public boolean visible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public boolean editable() { return editable; }
    public void setEditable(boolean editable) { this.editable = editable; }

    public boolean isContainer() {
        return kind == Kind.OBJECT || kind == Kind.ARRAY;
    }

    /** Update a scalar value without changing its inferred JSON kind. */
    public void setScalarValue(String value) {
        if (kind == Kind.OBJECT || kind == Kind.ARRAY) {
            throw new IllegalStateException("Container nodes do not have scalar values");
        }
        this.scalarValue = value;
    }

    public void addChild(UniversalRecipeNode child) {
        if (!isContainer()) {
            throw new IllegalStateException("Only object/array nodes can have children");
        }
        children.add(child);
    }

    public UniversalRecipeNode removeChild(int index) {
        if (!isContainer()) {
            throw new IllegalStateException("Only object/array nodes can have children");
        }
        return children.remove(index);
    }

    /**
     * Serializes the edited tree. Hidden nodes are retained by default so
     * "hide" is a presentation setting, never silent data loss.
     */
    public JsonElement toJson() {
        return toJson(true);
    }

    public JsonElement toJson(boolean includeHidden) {
        return switch (kind) {
            case NULL -> com.google.gson.JsonNull.INSTANCE;
            case STRING -> new JsonPrimitive(scalarValue == null ? "" : scalarValue);
            case NUMBER -> {
                try {
                    yield new JsonPrimitive(new java.math.BigDecimal(scalarValue));
                } catch (NumberFormatException e) {
                    yield new JsonPrimitive(scalarValue);
                }
            }
            case BOOLEAN -> new JsonPrimitive(Boolean.parseBoolean(scalarValue));
            case OBJECT -> {
                JsonObject object = new JsonObject();
                for (UniversalRecipeNode child : children) {
                    if (includeHidden || child.visible()) {
                        object.add(child.key(), child.toJson(includeHidden));
                    }
                }
                yield object;
            }
            case ARRAY -> {
                JsonArray array = new JsonArray();
                for (UniversalRecipeNode child : children) {
                    if (includeHidden || child.visible()) {
                        array.add(child.toJson(includeHidden));
                    }
                }
                yield array;
            }
        };
    }
}
