package com.visualcrafting.recipe.universal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * A recipe whose complete source JSON is retained alongside its recursively
 * analysed tree. The raw JSON is authoritative; the tree is a presentation
 * and editing model.
 */
public final class UniversalRecipe {
    private final ResourceLocation id;
    private final ResourceLocation sourceResource;
    private final JsonObject rawJson;
    private final UniversalRecipeNode root;

    public UniversalRecipe(ResourceLocation id, ResourceLocation sourceResource, JsonObject rawJson) {
        this.id = Objects.requireNonNull(id);
        this.sourceResource = Objects.requireNonNull(sourceResource);
        this.rawJson = rawJson.deepCopy();
        this.root = UniversalRecipeNode.fromJson("$", this.rawJson);
    }

    public ResourceLocation id() { return id; }
    public ResourceLocation sourceResource() { return sourceResource; }
    public JsonObject rawJson() { return rawJson.deepCopy(); }
    public UniversalRecipeNode root() { return root; }

    /** Current edited representation. Hidden nodes remain present. */
    public JsonObject toJson() {
        return root.toJson(true).getAsJsonObject();
    }

    /** Representation suitable for a UI/export that intentionally hides fields. */
    public JsonObject toVisibleJson() {
        return root.toJson(false).getAsJsonObject();
    }

    /** Commit the edited tree back into this recipe's retained raw document. */
    public void applyTree() {
        JsonObject edited = toJson();
        rawJson.entrySet().clear();
        for (var entry : edited.entrySet()) {
            rawJson.add(entry.getKey(), entry.getValue());
        }
    }

    public String typeId() {
        JsonElement type = rawJson.get("type");
        return type != null && type.isJsonPrimitive() ? type.getAsString() : "";
    }
}
